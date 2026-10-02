package com.anytext.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anytext.app.logic.Fs
import com.anytext.app.logic.FsEntry
import com.anytext.app.logic.RootShell
import com.anytext.app.util.Friendly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BrowseViewModel(app: Application) : AndroidViewModel(app) {

    data class Ui(
        val loading: Boolean = true,
        val error: String? = null,
        val entries: List<FsEntry> = emptyList()
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    /** Буфер копирования: ("copy"|"cut") to исходный путь */
    private val _clip = MutableStateFlow<Pair<String, String>?>(null)
    val clip: StateFlow<Pair<String, String>?> = _clip

    private var currentPath = ""
    private var currentRoot = false

    fun open(path: String, root: Boolean) {
        currentPath = path
        currentRoot = root
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = if (root) {
                    if (RootShell.available == false) {
                        throw IllegalStateException("Root (su) недоступен на этом устройстве")
                    }
                    if (RootShell.available == null) RootShell.detect()
                    RootShell.list(path).map {
                        FsEntry(it.name, it.isDir, 0L, 0L, it.path, true)
                    }
                } else {
                    Fs.listDir(path)
                }
                _ui.value = _ui.value.copy(loading = false, entries = entries)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = Friendly.of(e))
            }
        }
    }

    /** mime по расширению; для неизвестных — octet-stream, чтобы системный диалог не сбоил */
    private fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }

    /** Файл, который приложение умеет читать (root-файлы копируются в кэш) */
    private suspend fun readableTarget(entry: FsEntry, context: Context): File? {
        val direct = File(entry.path)
        if (!entry.isRoot || direct.canRead()) return direct
        val tmp = File(context.cacheDir, "ro_${entry.name}")
        return if (RootShell.catToFile(entry.path, tmp)) tmp else null
    }

    /**
     * Системный диалог «Открыть через…»: архивы уйдут в WinRAR/7z и т.п.
     * Если пользователь выберет нашу читалку — MainActivity запомнит расширение.
     */
    fun openSystemChooser(entry: FsEntry, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val target = readableTarget(entry, context)
                    ?: run {
                        withContext(Dispatchers.Main) { toast("Не удалось прочитать файл через root") }
                        return@launch
                    }
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", target)
                val view = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeFor(entry.name))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    try {
                        context.startActivity(view)
                    } catch (e: Exception) {
                        toast("Нет приложения для этого файла")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Ошибка: ${e.message}") }
            }
        }
    }

    /** Готовит спеку для читалки: root-файлы, недоступные приложению, копируются в кэш */
    fun specFor(entry: FsEntry, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val spec: String = if (!entry.isRoot) {
                    "path|" + entry.path
                } else if (File(entry.path).canRead()) {
                    "path|" + entry.path
                } else {
                    val tmp = File(getApplication<Application>().cacheDir, "ro_${entry.name}")
                    if (!RootShell.catToFile(entry.path, tmp)) {
                        throw IllegalStateException("Не удалось прочитать файл через root")
                    }
                    "tmp|${entry.name}|${tmp.absolutePath}"
                }
                withContext(Dispatchers.Main) { onReady(spec) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Ошибка: ${e.message}") }
            }
        }
    }

    fun share(entry: FsEntry, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var target = File(entry.path)
                if (entry.isRoot && !target.canRead()) {
                    target = File(context.cacheDir, "sh_${entry.name}")
                    if (!RootShell.catToFile(entry.path, target)) {
                        withContext(Dispatchers.Main) { toast("Не удалось прочитать файл через root") }
                        return@launch
                    }
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    target
                )
                withContext(Dispatchers.Main) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(send, "Поделиться файлом"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Ошибка: ${e.message}") }
            }
        }
    }

    fun copy(e: FsEntry) {
        _clip.value = "copy" to e.path
    }

    fun cut(e: FsEntry) {
        _clip.value = "cut" to e.path
    }

    fun cancelClip() {
        _clip.value = null
    }

    /** Вставка из буфера в текущую папку (java-операции, для root — через su) */
    fun paste(context: Context) {
        val (op, src) = _clip.value ?: return
        val name = src.trimEnd('/').substringAfterLast('/')
        val dst = RootShell.joinPath(currentPath, name)
        viewModelScope.launch(Dispatchers.IO) {
            val useSu = currentRoot || !File(currentPath).canWrite()
            var ok = false
            try {
                ok = if (useSu) {
                    if (op == "cut") RootShell.sh("mv ${RootShell.quote(src)} ${RootShell.quote(dst)}")
                    else RootShell.sh("cp -R ${RootShell.quote(src)} ${RootShell.quote(dst)}")
                } else {
                    val s = File(src)
                    val d = File(dst)
                    if (op == "cut") {
                        s.renameTo(d) || (s.copyRecursively(d) && s.deleteRecursively())
                    } else {
                        if (s.isDirectory) s.copyRecursively(d)
                        else runCatching { s.copyTo(d); true }.getOrDefault(false)
                    }
                }
            } catch (e: Exception) {
                ok = false
            }
            if (op == "cut" || ok) _clip.value = null
            withContext(Dispatchers.Main) {
                toast(
                    when {
                        ok -> if (op == "cut") "Перемещено" else "Скопировано"
                        else -> "Не удалось ${if (op == "cut") "переместить" else "скопировать"} (нет доступа?)"
                    }
                )
            }
            open(currentPath, currentRoot)
        }
    }

    /** Создание папки в родительском каталоге */
    fun newFolder(parent: String, name: String, root: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = RootShell.joinPath(parent, name)
            val ok = if (root) RootShell.sh("mkdir -p ${RootShell.quote(p)}") else File(p).mkdirs()
            if (!ok) withContext(Dispatchers.Main) { toast("Не удалось создать папку") }
            open(parent, root)
        }
    }

    fun delete(entry: FsEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            if (entry.isRoot) RootShell.rm(entry.path) else File(entry.path).delete()
            open(currentPath, currentRoot)
        }
    }

    fun rename(entry: FsEntry, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(entry.path)
            f.renameTo(File(f.parentFile, newName))
            open(currentPath, currentRoot)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}

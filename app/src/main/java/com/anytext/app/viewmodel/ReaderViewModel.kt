package com.anytext.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anytext.app.data.RecentEntry
import com.anytext.app.data.RecentStore
import com.anytext.app.logic.Detected
import com.anytext.app.logic.Encodings
import com.anytext.app.logic.FileLoader
import com.anytext.app.util.Friendly
import com.anytext.app.logic.LoadedFile
import com.anytext.app.logic.Stats
import com.anytext.app.logic.Syntax
import com.anytext.app.logic.TextStats
import com.anytext.app.logic.decodeText
import com.anytext.app.logic.splitLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.Charset

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    data class Ui(
        val loading: Boolean = true,
        val busy: Boolean = false,
        val error: String? = null,
        val file: LoadedFile? = null,
        val text: String = "",
        val lines: List<String> = emptyList(),
        val lineStarts: IntArray = IntArray(0),
        val encodingName: String = Encodings.AUTO,
        val effectiveEncodingLabel: String = "",
        val stats: Stats? = null,
        val savedProgress: Float = 0f,
        val query: String = "",
        val ignoreCase: Boolean = true,
        val matches: List<Int> = emptyList(),
        val currentMatch: Int = -1,
        val hexMode: Boolean = false,
        val lang: Syntax.Lang? = null,
        val editablePath: String? = null,
        val editing: Boolean = false,
        val draft: String = "",
        val saving: Boolean = false,
        val notice: String? = null
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    private val cr = getApplication<Application>().contentResolver
    private var uriStr: String? = null
    private var limit: Long = FileLoader.INITIAL_BYTES

    fun load(spec: String) {
        if (spec == uriStr && _ui.value.file != null) return
        uriStr = spec
        limit = FileLoader.INITIAL_BYTES
        _ui.value = Ui(loading = true, ignoreCase = _ui.value.ignoreCase)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedProgress = RecentStore.progress(getApplication(), spec)
                val f = resolve(spec, limit)
                _ui.value = _ui.value.copy(savedProgress = savedProgress)
                decodeAndSet(f)
                RecentStore.add(
                    getApplication(),
                    RecentEntry(spec, f.name, f.ext, f.realSize, System.currentTimeMillis())
                )
            } catch (e: Exception) {
                _ui.value = Ui(loading = false, error = Friendly.of(e))
            }
        }
    }

    /**
     * Спека источника:
     *  "uri|<content uri>"  — файл через SAF;
     *  "path|<абс. путь>"   — файл по прямому пути;
     *  "tmp|<имя>|<путь>"   — временная копия root-файла, показываем с исходным именем.
     */
    private fun resolve(spec: String, limit: Long): LoadedFile = when {
        spec.startsWith("path|") -> FileLoader.readPath(spec.removePrefix("path|"), limit)
        spec.startsWith("tmp|") -> {
            val rest = spec.removePrefix("tmp|")
            val name = rest.substringBefore('|')
            val path = rest.substringAfter('|')
            FileLoader.readPath(path, limit).copy(name = name)
        }
        else -> FileLoader.read(cr, spec.removePrefix("uri|"), limit)
    }

    fun loadMore() {
        val u = uriStr ?: return
        if (_ui.value.busy || limit >= FileLoader.MAX_BYTES) return
        limit = minOf(limit + FileLoader.STEP_BYTES, FileLoader.MAX_BYTES)
        _ui.value = _ui.value.copy(busy = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                decodeAndSet(resolve(u, limit))
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, error = Friendly.of(e))
            }
        }
    }

    fun setEncoding(name: String) {
        val f = _ui.value.file ?: run {
            _ui.value = _ui.value.copy(encodingName = name)
            return
        }
        _ui.value = _ui.value.copy(encodingName = name, busy = true)
        viewModelScope.launch(Dispatchers.Default) { decodeAndSet(f) }
    }

    fun setHexMode(v: Boolean) {
        _ui.value = _ui.value.copy(hexMode = v)
    }

    fun startEdit() {
        val st = _ui.value
        if (st.editablePath == null || st.editing) return
        _ui.value = st.copy(editing = true, draft = st.text)
    }

    fun cancelEdit() {
        _ui.value = _ui.value.copy(editing = false, draft = "", saving = false)
    }

    fun setDraft(s: String) {
        _ui.value = _ui.value.copy(draft = s)
    }

    fun dismissNotice() {
        _ui.value = _ui.value.copy(notice = null)
    }

    fun saveEdit() {
        val st = _ui.value
        val path = st.editablePath ?: return
        if (st.saving || !st.editing) return
        _ui.value = st.copy(saving = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cur = _ui.value
                val cs = charsetFor(cur.encodingName, cur.file!!.detected)
                File(path).writeBytes(cur.draft.toByteArray(cs))
                val ld = splitLines(cur.draft)
                _ui.value = _ui.value.copy(
                    editing = false,
                    saving = false,
                    text = cur.draft,
                    lines = ld.lines,
                    lineStarts = ld.starts,
                    stats = TextStats.of(cur.draft),
                    matches = emptyList(),
                    currentMatch = -1,
                    draft = ""
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(saving = false, notice = "Не удалось сохранить: ${e.message}")
            }
        }
    }

    fun setQuery(q: String) {
        _ui.value = _ui.value.copy(query = q)
        recomputeMatches()
    }

    fun setIgnoreCase(v: Boolean) {
        _ui.value = _ui.value.copy(ignoreCase = v)
        recomputeMatches()
    }

    fun nextMatch() = stepMatch(1)

    fun prevMatch() = stepMatch(-1)

    fun saveProgress(ratio: Float) {
        val u = uriStr ?: return
        viewModelScope.launch(Dispatchers.IO) {
            RecentStore.updateProgress(getApplication(), u, ratio.coerceIn(0f, 1f))
        }
    }

    private fun stepMatch(d: Int) {
        val st = _ui.value
        val n = st.matches.size
        if (n == 0) return
        val cur = if (st.currentMatch < 0) (if (d > 0) -1 else 0) else st.currentMatch
        _ui.value = st.copy(currentMatch = ((cur + d) % n + n) % n)
    }

    private fun recomputeMatches() {
        val st = _ui.value
        val q = st.query
        val text = st.text
        if (q.isEmpty()) {
            _ui.value = st.copy(matches = emptyList(), currentMatch = -1)
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val hay = if (st.ignoreCase) text.lowercase() else text
            val needle = if (st.ignoreCase) q.lowercase() else q
            if (needle.isEmpty()) return@launch
            val list = ArrayList<Int>()
            var idx = hay.indexOf(needle)
            while (idx >= 0 && list.size < 10_000) {
                list.add(idx)
                idx = hay.indexOf(needle, idx + maxOf(1, needle.length))
            }
            if (_ui.value.query == q && _ui.value.text == text) {
                _ui.value = _ui.value.copy(
                    matches = list,
                    currentMatch = if (list.isEmpty()) -1 else 0
                )
            }
        }
    }

    private fun decodeAndSet(f: LoadedFile) {
        val cur = _ui.value
        val cs = charsetFor(cur.encodingName, f.detected)
        val text = decodeText(f.bytes, cs)
        val ld = splitLines(text)
        _ui.value = cur.copy(
            loading = false,
            busy = false,
            file = f,
            text = text,
            lines = ld.lines,
            lineStarts = ld.starts,
            effectiveEncodingLabel = effectiveLabel(cur.encodingName, f.detected),
            stats = TextStats.of(text),
            matches = emptyList(),
            currentMatch = -1,
            lang = Syntax.fromExt(f.ext),
            editablePath = if (uriStr?.startsWith("path|") == true && File(f.uri).canWrite()) f.uri else null,
            editing = false,
            draft = ""
        )
        recomputeMatches()
    }

    private fun charsetFor(manual: String, detected: Detected): Charset =
        if (manual == Encodings.AUTO) Encodings.charset(detected.charsetName)
        else Encodings.charset(manual)

    private fun effectiveLabel(manual: String, detected: Detected): String =
        if (manual == Encodings.AUTO) detected.label else Encodings.label(manual)
}

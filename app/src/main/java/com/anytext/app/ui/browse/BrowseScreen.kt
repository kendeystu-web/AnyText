@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.anytext.app.ui.browse

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anytext.app.logic.Fs
import com.anytext.app.logic.FsEntry
import com.anytext.app.logic.Fmt
import com.anytext.app.viewmodel.AppViewModel
import com.anytext.app.viewmodel.BrowseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fileColors = listOf(
    Color(0xFF54D6E8),
    Color(0xFF9B8CFF),
    Color(0xFFFF8FB8),
    Color(0xFFFFC466),
    Color(0xFF7BE39A),
    Color(0xFFFF9E80)
)

private fun extTint(name: String): Color {
    val ext = name.substringAfterLast('.', "")
    if (ext.isEmpty()) return fileColors[0]
    val n = fileColors.size
    val h = ext.hashCode()
    return fileColors[((h % n) + n) % n]
}

private fun fileIcon(name: String): ImageVector = when (name.substringAfterLast('.', "").lowercase()) {
    "mp3", "wav", "ogg", "flac", "m4a", "aac", "opus" -> Icons.Filled.Audiotrack
    "mp4", "mkv", "avi", "mov", "webm" -> Icons.Filled.Movie
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> Icons.Filled.Image
    "zip", "rar", "7z", "tar", "gz", "apk", "jar" -> Icons.Filled.FolderZip
    "kt", "java", "py", "c", "cpp", "h", "js", "ts", "html", "css", "xml", "json", "sh", "rs", "go" -> Icons.Filled.Code
    else -> Icons.Filled.Description
}

private val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

/** Архивы никогда не открываем сами и не запоминаем за читалкой — только в архиваторы */
private val archiveExts = setOf(
    "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "cab", "iso", "apk", "jar"
)

@Composable
fun BrowseScreen(
    path: String,
    appVm: AppViewModel,
    onOpenDir: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onBack: () -> Unit
) {
    val vm: BrowseViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val settings by appVm.settings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val isRoot = Fs.isRootSection(path)
    LaunchedEffect(path) { vm.open(path, isRoot) }

    var actionEntry by remember { mutableStateOf<FsEntry?>(null) }
    var renameEntry by remember { mutableStateOf<FsEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<FsEntry?>(null) }
    var filterOn by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = Fs.dirName(path),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                if (isRoot) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "ROOT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = {
                    filterOn = !filterOn
                    if (!filterOn) filter = ""
                }) {
                    Icon(
                        if (filterOn) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Фильтр"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        AnimatedVisibility(visible = filterOn) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                placeholder = { Text("Фильтр по имени…", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.error != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = ui.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(onClick = { vm.open(path, isRoot) }) { Text("Повторить") }
                    }
                }
                else -> {
                    val shown = if (filter.isEmpty()) ui.entries
                    else ui.entries.filter { it.name.contains(filter, ignoreCase = true) }
                    if (shown.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (filter.isEmpty()) "Папка пуста" else "Ничего не найдено",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(state = listState) {
                            items(shown.size) { i ->
                                val e = shown[i]
                                EntryRow(
                                    e = e,
                                    units = settings.units,
                                    onClick = {
                                        val ext = e.name.substringAfterLast('.', "").lowercase()
                                        when {
                                            e.isDir -> onOpenDir(e.path)
                                            // архивы всегда в системный диалог (WinRAR/7z и т.п.)
                                            ext in archiveExts -> vm.openSystemChooser(e, context)
                                            // расширение запомнено за нашей читалкой — открываем сразу в ней
                                            settings.assoc[ext] == "reader" ->
                                                vm.specFor(e) { spec -> onOpenFile(spec) }
                                            // иначе — системный диалог выбора приложения
                                            else -> vm.openSystemChooser(e, context)
                                        }
                                    },
                                    onLongClick = { actionEntry = e }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    actionEntry?.let { e ->
        ModalBottomSheet(onDismissRequest = { actionEntry = null }) {
            Text(
                text = e.name,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (e.isDir) "папка" else "${Fmt.size(e.size, settings.units)} · ${dateFormat.format(Date(e.lastModified))}",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (!e.isDir) {
                SheetAction(Icons.Filled.OpenInNew, "Открыть в читалке") {
                    actionEntry = null
                    vm.specFor(e) { spec -> onOpenFile(spec) }
                }
                SheetAction(Icons.Filled.Share, "Поделиться") {
                    actionEntry = null
                    vm.share(e, context)
                }
            }
            SheetAction(Icons.Filled.ContentCopy, "Копировать путь") {
                actionEntry = null
                clipboard.setText(AnnotatedString(e.path))
                toast(context, "Путь скопирован")
            }
            if (!e.isRoot) {
                SheetAction(Icons.Filled.Edit, "Переименовать") {
                    actionEntry = null
                    renameEntry = e
                }
            }
            val ext = e.name.substringAfterLast('.', "").lowercase()
            if (settings.assoc[ext] == "reader") {
                SheetAction(Icons.Filled.Close, "Сбросить «всегда через читалку» (.$ext)") {
                    actionEntry = null
                    appVm.updateSettings { it.copy(assoc = it.assoc - ext) }
                }
            }
            SheetAction(Icons.Filled.Delete, "Удалить", danger = true) {
                actionEntry = null
                deleteEntry = e
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    renameEntry?.let { e ->
        RenameDialog(
            current = e.name,
            onConfirm = {
                vm.rename(e, it)
                renameEntry = null
            },
            onDismiss = { renameEntry = null }
        )
    }

    deleteEntry?.let { e ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text("Удалить «${e.name}»?") },
            text = {
                Text(
                    text = "Действие необратимо." +
                        if (e.isRoot) " Удаление выполнится через root (rm -rf)." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(e)
                    deleteEntry = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteEntry = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun EntryRow(e: FsEntry, units: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (e.isDir) Color(0xFFFFC466) else extTint(e.name)
            Icon(
                imageVector = if (e.isDir) Icons.Filled.Folder else fileIcon(e.name),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = e.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        e.isDir -> "Папка"
                        e.lastModified > 0 -> "${Fmt.size(e.size, units)} · ${dateFormat.format(Date(e.lastModified))}"
                        else -> "root"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

package com.anytext.app.ui.manager

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anytext.app.data.RecentEntry
import com.anytext.app.logic.Fmt
import com.anytext.app.logic.RootShell
import com.anytext.app.logic.StorageScan
import com.anytext.app.ui.storage.Pie3DChart
import com.anytext.app.ui.storage.PieSlice
import com.anytext.app.ui.storage.buildDisplaySlices
import com.anytext.app.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val STORAGE = "/storage/emulated/0"

private data class Section(val title: String, val path: String, val icon: ImageVector)

private val sections = listOf(
    Section("Внутренняя память", STORAGE, Icons.Filled.Storage),
    Section("Загрузки", "$STORAGE/Download", Icons.Filled.Download),
    Section("Документы", "$STORAGE/Documents", Icons.Filled.Folder),
    Section("Изображения", "$STORAGE/DCIM", Icons.Filled.Image),
    Section("Музыка", "$STORAGE/Music", Icons.Filled.Audiotrack)
)

private val rootSections = listOf(
    Section("Корень файловой системы", "/", Icons.Filled.Memory),
    Section("Система", "/system", Icons.Filled.Memory),
    Section("Данные", "/data", Icons.Filled.Memory),
    Section("Данные приложений", "/data/data", Icons.Filled.Memory),
    Section("Root-папка", "/root", Icons.Filled.Memory)
)

private val extColors = listOf(
    Color(0xFF54D6E8),
    Color(0xFF9B8CFF),
    Color(0xFFFF8FB8),
    Color(0xFFFFC466),
    Color(0xFF7BE39A),
    Color(0xFFFF9E80)
)

private fun extTint(ext: String): Color {
    if (ext.isEmpty()) return extColors[0]
    val n = extColors.size
    val h = ext.hashCode()
    return extColors[((h % n) + n) % n]
}

private val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

@Composable
fun ManagerScreen(
    vm: AppViewModel,
    onBrowse: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStorage: () -> Unit
) {
    val recents by vm.recents.collectAsState()
    val settings by vm.settings.collectAsState()
    val storageScan by vm.storage.collectAsState()
    val scanProgress by vm.scanProgress.collectAsState()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var storageGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    var rootAvailable by remember { mutableStateOf<Boolean?>(null) }
    var storageInfo by remember { mutableStateOf<Pair<Long, Long>?>(null) } // used to total

    LaunchedEffect(Unit) {
        rootAvailable = RootShell.available ?: RootShell.detect()
        storageInfo = withContext(Dispatchers.IO) {
            try {
                val s = StatFs(Environment.getExternalStorageDirectory().absolutePath)
                val total = s.totalBytes
                (total - s.availableBytes) to total
            } catch (e: Exception) {
                null
            }
        }
        vm.ensureStorageScan()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // откроется и без persistable, просто не сохранится в недавних
            }
            onOpenFile("uri|$uri")
        }
    }

    val allFilesAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { storageGranted = Environment.isExternalStorageManager() }

    fun requestAllFilesAccess() {
        val intent = try {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } catch (e: Exception) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
        allFilesAccess.launch(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "AnyText",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Проводник и читалка всего",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.updateSettings { it.copy(darkTheme = !it.darkTheme) } }) {
                Icon(
                    imageVector = if (settings.darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Сменить тему",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Диаграмма памяти, как в CX-проводнике
        StorageCard(
            info = storageInfo,
            scan = storageScan,
            progress = scanProgress,
            units = settings.units,
            onClick = onOpenStorage
        )

        if (!storageGranted) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = "Нужен доступ ко всем файлам",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Чтобы проводник видел файлы во внутренней памяти, выдайте приложению доступ ко всем файлам.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { requestAllFilesAccess() }) {
                        Text("Выдать доступ")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Память устройства")
        Spacer(Modifier.height(6.dp))
        Column {
            sections.forEach { s ->
                SectionRow(
                    icon = s.icon,
                    title = s.title,
                    subtitle = s.path,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onBrowse(s.path) }
                )
            }
        }

        if (settings.widgets.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Быстрый доступ")
            Spacer(Modifier.height(6.dp))
            Column {
                settings.widgets.forEach { w ->
                    SectionRow(
                        icon = Icons.Filled.Folder,
                        title = w.label,
                        subtitle = w.path,
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = { onBrowse(w.path) }
                    )
                }
            }
        }

        if (rootAvailable == true) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Root")
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = "SU",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Column {
                rootSections.forEach { s ->
                    SectionRow(
                        icon = s.icon,
                        title = s.title,
                        subtitle = s.path,
                        tint = Color(0xFFFF6B81),
                        onClick = { onBrowse(s.path) }
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "Открыть файл",
            icon = Icons.Filled.FolderOpen,
            onClick = { picker.launch(arrayOf("*/*")) }
        )

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Недавние",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            if (recents.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) {
                    Text("Очистить", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        if (recents.isEmpty()) {
            Text(
                text = "Пока пусто — откройте первый файл, он появится здесь.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column {
                recents.forEach { entry ->
                    RecentCard(
                        entry = entry,
                        units = settings.units,
                        onClick = { onOpenFile(entry.uri) },
                        onRemove = { vm.removeRecent(entry.uri) }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Тап по файлу — системный выбор «Открыть через…». Долгий тап — поделиться, переименовать, удалить. Любой файл открывается как текст или HEX.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить недавние?") },
            text = { Text("Список файлов будет удалён. Сами файлы останутся на месте.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearRecents()
                }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun StorageCard(
    info: Pair<Long, Long>?,
    scan: StorageScan?,
    progress: String?,
    units: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val slices = scan?.let { buildDisplaySlices(it) }
                ?: listOf(
                    PieSlice(
                        "",
                        1,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            Pie3DChart(
                slices = slices,
                modifier = Modifier
                    .width(104.dp)
                    .height(76.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Внутренняя память",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                if (info != null) {
                    val (used, total) = info
                    Text(
                        text = "Занято ${Fmt.size(used, units)} из ${Fmt.size(total, units)} · свободно ${Fmt.size(total - used, units)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Подсчёт…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (scan == null) {
                        "Сканируем${progress?.let { ": $it" } ?: "…"}"
                    } else {
                        "Нажмите для анализа"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun GradientButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun RecentCard(
    entry: RecentEntry,
    units: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = extTint(entry.ext)
            Text(
                text = (entry.ext.ifEmpty { "?" }).uppercase().take(3),
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.width(44.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${Fmt.size(entry.size, units)} · ${dateFormat.format(Date(entry.lastOpened))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.progress > 0.02f) {
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { entry.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(96.dp)
                            .height(3.dp),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Убрать из недавних",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }
}

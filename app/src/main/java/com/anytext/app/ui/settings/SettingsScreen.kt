@file:OptIn(ExperimentalMaterial3Api::class)

package com.anytext.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anytext.app.data.PinnedPath
import com.anytext.app.viewmodel.AppViewModel

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    var showAddWidget by remember { mutableStateOf(false) }
    var fontLocal by remember(settings.fontSize) { mutableStateOf(settings.fontSize) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Настройки", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionTitle("Внешний вид")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Тема",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = settings.darkTheme,
                        onClick = { vm.updateSettings { it.copy(darkTheme = true) } },
                        label = { Text("Тёмная") }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = !settings.darkTheme,
                        onClick = { vm.updateSettings { it.copy(darkTheme = false) } },
                        label = { Text("Светлая") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Гарнитура", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = settings.fontFamily == "sans",
                        onClick = { vm.updateSettings { it.copy(fontFamily = "sans") } },
                        label = { Text("Sans") }
                    )
                    FilterChip(
                        selected = settings.fontFamily == "serif",
                        onClick = { vm.updateSettings { it.copy(fontFamily = "serif") } },
                        label = { Text("Serif") }
                    )
                    FilterChip(
                        selected = settings.fontFamily == "mono",
                        onClick = { vm.updateSettings { it.copy(fontFamily = "mono") } },
                        label = { Text("Mono") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Размер шрифта: ${fontLocal.toInt()}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = fontLocal,
                    onValueChange = { fontLocal = (it.toInt()).toFloat() },
                    valueRange = 10f..32f,
                    steps = 21,
                    onValueChangeFinished = {
                        vm.updateSettings { it.copy(fontSize = fontLocal) }
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text("Единицы размера", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = settings.units == "gb",
                        onClick = { vm.updateSettings { it.copy(units = "gb") } },
                        label = { Text("ГБ") }
                    )
                    FilterChip(
                        selected = settings.units == "mb",
                        onClick = { vm.updateSettings { it.copy(units = "mb") } },
                        label = { Text("МБ") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Читалка и редактор")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                ToggleRow(
                    title = "Подсветка синтаксиса",
                    subtitle = "Для кода: Python, JSON, C++, SQL и других",
                    checked = settings.syntaxHighlight,
                    onChange = { vm.updateSettings { it.copy(syntaxHighlight = !it.syntaxHighlight) } }
                )
                ToggleRow(
                    title = "Перенос длинных строк",
                    subtitle = null,
                    checked = settings.wrap,
                    onChange = { vm.updateSettings { it.copy(wrap = !it.wrap) } }
                )
                ToggleRow(
                    title = "Номера строк",
                    subtitle = null,
                    checked = settings.lineNumbers,
                    onChange = { vm.updateSettings { it.copy(lineNumbers = !it.lineNumbers) } }
                )
                ToggleRow(
                    title = "Поиск без учёта регистра",
                    subtitle = null,
                    checked = settings.ignoreCase,
                    onChange = { vm.updateSettings { it.copy(ignoreCase = !it.ignoreCase) } }
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Быстрый доступ")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                if (settings.widgets.isEmpty()) {
                    Text(
                        text = "Добавьте ярлыки папок — они появятся на главном экране.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                } else {
                    settings.widgets.forEach { w ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = w.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = w.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                vm.updateSettings {
                                    it.copy(widgets = it.widgets.filter { w2 -> w2.path != w.path })
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Убрать",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Button(onClick = { showAddWidget = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Добавить путь")
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Анализ памяти")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                ToggleRow(
                    title = "Запоминать анализ",
                    subtitle = "Экономит ресурс: диаграмма не пересканируется при каждом запуске. Выключено — пересчёт при каждом входе в приложение.",
                    checked = settings.cacheStorageScan,
                    onChange = { vm.updateSettings { it.copy(cacheStorageScan = !it.cacheStorageScan) } }
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("О приложении")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                Text(
                    text = "AnyText 1.1.0",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Проводник и читалка всего: любой файл — как текст, код — с подсветкой, бинарники — в HEX. Root-разделы доступны при наличии su.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    if (showAddWidget) {
        AddWidgetDialog(
            onAdd = { label, path ->
                vm.updateSettings {
                    it.copy(widgets = it.widgets + PinnedPath(label, path))
                }
                showAddWidget = false
            },
            onDismiss = { showAddWidget = false }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) { content() }
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
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AddWidgetDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ярлык папки") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Путь, напр. /storage/emulated/0/Download") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (label.isNotBlank() && path.isNotBlank()) onAdd(label.trim(), path.trim()) },
                enabled = label.isNotBlank() && path.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

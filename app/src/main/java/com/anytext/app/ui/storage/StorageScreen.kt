@file:OptIn(ExperimentalMaterial3Api::class)

package com.anytext.app.ui.storage

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anytext.app.logic.Fmt
import com.anytext.app.viewmodel.AppViewModel

@Composable
fun StorageScreen(vm: AppViewModel, onBrowse: (String) -> Unit, onBack: () -> Unit) {
    val scan by vm.storage.collectAsState()
    val progress by vm.scanProgress.collectAsState()
    val settings by vm.settings.collectAsState()

    LaunchedEffect(Unit) { vm.ensureStorageScan() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Внутренняя память — анализ",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = { vm.rescanStorage() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Пересканировать")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        val s = scan
        if (s == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Считаем размеры папок${progress?.let { ": $it" } ?: "…"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val display = remember(s) { buildDisplaySlices(s) }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))
                Pie3DChart(
                    slices = display,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    onSliceTap = { i ->
                        val slice = display.getOrNull(i) ?: return@Pie3DChart
                        if (slice.path != null && slice.isDir) onBrowse(slice.path)
                    }
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "${Fmt.size(s.used, settings.units)} занято из ${Fmt.size(s.total, settings.units)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                display.forEach { slice ->
                    LegendRow(
                        slice = slice,
                        total = s.total,
                        units = settings.units,
                        onClick = if (slice.path != null && slice.isDir) {
                            { onBrowse(slice.path) }
                        } else null
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Тап по папке — открыть её в проводнике. Размеры посчитаны рекурсивно; часть системных папок может быть недоступна и учтена как «Другое».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun LegendRow(slice: PieSlice, total: Long, units: String, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(slice.color, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${Fmt.size(slice.value, units)} · ${slicePercent(slice.value, total)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.anytext.app.ui.storage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anytext.app.logic.Fmt
import com.anytext.app.logic.StorageScan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PieSlice(
    val label: String,
    val value: Long,
    val color: Color,
    val path: String? = null,
    val isDir: Boolean = false
)

private val palette = listOf(
    Color(0xFF7CBF4B),
    Color(0xFF5B7FD4),
    Color(0xFFE05252),
    Color(0xFF37B3B3),
    Color(0xFFE8A13C),
    Color(0xFFB06BD4),
    Color(0xFFE06BA8),
    Color(0xFF4BBF8F),
    Color(0xFFD4763C),
    Color(0xFF6BA3E0)
)

/** Сектора для диаграммы: папки + «Другое» + «Свободно» */
fun buildDisplaySlices(scan: StorageScan): List<PieSlice> {
    val list = ArrayList<PieSlice>()
    scan.items.filter { it.size > 0 }.forEachIndexed { i, item ->
        list += PieSlice(item.name, item.size, palette[i % palette.size], item.path, item.isDir)
    }
    val other = (scan.used - scan.itemsTotal).coerceAtLeast(0)
    if (other > 1024 * 1024) list += PieSlice("Другое", other, Color(0xFF6B7280))
    if (scan.free > 0) list += PieSlice("Свободно", scan.free, Color(0xFF2C3140))
    return list
}

private fun Color.darken(f: Float): Color = Color(red * f, green * f, blue * f, alpha)

/**
 * Объёмная круговая диаграмма в стиле WinDirStat: сектора «выдернуты» наружу,
 * снизу тёмный ободок-толщина. Тап по сектору — обратный вызов с индексом.
 */
@Composable
fun Pie3DChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    onSliceTap: ((Int) -> Unit)? = null
) {
    val density = LocalDensity.current
    val depth = with(density) { 10.dp.toPx() }
    val push = with(density) { 3.dp.toPx() }
    val total = slices.sumOf { it.value }.coerceAtLeast(1L).toFloat()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(slices, canvasSize) {
                if (onSliceTap == null || canvasSize == IntSize.Zero) return@pointerInput
                detectTapGestures { pos ->
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat() - depth
                    if (w <= 0f || h <= 0f) return@detectTapGestures
                    val dx = (pos.x - w / 2f) / (w / 2f)
                    val dy = (pos.y - h / 2f) / (h / 2f)
                    if (sqrt(dx * dx + dy * dy) > 1f) return@detectTapGestures
                    var ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                    if (ang < 0) ang += 360.0
                    var acc = 0.0
                    slices.forEachIndexed { i, s ->
                        val sweep = s.value / total * 360.0
                        if (ang >= acc && ang < acc + sweep) {
                            onSliceTap(i)
                            return@detectTapGestures
                        }
                        acc += sweep
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height - depth
            val cx = w / 2f
            val cy = h / 2f
            // тёмная «толщина» пирамидки
            var start = -90f
            slices.forEach { s ->
                val sweep = s.value / total * 360f
                val midRad = Math.toRadians((start + sweep / 2).toDouble())
                val ox = (cos(midRad) * push).toFloat()
                val oy = (sin(midRad) * push).toFloat()
                drawArc(
                    color = s.color.darken(0.55f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(cx - w / 2f + ox, cy - h / 2f + oy + depth),
                    size = Size(w, h)
                )
                start += sweep
            }
            // верхние срезы, выдвинутые наружу
            start = -90f
            slices.forEach { s ->
                val sweep = s.value / total * 360f
                val midRad = Math.toRadians((start + sweep / 2).toDouble())
                val ox = (cos(midRad) * push).toFloat()
                val oy = (sin(midRad) * push).toFloat()
                drawArc(
                    color = s.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(cx - w / 2f + ox, cy - h / 2f + oy),
                    size = Size(w, h)
                )
                start += sweep
            }
        }
    }
}

@Composable
fun slicePercent(value: Long, total: Long): String {
    val pct = if (total > 0) value * 100.0 / total else 0.0
    return String.format(java.util.Locale.getDefault(), "%.1f%%", pct)
}

fun sliceSizeLabel(value: Long): String = Fmt.size(value)

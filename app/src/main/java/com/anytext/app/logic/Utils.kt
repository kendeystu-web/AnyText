package com.anytext.app.logic

import java.util.Locale

data class Stats(val lines: Int, val words: Int, val chars: Int)

object TextStats {
    fun of(text: String): Stats {
        if (text.isEmpty()) return Stats(0, 0, 0)
        val lines = text.count { it == '\n' } + if (text.last() == '\n') 0 else 1
        val words = text.split(Regex("\\s+")).count { it.isNotEmpty() }
        return Stats(lines, words, text.length)
    }
}

object Fmt {
    /** Авто-формат: Б/КБ/МБ/ГБ (для мест, где единицы не настраиваются) */
    fun size(bytes: Long): String = when {
        bytes < 1024 -> "$bytes Б"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f КБ", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f МБ", bytes / 1024.0 / 1024.0)
        else -> String.format(Locale.getDefault(), "%.2f ГБ", bytes / 1024.0 / 1024.0 / 1024.0)
    }

    /** Формат по настройке: "gb" (по умолчанию) или "mb" */
    fun size(bytes: Long, units: String): String = when (units) {
        "mb" -> String.format(Locale.getDefault(), "%.1f МБ", bytes / 1024.0 / 1024.0)
        else -> String.format(Locale.getDefault(), "%.2f ГБ", bytes / 1024.0 / 1024.0 / 1024.0)
    }

    fun num(n: Int): String = String.format(Locale.getDefault(), "%,d", n)
}

data class LinesData(val lines: List<String>, val starts: IntArray)

/** Разбивка текста на строки с офсетами начала каждой строки в исходном тексте */
fun splitLines(text: String): LinesData {
    if (text.isEmpty()) return LinesData(emptyList(), IntArray(0))
    val lines = ArrayList<String>()
    val starts = ArrayList<Int>()
    var start = 0
    var i = text.indexOf('\n')
    while (i >= 0) {
        lines.add(text.substring(start, i))
        starts.add(start)
        start = i + 1
        i = text.indexOf('\n', start)
    }
    lines.add(text.substring(start))
    starts.add(start)
    return LinesData(lines, starts.toIntArray())
}

/** Номер строки, в которой находится офсет [offset] */
fun lineOfOffset(starts: IntArray, offset: Int): Int {
    if (starts.isEmpty()) return 0
    val r = starts.binarySearch(offset)
    return if (r >= 0) r else (-r - 2).coerceIn(0, starts.size - 1)
}

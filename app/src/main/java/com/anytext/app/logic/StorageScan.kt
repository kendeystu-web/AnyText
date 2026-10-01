package com.anytext.app.logic

import android.os.StatFs
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class StorageItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDir: Boolean
)

@Serializable
data class StorageScan(
    val rootPath: String,
    val total: Long,
    val free: Long,
    val items: List<StorageItem>
) {
    val used: Long get() = (total - free).coerceAtLeast(0)
    val itemsTotal: Long get() = items.sumOf { it.size }
}

object StorageScanner {
    private const val MAX_NODES = 80_000L
    private const val MAX_DEPTH = 14
    private const val TIME_BUDGET_MS = 20_000L

    /** Размеры папок верхнего уровня rootPath, посчитанные рекурсивно (с бюджетом времени/узлов) */
    fun scan(rootPath: String, onProgress: (String) -> Unit = {}): StorageScan {
        val root = File(rootPath)
        val entries = root.listFiles() ?: emptyArray()
        val deadline = System.currentTimeMillis() + TIME_BUDGET_MS
        val nodes = longArrayOf(0)

        val items = ArrayList<StorageItem>()
        val dirs = entries.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
        for (d in dirs) {
            onProgress(d.name)
            val size = dirSize(d, nodes, deadline, 0)
            items += StorageItem(d.name, d.absolutePath, size, true)
        }
        val loose = entries.filter { it.isFile }.sumOf { it.length() }
        if (loose > 0) items += StorageItem("Файлы", root.absolutePath, loose, false)
        items.sortByDescending { it.size }

        return try {
            val s = StatFs(rootPath)
            StorageScan(rootPath, s.totalBytes, s.availableBytes, items)
        } catch (e: Exception) {
            StorageScan(rootPath, items.sumOf { it.size }, 0, items)
        }
    }

    private fun dirSize(dir: File, nodes: LongArray, deadline: Long, depth: Int): Long {
        if (depth > MAX_DEPTH) return 0L
        var sum = 0L
        val files = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        } ?: return 0L
        for (f in files) {
            if (nodes[0] > MAX_NODES || System.currentTimeMillis() > deadline) return sum
            nodes[0]++
            sum += if (f.isDirectory) dirSize(f, nodes, deadline, depth + 1) else f.length()
        }
        return sum
    }
}

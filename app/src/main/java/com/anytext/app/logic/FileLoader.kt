package com.anytext.app.logic

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

data class LoadedFile(
    val uri: String,
    val name: String,
    val ext: String,
    val realSize: Long,
    val bytes: ByteArray,
    val truncated: Boolean,
    val detected: Detected,
    val isBinary: Boolean
) {
    val loadedSize: Long get() = bytes.size.toLong()
}

object FileLoader {
    const val INITIAL_BYTES: Long = 2L * 1024 * 1024
    const val STEP_BYTES: Long = 2L * 1024 * 1024
    const val MAX_BYTES: Long = 16L * 1024 * 1024

    /** Чтение через SAF (content:// uri), любое расширение */
    fun read(cr: ContentResolver, uriStr: String, limit: Long): LoadedFile {
        val uri = Uri.parse(uriStr)
        var name = ""
        var realSize = -1L
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni) ?: ""
                    if (si >= 0 && !c.isNull(si)) realSize = c.getLong(si)
                }
            }
        val bytes = readUpTo(
            cr.openInputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть поток файла"),
            limit
        )
        val truncated = if (realSize > 0) {
            bytes.size.toLong() >= limit && realSize > bytes.size
        } else {
            bytes.size.toLong() >= limit
        }
        return build(uriStr, name, realSize, bytes, truncated)
    }

    /** Чтение по прямому пути файловой системы (в т.ч. из читаемых root-путей) */
    fun readPath(pathStr: String, limit: Long): LoadedFile {
        val f = File(pathStr)
        if (!f.exists()) throw FileNotFoundException("Файл не найден: $pathStr")
        val bytes = readUpTo(FileInputStream(f), limit)
        return build(pathStr, f.name, f.length(), bytes, f.length() > bytes.size)
    }

    private fun readUpTo(ins: InputStream, limit: Long): ByteArray {
        val out = ByteArrayOutputStream(minOf(limit, 32L * 1024 * 1024).toInt() + 64)
        var total = 0L
        ins.use {
            val buf = ByteArray(64 * 1024)
            while (total < limit) {
                val want = minOf(buf.size.toLong(), limit - total).toInt()
                val n = it.read(buf, 0, want)
                if (n < 0) break
                out.write(buf, 0, n)
                total += n
            }
        }
        return out.toByteArray()
    }

    private fun build(
        uri: String,
        name: String,
        realSize: Long,
        bytes: ByteArray,
        truncated: Boolean
    ): LoadedFile {
        val detected = EncodingDetector.detect(bytes)
        val ext = if (name.contains('.')) name.substringAfterLast('.', "").lowercase() else ""
        return LoadedFile(
            uri = uri,
            name = name.ifBlank { "файл" },
            ext = ext,
            realSize = if (realSize > 0) realSize else bytes.size.toLong(),
            bytes = bytes,
            truncated = truncated,
            detected = detected,
            isBinary = isLikelyBinary(bytes)
        )
    }

    /** Нулевой байт или >30% управляющих символов в начале — почти наверняка бинарник */
    fun isLikelyBinary(b: ByteArray): Boolean {
        val n = minOf(b.size, 8192)
        if (n == 0) return false
        var ctrl = 0
        for (i in 0 until n) {
            val v = b[i].toInt() and 0xFF
            if (v == 0) return true
            if (v < 9 || v in 14..31) ctrl++
        }
        return ctrl * 100 / n > 30
    }
}

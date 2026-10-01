package com.anytext.app.logic

import java.io.File

data class FsEntry(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val path: String,
    val isRoot: Boolean
)

object Fs {

    fun listDir(path: String): List<FsEntry> {
        val files = File(path).listFiles() ?: return emptyList()
        return files.map { f ->
            FsEntry(f.name, f.isDirectory, f.length(), f.lastModified(), f.absolutePath, false)
        }.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
    }

    /** Пути, которые в этом приложении открываются через root-оболочку */
    fun isRootSection(path: String): Boolean =
        path == "/" ||
            path.startsWith("/system") ||
            path.startsWith("/data") ||
            path.startsWith("/root") ||
            path.startsWith("/vendor") ||
            path.startsWith("/etc")

    fun dirName(path: String): String =
        path.trimEnd('/').substringAfterLast('/').ifEmpty { "/" }
}

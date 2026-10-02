package com.anytext.app.logic

import java.io.File
import java.util.concurrent.TimeUnit

data class RootEntry(val name: String, val isDir: Boolean, val path: String)

/** Минимальная оболочка для чтения через su: листинг, чтение файла, удаление */
object RootShell {

    @Volatile
    var available: Boolean? = null
        private set

    fun quote(p: String): String = "'" + p.replace("'", "'\\''") + "'"

    fun detect(): Boolean {
        available = try {
            val proc = ProcessBuilder("su", "-c", "id").start()
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor(5, TimeUnit.SECONDS)
            out.contains("uid=0")
        } catch (e: Exception) {
            false
        }
        return available!!
    }

    /** ls -A -p: каталоги со слэшем на конце, построчно */
    fun list(path: String): List<RootEntry> = try {
        val proc = ProcessBuilder("su", "-c", "ls -A -p ${quote(path)}").start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.errorStream.close()
        proc.waitFor(15, TimeUnit.SECONDS)
        if (proc.exitValue() != 0) {
            emptyList()
        } else {
            out.lines().filter { it.isNotEmpty() }.map { line ->
                val isDir = line.endsWith("/")
                val name = if (isDir) line.dropLast(1) else line
                RootEntry(name, isDir, joinPath(path, name))
            }.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun catToFile(src: String, dst: File): Boolean = try {
        val proc = ProcessBuilder(
            "su", "-c", "cat ${quote(src)} > ${quote(dst.absolutePath)}"
        ).start()
        proc.waitFor(60, TimeUnit.SECONDS)
        proc.exitValue() == 0
    } catch (e: Exception) {
        false
    }

    fun rm(path: String): Boolean = sh("rm -rf ${quote(path)}")

    /** Произвольная команда через su */
    fun sh(cmd: String): Boolean = try {
        val proc = ProcessBuilder("su", "-c", cmd).start()
        proc.errorStream.close()
        proc.waitFor(30, TimeUnit.SECONDS)
        proc.exitValue() == 0
    } catch (e: Exception) {
        false
    }

    fun joinPath(dir: String, name: String): String =
        if (dir.endsWith("/")) dir + name else dir + "/" + name
}

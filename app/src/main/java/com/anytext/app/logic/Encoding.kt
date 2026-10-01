package com.anytext.app.logic

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object Encodings {
    const val AUTO = "auto"

    /** (значение, человекочитаемая метка) */
    fun available(): List<Pair<String, String>> = buildList {
        add(AUTO to "Автоопределение")
        add("UTF-8" to "UTF-8")
        add("UTF-16LE" to "UTF-16 LE")
        add("UTF-16BE" to "UTF-16 BE")
        add("windows-1251" to "Windows-1251")
        orNull("KOI8-R")?.let { add("KOI8-R" to "KOI8-R") }
        orNull("ISO-8859-5")?.let { add("ISO-8859-5" to "ISO-8859-5") }
        orNull("UTF-32LE")?.let { add("UTF-32LE" to "UTF-32 LE") }
        add("ISO-8859-1" to "Latin-1 (ISO-8859-1)")
        add("US-ASCII" to "US-ASCII")
    }

    private fun orNull(name: String): Charset? = try {
        Charset.forName(name)
    } catch (e: Exception) {
        null
    }

    fun charset(name: String): Charset = when (name) {
        AUTO, "" -> Charsets.UTF_8
        "UTF-16LE" -> Charsets.UTF_16LE
        "UTF-16BE" -> Charsets.UTF_16BE
        else -> orNull(name) ?: Charsets.UTF_8
    }

    fun label(name: String): String =
        available().firstOrNull { it.first == name }?.second ?: name
}

data class Detected(
    val charsetName: String,
    val label: String,
    val hadBom: Boolean,
    val guessed: Boolean
)

object EncodingDetector {

    fun detect(bytes: ByteArray): Detected {
        if (bytes.size >= 4 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() &&
            bytes[2] == 0x00.toByte() && bytes[3] == 0x00.toByte()
        ) return Detected("UTF-32LE", "UTF-32 LE (BOM)", true, false)
        if (bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
            bytes[2] == 0xFE.toByte() && bytes[3] == 0xFF.toByte()
        ) return Detected("UTF-32BE", "UTF-32 BE (BOM)", true, false)
        if (bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()
        ) return Detected("UTF-16LE", "UTF-16 LE (BOM)", true, false)
        if (bytes.size >= 2 &&
            bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()
        ) return Detected("UTF-16BE", "UTF-16 BE (BOM)", true, false)
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) return Detected("UTF-8", "UTF-8 (BOM)", true, false)

        if (isValidUtf8(bytes)) return Detected("UTF-8", "UTF-8", false, false)

        // Кириллица почти всегда живёт в Windows-1251; иначе каша решается вручную
        return Detected("windows-1251", "Windows-1251 (предположительно)", false, true)
    }

    private fun decodeStrict(bytes: ByteArray, len: Int): Boolean = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes, 0, len))
        true
    } catch (e: CharacterCodingException) {
        false
    }

    fun isValidUtf8(bytes: ByteArray): Boolean {
        if (decodeStrict(bytes, bytes.size)) return true
        // пример мог обрезать многобайтовый символ на границе — даём второй шанс
        for (trim in 1..3) {
            if (bytes.size > trim && decodeStrict(bytes, bytes.size - trim)) return true
        }
        return false
    }
}

fun decodeText(bytes: ByteArray, charset: Charset): String {
    var s = String(bytes, charset)
    if (s.isNotEmpty() && s[0] == '\uFEFF') s = s.substring(1)
    return s
}

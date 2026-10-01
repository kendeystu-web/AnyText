package com.anytext.app.logic

object HexDump {
    const val PER_ROW = 16

    fun rowCount(bytes: ByteArray): Int = (bytes.size + PER_ROW - 1) / PER_ROW

    fun offset(row: Int): String = "%08X".format(row.toLong() * PER_ROW)

    fun hex(bytes: ByteArray, row: Int): String {
        val sb = StringBuilder(PER_ROW * 3)
        val from = row * PER_ROW
        val to = minOf(from + PER_ROW, bytes.size)
        for (i in from until to) {
            if (i > from) sb.append(' ')
            sb.append("%02X".format(bytes[i]))
        }
        return sb.toString()
    }

    fun ascii(bytes: ByteArray, row: Int): String {
        val from = row * PER_ROW
        val to = minOf(from + PER_ROW, bytes.size)
        val sb = StringBuilder(to - from)
        for (i in from until to) {
            val v = bytes[i].toInt() and 0xFF
            sb.append(if (v in 0x20..0x7E) v.toChar() else '·')
        }
        return sb.toString()
    }
}

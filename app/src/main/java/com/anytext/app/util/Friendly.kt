package com.anytext.app.util

import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.CharacterCodingException

object Friendly {
    fun of(e: Exception): String = when (e) {
        is SecurityException ->
            "Нет доступа к этому файлу — разрешение истекло. Откройте файл заново с главного экрана."
        is FileNotFoundException ->
            "Файл не найден или был перемещён."
        is IllegalStateException ->
            "Не удалось прочитать файл: ${e.message ?: "поток недоступен"}"
        is IOException ->
            "Ошибка ввода-вывода: ${e.message ?: "неизвестная"}"
        is CharacterCodingException ->
            "Не удалось декодировать файл в этой кодировке — попробуйте другую."
        is java.nio.charset.UnsupportedCharsetException ->
            "Кодировка не поддерживается этим устройством."
        else -> "Ошибка: ${e.message ?: e.javaClass.simpleName}"
    }
}

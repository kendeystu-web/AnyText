package com.anytext.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anytext.app.logic.StorageScan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsStore by preferencesDataStore(name = "anytext_settings")
private val Context.recentsStore by preferencesDataStore(name = "anytext_recents")

@Serializable
data class PinnedPath(
    val label: String,
    val path: String
)

@Serializable
data class AppSettings(
    val darkTheme: Boolean = true,
    val fontSize: Float = 15f,
    val fontFamily: String = "sans",
    val wrap: Boolean = true,
    val lineNumbers: Boolean = false,
    val ignoreCase: Boolean = true,
    val syntaxHighlight: Boolean = true,
    val widgets: List<PinnedPath> = emptyList(),
    val units: String = "gb",
    val cacheStorageScan: Boolean = true
)

@Serializable
data class RecentEntry(
    val uri: String,
    val name: String,
    val ext: String,
    val size: Long,
    val lastOpened: Long,
    val progress: Float = 0f
)

object SettingsStore {
    private val K_DARK = booleanPreferencesKey("dark_theme")
    private val K_FONT_SIZE = floatPreferencesKey("font_size")
    private val K_FONT_FAMILY = stringPreferencesKey("font_family")
    private val K_WRAP = booleanPreferencesKey("wrap")
    private val K_LINE_NUMBERS = booleanPreferencesKey("line_numbers")
    private val K_IGNORE_CASE = booleanPreferencesKey("ignore_case")
    private val K_SYNTAX = booleanPreferencesKey("syntax_highlight")
    private val K_WIDGETS = stringPreferencesKey("widgets_json")
    private val K_UNITS = stringPreferencesKey("size_units")
    private val K_CACHE_SCAN = booleanPreferencesKey("cache_storage_scan")
    private val K_SCAN_JSON = stringPreferencesKey("storage_scan_json")
    private val widgetsJson = Json { ignoreUnknownKeys = true }
    private val scanJson = Json { ignoreUnknownKeys = true }

    fun flow(ctx: Context): Flow<AppSettings> = ctx.settingsStore.data.map { p ->
        AppSettings(
            darkTheme = p[K_DARK] ?: true,
            fontSize = p[K_FONT_SIZE] ?: 15f,
            fontFamily = p[K_FONT_FAMILY] ?: "sans",
            wrap = p[K_WRAP] ?: true,
            lineNumbers = p[K_LINE_NUMBERS] ?: false,
            ignoreCase = p[K_IGNORE_CASE] ?: true,
            syntaxHighlight = p[K_SYNTAX] ?: true,
            widgets = p[K_WIDGETS]?.let {
                runCatching { widgetsJson.decodeFromString<List<PinnedPath>>(it) }.getOrNull()
            } ?: emptyList(),
            units = p[K_UNITS] ?: "gb",
            cacheStorageScan = p[K_CACHE_SCAN] ?: true
        )
    }

    suspend fun save(ctx: Context, s: AppSettings) {
        ctx.settingsStore.edit { p ->
            p[K_DARK] = s.darkTheme
            p[K_FONT_SIZE] = s.fontSize
            p[K_FONT_FAMILY] = s.fontFamily
            p[K_WRAP] = s.wrap
            p[K_LINE_NUMBERS] = s.lineNumbers
            p[K_IGNORE_CASE] = s.ignoreCase
            p[K_SYNTAX] = s.syntaxHighlight
            p[K_WIDGETS] = widgetsJson.encodeToString(s.widgets)
            p[K_UNITS] = s.units
            p[K_CACHE_SCAN] = s.cacheStorageScan
        }
    }

    /** Сохранённый анализ памяти (если включено «Запоминать анализ») */
    suspend fun readCachedScan(ctx: Context): StorageScan? =
        ctx.settingsStore.data.first()[K_SCAN_JSON]?.let {
            runCatching { scanJson.decodeFromString<StorageScan>(it) }.getOrNull()
        }

    suspend fun saveCachedScan(ctx: Context, scan: StorageScan) {
        ctx.settingsStore.edit { p ->
            p[K_SCAN_JSON] = scanJson.encodeToString(scan)
        }
    }
}

object RecentStore {
    private val K_LIST = stringPreferencesKey("recents_json")
    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(raw: String?): List<RecentEntry> =
        raw?.let { runCatching { json.decodeFromString<List<RecentEntry>>(it) }.getOrNull() } ?: emptyList()

    fun flow(ctx: Context): Flow<List<RecentEntry>> =
        ctx.recentsStore.data.map { p -> parse(p[K_LIST]) }

    suspend fun progress(ctx: Context, uri: String): Float =
        flow(ctx).first().firstOrNull { it.uri == uri }?.progress ?: 0f

    suspend fun add(ctx: Context, entry: RecentEntry) {
        ctx.recentsStore.edit { p ->
            val cur = parse(p[K_LIST])
            val next = (listOf(entry) + cur.filter { it.uri != entry.uri }).take(20)
            p[K_LIST] = json.encodeToString(next)
        }
    }

    suspend fun updateProgress(ctx: Context, uri: String, progress: Float) {
        ctx.recentsStore.edit { p ->
            val cur = parse(p[K_LIST])
            if (cur.none { it.uri == uri }) return@edit
            p[K_LIST] = json.encodeToString(cur.map { if (it.uri == uri) it.copy(progress = progress) else it })
        }
    }

    suspend fun remove(ctx: Context, uri: String) {
        ctx.recentsStore.edit { p ->
            val cur = parse(p[K_LIST])
            p[K_LIST] = json.encodeToString(cur.filter { it.uri != uri })
        }
        try {
            ctx.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // разрешения могло уже не быть — не страшно
        }
    }

    suspend fun clear(ctx: Context) {
        val all = flow(ctx).first()
        all.forEach { remove(ctx, it.uri) }
        ctx.recentsStore.edit { p -> p.remove(K_LIST) }
    }
}

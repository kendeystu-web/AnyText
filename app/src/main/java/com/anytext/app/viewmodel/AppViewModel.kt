package com.anytext.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anytext.app.data.AppSettings
import com.anytext.app.data.RecentEntry
import com.anytext.app.data.RecentStore
import com.anytext.app.data.SettingsStore
import com.anytext.app.logic.StorageScan
import com.anytext.app.logic.StorageScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _recents = MutableStateFlow<List<RecentEntry>>(emptyList())
    val recents: StateFlow<List<RecentEntry>> = _recents

    private val _storage = MutableStateFlow<StorageScan?>(null)
    val storage: StateFlow<StorageScan?> = _storage

    private val _scanProgress = MutableStateFlow<String?>(null)
    val scanProgress: StateFlow<String?> = _scanProgress

    @Volatile
    private var scanning = false

    init {
        viewModelScope.launch {
            SettingsStore.flow(ctx).collect { _settings.value = it }
        }
        viewModelScope.launch {
            RecentStore.flow(ctx).collect { _recents.value = it }
        }
        // если включено «Запоминать анализ» — поднимаем сохранённый скан вместо пересканирования
        viewModelScope.launch(Dispatchers.IO) {
            val s = SettingsStore.flow(ctx).first()
            if (s.cacheStorageScan) {
                SettingsStore.readCachedScan(ctx)?.let { cached ->
                    _storage.value = cached
                }
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        viewModelScope.launch(Dispatchers.IO) { SettingsStore.save(ctx, next) }
    }

    fun removeRecent(uri: String) =
        viewModelScope.launch(Dispatchers.IO) { RecentStore.remove(ctx, uri) }

    fun clearRecents() =
        viewModelScope.launch(Dispatchers.IO) { RecentStore.clear(ctx) }

    fun ensureStorageScan() {
        // при наличии результата (из кэша или прошлого скана за сессию) не тратим ресурс
        if (_storage.value != null || scanning) return
        doStorageScan()
    }

    fun rescanStorage() {
        if (!scanning) doStorageScan()
    }

    private fun doStorageScan() {
        scanning = true
        viewModelScope.launch(Dispatchers.IO) {
            _scanProgress.value = "…"
            try {
                val result = StorageScanner.scan("/storage/emulated/0") { name ->
                    _scanProgress.value = name
                }
                _storage.value = result
                if (_settings.value.cacheStorageScan) {
                    SettingsStore.saveCachedScan(ctx, result)
                }
            } catch (e: Exception) {
                // результат просто останется пустым
            } finally {
                _scanProgress.value = null
                scanning = false
            }
        }
    }
}

package com.multik.amtweaks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multik.amtweaks.core.Catalog
import com.multik.amtweaks.core.Hidden
import com.multik.amtweaks.core.RootBackend
import com.multik.amtweaks.core.ShizukuBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class Mode { ROOT, SHIZUKU }
enum class RootState { CHECKING, AVAILABLE, UNAVAILABLE }

data class UiState(
    val mode: Mode = Mode.ROOT,
    val appInstalled: Boolean = true,
    val root: RootState = RootState.CHECKING,
    val shizukuRunning: Boolean = false,
    val shizukuPermission: Boolean = false,
    val values: Map<String, String?> = emptyMap(),
    val busy: Boolean = false,
    val snackbar: String? = null
) {
    val hiddenCount: Int get() = Catalog.all.size
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val root = RootBackend(app.cacheDir)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val allItems = Catalog.playbackToggles + Catalog.audioControls + Catalog.applicationDebug

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        _state.update { it.copy(shizukuPermission = result == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    }
    private val binderReceived = Shizuku.OnBinderReceivedListener { refreshShizuku() }
    private val binderDead = Shizuku.OnBinderDeadListener { refreshShizuku() }

    init {
        _state.update { it.copy(appInstalled = isAppInstalled()) }
        ShizukuBackend.addPermissionListener(permListener)
        ShizukuBackend.addBinderListeners(binderReceived, binderDead)
        refreshShizuku()
        checkRootThenLoad()
    }

    override fun onCleared() {
        ShizukuBackend.removePermissionListener(permListener)
        ShizukuBackend.removeBinderListeners(binderReceived, binderDead)
        super.onCleared()
    }

    private fun isAppInstalled(): Boolean = try {
        getApplication<Application>().packageManager.getPackageInfo(Catalog.PACKAGE, 0)
        true
    } catch (t: Throwable) {
        false
    }

    private fun refreshShizuku() {
        val running = ShizukuBackend.isBinderAlive()
        val perm = ShizukuBackend.hasPermission()
        _state.update { it.copy(shizukuRunning = running, shizukuPermission = perm) }
    }

    private fun checkRootThenLoad() {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { root.isRootAvailable() }
            _state.update { it.copy(root = if (ok) RootState.AVAILABLE else RootState.UNAVAILABLE) }
            if (ok) reloadValues()
        }
    }

    fun setMode(mode: Mode) {
        _state.update { it.copy(mode = mode) }
        if (mode == Mode.SHIZUKU) refreshShizuku()
        if (mode == Mode.ROOT && _state.value.root == RootState.AVAILABLE) reloadValues()
    }

    private fun reloadValues() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val values = withContext(Dispatchers.IO) { root.readValues(allItems) }
            _state.update { it.copy(values = values, busy = false) }
        }
    }

    /** Текущее значение настройки с учётом дефолта, если ключа ещё нет. */
    fun current(h: Hidden): String = _state.value.values[h.id] ?: h.default

    fun setValue(h: Hidden, value: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val r = withContext(Dispatchers.IO) { root.apply(listOf(h to value)) }
            val values = if (r.success) withContext(Dispatchers.IO) { root.readValues(allItems) } else _state.value.values
            _state.update { it.copy(values = values, busy = false, snackbar = r.message) }
        }
    }

    /** Быстрая кнопка: выключить авто-снижение качества. */
    fun disableAdaptive() {
        val h = Catalog.playbackToggles.first { it.key == "key_is_bit_stream_switching_enabled" }
        setValue(h, "false")
    }

    /** Сбросить переключатели потока/качества к заводским значениям. */
    fun resetPlaybackDefaults() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val changes = Catalog.playbackToggles.map { it to it.default }
            val r = withContext(Dispatchers.IO) { root.apply(changes) }
            val values = if (r.success) withContext(Dispatchers.IO) { root.readValues(allItems) } else _state.value.values
            _state.update { it.copy(values = values, busy = false, snackbar = r.message) }
        }
    }

    fun requestShizukuPermission() {
        ShizukuBackend.requestPermission()
    }

    fun openInternalMenu() {
        val r = ShizukuBackend.openInternalDebug()
        _state.update {
            it.copy(
                snackbar = if (r.isSuccess) "Открываю скрытое меню…"
                else "Не вышло: ${r.exceptionOrNull()?.message ?: "ошибка"}. Попробуй root."
            )
        }
    }

    fun openUserMenu() {
        val r = ShizukuBackend.openUserDebug()
        _state.update {
            it.copy(
                snackbar = if (r.isSuccess) "Открываю меню…"
                else "Не вышло: ${r.exceptionOrNull()?.message ?: "ошибка"}."
            )
        }
    }

    fun consumeSnackbar() = _state.update { it.copy(snackbar = null) }
}

package com.multik.amtweaks.core

import android.content.pm.PackageManager
import com.apple.android.music.navigation.NavigationIntentFragmentRequestCode
import rikka.shizuku.Shizuku

/**
 * Бэкенд через Shizuku.
 *
 * Ограничение: Shizuku работает от UID shell и НЕ имеет доступа к приватным
 * файлам Apple Music (/data/data/.../shared_prefs). Поэтому пощёлкать
 * отдельные переключатели он не может — для этого нужен root.
 *
 * Что Shizuku умеет здесь: открыть НАСТОЯЩЕЕ скрытое меню отладки Apple Music,
 * где переключатели стоят внутри самого приложения.
 */
object ShizukuBackend {

    const val PERMISSION_REQUEST_CODE = 4201

    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        if (!isBinderAlive()) false
        else if (Shizuku.isPreV11()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun shouldShowRationale(): Boolean = try {
        !Shizuku.isPreV11() && Shizuku.shouldShowRequestPermissionRationale()
    } catch (t: Throwable) {
        false
    }

    fun requestPermission() {
        try {
            if (isBinderAlive() && !Shizuku.isPreV11()) {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        } catch (_: Throwable) {
        }
    }

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (_: Throwable) {
        }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (_: Throwable) {
        }
    }

    fun addBinderListeners(
        received: Shizuku.OnBinderReceivedListener,
        dead: Shizuku.OnBinderDeadListener
    ) {
        try {
            Shizuku.addBinderReceivedListenerSticky(received)
            Shizuku.addBinderDeadListener(dead)
        } catch (_: Throwable) {
        }
    }

    fun removeBinderListeners(
        received: Shizuku.OnBinderReceivedListener,
        dead: Shizuku.OnBinderDeadListener
    ) {
        try {
            Shizuku.removeBinderReceivedListener(received)
            Shizuku.removeBinderDeadListener(dead)
        } catch (_: Throwable) {
        }
    }

    /** Открыть скрытое меню отладки (экспериментально). */
    fun openInternalDebug(): Result<Unit> =
        ShizukuActivityLauncher.open(NavigationIntentFragmentRequestCode.INTERNAL_DEBUG_SETTINGS)

    fun openUserDebug(): Result<Unit> =
        ShizukuActivityLauncher.open(NavigationIntentFragmentRequestCode.USER_DEBUG_SETTINGS)
}

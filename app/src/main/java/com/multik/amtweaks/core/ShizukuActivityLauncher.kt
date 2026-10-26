package com.multik.amtweaks.core

import android.content.Intent
import android.os.IBinder
import com.apple.android.music.navigation.NavigationIntentFragmentRequestCode
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Запуск скрытого экрана отладки Apple Music через Shizuku (shell-права).
 *
 * Shizuku работает от UID shell, у которого есть разрешение
 * android.permission.START_ANY_ACTIVITY — поэтому он может запустить
 * НЕэкспортируемую MainContentActivity. Но передать extra-перечисление
 * обычным `am` нельзя, поэтому собираем Intent сами и дёргаем системный
 * IActivityManager.startActivity через Shizuku-биндер.
 *
 * extra "intent_fragment_key" — значение нашего enum-двойника; Apple Music
 * восстановит его своим настоящим классом по имени константы.
 *
 * Это самая хрупкая часть: сигнатура startActivity меняется между версиями
 * Android. Ниже учтены startActivity и startActivityWithFeature (API 30–35).
 */
object ShizukuActivityLauncher {

    private const val PKG = Catalog.PACKAGE
    private const val MAIN = "com.apple.android.music.common.MainContentActivity"

    fun open(target: NavigationIntentFragmentRequestCode): Result<Unit> {
        return try {
            val intent = Intent().apply {
                setClassName(PKG, MAIN)
                putExtra("intent_fragment_key", target)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityPrivileged(intent)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun startActivityPrivileged(intent: Intent) {
        val amBinder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity"))
        val stub = Class.forName("android.app.IActivityManager\$Stub")
        val am = stub.getMethod("asInterface", IBinder::class.java).invoke(null, amBinder)
        val iam = Class.forName("android.app.IActivityManager")

        // Предпочитаем startActivityWithFeature (есть с API 30), иначе startActivity.
        val method = iam.methods.firstOrNull { it.name == "startActivityWithFeature" }
            ?: iam.methods.firstOrNull { it.name == "startActivity" }
            ?: error("startActivity не найден в IActivityManager")

        val types = method.parameterTypes
        val args = arrayOfNulls<Any?>(types.size)
        var firstStringUsed = false
        for (i in types.indices) {
            val t = types[i]
            args[i] = when {
                t == Intent::class.java -> intent
                t == Int::class.javaPrimitiveType -> 0
                t == String::class.java && !firstStringUsed -> {
                    firstStringUsed = true
                    "com.android.shell" // callingPackage
                }
                else -> null
            }
        }
        method.invoke(am, *args)
    }
}

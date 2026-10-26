package com.multik.amtweaks.core

import android.util.Base64
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * Бэкенд через root (su). Использует libsu. Умеет всё: читать и писать
 * любые ключи в файлах SharedPreferences Apple Music.
 *
 * Файлы приложения приватны (owner = uid Apple Music, режим 0700),
 * поэтому доступ к ним возможен только из-под root.
 */
class RootBackend(private val cacheDir: File) {

    data class ApplyResult(val success: Boolean, val message: String)

    private val dir = "/data/data/${Catalog.PACKAGE}/shared_prefs"
    private val playbackPath = "$dir/${Catalog.PLAYBACK_FILE}.xml"
    private var applicationPathCache: String? = null

    init {
        // Просим libsu использовать su и короткий таймаут.
        // setDefaultBuilder можно вызвать только до создания главной оболочки,
        // поэтому при повторном создании ViewModel просто пропускаем.
        try {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(15L)
            )
        } catch (_: Throwable) {
        }
    }

    fun isRootAvailable(): Boolean = try {
        Shell.getShell().isRoot
    } catch (t: Throwable) {
        false
    }

    private fun run(cmd: String): Shell.Result = Shell.cmd(cmd).exec()

    private fun fileExists(path: String): Boolean =
        run("[ -f '$path' ] && echo Y").out.firstOrNull()?.trim() == "Y"

    private fun readFile(path: String): String? {
        if (!fileExists(path)) return null
        val res = run("base64 '$path'")
        if (!res.isSuccess) return null
        val b64 = res.out.joinToString("")
        return try {
            String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
        } catch (t: Throwable) {
            null
        }
    }

    private fun writeFile(path: String, content: String): Boolean {
        val b64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val tmp = File(cacheDir, "am_write.b64")
        return try {
            tmp.writeText(b64)
            // '>' усечёт существующий файл, сохранив inode/owner/mode/контекст.
            // Если файла не было — создаём и чиним владельца/режим/SELinux-метку по папке.
            val res = run(
                "base64 -d '${tmp.absolutePath}' > '$path' && " +
                    "u=\$(stat -c %u '$dir') && chown \$u:\$u '$path' 2>/dev/null; " +
                    "chmod 660 '$path' 2>/dev/null; restorecon '$path' 2>/dev/null; true"
            )
            res.isSuccess
        } catch (t: Throwable) {
            false
        } finally {
            tmp.delete()
        }
    }

    /** Найти файл настроек приложения: тот xml, где уже есть один из известных ключей. */
    private fun applicationPath(keysHint: List<String>): String? {
        applicationPathCache?.let { return it }
        for (key in keysHint) {
            val res = run("grep -l 'name=\"$key\"' '$dir'/*.xml 2>/dev/null")
            val p = res.out.firstOrNull()?.trim()
            if (!p.isNullOrEmpty()) {
                applicationPathCache = p
                return p
            }
        }
        return null
    }

    /** Прочитать текущие значения для набора настроек. null — ключа нет (значит, дефолт). */
    fun readValues(items: List<Hidden>): Map<String, String?> {
        val playbackText = readFile(playbackPath)
        val appKeys = items.filter { it.file == PrefFile.APPLICATION }.map { it.key }
        val appText = if (appKeys.isEmpty()) null else applicationPath(appKeys)?.let { readFile(it) }

        val result = HashMap<String, String?>()
        for (i in items) {
            val text = if (i.file == PrefFile.PLAYBACK) playbackText else appText
            result[i.id] = text?.let { PrefsXml.read(it, i.key) }
        }
        return result
    }

    /** Применить изменения. Приложение закрывается, файлы правятся, значения остаются. */
    fun apply(changes: List<Pair<Hidden, String>>): ApplyResult {
        if (changes.isEmpty()) return ApplyResult(true, "Нечего менять")
        if (!isRootAvailable()) return ApplyResult(false, "Нет root")

        // 1) закрыть приложение, чтобы оно не перезаписало файл своей копией
        run("am force-stop ${Catalog.PACKAGE}")

        val appKeys = changes.map { it.first.key }
        val byFile = changes.groupBy { it.first.file }

        for ((file, group) in byFile) {
            val path = when (file) {
                PrefFile.PLAYBACK -> playbackPath
                PrefFile.APPLICATION -> applicationPath(appKeys)
                    ?: return ApplyResult(
                        false,
                        "Не нашёл файл настроек приложения для этих ключей " +
                            "(нужно, чтобы ключ уже существовал). Открой соответствующий " +
                            "экран в Apple Music один раз и попробуй снова."
                    )
            }
            var text = readFile(path) ?: PrefsXml.emptyDocument()
            for ((item, value) in group) {
                text = PrefsXml.set(text, item.key, item.type, value)
            }
            if (!writeFile(path, text)) {
                return ApplyResult(false, "Не удалось записать $path")
            }
        }
        return ApplyResult(true, "Готово. Открывай Apple Music.")
    }
}

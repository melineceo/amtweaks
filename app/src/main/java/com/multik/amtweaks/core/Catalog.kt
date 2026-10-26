package com.multik.amtweaks.core

/**
 * Каталог скрытых настроек Apple Music (версия APK 6.5.3, com.apple.android.music).
 *
 * Все значения вытащены из кода приложения:
 *   - MediaPlaybackPreferences  -> файл com.apple.android.music.playback_preferences.xml
 *   - AppSharedPreferences      -> файл настроек приложения (имя ищем на устройстве)
 *
 * Поле persists:
 *   true  — переживает обычные перезапуски (сбрасывается только при обновлении
 *           приложения из Play), если НЕ выставлять key_has_debug_preferences.
 *   false — приложение само сбрасывает это при выходе на передний план
 *           (clearDebugPreferences) или гасит при запуске (setDebugIsBuild(false)).
 */

/** В каком файле shared_prefs живёт ключ. */
enum class PrefFile {
    /** com.apple.android.music.playback_preferences.xml — путь известен точно. */
    PLAYBACK,
    /** Главный файл настроек приложения — имя ищем на устройстве среди shared_prefs. */
    APPLICATION
}

sealed interface PrefType {
    data object Bool : PrefType
    /** Перечисление: список допустимых строковых значений (первое — «меньше/выкл»). */
    data class Enum(val values: List<String>, val labels: List<String>) : PrefType
    /** Целое-перечисление: подписи для 0,1,2,... */
    data class IntEnum(val labels: List<String>) : PrefType
}

data class Hidden(
    val key: String,
    val file: PrefFile,
    val type: PrefType,
    val title: String,
    val summary: String,
    /** Значение по умолчанию как строка в XML ("true"/"false"/"LOSSLESS"/"0"). */
    val default: String,
    val persists: Boolean,
    /** Рекомендуемое значение «как нужно пользователю» (для кнопки «Сделать как надо»). */
    val recommended: String? = null
) {
    val id: String get() = "${file.name}:$key"
}

object Catalog {

    private val AQ = PrefType.Enum(
        values = listOf("HIGH_EFFICIENCY", "HIGH_QUALITY", "LOSSLESS", "HIGH_RES_LOSSLESS"),
        labels = listOf(
            "High Efficiency (AAC, экономия)",
            "High Quality (AAC 256)",
            "Lossless (ALAC 24/48)",
            "Hi-Res Lossless (24/192)"
        )
    )
    private val ATMOS = PrefType.Enum(
        values = listOf("OFF", "AUTOMATIC", "ALWAYS_ON"),
        labels = listOf("Выкл", "Автоматически", "Всегда включён")
    )

    /** Главное, ради чего всё: переключатели качества/потока, которые реально держатся. */
    val playbackToggles: List<Hidden> = listOf(
        Hidden(
            key = "key_is_bit_stream_switching_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Адаптивное качество (авто-снижение)",
            summary = "Когда ВЫКЛ — плеер держит лучший разрешённый поток и не опускает " +
                "качество под скорость сети. Это и есть та самая скрытая настройка.",
            default = "true",
            persists = true,
            recommended = "false"
        ),
        Hidden(
            key = "experimental_adaptive_track_selection",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Экспериментальный выбор дорожки",
            summary = "Экспериментальный адаптивный выбор дорожки ExoPlayer. По умолчанию выкл.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_use_default_bandwidth_meter",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Стандартный измеритель скорости",
            summary = "Использовать стандартный BandwidthMeter ExoPlayer вместо своего.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_audio_flavor_mode_selection",
            file = PrefFile.PLAYBACK,
            type = PrefType.IntEnum(labels = listOf("Выкл", "Low/High", "Случайно")),
            title = "Режим выбора варианта (flavor)",
            summary = "Отладочный режим перебора вариантов потока. 0 — выкл.",
            default = "0",
            persists = true
        ),
        Hidden(
            key = "key_debug_data_saver_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Форс-экономия трафика",
            summary = "Принудительная экономия трафика. Действует только если приложение " +
                "считает себя debug-сборкой (см. ниже), иначе эффекта нет.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_paf_file",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Писать PAF-события в файл",
            summary = "Диагностика воспроизведения в файл.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_play_summary_file",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Писать Play Summary в файл",
            summary = "Диагностика сводки прослушивания в файл.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_renderv2_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Render v2",
            summary = "Новый конвейер отрисовки. По умолчанию включён.",
            default = "true",
            persists = true
        ),
        Hidden(
            key = "key_debug_is_debug_build",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Пометить как debug-сборку",
            summary = "Разблокирует часть отладочного поведения. ВНИМАНИЕ: приложение " +
                "принудительно сбрасывает это в false при каждом запуске — не держится.",
            default = "false",
            persists = false
        )
    )

    /** Обычные, но полезные аудио-настройки (тот же файл, путь известен). */
    val audioControls: List<Hidden> = listOf(
        Hidden(
            key = "key_lossless_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Lossless включён",
            summary = "Разрешает выбор Lossless/Hi-Res в качестве.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_wifi_audio_quality",
            file = PrefFile.PLAYBACK,
            type = AQ,
            title = "Качество по Wi-Fi",
            summary = "Целевое качество на Wi-Fi. Для Lossless/Hi-Res нужен Lossless включён.",
            default = "HIGH_QUALITY",
            persists = true
        ),
        Hidden(
            key = "key_cellular_audio_quality",
            file = PrefFile.PLAYBACK,
            type = AQ,
            title = "Качество по мобильной сети",
            summary = "Целевое качество на сотовой сети.",
            default = "HIGH_EFFICIENCY",
            persists = true
        ),
        Hidden(
            key = "high_quality_on_cellular_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Высокое качество на мобильной",
            summary = "Разрешить высокое качество по сотовой сети.",
            default = "false",
            persists = true
        ),
        Hidden(
            key = "key_dolby_atmos_state",
            file = PrefFile.PLAYBACK,
            type = ATMOS,
            title = "Dolby Atmos",
            summary = "Режим пространственного звука.",
            default = "OFF",
            persists = true
        ),
        Hidden(
            key = "key_volume_normalization_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Нормализация громкости (Sound Check)",
            summary = "Выравнивание громкости между треками. По умолчанию включено.",
            default = "true",
            persists = true
        ),
        Hidden(
            key = "key_enhanced_audio_enabled",
            file = PrefFile.PLAYBACK,
            type = PrefType.Bool,
            title = "Enhance Dialogue / улучшение звука",
            summary = "Доп. обработка звука.",
            default = "false",
            persists = true
        )
    )

    /**
     * Отладочные флаги из главного файла настроек приложения.
     * Их имя файла точно не зашито — ищем на устройстве. Многие из них
     * приложение сбрасывает само (persists = false), поэтому в UI они
     * показаны отдельно, «только для продвинутых».
     */
    val applicationDebug: List<Hidden> = listOf(
        dbg("key_extra_logs_downloads", "Подробные логи загрузок", "true"),
        dbg("key_chromecast_qa_enabled", "Chromecast QA", "false"),
        dbg("key_chromecast_all_types", "Chromecast: все типы медиа", "false"),
        dbg("key_cpu_logging_when_backgrounded", "Лог CPU в фоне", "false"),
        dbg("key_force_performance_metrics", "Форс метрик производительности", "false"),
        dbg("key_save_page_render_metrics_disk", "Метрики отрисовки на диск", "false"),
        dbg("key_is_library_initial_load_optimisation", "Оптимизация первой загрузки библиотеки", "true"),
        dbg("key_is_show_search_weights_enabled", "Показывать веса поиска", "false"),
        dbg("key_notifications_enabled", "Отладка уведомлений", "true"),
        dbg("key_debug_enable_apns_channel_subscriptions", "APNS: подписки на каналы", "false"),
        dbg("key_anthem_fallback_enabled", "Anthem fallback", "false")
    )

    private fun dbg(key: String, title: String, default: String) = Hidden(
        key = key,
        file = PrefFile.APPLICATION,
        type = PrefType.Bool,
        title = title,
        summary = "Отладочный флаг. Приложение может сбрасывать его при запуске.",
        default = default,
        persists = false
    )

    /** Всё вместе — для подсчёта «сколько скрытых функций». */
    val all: List<Hidden> get() = playbackToggles + audioControls + applicationDebug

    const val PACKAGE = "com.apple.android.music"
    const val PLAYBACK_FILE = "com.apple.android.music.playback_preferences"
}

package com.apple.android.music.navigation

/**
 * Заглушка enum'а из Apple Music.
 *
 * Зачем: скрытый экран отладки открывается так — в Intent кладётся
 * extra "intent_fragment_key" со значением-перечислением этого класса,
 * а Apple Music читает его как Serializable своим ClassLoader'ом.
 *
 * Сериализация enum в Java идёт ТОЛЬКО по полному имени класса и имени
 * константы. Поэтому нам достаточно объявить класс с тем же полным
 * именем (com.apple.android.music.navigation.NavigationIntentFragmentRequestCode)
 * и теми же именами констант. При передаче через Binder Apple Music
 * восстановит значение своим настоящим enum'ом по имени.
 *
 * ВАЖНО: имя пакета, имя класса и имена констант менять нельзя.
 * Порядок/ordinal значения не важен — десериализация идёт по имени.
 */
enum class NavigationIntentFragmentRequestCode {
    INTERNAL_DEBUG_SETTINGS,
    USER_DEBUG_SETTINGS
}

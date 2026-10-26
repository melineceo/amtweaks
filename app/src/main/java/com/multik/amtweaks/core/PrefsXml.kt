package com.multik.amtweaks.core

/**
 * Чистая правка файла SharedPreferences (XML) как текста.
 *
 * Важно НЕ пересобирать файл с нуля: Android хранит там кучу ключей,
 * мы меняем ровно одну строку, остальное байт-в-байт остаётся как было.
 * Логика повторяет проверенный sed-скрипт.
 *
 * Класс не трогает файловую систему — только строки. Чтение/запись файла
 * делают бэкенды (root/Shizuku).
 */
object PrefsXml {

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun element(key: String, type: PrefType, value: String): String = when (type) {
        is PrefType.Bool ->
            "<boolean name=\"${esc(key)}\" value=\"$value\" />"
        is PrefType.IntEnum ->
            "<int name=\"${esc(key)}\" value=\"$value\" />"
        is PrefType.Enum ->
            "<string name=\"${esc(key)}\">${esc(value)}</string>"
    }

    /** Прочитать текущее значение ключа, либо null если его нет. */
    fun read(xml: String, key: String): String? {
        val k = Regex.escape(key)
        // boolean/int/long/float: value="..."
        Regex("""<(?:boolean|int|long|float)\s+name="$k"\s+value="([^"]*)"\s*/>""")
            .find(xml)?.let { return it.groupValues[1] }
        // string: <string name="k">...</string>
        Regex("""<string\s+name="$k"\s*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.let { return unesc(it.groupValues[1]) }
        return null
    }

    private fun unesc(s: String): String = s
        .replace("&quot;", "\"")
        .replace("&gt;", ">")
        .replace("&lt;", "<")
        .replace("&amp;", "&")

    /**
     * Вернуть новый текст XML с выставленным значением.
     * Если ключ есть — заменяем его элемент. Если нет — вставляем перед </map>.
     * Пустой <map /> превращаем в <map>...</map>.
     */
    fun set(xml: String, key: String, type: PrefType, value: String): String {
        val k = Regex.escape(key)
        val newEl = element(key, type, value)

        // 1) ключ уже есть — заменить его элемент целиком (self-closing или парный)
        val existing = Regex(
            """<(boolean|int|long|float|string)\s+name="$k"(?:\s+value="[^"]*"\s*/>|\s*>.*?</\1>)""",
            RegexOption.DOT_MATCHES_ALL
        )
        if (existing.containsMatchIn(xml)) {
            return existing.replaceFirst(xml, Regex.escapeReplacement(newEl))
        }

        // 2) пустой <map /> -> <map>EL</map>
        val emptyMap = Regex("""<map\s*/>""")
        if (emptyMap.containsMatchIn(xml)) {
            return emptyMap.replaceFirst(xml, Regex.escapeReplacement("<map>\n    $newEl\n</map>"))
        }

        // 3) вставить перед </map>
        val close = xml.lastIndexOf("</map>")
        if (close >= 0) {
            return xml.substring(0, close) + "    $newEl\n" + xml.substring(close)
        }

        // 4) файла-карты нет вообще — собрать минимальный валидный файл
        return "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    $newEl\n</map>\n"
    }

    /** Пустой, но валидный файл настроек — на случай, если файла ещё нет. */
    fun emptyDocument(): String =
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n"
}

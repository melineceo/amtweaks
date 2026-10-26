# AM Tweaks

**Hidden Apple Music settings for Android — root or Shizuku, Material You.**

AM Tweaks exposes playback settings that Apple Music for Android ships but never shows,
most notably the switch that stops the player from lowering audio quality when the
network gets slow. Apple Music itself is never patched or re-signed: in root mode the
app edits Apple Music's own settings file on your device; in Shizuku mode it opens
Apple Music's built-in debug screen.

[Download the latest release](../../releases/latest) · [Русский ниже](#ru)

> Unofficial project, not affiliated with or endorsed by Apple Inc.
> Apple Music is a trademark of Apple Inc.

---

<a id="ru"></a>

## Что это

Приложение на Jetpack Compose (Material 3 / Material You) для скрытых настроек
Apple Music на Android. Главная из них — **адаптивное качество**: если его выключить,
плеер держит лучший разрешённый поток и не опускает качество под скорость сети.

Apple Music не пересобирается и не переподписывается, поэтому загруженная музыка
и обновления из Google Play остаются на месте.

Каталог собран по коду Apple Music **6.5.3** (`com.apple.android.music`):
**27 скрытых функций**.

## Режимы

| | Root | Shizuku |
|---|---|---|
| Переключать настройки по одной | да | нет* |
| Открыть встроенное debug-меню Apple Music | — | да (экспериментально) |

\* Shizuku работает от UID `shell`, а файлы настроек Apple Music приватные
(владелец — сама Apple Music, режим `0700`). Читать и писать их может только root.
Зато у `shell` есть право `START_ANY_ACTIVITY`, и он может открыть скрытый экран
отладки, где переключатели стоят внутри самого приложения.

## Установка

1. Скачай `AMTweaks-x.y.z.apk` из [Releases](../../releases/latest) и установи.
2. **Root:** при первом запуске выдай доступ суперпользователя.
   **Shizuku:** запусти службу Shizuku, переключи режим наверху и дай доступ.
3. В режиме root нажми «Выключить» в карточке авто-снижения качества
   или настрой остальные переключатели.

> После обновления Apple Music из Play она может сбросить эти настройки —
> открой её один раз и примени заново. Выход из аккаунта Apple стирает все её настройки.

## Как это работает

- Настройки воспроизведения живут в
  `/data/data/com.apple.android.music/shared_prefs/com.apple.android.music.playback_preferences.xml`.
  В режиме root приложение закрывает Apple Music, меняет ровно одну строку XML
  и записывает файл обратно в тот же inode, так что владелец, права и SELinux-метка
  не меняются.
- Ключевой флаг — `key_is_bit_stream_switching_enabled`. Когда он `false`, трек-селектор
  плеера оставляет одну лучшую дорожку из разрешённых настройками качества
  вместо адаптивного набора.
- Скрытое меню — фрагмент внутри `MainContentActivity`, который открывается extra
  `intent_fragment_key` с enum-значением. `am start` такое не передаёт, поэтому режим
  Shizuku собирает Intent сам и вызывает `IActivityManager.startActivity` через биндер
  Shizuku. Файл
  `app/src/main/java/com/apple/android/music/navigation/NavigationIntentFragmentRequestCode.kt`
  — заглушка с тем же полным именем, что и enum в Apple Music: Java сериализует enum
  по имени класса и константы, и Apple Music восстанавливает значение своим классом.
  Это нужно для совместимости, код Apple в репозитории не содержится.

## Сборка

Каждый пуш в `main` и каждый PR собирается в GitHub Actions; APK лежит в артефактах.
Релиз: **Releases → Draft a new release → новый тег `vX.Y.Z` → Publish** —
workflow соберёт подписанный APK, прикрепит его к релизу и подставит заметки из
`docs/release-notes/vX.Y.Z.md`.

Локально: Android Studio (AGP 8.5, JDK 17, SDK 35) → `Build → Build APK(s)`.
Без своих ключей релизная сборка подписывается debug-ключом.

### Подпись (для мейнтейнера)

Ключ релизов не хранится в репозитории. Workflow берёт его из секретов
репозитория (**Settings → Secrets and variables → Actions**):

| Секрет | Значение |
|---|---|
| `AMT_KEYSTORE_B64` | keystore (PKCS12, alias `amtweaks`) в base64 одной строкой |
| `AMT_KEYSTORE_PASSWORD` | пароль keystore и ключа |

Без секретов релиз не получит APK: workflow остановится, чтобы не выложить сборку с debug-подписью.

## Ограничения

- Запуск меню через Shizuku зависит от сигнатуры `IActivityManager.startActivity`,
  которая меняется между версиями Android. Учтены `startActivity` и
  `startActivityWithFeature` (Android 11–15). Если не сработает — используй root.
- Ключи настроек проверены на Apple Music 6.5.3. В новых версиях Apple может их
  переименовать.

## Благодарности

- [libsu](https://github.com/topjohnwu/libsu) — root
- [Shizuku](https://github.com/RikkaApps/Shizuku-API) — режим Shizuku

## Лицензия

[MIT](LICENSE)

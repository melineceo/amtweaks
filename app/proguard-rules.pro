# Shizuku использует hidden-API через reflection
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# Заглушка-enum Apple Music: имя класса и констант менять нельзя —
# десериализация на стороне Apple Music идёт по полному имени.
-keep class com.apple.android.music.navigation.NavigationIntentFragmentRequestCode { *; }

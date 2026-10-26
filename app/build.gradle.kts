plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Ключ подписи релизов в репозитории НЕ хранится.
// В GitHub Actions он приходит из секретов (AMT_KEYSTORE_B64 / AMT_KEYSTORE_PASSWORD),
// workflow кладёт файл и передаёт путь в AMT_KEYSTORE_FILE.
// Локально и в форках без этих переменных релиз подписывается debug-ключом.
val releaseKeystore: File? = System.getenv("AMT_KEYSTORE_FILE")
    ?.let { file(it) }
    ?.takeIf { it.exists() }
val releasePassword: String? = System.getenv("AMT_KEYSTORE_PASSWORD")

android {
    namespace = "com.multik.amtweaks"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.multik.amtweaks"
        minSdk = 26
        targetSdk = 35
        versionCode = 10000 // 1.0.0 -> MAJOR*10000 + MINOR*100 + PATCH
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseKeystore != null && !releasePassword.isNullOrEmpty()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releasePassword
                keyAlias = "amtweaks"
                keyPassword = releasePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // root (su) — libsu от topjohnwu
    implementation("com.github.topjohnwu.libsu:core:6.0.0")

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}

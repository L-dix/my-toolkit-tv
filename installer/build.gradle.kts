plugins {
    alias(libs.plugins.android.application)
}

// Same signing and versioning as :app (see app/build.gradle.kts), so the installer can check
// that the Toolkit APK it downloads was signed with the same key as itself.
val releaseKeystore = System.getenv("ANDROID_KEYSTORE_FILE")?.let(::file)?.takeIf { it.isFile }
val buildNumber = System.getenv("VERSION_CODE")?.toIntOrNull()

// Toolkit is downloaded from this repository's latest GitHub Release.
val repository = System.getenv("GITHUB_REPOSITORY") ?: "L-dix/my-toolkit-tv"

android {
    namespace = "com.tk.toolkit.tv.installer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tk.toolkit.tv.installer"
        minSdk = 28
        targetSdk = 37
        versionCode = buildNumber ?: 1
        versionName = if (buildNumber != null) "1.0.$buildNumber" else "1.0"
        buildConfigField(
            "String",
            "TOOLKIT_APK_URL",
            "\"https://github.com/$repository/releases/latest/download/toolkit-tv.apk\"",
        )
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                val password = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                storeFile = releaseKeystore
                storePassword = password
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = password
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

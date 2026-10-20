plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Stable release key, supplied by CI from GitHub Secrets (see .github/workflows/release.yml).
// Without it (local builds), release builds fall back to the debug key as before.
val releaseKeystore = System.getenv("ANDROID_KEYSTORE_FILE")?.let(::file)?.takeIf { it.isFile }

// CI passes an ever-increasing build number so each APK installs over the previous one.
val buildNumber = System.getenv("VERSION_CODE")?.toIntOrNull()

android {
    namespace = "com.tk.toolkit.tv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tk.toolkit.tv"
        minSdk = 28
        targetSdk = 37
        versionCode = buildNumber ?: 1
        versionName = if (buildNumber != null) "1.0.$buildNumber" else "1.0"

    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Personal sideloaded app: the CI release key when available, else the debug key
            // so local release builds install over debug builds.
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
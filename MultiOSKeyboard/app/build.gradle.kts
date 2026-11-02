plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.multios.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.multios.keyboard"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-MultiOS"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
        }
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.coroutines.android)
}

tasks.register("exportKeyboardApk") {
    dependsOn("assembleRelease")
    doNotTrackState("Direct APK file export")
    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val srcApk = releaseDir.listFiles()?.firstOrNull { it.extension == "apk" }
            ?: error("No release APK found in ${releaseDir.absolutePath}")
        val localOutDir = file("C:/Users/HUAWEI/Downloads/m21_desktop_launcher_source/apks")
        localOutDir.mkdirs()
        srcApk.copyTo(File(localOutDir, "MultiOS-Keyboard.apk"), overwrite = true)
        srcApk.copyTo(File("C:/Users/HUAWEI/Downloads/MultiOS-Keyboard.apk"), overwrite = true)
    }
}

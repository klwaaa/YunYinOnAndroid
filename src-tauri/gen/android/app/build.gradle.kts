import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("rust")
}

val tauriProperties = Properties().apply {
    val propFile = file("tauri.properties")
    if (propFile.exists()) {
        propFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.YunYin.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.YunYin.app"
        minSdk = 24
        targetSdk = 36
        versionCode = tauriProperties.getProperty("tauri.android.versionCode", "1").toInt()
        versionName = tauriProperties.getProperty("tauri.android.versionName", "1.0")
        manifestPlaceholders["usesCleartextTraffic"] = "true"

        // 多架构支持
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false

            packaging {
                jniLibs {
                    keepDebugSymbols.add("*/arm64-v8a/*.so")
                    keepDebugSymbols.add("*/armeabi-v7a/*.so")
                    keepDebugSymbols.add("*/x86/*.so")
                    keepDebugSymbols.add("*/x86_64/*.so")
                }
            }
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                *fileTree(".") { include("**/*.pro") }
                    .plus(getDefaultProguardFile("proguard-android-optimize.txt"))
                    .toList().toTypedArray()
            )
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // 指向 Rust 生成的 JNI lib
            jniLibs.srcDirs(
                file("../../target/armv7-linux-androideabi/debug"),
                file("../../target/aarch64-linux-android/debug"),
                file("../../target/i686-linux-android/debug"),
                file("../../target/x86_64-linux-android/debug")
            )
        }
    }
}

rust {
    // 指向 Rust 项目根目录
    rootDirRel = "../../../"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
}

// 不依赖 Maven 上不存在的 Tauri Android SDK
// apply(from = "tauri.build.gradle.kts") // 可选，如你需要 Tauri 的 gradle 脚本

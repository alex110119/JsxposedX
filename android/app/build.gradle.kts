import java.util.Properties


plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

repositories {
    google()
    mavenCentral()
    maven("https://api.xposed.info/")
    maven("https://jitpack.io")
}
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
val hasKeystoreProperties = keystorePropertiesFile.exists()

if (hasKeystoreProperties) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}
android {
    namespace = "com.jsxposed.x"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.jsxposed.x"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        ndk {
            abiFilters.addAll(
                listOf(
                    "arm64-v8a", "armeabi-v7a",
                    "x86", "x86_64"
                )
            )
        }
    }

    signingConfigs {
        if (hasKeystoreProperties) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            if (hasKeystoreProperties) {
                signingConfig = signingConfigs.getByName("release")
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "baksmali.properties",
                "baksmali.properties/**",
                "META-INF/*.version",
            )
        }
    }
}

dependencies {
    compileOnly(files("src/lib/XposedBridgeAPI-89.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.github.suzhelan:XpHelper:2.7") {
        exclude(group = "com.alibaba.fastjson2", module = "fastjson2")
    }

    // Pure QuickJS Wrapper for Android
    implementation("wang.harlon.quickjs:wrapper-android:3.2.0")

    // DEX decompilation
    implementation("io.github.skylot:jadx-core:1.5.1") { isTransitive = true }
    implementation("io.github.skylot:jadx-dex-input:1.5.1") { isTransitive = true }
    implementation("org.smali:baksmali:2.5.2")
    implementation("org.smali:dexlib2:2.5.2")
    // Xposed service 101 将API订正至api101 2026-04-02 19:00
    compileOnly("io.github.libxposed:api:101.0.1")
    
}

flutter {
    source = "../.."
}

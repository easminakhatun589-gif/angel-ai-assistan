plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

fun configuredValue(name: String): String? {
    val fromEnvFile = file("${rootDir}/.env").takeIf { it.exists() }?.readLines()
        ?.firstOrNull { it.trimStart().startsWith("$name=") }
        ?.substringAfter("=")?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
    return providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: fromEnvFile?.ifBlank { null }
}

fun buildConfigString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseStorePath = configuredValue("JARVIS_RELEASE_STORE_FILE")
val releaseStorePassword = configuredValue("JARVIS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = configuredValue("JARVIS_RELEASE_KEY_ALIAS")
val releaseKeyPassword = configuredValue("JARVIS_RELEASE_KEY_PASSWORD")
val releaseCertSha256 = configuredValue("JARVIS_RELEASE_CERT_SHA256")?.replace(":", "")?.uppercase().orEmpty()
val developerName = configuredValue("JARVIS_DEVELOPER_NAME")?.ifBlank { null } ?: "MD ASLAM"
val copyrightOwner = configuredValue("JARVIS_COPYRIGHT_OWNER")?.ifBlank { null } ?: "MD ASLAM"
val verificationEndpoint = configuredValue("JARVIS_VERIFICATION_ENDPOINT").orEmpty()
val geminiApiKey = configuredValue("GEMINI_API_KEY").orEmpty()
val grokApiKey = configuredValue("GROK_API_KEY").orEmpty()
val deepseekApiKey = configuredValue("DEEPSEEK_API_KEY").orEmpty()
val hiveApiKey = configuredValue("HIVE_API_KEY").orEmpty()
val metaApiKey = configuredValue("META_API_KEY").orEmpty()
val releaseSigningReady = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() } && releaseStorePath?.let { file(it).exists() } == true

android {
    namespace = "com.jarvis.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jarvis.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 144
        versionName = "1.8.8"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", buildConfigString(geminiApiKey))
        buildConfigField("String", "GROK_API_KEY", buildConfigString(grokApiKey))
        buildConfigField("String", "DEEPSEEK_API_KEY", buildConfigString(deepseekApiKey))
        buildConfigField("String", "HIVE_API_KEY", buildConfigString(hiveApiKey))
        buildConfigField("String", "META_API_KEY", buildConfigString(metaApiKey))
        buildConfigField("String", "DEVELOPER_NAME", buildConfigString(developerName.ifBlank { "Unverified developer" }))
        buildConfigField("String", "COPYRIGHT_OWNER", buildConfigString(copyrightOwner.ifBlank { "Unconfigured" }))
        buildConfigField("String", "PINNED_RELEASE_CERT_SHA256", buildConfigString(releaseCertSha256))
        buildConfigField("String", "VERIFICATION_ENDPOINT", buildConfigString(verificationEndpoint))
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            } else {
                val debugConfig = getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (releaseSigningReady) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
        buildConfig = true
    }

    lint {
        // AndroidX Lifecycle 2.8.6's NullSafeMutableLiveData detector crashes under this project's Kotlin 2.0 UAST.
        // Keep all other lint checks enabled while that upstream detector is incompatible.
        disable += "NullSafeMutableLiveData"
    }
}

tasks.register("verifyReleaseSecurityInputs") {
    doLast {
        check(releaseSigningReady) {
            "Release signing is not configured. Set JARVIS_RELEASE_STORE_FILE, JARVIS_RELEASE_STORE_PASSWORD, JARVIS_RELEASE_KEY_ALIAS, and JARVIS_RELEASE_KEY_PASSWORD in the local environment or Gradle user properties."
        }
        check(releaseCertSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Set JARVIS_RELEASE_CERT_SHA256 to the SHA-256 fingerprint of the release signing certificate."
        }
        check(developerName.isNotBlank() && copyrightOwner.isNotBlank()) {
            "Set JARVIS_DEVELOPER_NAME and JARVIS_COPYRIGHT_OWNER to the verified publisher identity."
        }
    }
}

tasks.matching { it.name == "packageRelease" }.configureEach {
    dependsOn("verifyReleaseSecurityInputs")
}

dependencies {
    // AndroidX Core & UI (Pinned to 1.13.1 / 1.9.3 to ensure full compatibility with compileSdk 34)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // AndroidX Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Networking, Serialization & WebKit
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.webkit:webkit:1.12.1")

    // Native & Speech Recognition (Vosk)
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.75@aar")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Security Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Firebase (BOM aligned for stability)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    
    // Credentials Manager for Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Image Loading
    implementation("io.coil-kt:coil:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
}




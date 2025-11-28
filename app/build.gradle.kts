import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.github.triplet.play") version "3.10.0"
}

// Helper to run git commands
fun runGitCommand(vararg args: String): String {
    try {
        // Use Java's ProcessBuilder so we don't depend on Gradle Exec services at configuration time
        val command = listOf("git", *args)
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        return if (exitCode == 0) output else ""
    } catch (_: Exception) {
        // If git isn't available or something fails, return empty string
        return ""
    }
}

// Get commit count for versionCode, safe fallback when git isn't available or output is invalid
fun gitCommitCount(default: Int = 1): Int {
    val out = runGitCommand("rev-list", "--count", "HEAD").trim()
    return out.toIntOrNull() ?: default
}

// Get latest tag for versionName (fallback if none)
fun gitTagOrDefault(): String {
    val tag = runGitCommand("describe", "--tags", "--abbrev=0")
    return if (tag.isNotEmpty()) tag.removePrefix("v") else "0.0.0"
}

tasks.register("printVersion") {
    doLast {
        println("Version Code: ${android.defaultConfig.versionCode}")
        println("Version Name: ${android.defaultConfig.versionName}")
    }
}

// Load keystore properties (created by CI from secrets) if present — parse manually to avoid java.* imports
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProps: Map<String, String> = if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                key to value
            } else null
        }
        .toMap()
} else emptyMap()

// Helper function to get keystore property with optional default
fun keystoreProp(key: String, default: String? = null): String? = keystoreProps[key] ?: default

android {
    namespace = "com.fiospace.bitcointicker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fiospace.bitcointicker"
        minSdk = 26
        targetSdk = 35
        // Auto-generated versionCode from commit count with safe fallback
        versionCode = gitCommitCount()
        versionName = gitTagOrDefault()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProp("storeFile") ?: "keystore.jks"
            storeFile = file(storeFilePath)
            storePassword = keystoreProp("storePassword")
            keyAlias = keystoreProp("keyAlias")
            keyPassword = keystoreProp("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_10
        targetCompatibility = JavaVersion.VERSION_1_10
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.play.services.location)
    implementation(libs.preference)
    //implementation(files("/Users/jtashiro/AndroidStudioProjects/bitcoin_price_fetcher2/build/libs/bitcoin_price_fetcher-1.0.jar"))
    //implementation(fileTree(dir: 'libs', include: ['*.jar']))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.leanback)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Configure Gradle Play Publisher to use a service account JSON written by CI at runtime
play {
    // CI will create `play-service-account.json` in the repo root during the workflow
    serviceAccountCredentials.set(file("play-service-account.json"))
    // Publish app bundles by default
    defaultToAppBundles.set(true)
    // Track will be passed from workflow or default to 'production'
}

import java.util.concurrent.TimeUnit

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
// Kotlin
tasks.register("runPythonScript") {
    doLast {
        // Candidate interpreter locations to check (Homebrew M1/M2, Intel, system, and PATH fallback)
        val candidates = listOf(
            "/opt/homebrew/bin/python3",
            "/usr/local/bin/python3",
            "/usr/bin/python3",
            "python3"
        )

        // Find a candidate that returns a zero exit code for `--version` quickly
        val pythonCmd: String? = candidates.firstOrNull { cmd ->
            try {
                val p = ProcessBuilder(listOf(cmd, "--version"))
                    .redirectErrorStream(true)
                    .start()
                if (!p.waitFor(3, TimeUnit.SECONDS)) {
                    p.destroy()
                    false
                } else {
                    p.exitValue() == 0
                }
            } catch (_: Exception) {
                false
            }
        }

        if (pythonCmd == null) {
            logger.warn("No python3 interpreter found (checked /opt/homebrew/bin, /usr/local/bin, /usr/bin, and PATH). Skipping banner generation.")
            return@doLast
        }

        logger.lifecycle("Using python interpreter: $pythonCmd")

        val scriptFile = file("../tools/generate_banner_png.py")
        if (!scriptFile.exists()) {
            logger.lifecycle("Banner generation script not found at ${scriptFile.absolutePath}; skipping.")
            return@doLast
        }

        // Run the python script using ProcessBuilder directly to avoid Gradle treating non-zero exit as exception
        try {
            val pb = ProcessBuilder(listOf(pythonCmd, scriptFile.absolutePath))
            pb.redirectErrorStream(false)
            val process = pb.start()

            val stdoutBytes = process.inputStream.readAllBytes()
            val stderrBytes = process.errorStream.readAllBytes()

            // wait with timeout
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                logger.warn("Banner generation timed out after 60s. Process destroyed.")
                return@doLast
            }

            val exitCode = process.exitValue()
            val stdout = String(stdoutBytes)
            val stderr = String(stderrBytes)

            if (exitCode != 0) {
                logger.warn("Python script exited with code $exitCode")
                logger.warn("Interpreter used: $pythonCmd")
                logger.warn("Script: ${scriptFile.absolutePath}")
                if (stdout.isNotBlank()) logger.warn("stdout:\n$stdout")
                if (stderr.isNotBlank()) logger.warn("stderr:\n$stderr")
                logger.warn("Continuing build despite banner script failure. If you want the build to fail on script errors, adjust the task.")
            } else {
                logger.lifecycle("Banner generation succeeded. Output:\n$stdout")
            }
        } catch (e: Exception) {
            logger.warn("Exception while running banner generation script: ${e.message}")
            // don't fail the build; just warn
        }
    }
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

    // compute a stable base version from git and use flavor offsets to guarantee uniqueness
    val baseVersion = gitCommitCount()

    defaultConfig {
        applicationId = "com.fiospace.bitcointicker"
        minSdk = 26
        targetSdk = 35
        // keep a readable base versionCode for simple debug builds
        versionCode = baseVersion
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
    // Publish app bundles by default (AAB)
    defaultToAppBundles.set(true)
    // Default track for automated publishing — change to `internal`/`beta` as needed
    track.set("production")

    // By default the plugin will publish the `release` variant/bundle. If you need to
    // publish a specific variant, you can set `variantToPublish` (example commented out):
    // variantToPublish.set("release")

    // Optional: configure release status (e.g. "draft", "inProgress", "completed")
    // releaseStatus.set("draft")
}

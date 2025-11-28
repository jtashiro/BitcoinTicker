import java.io.ByteArrayOutputStream

// Helper to run git commands
fun runGitCommand(vararg args: String): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", *args)
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

// Get commit count for versionCode
fun gitCommitCount(): Int = runGitCommand("rev-list", "--count", "HEAD").toInt()

// Get latest tag for versionName (fallback if none)
fun gitTagOrDefault(): String {
    val tag = runGitCommand("describe", "--tags", "--abbrev=0")
    return if (tag.isNotEmpty()) tag.removePrefix("v") else "0.0.0"
}

plugins {
    alias(libs.plugins.android.application)

}

android {
    namespace = "com.fiospace.bitcointicker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fiospace.bitcointicker"
        minSdk = 26
        targetSdk = 35
    //    versionCode = 8
    //    versionName = "1.8"
        // Auto-generated versionCode from commit count
        versionCode = gitCommitCount()

        // Semantic versionName with commit suffix
        //versionName = "1.8.${versionCode}"
        versionName = gitTagOrDefault()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

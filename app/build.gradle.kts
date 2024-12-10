import org.gradle.internal.impldep.bsh.commands.dir

plugins {
    alias(libs.plugins.android.application)

}

android {
    namespace = "com.fiospace.bitcointicker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fiospace.bitcointicker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
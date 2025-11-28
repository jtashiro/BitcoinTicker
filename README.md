BitcoinTicker
==============

A small Android app that shows Bitcoin prices.

This README covers how to build, test, sign and prepare the app for publishing to the Google Play Store.

Prerequisites
-------------
- Android Studio (recommended) or command-line Gradle.
- Android SDK (installed via Android Studio or sdkmanager).
- Java JDK compatible with your Android Gradle Plugin (commonly Temurin/AdoptOpenJDK 17 for recent AGP versions).
- Gradle wrapper (this repo includes `./gradlew`).
- Access to a Google Play Developer account to publish.

Quick contract
-------------
- Input: this repository (root contains `settings.gradle.kts`, `build.gradle.kts`, `app/`).
- Output: debug APK/AAB for testing, signed AAB for Play Store.
- Error modes: missing SDK/JDK, missing signing keystore, mismatched AGP/JDK versions.

Common tasks
------------
Open in Android Studio
- File > Open... > select this project's folder.

Build (local)
-------------
From the project root in a terminal:

# Build debug APK and install on a connected device or emulator
./gradlew assembleDebug
./gradlew installDebug

# Build a release APK
./gradlew assembleRelease

# Build a release Android App Bundle (AAB) — required for Play Store
./gradlew bundleRelease

Run (debug)
-----------
- Use Android Studio Run button (chooses installed emulator/device), or:

./gradlew installDebug
adb shell am start -n com.fiospace.bitcointicker/.MainActivity

Tests & Quality
---------------
# Run unit tests
./gradlew test

# Run instrumentation (on a connected device/emulator)
./gradlew connectedAndroidTest

# Run lint
./gradlew lint

Signing for release
-------------------
Google Play requires a signed App Bundle (AAB) or APK. You can sign with your own upload key and let Play manage app signing.

Create a keystore (example):

keytool -genkeypair -v -keystore release-keystore.jks -alias release -keyalg RSA -keysize 2048 -validity 10000

Store the keystore somewhere safe (do NOT commit it to git).

Configure Gradle (Kotlin DSL snippet for `app/build.gradle.kts`):

// ...existing code...
android {
    // ...existing code...
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_KEYSTORE") ?: "release-keystore.jks")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
// ...existing code...

Recommended: put sensitive values into `~/.gradle/gradle.properties` or CI secret variables, e.g.:

RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...

or pass via environment variables.

Generate a signed AAB (CLI)
--------------------------
# produce a signed bundle using the signing configuration above (Gradle picks values from properties)
./gradlew bundleRelease

The output AAB will be at `app/build/outputs/bundle/release/app-release.aab`.

Publishing to Google Play
-------------------------
1. Open Google Play Console and create a new app if needed.
2. Use the "Internal testing" track first to smoke-test your release.
3. Upload the generated `app-release.aab`.
4. Fill store listing, content rating, privacy policy, and required assets.
5. Submit to the desired track and follow Play Console prompts.

Automating publish (optional)
-----------------------------
- Fastlane supply or the Google Play Developer Publishing API can automate uploads.
- Example: set up `fastlane` and `supply` with service account JSON credentials and call `fastlane supply --aab app-release.aab`.

Troubleshooting
---------------
- "You need to use a Theme.AppCompat theme (or descendant) with this activity." — ensure activities that extend `AppCompatActivity` use an AppCompat-compatible theme (e.g. `Theme.MaterialComponents.DayNight.DarkActionBar` or a theme that inherits from `Theme.AppCompat`). Check `android:theme` in `AndroidManifest.xml` and your `styles.xml`.

- brew sudo prompt on macOS: Homebrew sometimes runs installers that call `sudo` (e.g. installing a cask that needs privileged operations). If the macOS prompt didn't appear, try running the failing command directly in a terminal (e.g. `sudo installer ...`) to see the interactive prompt. Also ensure your user is an administrator.

- JDK mismatch: If Gradle/AGP complains about your JDK version, install the JDK version recommended by the Android Gradle Plugin and set `JAVA_HOME` accordingly, e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.

Useful commands reference
-------------------------
# Clean build
./gradlew clean

# Show available build tasks
./gradlew tasks --all

# Build and sign with a specific keystore if you prefer passing properties on the command line:
./gradlew bundleRelease -PRELEASE_STORE_PASSWORD=... -PRELEASE_KEY_ALIAS=... -PRELEASE_KEY_PASSWORD=...

Notes and next steps
--------------------
- Keep your keystore safe — losing your upload key may require key reset through Play Console.
- Consider adding CI (GitHub Actions) to run lint/tests and produce artifacts.

If you'd like, I can:
- Add a `signingConfigs` example directly into `app/build.gradle.kts` in a safe way (using properties),
- Add a small GitHub Actions workflow to build and optionally upload an internal test AAB,
- Or help you fix the AppCompat theme runtime crash by inspecting `AndroidManifest.xml` and your activity theme.



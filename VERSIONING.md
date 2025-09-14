Versioning Plugin (com.yy.versioning)

Purpose
- Provide versionName/versionCode from CI (env/Gradle -P) with fallback to a local config file for developer builds.

How It Works
- Env first: `VERSION_NAME`, `VERSION_CODE`
- Then Gradle project properties: `-PVERSION_NAME=... -PVERSION_CODE=...`
- Fallback file: `version.properties` in project root with keys `versionName` and `versionCode`.
- Applies values to `android.defaultConfig` when `com.android.application` is applied.

Configuration (optional)
In `app/build.gradle.kts`, you can override defaults:

versioning {
    // Custom file path (defaults to root/version.properties)
    configFile.set(layout.projectDirectory.file("custom/version.properties"))
    // Custom key names
    fileNameKey.set("versionName")
    fileCodeKey.set("versionCode")
    // Custom env var names
    envNameKey.set("VERSION_NAME")
    envCodeKey.set("VERSION_CODE")
}

Examples
- Local dev uses `version.properties`:
  versionName=1.2.3
  versionCode=45

- Jenkins pipeline injects env:
  export VERSION_NAME=2.0.0
  export VERSION_CODE=200
  ./gradlew assembleRelease

- Or pass via Gradle properties:
  ./gradlew assembleRelease -PVERSION_NAME=2.0.0 -PVERSION_CODE=200


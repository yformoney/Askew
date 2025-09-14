Versioning Plugin (com.yy.versioning)

Purpose
- Provide versionName/versionCode from CI (env/Gradle -P) with fallback to a local config file for developer builds.

How It Works
- Env first: `VERSION_NAME`, `VERSION_CODE`, and any custom keys you bind
- Then Gradle project properties: `-P<KEY>=...`
- Fallback file: `version.properties` in project root; keys are the same as your bindings (e.g., `API_BASE_URL=https://...`)
- The plugin can map resolved values to:
  - Android defaultConfig versionName/versionCode
  - Manifest placeholders
  - Res values (`resValue`)
  - BuildConfig fields
  - Gradle `extra` properties

Configuration (app/build.gradle.kts)

plugins {
    id("com.yy.versioning")
}

versioning {
    // Optional: custom file path (defaults to root/version.properties)
    // configFile.set(layout.projectDirectory.file("ci/config.properties"))

    // Optional: override version keys
    // envNameKey.set("VERSION_NAME")
    // envCodeKey.set("VERSION_CODE")
    // fileNameKey.set("versionName")
    // fileCodeKey.set("versionCode")

    // Bind any custom keys you need
    manifestPlaceholder(name = "appAuthRedirectScheme", fromKey = "APP_AUTH_REDIRECT_SCHEME")

    // Will become <string name="api_env">...</string>
    resValue(type = "string", name = "api_env", fromKey = "API_ENV")

    // Will generate BuildConfig.API_BASE_URL = "..."
    buildConfigField(type = "String", name = "API_BASE_URL", fromKey = "API_BASE_URL")

    // Expose to Gradle as extra["channel"]
    extra(name = "channel", fromKey = "CI_CHANNEL")
}

Examples
- Local dev uses `version.properties`:
  versionName=1.2.3
  versionCode=45
  API_BASE_URL=https://dev.example.com
  APP_AUTH_REDIRECT_SCHEME=com.yy.askew

- Jenkins pipeline injects env:
  export VERSION_NAME=2.0.0
  export VERSION_CODE=200
  export API_BASE_URL=https://api.example.com
  export CI_CHANNEL=play
  ./gradlew :app:assembleRelease

- Or pass via Gradle properties (on PowerShell, quote the args):
  ./gradlew :app:assembleRelease "-PVERSION_NAME=2.0.0" "-PVERSION_CODE=200" "-PAPI_BASE_URL=https://api.example.com"

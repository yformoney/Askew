Params Plugin (com.yy.params)

Purpose
- Generic parameter fetcher for CI/local: prefers environment variables, then a config file. Supports typed access and mapping into Android build configuration.

Precedence
1) Environment variables
2) Config file: root `version.properties` by default (customizable)
3) Optional last fallback: `-P<KEY>=...`

What it can map
- defaultConfig: versionName/versionCode (from `VERSION_NAME`/`VERSION_CODE` or file keys `versionName`/`versionCode`)
- Manifest placeholders
- resValue entries (string/integer/bool/etc.)
- BuildConfig fields (String/int/boolean/...)
- Gradle `extra` properties

Apply (app/build.gradle.kts)
plugins {
    id("com.yy.params")
}

params {
    // Optional: custom file path (defaults to root/version.properties)
    // configFile.set(layout.projectDirectory.file("ci/config.properties"))

    // Bind custom keys
    manifestPlaceholder(name = "appAuthRedirectScheme", fromKey = "APP_AUTH_REDIRECT_SCHEME")
    resValue(type = "string", name = "api_env", fromKey = "API_ENV")
    buildConfigField(type = "String", name = "API_BASE_URL", fromKey = "API_BASE_URL")
    buildConfigField(type = "int", name = "FEATURE_FLAG_BITS", fromKey = "FEATURE_FLAG_BITS")
    buildConfigField(type = "boolean", name = "ENABLE_ANALYTICS", fromKey = "ENABLE_ANALYTICS")
    extra(name = "releaseChannel", fromKey = "CI_CHANNEL")
}

Typed getters (optional)
- params.getString("KEY", default)
- params.getInt("KEY", default)
- params.getBoolean("KEY", default)
- params.getLong("KEY", default)
- params.getDouble("KEY", default)

Examples
- Local file `version.properties`:
  versionName=1.2.3
  versionCode=123
  API_BASE_URL=https://dev.example.com
  APP_AUTH_REDIRECT_SCHEME=com.yy.askew
  ENABLE_ANALYTICS=true
  FEATURE_FLAG_BITS=7

- Jenkins env:
  export VERSION_NAME=2.0.0
  export VERSION_CODE=200
  export API_BASE_URL=https://api.example.com
  export CI_CHANNEL=play
  ./gradlew :app:assembleRelease

- Optional Gradle properties fallback (quote on PowerShell):
  ./gradlew :app:assembleRelease "-PAPI_BASE_URL=https://api.example.com"


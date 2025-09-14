import com.yy.askew.params.ParamsExtension

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    id("com.yy.params")
}

val paramsExt = extensions.getByType<ParamsExtension>()
paramsExt.configFile.set(rootProject.layout.projectDirectory.file("config.properties"))

android {
    namespace = "com.yy.askew"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yy.askew"
        minSdk = 24
        targetSdk = 35
        // 使用 params 插件按优先级读取版本�?名（环境变量 > 配置文件�?        versionName = paramsExt.getString("VERSION_NAME", "1.0.0") ?: "1.0.0"
        versionCode = paramsExt.getInt("VERSION_CODE", 1) ?: 1
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // AppAuth redirect scheme configuration
        // This value can be overridden by params plugin bindings below
        manifestPlaceholders["appAuthRedirectScheme"] = "com.yy.askew"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    // Compose Compiler is configured via Kotlin Compose plugin (Kotlin 2.0+)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // 使用最新稳定版�?
    implementation(libs.androidx.material3)
//    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose) // M3 核心�?


    implementation("com.squareup.okhttp3:okhttp:4.10.0")  // 网络请求
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0") // 日志
    implementation("com.google.code.gson:gson:2.9.0")  // JSON处理
    implementation("net.openid:appauth:0.11.1")  // OAuth2客户端库
    implementation("androidx.security:security-crypto:1.1.0-alpha06")  // 加密存储
    
    // 高德地图SDK - 使用统一版本避免冲突
    implementation("com.amap.api:map2d:6.0.0")  // 2D地图
    implementation("com.amap.api:location:6.4.3")  // 定位
    implementation("com.amap.api:search:9.5.0")  // 搜索


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Unit test helpers
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// Parameter bindings and typed fetches




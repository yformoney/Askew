plugins {
    `kotlin-dsl`
}

repositories {
    // Keep mirrors minimal; use Google + Maven Central for reliability
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // For typed access to Android DSL
    compileOnly("com.android.tools.build:gradle:8.6.1")
}

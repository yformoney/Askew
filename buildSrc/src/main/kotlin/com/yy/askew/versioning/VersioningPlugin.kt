package com.yy.askew.versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty
import java.util.Properties
import javax.inject.Inject

open class VersioningExtension @Inject constructor(objects: ObjectFactory) {
    val configFile: RegularFileProperty = objects.fileProperty()
    val envNameKey: Property<String> = objects.property(String::class.java)
    val envCodeKey: Property<String> = objects.property(String::class.java)
    val fileNameKey: Property<String> = objects.property(String::class.java)
    val fileCodeKey: Property<String> = objects.property(String::class.java)
}

class VersioningPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("versioning", VersioningExtension::class.java)

        // Sensible defaults
        ext.envNameKey.convention("VERSION_NAME")
        ext.envCodeKey.convention("VERSION_CODE")
        ext.fileNameKey.convention("versionName")
        ext.fileCodeKey.convention("versionCode")
        ext.configFile.convention(project.rootProject.layout.projectDirectory.file("version.properties"))

        // Only configure when Android Application plugin is present
        project.plugins.withId("com.android.application") {
            val values = resolveVersions(project, ext)
            setAndroidVersionsReflectively(project, values)
        }
    }

    private data class Versions(val name: String?, val code: Int?)

    private fun resolveVersions(project: Project, ext: VersioningExtension): Versions {
        val logger = project.logger

        val envNameKey = ext.envNameKey.get()
        val envCodeKey = ext.envCodeKey.get()

        // 1) Environment variables (Jenkins-friendly)
        val envName = System.getenv(envNameKey)?.takeIf { it.isNotBlank() }
        val envCodeStr = System.getenv(envCodeKey)?.takeIf { it.isNotBlank() }

        // 2) Gradle -P project properties fallback (also CI-friendly)
        val propName = (project.findProperty(envNameKey) as String?)?.takeIf { it.isNotBlank() }
        val propCodeStr = (project.findProperty(envCodeKey) as String?)?.takeIf { it.isNotBlank() }

        // 3) version.properties file fallback (local-dev friendly)
        val file = ext.configFile.orNull?.asFile
        val fileProps = Properties()
        if (file != null && file.exists()) {
            file.inputStream().use { fileProps.load(it) }
        }
        val fileName = fileProps.getProperty(ext.fileNameKey.get())?.takeIf { it.isNotBlank() }
        val fileCodeStr = fileProps.getProperty(ext.fileCodeKey.get())?.takeIf { it.isNotBlank() }

        val resolvedName = envName ?: propName ?: fileName
        val resolvedCode = (envCodeStr ?: propCodeStr ?: fileCodeStr)?.toIntOrNull()

        logger.lifecycle("[versioning] Resolved versionName={}, versionCode={} (env={}, file={})",
            resolvedName, resolvedCode, (envName != null || envCodeStr != null), (file != null && file.exists()))

        return Versions(resolvedName, resolvedCode)
    }

    private fun setAndroidVersionsReflectively(project: Project, versions: Versions) {
        val android = project.extensions.findByName("android") ?: return
        val defaultConfig = android.javaClass.methods
            .firstOrNull { it.name == "getDefaultConfig" && it.parameterCount == 0 }
            ?.invoke(android) ?: return

        // Debug: list setter methods once
        try {
            val names = defaultConfig.javaClass.methods.map { it.toString() }.sorted().joinToString("\n  ")
            project.logger.info("[versioning] DefaultConfig methods:\n  $names")
        } catch (_: Throwable) {}

        // Set versionName if provided
        versions.name?.let { name ->
            try {
                val m = defaultConfig.javaClass.methods.firstOrNull { it.name == "setVersionName" && it.parameterCount == 1 }
                if (m != null) {
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, name)
                    project.logger.lifecycle("[versioning] Applied versionName={}", name)
                } else {
                    project.logger.warn("[versioning] setVersionName method not found on DefaultConfig")
                }
            } catch (e: Throwable) {
                project.logger.warn("[versioning] Failed to set versionName via reflection: ${e.message}")
            }
        }

        // Set versionCode if provided
        versions.code?.let { code ->
            try {
                val m = defaultConfig.javaClass.methods.firstOrNull { it.name == "setVersionCode" && it.parameterCount == 1 }
                if (m != null) {
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, code)
                    project.logger.lifecycle("[versioning] Applied versionCode={}", code)
                } else {
                    project.logger.warn("[versioning] setVersionCode method not found on DefaultConfig")
                }
            } catch (e: Throwable) {
                project.logger.warn("[versioning] Failed to set versionCode via reflection: ${e.message}")
            }
        }
    }
}

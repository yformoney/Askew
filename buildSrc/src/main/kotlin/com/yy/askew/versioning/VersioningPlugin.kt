package com.yy.askew.versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty
import java.util.Properties
import javax.inject.Inject

internal data class ManifestBinding(val name: String, val fromKey: String)
internal data class ResValueBinding(val type: String, val name: String, val fromKey: String)
internal data class BuildConfigBinding(val type: String, val name: String, val fromKey: String)
internal data class ExtraBinding(val name: String, val fromKey: String)

open class VersioningExtension @Inject constructor(objects: ObjectFactory) {
    val configFile: RegularFileProperty = objects.fileProperty()
    val envNameKey: Property<String> = objects.property(String::class.java)
    val envCodeKey: Property<String> = objects.property(String::class.java)
    val fileNameKey: Property<String> = objects.property(String::class.java)
    val fileCodeKey: Property<String> = objects.property(String::class.java)

    // General-purpose bindings (key -> various sinks)
    internal val manifestBindings: MutableList<ManifestBinding> = mutableListOf()
    internal val resValueBindings: MutableList<ResValueBinding> = mutableListOf()
    internal val buildConfigBindings: MutableList<BuildConfigBinding> = mutableListOf()
    internal val extraBindings: MutableList<ExtraBinding> = mutableListOf()

    fun manifestPlaceholder(name: String, fromKey: String) {
        manifestBindings += ManifestBinding(name, fromKey)
    }

    fun resValue(type: String, name: String, fromKey: String) {
        resValueBindings += ResValueBinding(type, name, fromKey)
    }

    fun buildConfigField(type: String, name: String, fromKey: String) {
        buildConfigBindings += BuildConfigBinding(type, name, fromKey)
    }

    fun extra(name: String, fromKey: String) {
        extraBindings += ExtraBinding(name, fromKey)
    }
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
            setAndroidVersionsReflectively(project, values, ext)
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

        logger.lifecycle(
            "[versioning] Resolved versionName={}, versionCode={} (env={}, file={})",
            resolvedName,
            resolvedCode,
            (envName != null || envCodeStr != null),
            (file != null && file.exists())
        )

        return Versions(resolvedName, resolvedCode)
    }

    private fun setAndroidVersionsReflectively(project: Project, versions: Versions, ext: VersioningExtension) {
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

        // Apply generic bindings
        val resolved = resolveAll(project, ext)
        applyManifestBindings(project, defaultConfig, ext.manifestBindings, resolved)
        applyResValueBindings(project, defaultConfig, ext.resValueBindings, resolved)
        applyBuildConfigBindings(project, defaultConfig, ext.buildConfigBindings, resolved)
        applyExtraBindings(project, ext.extraBindings, resolved)
    }

    private fun resolveAll(project: Project, ext: VersioningExtension): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val file = ext.configFile.orNull?.asFile
        val props = Properties()
        if (file != null && file.exists()) file.inputStream().use { props.load(it) }

        val keys = mutableSetOf<String>()
        keys += ext.manifestBindings.map { it.fromKey }
        keys += ext.resValueBindings.map { it.fromKey }
        keys += ext.buildConfigBindings.map { it.fromKey }
        keys += ext.extraBindings.map { it.fromKey }
        // include version keys as well
        keys += ext.envNameKey.orNull ?: "VERSION_NAME"
        keys += ext.envCodeKey.orNull ?: "VERSION_CODE"

        for (k in keys) {
            val env = System.getenv(k)
            val prop = project.findProperty(k) as String?
            val fileVal = props.getProperty(k)
            val v = env?.takeIf { it.isNotBlank() } ?: prop?.takeIf { it.isNotBlank() } ?: fileVal?.takeIf { it.isNotBlank() }
            if (v != null) map[k] = v
        }
        return map
    }

    private fun applyManifestBindings(project: Project, defaultConfig: Any, binds: List<ManifestBinding>, values: Map<String, String>) {
        if (binds.isEmpty()) return
        try {
            val getMap = defaultConfig.javaClass.methods.firstOrNull { it.name == "getManifestPlaceholders" && it.parameterCount == 0 }
            val setMap = defaultConfig.javaClass.methods.firstOrNull { it.name == "setManifestPlaceholders" && it.parameterCount == 1 }
            val current = (getMap?.invoke(defaultConfig) as? MutableMap<String, Any>) ?: mutableMapOf()
            for (b in binds) {
                val v = values[b.fromKey]
                if (v != null) {
                    current[b.name] = v
                    project.logger.lifecycle("[versioning] manifestPlaceholder {} = {}", b.name, v)
                }
            }
            if (setMap != null) {
                try { setMap.isAccessible = true } catch (_: Throwable) {}
                setMap.invoke(defaultConfig, current)
            }
        } catch (e: Throwable) {
            project.logger.warn("[versioning] Failed to apply manifest placeholders: ${e.message}")
        }
    }

    private fun applyResValueBindings(project: Project, defaultConfig: Any, binds: List<ResValueBinding>, values: Map<String, String>) {
        if (binds.isEmpty()) return
        val m = defaultConfig.javaClass.methods.firstOrNull { it.name == "resValue" && it.parameterCount == 3 }
        for (b in binds) {
            val v = values[b.fromKey]
            if (v != null && m != null) {
                try {
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, b.type, b.name, v)
                    project.logger.lifecycle("[versioning] resValue {} ({}): {}", b.name, b.type, v)
                } catch (e: Throwable) {
                    project.logger.warn("[versioning] Failed to set resValue {}: ${e.message}", b.name)
                }
            }
        }
    }

    private fun applyBuildConfigBindings(project: Project, defaultConfig: Any, binds: List<BuildConfigBinding>, values: Map<String, String>) {
        if (binds.isEmpty()) return
        val m = defaultConfig.javaClass.methods.firstOrNull { it.name == "buildConfigField" && it.parameterCount == 3 }
        for (b in binds) {
            val raw = values[b.fromKey] ?: continue
            val valueArg = when (b.type) {
                "String" -> if (raw.startsWith("\"") && raw.endsWith("\"")) raw else "\"$raw\""
                else -> raw
            }
            if (m != null) {
                try {
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, b.type, b.name, valueArg)
                    project.logger.lifecycle("[versioning] buildConfigField {} ({}): {}", b.name, b.type, valueArg)
                } catch (e: Throwable) {
                    project.logger.warn("[versioning] Failed to set buildConfigField {}: ${e.message}", b.name)
                }
            }
        }
    }

    private fun applyExtraBindings(project: Project, binds: List<ExtraBinding>, values: Map<String, String>) {
        if (binds.isEmpty()) return
        val extra = project.extensions.extraProperties
        for (b in binds) {
            val v = values[b.fromKey]
            if (v != null) {
                extra.set(b.name, v)
                project.logger.lifecycle("[versioning] extra {} = {}", b.name, v)
            }
        }
    }
}

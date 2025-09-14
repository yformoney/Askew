package com.yy.askew.params

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject
import java.util.Properties

open class ParamsExtension @Inject constructor(objects: ObjectFactory) {
    val configFile: RegularFileProperty = objects.fileProperty()

    // Default keys for versioning
    val envNameKey: Property<String> = objects.property(String::class.java)
    val envCodeKey: Property<String> = objects.property(String::class.java)
    val fileNameKey: Property<String> = objects.property(String::class.java)
    val fileCodeKey: Property<String> = objects.property(String::class.java)

    internal val manifestBindings: MutableList<ManifestBinding> = mutableListOf()
    internal val resValueBindings: MutableList<ResValueBinding> = mutableListOf()
    internal val buildConfigBindings: MutableList<BuildConfigBinding> = mutableListOf()
    internal val extraBindings: MutableList<ExtraBinding> = mutableListOf()

    // General getters for use in build scripts
    fun getString(key: String, default: String? = null): String? = resolver(key) ?: default
    fun getInt(key: String, default: Int? = null): Int? = resolver(key)?.toIntOrNull() ?: default
    fun getLong(key: String, default: Long? = null): Long? = resolver(key)?.toLongOrNull() ?: default
    fun getBoolean(key: String, default: Boolean? = null): Boolean? = resolver(key)?.let { toBoolean(it) } ?: default
    fun getDouble(key: String, default: Double? = null): Double? = resolver(key)?.toDoubleOrNull() ?: default

    fun manifestPlaceholder(name: String, fromKey: String) { manifestBindings += ManifestBinding(name, fromKey) }
    fun resValue(type: String, name: String, fromKey: String) { resValueBindings += ResValueBinding(type, name, fromKey) }
    fun buildConfigField(type: String, name: String, fromKey: String) { buildConfigBindings += BuildConfigBinding(type, name, fromKey) }
    fun extra(name: String, fromKey: String) { extraBindings += ExtraBinding(name, fromKey) }

    // Internal resolver instance bound at apply()
    internal lateinit var resolver: (String) -> String?
}

private fun toBoolean(v: String): Boolean? = when (v.trim().lowercase()) {
    "true", "1", "yes", "y", "on" -> true
    "false", "0", "no", "n", "off" -> false
    else -> null
}

class ParamsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("params", ParamsExtension::class.java)

        // Defaults
        ext.envNameKey.convention("VERSION_NAME")
        ext.envCodeKey.convention("VERSION_CODE")
        ext.fileNameKey.convention("versionName")
        ext.fileCodeKey.convention("versionCode")
        ext.configFile.convention(project.rootProject.layout.projectDirectory.file("version.properties"))

        // Wire resolver with precedence: env > file (optionally -P as last fallback if present)
        val vr = ValueResolver(project) { ext.configFile.orNull?.asFile }
        ext.resolver = { key -> vr.resolve(key) }

        project.plugins.withId("com.android.application") {
            // Apply versions first
            val versionName = ext.resolver(ext.envNameKey.get()) ?: ext.resolver(ext.fileNameKey.get())
            val versionCode = ext.resolver(ext.envCodeKey.get())?.toIntOrNull()
                ?: ext.resolver(ext.fileCodeKey.get())?.toIntOrNull()

            setAndroidConfig(project, versionName, versionCode, ext)
        }
    }

    private fun setAndroidConfig(project: Project, versionName: String?, versionCode: Int?, ext: ParamsExtension) {
        val android = project.extensions.findByName("android") ?: return
        val defaultConfig = android.javaClass.methods.firstOrNull { it.name == "getDefaultConfig" && it.parameterCount == 0 }?.invoke(android) ?: return

        // versionName
        versionName?.let { name ->
            try {
                defaultConfig.javaClass.methods.firstOrNull { it.name == "setVersionName" && it.parameterCount == 1 }?.let { m ->
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, name)
                    project.logger.lifecycle("[params] versionName={}", name)
                }
            } catch (e: Throwable) {
                project.logger.warn("[params] Failed to set versionName: ${e.message}")
            }
        }

        // versionCode
        versionCode?.let { code ->
            try {
                defaultConfig.javaClass.methods.firstOrNull { it.name == "setVersionCode" && it.parameterCount == 1 }?.let { m ->
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, code)
                    project.logger.lifecycle("[params] versionCode={}", code)
                }
            } catch (e: Throwable) {
                project.logger.warn("[params] Failed to set versionCode: ${e.message}")
            }
        }

        // Resolve and apply bindings
        val values = resolveAll(project, ext)
        applyManifestBindings(project, defaultConfig, ext.manifestBindings, values)
        applyResValueBindings(project, defaultConfig, ext.resValueBindings, values)
        applyBuildConfigBindings(project, defaultConfig, ext.buildConfigBindings, values)
        applyExtraBindings(project, ext.extraBindings, values)
    }

    private fun resolveAll(project: Project, ext: ParamsExtension): Map<String, String> {
        val keys = mutableSetOf<String>()
        keys += ext.manifestBindings.map { it.fromKey }
        keys += ext.resValueBindings.map { it.fromKey }
        keys += ext.buildConfigBindings.map { it.fromKey }
        keys += ext.extraBindings.map { it.fromKey }
        keys += setOf(ext.envNameKey.get(), ext.envCodeKey.get(), ext.fileNameKey.get(), ext.fileCodeKey.get())

        val map = mutableMapOf<String, String>()
        for (k in keys) {
            ext.resolver(k)?.let { map[k] = it }
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
                values[b.fromKey]?.let { v ->
                    current[b.name] = v
                    project.logger.lifecycle("[params] manifestPlaceholder {} = {}", b.name, v)
                }
            }
            if (setMap != null) {
                try { setMap.isAccessible = true } catch (_: Throwable) {}
                setMap.invoke(defaultConfig, current)
            }
        } catch (e: Throwable) {
            project.logger.warn("[params] Failed to apply manifest placeholders: ${e.message}")
        }
    }

    private fun applyResValueBindings(project: Project, defaultConfig: Any, binds: List<ResValueBinding>, values: Map<String, String>) {
        if (binds.isEmpty()) return
        val m = defaultConfig.javaClass.methods.firstOrNull { it.name == "resValue" && it.parameterCount == 3 }
        for (b in binds) {
            val raw = values[b.fromKey] ?: continue
            val valueArg = when (b.type.lowercase()) {
                "string" -> raw
                "bool", "boolean" -> if (toBoolean(raw) == true) "true" else "false"
                else -> raw // integer, color, etc. pass through
            }
            if (m != null) {
                try {
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, b.type, b.name, valueArg)
                    project.logger.lifecycle("[params] resValue {} ({}): {}", b.name, b.type, valueArg)
                } catch (e: Throwable) {
                    project.logger.warn("[params] Failed to set resValue {}: ${e.message}", b.name)
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
                "boolean", "Boolean" -> if (toBoolean(raw) == true) "true" else "false"
                else -> raw // int/long/double/etc. should be a literal
            }
            if (m != null) {
                try {
                    try { m.isAccessible = true } catch (_: Throwable) {}
                    m.invoke(defaultConfig, b.type, b.name, valueArg)
                    project.logger.lifecycle("[params] buildConfigField {} ({}): {}", b.name, b.type, valueArg)
                } catch (e: Throwable) {
                    project.logger.warn("[params] Failed to set buildConfigField {}: ${e.message}", b.name)
                }
            }
        }
    }

    private fun applyExtraBindings(project: Project, binds: List<ExtraBinding>, values: Map<String, String>) {
        if (binds.isEmpty()) return
        val extra = project.extensions.extraProperties
        for (b in binds) {
            values[b.fromKey]?.let { v ->
                extra.set(b.name, v)
                project.logger.lifecycle("[params] extra {} = {}", b.name, v)
            }
        }
    }
}

private class ValueResolver(private val project: Project, private val fileProvider: () -> java.io.File?) {
    private var fileProps: Properties? = null
    private fun props(): Properties {
        if (fileProps == null) {
            val p = Properties()
            val f = fileProvider()
            if (f != null && f.exists()) f.inputStream().use { p.load(it) }
            fileProps = p
        }
        return fileProps!!
    }

    fun resolve(key: String): String? {
        // Env first
        val env = System.getenv(key)
        if (!env.isNullOrBlank()) return env
        // Then file
        val fileVal = props().getProperty(key)
        if (!fileVal.isNullOrBlank()) return fileVal
        // Optionally, accept -P as last fallback if explicitly provided
        val prop = project.findProperty(key) as String?
        if (!prop.isNullOrBlank()) return prop
        return null
    }
}

internal data class ManifestBinding(val name: String, val fromKey: String)
internal data class ResValueBinding(val type: String, val name: String, val fromKey: String)
internal data class BuildConfigBinding(val type: String, val name: String, val fromKey: String)
internal data class ExtraBinding(val name: String, val fromKey: String)

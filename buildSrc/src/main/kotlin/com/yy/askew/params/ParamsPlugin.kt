package com.yy.askew.params

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject
import java.util.Properties

open class ParamsExtension @Inject constructor(objects: ObjectFactory) {
    val configFile: RegularFileProperty = objects.fileProperty()

    // Typed getters for use in build scripts
    internal lateinit var resolver: (String) -> String?
    fun getString(key: String, default: String? = null): String? = resolver(key) ?: default
    fun getInt(key: String, default: Int? = null): Int? = resolver(key)?.toIntOrNull() ?: default
    fun getLong(key: String, default: Long? = null): Long? = resolver(key)?.toLongOrNull() ?: default
    fun getBoolean(key: String, default: Boolean? = null): Boolean? = resolver(key)?.let { toBoolean(it) } ?: default
    fun getDouble(key: String, default: Double? = null): Double? = resolver(key)?.toDoubleOrNull() ?: default
}

private fun toBoolean(v: String): Boolean? = when (v.trim().lowercase()) {
    "true", "1", "yes", "y", "on" -> true
    "false", "0", "no", "n", "off" -> false
    else -> null
}

class ParamsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("params", ParamsExtension::class.java)
        // Default config file path
        ext.configFile.convention(project.rootProject.layout.projectDirectory.file("version.properties"))

        // Wire resolver with precedence: env > file
        val vr = ValueResolver { ext.configFile.orNull?.asFile }
        ext.resolver = { key -> vr.resolve(key) }
    }
}

private class ValueResolver(private val fileProvider: () -> java.io.File?) {
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
        // Env first, then file
        val env = System.getenv(key)
        if (!env.isNullOrBlank()) return env
        val fileVal = props().getProperty(key)
        if (!fileVal.isNullOrBlank()) return fileVal
        return null
    }
}

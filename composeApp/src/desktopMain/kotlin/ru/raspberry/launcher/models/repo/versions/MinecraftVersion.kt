package ru.raspberry.launcher.models.repo.versions

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.exceptions.MinecraftException
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.repo.Argument
import ru.raspberry.launcher.models.repo.ArgumentType
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.models.repo.Rule
import ru.raspberry.launcher.models.repo.assets.AssetIndex
import ru.raspberry.launcher.models.repo.java.JavaVersion
import ru.raspberry.launcher.models.repo.library.Library
import ru.raspberry.launcher.models.repo.library.LibraryReplaceList
import ru.raspberry.launcher.service.LauncherServiceV1
import java.io.File
import java.util.regex.Pattern

private val libDataExtractor = "([^:]+?:[^:]+?):([^:]+)(?::([^:]+))?".toRegex()

@Serializable
data class MinecraftVersion(
    val id: String,
    val type: VersionType,
    var mainClass: String,
    var libraries: List<Library> = emptyList(),
    var downloads: Map<String, DownloadInfo> = emptyMap(),
    var assetIndex: AssetIndex? = null,
    var assets: String? = null,
    var arguments: Map<ArgumentType, List<Argument>> = emptyMap(),
    var javaVersion: JavaVersion? = null,
    var deleteEntries: List<String> = emptyList(),
    var rules: List<Rule> = emptyList(),
    var inheritsFrom: String? = null,
    @Transient
    val proceededFor: MutableSet<String?> = mutableSetOf(),
) {
    fun activeArguments(
        env: ArgumentType,
        os: OS,
        features: Map<String, Boolean> = emptyMap()
    ): List<String>? {
        return arguments[env]?.filter { it.isActive(os, features) }?.map { it.value }
    }

    fun activeLibraries(
        os: OS,
        features: Map<String, Boolean> = emptyMap()
    ): List<Library> {
        return libraries.filter { it.isActive(os, features) }
    }
    fun getClassPath(os: OS, features: Map<String, Boolean>, base: File): Collection<File> {
        val classPath = activeLibraries(os, features)
            .filter { it.name.split(":").size != 4 }
            .map { File(base, "libraries/${it.artifactPath}") }
            .toMutableList()
        classPath.add(getJarFile(base))
        return classPath
    }

    fun getJarFile(base: File?): File {
        return File(base, "versions/${id}/${id}.jar")
    }

    suspend fun<S> inherit(
        service: LauncherServiceV1<S>,
        client: HttpClient,
        json: Json
    ) {
        if (inheritsFrom == null) return
        val data = service.getMinecraftData(inheritsFrom!!)
        if (data == null) {
            throw MinecraftException("Failed to load Minecraft data for minecraft $inheritsFrom!")
        }
        val response = client.get(data.url)
        if (!response.status.isSuccess()) {
            throw MinecraftException("Failed to load Minecraft data for minecraft $inheritsFrom!")
        }
        val parent: MinecraftVersion = json.decodeFromString(response.bodyAsText())
        parent.inherit(service, client, json)

        cleanLibraries(parent.libraries + libraries)
        downloads = parent.downloads + downloads
        assetIndex = assetIndex ?: parent.assetIndex
        assets = assets ?: parent.assets
        val args = mutableMapOf<ArgumentType, List<Argument>>()
        ArgumentType.entries.forEach { type ->
            args[type] = (parent.arguments[type] ?: emptyList()) + (arguments[type] ?: emptyList())
        }
        arguments = args
        javaVersion = javaVersion ?: parent.javaVersion
        deleteEntries = parent.deleteEntries + deleteEntries
        rules = parent.rules + rules
        proceededFor.addAll(parent.proceededFor)
        inheritsFrom = null
    }
    private fun cleanLibraries(libsRaw: List<Library>) {
        val libs = mutableListOf<Triple<Library, String, List<Int>>>()
        libsRaw.forEach { library ->
            val match = libDataExtractor.find(library.name)
            if (match == null)
                throw MinecraftException("Invalid library name: ${library.name}")
            val name = if (match.groupValues.size == 3) match.groupValues[1]
            else "${match.groupValues[1]}:${match.groupValues[3]}"
            val version = match.groupValues[2]
                .split("-").first()
                .split('.').mapNotNull { it.toIntOrNull() }
            var add = true
            for (data in libs)
                if (data.second == name) {
                    // Similar library found, check if it is lower version
                    var depth = 0
                    while (data.third.getOrNull(depth) == version.getOrNull(depth)) {
                        depth++
                    }
                    if (data.third.getOrNull(depth) == null) {
                        libs.remove(data) // Parent newer
                        break
                    }
                    if (version.getOrNull(depth) == null) {
                        add = false // Current newer
                        break
                    }
                    if (version[depth] > data.third[depth])
                        libs.remove(data) // Parent newer
                    else
                        add = false // Current newer
                    break
                }
            if (add) libs.add(Triple(library, name, version))
        }
        libraries = libs.map { it.first }
    }
    fun patchFor(type: String, libs: LibraryReplaceList, minecraftArgs: MutableList<String>) {
        patch(type, libs, minecraftArgs)
        patch("patchy", libs, minecraftArgs)
    }
    private fun patch(type: String, libs: LibraryReplaceList, minecraftArgs: MutableList<String>) {
        if (proceededFor.contains(type)) return
        proceededFor.add(type)
        val replaces = libs.libraries[type]?.filter { lib ->
            lib.supports.contains(id) || libraries.stream().anyMatch(lib::replaces)
        }
        if (replaces == null || replaces.isEmpty()) return
        val mutableLibs = mutableListOf<Library>()
        mutableLibs.addAll(libraries)
        replaces.forEach { replace ->
            if (mutableLibs.any { it.name == replace.name })
                return@forEach // Skip if already exists
            println("Processing library replace: ${replace.name}")

            val requiredLibs = mutableListOf<Library>()
            if (replace.requires.isNotEmpty()) {
                requiredLibs.addAll(replace.requires)
                mutableLibs.addAll(this.libraries)
                mutableLibs.forEachIndexed { i, library ->
                    val requiredIterator = requiredLibs.iterator()
                    while (requiredIterator.hasNext()) {
                        val requiredLib = requiredIterator.next()
                        if (library.getPlainName() == requiredLib.getPlainName()) {
                            mutableLibs[i] = requiredLib
                            requiredIterator.remove()
                        }
                    }
                }
                if (requiredLibs.isNotEmpty())
                    mutableLibs.addAll(0, requiredLibs)
            }

            val pattern: Pattern? = replace.pattern()
            if (pattern != null) {
                mutableLibs.forEachIndexed { i, library ->
                    if (pattern.matcher(library.name).matches())
                        mutableLibs[i] = replace.library

                }
            } else mutableLibs.add(0, replace.library)


            if (replace.args.isNotBlank())
                minecraftArgs.add(replace.args)

            if (replace.mainClass != null)
                mainClass = replace.mainClass
        }
        libraries = mutableLibs
    }
}


package ru.raspberry.launcher.models.repo.versions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.exceptions.MinecraftException
import ru.raspberry.launcher.models.Arch
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.repo.Argument
import ru.raspberry.launcher.models.repo.ArgumentType
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.models.repo.java.JavaVersion
import ru.raspberry.launcher.models.repo.Rule
import ru.raspberry.launcher.models.repo.assets.AssetIndex
import ru.raspberry.launcher.models.repo.library.Library
import ru.raspberry.launcher.service.LauncherServiceV1
import ru.raspberry.launcher.tools.jna.JNA
import java.io.File

@Serializable
data class MinecraftVersion(
    val id: String,
    val type: VersionType,
    val mainClass: String,
    var libraries: List<Library> = emptyList(),
    var downloads: Map<String, DownloadInfo> = emptyMap(),
    var assetIndex: AssetIndex? = null,
    var assets: String? = null,
    var arguments: Map<ArgumentType, List<Argument>> = emptyMap(),
    var javaVersion: JavaVersion? = null,
    var deleteEntries: List<String> = emptyList(),
    var rules: List<Rule> = emptyList(),
    var inheritsFrom: String? = null,
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
        libraries = parent.libraries + libraries
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
        inheritsFrom = null
    }
}
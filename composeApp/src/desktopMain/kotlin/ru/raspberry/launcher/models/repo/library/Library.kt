package ru.raspberry.launcher.models.repo.library

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.sha1
import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.Arch
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.models.repo.ExtractRules
import ru.raspberry.launcher.models.repo.Rule
import ru.raspberry.launcher.service.download
import java.io.File
import java.util.*


@Serializable
data class Library(
    val name: String,
    val url: String? = null,
    val exact_url: String? = null,
    val checksum: String? = null,
    val rules: List<Rule> = emptyList(),
    var extract: ExtractRules? = null,
    val downloads: LibraryDownloadInfo? = null,
) {
    fun isActive(
        os: OS,
        features: Map<String, Boolean> = emptyMap()
    ): Boolean {
        return rules.isEmpty() || rules.any { it.isApplicable(os, features) }
    }
    fun isNative(os: OS, arch: Arch): Boolean {
        val split = name.split(":")
        if (split.size != 4) return false
        return split[3] == when (arch) {
            Arch.X64 -> "natives-${os.name.lowercase()}"
            Arch.X86 -> "natives-${os.name.lowercase()}-x86"
            Arch.Arm64 -> "natives-${os.name.lowercase()}-arm64"
            Arch.Arm -> "natives-${os.name.lowercase()}-arm"
        }
    }
    fun getArtifactBaseDir(): String {
        val parts: Array<String?> = name.split(":".toRegex(), limit = 4).toTypedArray()
        require(parts.size >= 3) { "bad library name: $name" }
        return String.format(Locale.ROOT, "%s/%s/%s", parts[0]!!.replace("\\.".toRegex(), "/"), parts[1], parts[2])
    }

    val artifactPath = getArtifactPath(null)

    fun getArtifactPath(classifier: String?): String {
        return String.format(Locale.ROOT, "%s/%s", getArtifactBaseDir(), getArtifactFilename(classifier))
    }

    fun getArtifactFilename(classifier: String?): String {
        val parts: Array<String?> = name.split(":".toRegex(), limit = 4).toTypedArray()
        val result = if (classifier == null) {
            if (parts.size == 4) {
                String.format(Locale.ROOT, "%s-%s-%s.jar", parts[1], parts[2], parts[3])
            } else {
                String.format(Locale.ROOT, "%s-%s.jar", parts[1], parts[2])
            }
        } else {
            String.format(Locale.ROOT, "%s-%s-%s.jar", parts[1], parts[2], classifier)
        }

        return result
    }

    fun getPlainName(): String {
        val split: Array<String?> = name.split(":".toRegex(), limit = 3).toTypedArray()
        require(split.size >= 3) { "bad library: $name" }
        return split[0] + "." + split[1]
    }

    suspend fun tryInstallLibrary(path: File, force: Boolean = false, progress: (Float) -> Unit = { _ -> }) {
        val file = File(path, artifactPath)
        if (!force && file.exists() && file.isFile) {
            progress(1.0f)
            return
        }
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        val response = downloadLibrary(progress)
        if (response == null) {
            throw IllegalStateException("Failed to download library: $name")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Failed to download library: $name, status: ${response.status}")
        }
        file.writeBytes(response.readRawBytes())
    }

    private val lib_roots = listOf(
        "https://libraries.minecraft.net/",
        "https://repo.legacylauncher.ru/libraries/"
    )

    private suspend fun downloadLibrary(progress: (Float) -> Unit = { _ -> }) : HttpResponse? {
        if (downloads != null) {
            val info: DownloadInfo? = downloads.artifact
            if (info != null) {
                if (info.url.startsWith("/"))
                    info.url = "https://repo.legacylauncher.ru/${info.url.substring(1)}"
                return download(info, progress)
            }
        }
        var path: String?

        if (exact_url == null) {
            path = getArtifactPath(null)
            if (url != null) {
                path = if (url.startsWith("/"))
                    "https://repo.legacylauncher.ru/${url.substring(1)}${path}"
                else url + path
            }
            else {
                // Try all libs
                for (root in lib_roots)
                    try {
                        val response = download(DownloadInfo(
                            url = root + path,
                            sha1 = checksum
                        ), progress)
                        if (response != null) return response
                    } catch (_: Exception) { }
                throw IllegalStateException("No URL provided for library: $name")
            }
        } else {
            path = exact_url
        }
        return download(DownloadInfo(
            url = path,
            sha1 = checksum
        ), progress)
    }
}

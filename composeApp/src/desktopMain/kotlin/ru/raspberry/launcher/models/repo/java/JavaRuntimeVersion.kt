package ru.raspberry.launcher.models.repo.java

import io.ktor.client.call.body
import io.ktor.client.statement.readRawBytes
import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.assets.AssetType
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.models.repo.VersionInfo
import ru.raspberry.launcher.service.download
import ru.raspberry.launcher.tools.sha1
import java.io.File

@Serializable
data class JavaRuntimeVersion(
    val manifest: DownloadInfo,
    val version: VersionInfo,
) {
    val major = version.name.substringBefore('.').toIntOrNull() ?: 8
    suspend fun tryInstallJava(path: File, force: Boolean = false, progress: (Float) -> Unit = { _ -> }) {
        val manifest: JavaRuntimeManifest? = download(manifest, { progress(it * 0.1f) })?.body()
        if (manifest == null) {
            throw IllegalStateException("Failed to download Java manifest")
        }
        val size = manifest.files.values.sumOf { it.downloads?.get("raw")?.size ?: 0 }
        var downloaded = 0F
        manifest.files.forEach { (key, value) ->
            when (value.type) {
                AssetType.File -> {
                    val info = value.downloads!!["raw"] ?: return@forEach
                    val file = File(path, key)
                    if (!force && file.exists() && file.isFile
                        && file.length() == info.size
                        && file.readBytes().sha1() == info.sha1) {
                        downloaded += info.size
                        progress(0.1f + (downloaded / size * 0.9f))
                        return@forEach
                    }

                    val oldDownloaded = downloaded
                    val bytes = download(info, {
                        downloaded = oldDownloaded + (it * info.size!!)
                        progress(0.1f + (downloaded / size * 0.9f))
                    })?.readRawBytes()
                    if (bytes == null || bytes.sha1() != info.sha1) {
                        throw IllegalStateException("Failed to download Java file: $key")
                    }
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)
                    if (!file.setExecutable(value.executable, false)) {
                        println("Failed to set executable permission for: $key")
                    }
                }
                AssetType.Directory -> {
                    val dir = File(path, key)
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw IllegalStateException("Failed to create directory: $key")
                    }
                }
            }
        }
    }
}
package ru.raspberry.launcher.models.repo.assets

import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.service.download
import ru.raspberry.launcher.tools.sha1
import java.io.File

private const val ASSETS_REPO = "https://resources.download.minecraft.net/"

@Serializable
data class Asset(
    val hash: String,
    val size: Long,
) {
    suspend fun tryInstall(objectsDir: File, force: Boolean, progress: (Float) -> Unit) {
        val file = File(objectsDir, filename)
        if (file.exists() && !force && file.readBytes().sha1() == hash) {
            progress(1f)
            return
        }
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()

        val response = download(DownloadInfo(
            sha1 = hash,
            url = ASSETS_REPO + filename,
        ), progress)
        if (response == null || !response.status.isSuccess()) {
            throw Exception("Failed to download asset: $hash")
        }
        file.writeBytes(response.readRawBytes())
    }

    val filename = hash.substring(0, 2) + "/" + hash
}
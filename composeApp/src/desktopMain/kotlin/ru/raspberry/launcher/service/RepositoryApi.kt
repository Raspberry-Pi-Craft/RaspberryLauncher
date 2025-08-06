package ru.raspberry.launcher.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.models.repo.java.JavaRuntimeVersion
import ru.raspberry.launcher.models.repo.library.LibraryReplaceList
import ru.raspberry.launcher.models.repo.versions.MinecraftVersionInfo
import ru.raspberry.launcher.models.repo.versions.MinecraftVersionList
import ru.raspberry.launcher.tools.sha1

private const val replacerLibs = "https://repo.legacylauncher.ru/libraries/replace.json"
private const val javaManifestUrl = "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json"

@OptIn(ExperimentalSerializationApi::class)
private val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json{
            ignoreUnknownKeys = true
            decodeEnumsCaseInsensitive = true
        })
    }
    headers {
        set(HttpHeaders.UserAgent, "Raspberry Launcher")
    }
    followRedirects = true
}

suspend fun getJavaList(): Map<String, Map<String, List<JavaRuntimeVersion>>>? {
    val response = client.get(javaManifestUrl)
    if (response.status.isSuccess()) return response.body()
    return null
}
suspend fun getLibraryReplaces(): LibraryReplaceList? {
    val response = client.get(replacerLibs)
    if (response.status.isSuccess()) return response.body()
    return null
}

suspend fun download(
    info: DownloadInfo,
    progress: (Float) -> Unit = { _ -> }
): HttpResponse? {
    val response = client.get(info.url) {
        onDownload { bytes, contentLength ->
            val length = info.size ?: contentLength ?: (1048576 + bytes)
            progress(bytes.toFloat() / length)
        }
    }
    progress(1f) // Ensure progress is complete
    if (!response.status.isSuccess()) return null
    if (info.sha1 != null && response.readRawBytes().sha1() != info.sha1) return null
    return response
}

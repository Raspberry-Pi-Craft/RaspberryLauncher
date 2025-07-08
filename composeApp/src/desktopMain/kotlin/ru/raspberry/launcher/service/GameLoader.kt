package ru.raspberry.launcher.service

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.dtos.ServerFile
import ru.raspberry.launcher.windows.MainWindowScreens
import java.io.File
import java.security.MessageDigest
import java.util.Base64

class GameLoader(
    private val state: WindowData<MainWindowScreens>
) {
    private var value : Float = 0f
    val progress: Float
        get() = value
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json)
        }
        headers {
            set(HttpHeaders.UserAgent, "Raspberry Launcher")
        }
    }

    suspend fun startGame(
        serverName: String,
        errorTitle: MutableState<@Composable () -> Unit>,
        errorText: MutableState<@Composable () -> Unit>
    ) {
        val account = state.activeAccount
        if (account != null)
            state.minecraftService.refreshToken(account = account)

        // Check files
        val files = state.launcherService.getFiles(serverName)
        if (files == null) {
            errorTitle.value = {
                Text(text = "Loading Error!")
            }
            errorText.value = {
                Text(text = "Failed to load game files!")
            }
            value = 0f
            return
        }
        val allSize = files.sumOf { file -> file.size } * 1.1f
        var loaded = 0L
        files.forEach {
            fileData ->
            val path = "${state.config.minecraftPath}/${fileData.path}"
            val file = File(path)
            if (!checkHashes(file, fileData)) {
                // Download new version
                val oldLoaded = loaded
                val response = client.get(
                    fileData.downloadUrl
                ) {
                    onDownload { bytesSentTotal, contentLength ->
                        loaded = oldLoaded + bytesSentTotal
                        value = loaded / allSize
                    }
                }
                if (response.status.isSuccess()) {
                    file.writeBytes(response.readRawBytes())
                    loaded = oldLoaded + fileData.size
                    value = oldLoaded / allSize
                }
                else {
                    errorTitle.value = {
                        Text(text = "Loading Error!")
                    }
                    errorText.value = {
                        Text(text = "Failed to load file ${path}!")
                    }
                    value = 0f
                    return
                }
            }
        }
        // Check address

        // After load
        state.minimize()
        value = 0f
    }

    private fun checkHashes(file: File, fileData: ServerFile): Boolean {
        if (!file.exists()) return false
        return when (fileData.hashType) {
            "Json" -> {
                val jsonHash = Json.decodeFromString<JsonHash>(
                    Base64.getDecoder().decode(fileData.hash).toString(Charsets.UTF_8)
                )
                val paths = pathsFromJson(file.readText())
                val hashPaths = jsonHash.paths.mapNotNull { path ->
                    if (paths[path] != null) path to paths[path] else null
                }.associate { pair -> pair }

                hashByAlgorithm(
                    Json.encodeToString(hashPaths).toByteArray(),
                    fileData.hashAlgorithm
                ) == jsonHash.hash
            }
            else -> {
                hashByAlgorithm(file.readBytes(), fileData.hashAlgorithm) == fileData.hash
            }
        }
    }

    private fun hashByAlgorithm(content: ByteArray, hashAlgorithm: String): String {
        val md = MessageDigest.getInstance( when(hashAlgorithm) {
            "MD5" -> "MD5"
            "SHA1" -> "SHA-1"
            "SHA256" -> "SHA-256"
            else ->  "SHA-512"
        })
        val digest = md.digest(content)
        return digest.joinToString("") { "%02x".format(it) }

    }
}

@Serializable
private data class JsonHash(
    val paths: List<String>,
    val hash: String,
)
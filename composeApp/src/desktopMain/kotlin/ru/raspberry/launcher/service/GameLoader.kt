package ru.raspberry.launcher.service

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.benwoodworth.knbt.*
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.java.JavaData
import ru.raspberry.launcher.models.server.files.ServerFile
import ru.raspberry.launcher.tools.CommandBuilder
import ru.raspberry.launcher.windows.MainWindowScreens
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.math.min

private enum class OsType {
    Windows, Linux, MacOS, Unknown
}

class GameLoader(
    private val state: WindowData<MainWindowScreens>
) {
    val osName = System.getProperty("os.name")

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
        followRedirects = true
    }

    suspend fun start(
        serverName: String,
        errorTitle: MutableState<@Composable () -> Unit>,
        errorText: MutableState<@Composable () -> Unit>
    ) {
        val account = state.activeAccount
        if (account != null)
            state.minecraftService.refreshToken(account = account)

        Files.createDirectories(Path("${state.config.minecraftPath}/servers/$serverName"))
        Path("${state.config.minecraftPath}/servers/$serverName").createDirectories()

        val os = if (osName.startsWith("Windows"))
            OsType.Windows
        else if (osName.startsWith("Linux"))
            OsType.Linux
        else if (osName.startsWith("Mac OS X") || osName.startsWith("MacOS"))
            OsType.MacOS
        else
            OsType.Unknown

        if (!checkFiles(os, serverName, errorTitle, errorText)) return
        val java = checkJava(os, serverName, errorTitle, errorText)
        if (java == null) return
        if (!checkAddress(os, serverName, errorTitle, errorText)) return
        launchGame(os, serverName, java, errorTitle, errorText)
    }

    private fun javaDownloadUrl(java: JavaData?, os: OsType): String? {
        return when (os) {
            OsType.Windows -> java?.windowsDownloadUrl
            OsType.Linux -> java?.linuxDownloadUrl
            OsType.MacOS -> java?.macosDownloadUrl
            else -> null
        }
    }
    private fun javaExecutablePath(java: JavaData?, os: OsType): String? {
        return when (os) {
            OsType.Windows -> java?.windowsExecutablePath
            OsType.Linux -> java?.linuxExecutablePath
            OsType.MacOS -> java?.macosExecutablePath
            else -> null
        }
    }

    private suspend fun checkFiles(
        os: OsType,
        serverName: String,
        errorTitle: MutableState<@Composable () -> Unit>,
        errorText: MutableState<@Composable () -> Unit>
    ) : Boolean {

        val files = state.launcherService.getFiles(serverName)
        if (files == null) {
            errorTitle.value = {
                Text(text = "Loading Error!")
            }
            errorText.value = {
                Text(text = "Failed to load game files!")
            }
            value = 0f
            return false
        }
        val allSize = files.sumOf { file -> file.size } * 1.25f
        var loaded = 0L
        files.forEach {
                fileData ->

            val path = "${state.config.minecraftPath}/servers/$serverName/${fileData.path}"
            val file = File(path)
            file.toPath().parent.createDirectories()
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
                    value = loaded / allSize
                }
                else {
                    errorTitle.value = {
                        Text(text = "Loading Error!")
                    }
                    errorText.value = {
                        Text(text = "Failed to load file ${path}!")
                    }
                    value = 0f
                    return false
                }
            }
        }
        return true
    }

    private suspend fun checkJava(
        os: OsType,
        serverName: String,
        errorTitle: MutableState<@Composable () -> Unit>,
        errorText: MutableState<@Composable () -> Unit>
    ): JavaData? {
        val java = state.launcherService.getJava(serverName)
        val downloadUrl = javaDownloadUrl(java, os)
        if (java == null
            || downloadUrl == null
            || java.name == null
            || java.version == null
            || javaExecutablePath(java, os) == null) {
            errorTitle.value = {
                Text(text = "Loading Error!")
            }
            errorText.value = {
                Text(text = "Failed to load java info!")
            }
            value = 0f
            return null
        }
        val file = File(
            "${state.config.minecraftPath}/java/${java.name}/${java.name}-${java.version}"
        ).canonicalFile
        if (!file.exists()) {
            val response = client.get(
                downloadUrl
            ) {
                onDownload { bytesSentTotal, contentLength ->
                    value = 0.8f + (
                            if (contentLength != null) bytesSentTotal.toFloat() / contentLength
                            else bytesSentTotal / (1048576f + bytesSentTotal)
                            ) * 0.1f
                }
            }
            if (response.status.isSuccess()) {
                ZipInputStream(
                    ByteArrayInputStream(
                        response.readRawBytes()
                    )
                ).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newFile = File(file, entry.name).canonicalFile
                        // Prevent Zip Slip: Ensure the newFile is within the intended directory
                        if (!newFile.toPath().startsWith(file.toPath())) {
                            throw SecurityException("Blocked Zip Slip attack attempt: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile.mkdirs()
                            FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                value = 0.95f
            }
            else {
                errorTitle.value = {
                    Text(text = "Loading Error!")
                }
                errorText.value = {
                    Text(text = "Failed to load file ${file.nameWithoutExtension}!")
                }
                value = 0f
                return null
            }
        }
        return java
    }

    private suspend fun checkAddress(
        os: OsType,
        serverName: String,
        errorTitle: MutableState<@Composable () -> Unit>,
        errorText: MutableState<@Composable () -> Unit>
    ): Boolean {
        val address = state.launcherService.getServerAddress(serverName)
        if (address == null) {
            errorTitle.value = {
                Text(text = "Loading Error!")
            }
            errorText.value = {
                Text(text = "Failed to load server address!")
            }
            value = 0f
            return false
        }
        val serversFile = File("${state.config.minecraftPath}/servers/$serverName/servers.dat")
        serversFile.toPath().parent.createDirectories()
        val nbt = Nbt {
            variant = NbtVariant.Java
            compression = NbtCompression.None
        }

        val compound = if (serversFile.exists()) {
            val old = nbt.decodeFromStream<NbtCompound>(serversFile.inputStream())
            val servers: NbtList<*> = old.get("servers")?.nbtList!!
            if (servers.any { it.nbtCompound.get("ip")?.nbtString?.value == address })
                old
            else {
                buildNbtCompound {
                    putNbtList("servers") {
                        addNbtCompound {
                            put("acceptTextures", true)
                            put("hidden", false)
                            put("ip", address)
                            put("name", serverName)
                        }
                        for (server in servers)
                            add(server.nbtCompound)
                    }
                }
            }
        } else {
            buildNbtCompound {
                putNbtList("servers") {
                    addNbtCompound {
                        put("acceptTextures", true)
                        put("hidden", false)
                        put("ip", address)
                        put("name", serverName)
                    }
                }
            }
        }
        nbt.encodeToStream(compound, serversFile.outputStream())
        return true
    }

    private suspend fun launchGame(
        os: OsType,
        serverName: String,
        java: JavaData,
        errorTitle: MutableState<@Composable () -> Unit>,
        errorText: MutableState<@Composable () -> Unit>
    ) {
        println("Launching game of $serverName")
        val path = File(
            "${state.config.minecraftPath}/java/${java.name}/${java.name}-${java.version}",
            javaExecutablePath(java, os)
        ).canonicalPath

        val command = CommandBuilder("\"$path\"")
            .addArg("-Xms${min(state.config.ram, 2048)}M")
            .addArg("-Xmx${state.config.ram}M")
            .addArg("-Dfile.encoding=UTF-8")
            .addArg("-Dfml.ignoreInvalidMinecraftCertificates=true")
            .addArg("-Dfml.ignorePatchDiscrepancies=true")
            .addArg("-Djava.net.useSystemProxies=true")

        when (os) {
            OsType.Windows -> {
                command.addArg("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump")
            }
            OsType.MacOS -> {
                command.addArg( "-Xdock:name=Minecraft")
            }
            OsType.Linux -> {

            }
            else -> {
                errorTitle.value = {
                    Text(text = "Loading Error!")
                }
                errorText.value = {
                    Text(text = "Unsupported OS: $osName!")
                }
                value = 0f
                return
            }
        }


        command.runCommandWithoutTimeout(
            File(
                "${state.config.minecraftPath}/servers/$serverName"
            )
        )

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
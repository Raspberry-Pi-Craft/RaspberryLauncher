package ru.raspberry.launcher.service

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.sun.jna.platform.win32.WinReg
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.benwoodworth.knbt.add
import net.benwoodworth.knbt.addNbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.decodeFromStream
import net.benwoodworth.knbt.encodeToStream
import net.benwoodworth.knbt.nbtCompound
import net.benwoodworth.knbt.nbtList
import net.benwoodworth.knbt.nbtString
import net.benwoodworth.knbt.put
import net.benwoodworth.knbt.putNbtCompound
import net.benwoodworth.knbt.putNbtList
import okhttp3.internal.format
import ru.raspberry.launcher.AppConfig
import ru.raspberry.launcher.exceptions.MinecraftException
import ru.raspberry.launcher.models.Arch
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.repo.ArgumentType
import ru.raspberry.launcher.models.repo.DownloadInfo
import ru.raspberry.launcher.models.repo.assets.AssetList
import ru.raspberry.launcher.models.repo.versions.MinecraftVersion
import ru.raspberry.launcher.models.repo.versions.VersionType
import ru.raspberry.launcher.models.server.AdvancedServerData
import ru.raspberry.launcher.models.server.files.ServerFile
import ru.raspberry.launcher.tools.FileUtil
import ru.raspberry.launcher.tools.UUIDTypeAdapter
import ru.raspberry.launcher.tools.jna.JNA
import ru.raspberry.launcher.tools.jna.JNAException
import ru.raspberry.launcher.tools.jna.JNAWindows
import ru.raspberry.launcher.tools.pathsFromJson
import ru.raspberry.launcher.tools.runCommandWithoutTimeout
import ru.raspberry.launcher.windows.MainWindowScreens
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.management.ManagementFactory
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.math.max
import kotlin.math.min

private const val ZGC_WINDOWS_BUILD: Int = 17134
private const val GPU_PREFERENCE_WINDOWS_BUILD: Int = 20190
private val replacer = """\$\{([^}]+)}""".toRegex()

class GameLoader(
    private val state: WindowData<MainWindowScreens>
) {
    private val features = mapOf(
        "has_custom_resolution" to true
    )
    private var working = false
    var progress = mutableStateOf(0f)

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        decodeEnumsCaseInsensitive = true
    }
    private val jsonPretty = Json(json) {
        prettyPrint = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        headers {
            set(HttpHeaders.UserAgent, "Raspberry Launcher")
        }
        followRedirects = true
    }
    private lateinit var serverName: String
    private val replacements: MutableMap<String, String> = mutableMapOf()
    private lateinit var version: MinecraftVersion
    private lateinit var data: AdvancedServerData

    suspend fun start(
        serverName: String,
        force: Boolean = false,
        error: (title: @Composable () -> Unit, text: @Composable () -> Unit) -> Unit
    ) {
        state.discord.details = state.translation(
            "discord.loading.details",
            "Loading into server %s"
        ).format(serverName)
        state.discord.state = state.translation(
            "discord.loading.state",
            "Loading..."
        )
        try {
            this.serverName = serverName
            if (working) {
                error(
                    { Text(text = "Already working") },
                    { Text(text = "Game already working!") }
                )
                progress.value = 0f
                return
            }
            working = true


            val account = state.activeAccount
            if (account == null) {
                error(
                    { Text(text = "Account not found") },
                    { Text(text = "Account not found! Please login to your account.") }
                )
                progress.value = 0f
                working = false
                return
            }

            state.minecraftService.refreshToken(account = account)

            Path("${state.config.minecraftPath}/servers/$serverName").createDirectories()
            val serverData = state.launcherService.getServerData(serverName, state.os)
            if (serverData == null) {
                error(
                    { Text(text = "No access") },
                    { Text(text = "You not have access to server $serverName!") }
                )
                progress.value = 0f
                working = false
                return
            }
            data = serverData
            buildMinecraftVersion()
            replacements.clear()
            replacements["auth_username"] = account.username
            val uuid = UUIDTypeAdapter.fromString(account.id)
            replacements["auth_session"] = format(
                "token:%s:%s",
                account.accessToken,
                UUIDTypeAdapter.fromUUID(uuid) ?: ""
            )
            replacements["auth_access_token"] = account.accessToken
            replacements["user_properties"] = "{}"
            replacements["auth_player_name"] = account.username
            replacements["auth_uuid"] = UUIDTypeAdapter.fromUUID(uuid) ?: ""
            replacements["user_type"] = "mojang"
            replacements["profile_name"] = account.username
            replacements["clientid"] = ""
            replacements["auth_xuid"] = ""
            replacements["version_name"] = version.id
            replacements["assets_index_name"] = version.assets ?: throw NullPointerException(
                "Assets index name is null for version ${version.id}"
            )
            replacements["version_type"] = when (version.type) {
                VersionType.Modified -> "modified"
                VersionType.Pending -> "pending"
                VersionType.Snapshot -> "snapshot"
                VersionType.Release -> "release"
                VersionType.OldBeta -> "old_beta"
                VersionType.OldAlpha -> "old_alpha"
            }
            replacements["forge_transformers"] = ""
            replacements["resolution_width"] = state.config.minecraftWindowWidth.toString()
            replacements["resolution_height"] = state.config.minecraftWindowHeight.toString()
            replacements["language"] = "en-us"
            replacements["launcher_name"] = "Raspberry_Launcher"
            replacements["launcher_version"] = AppConfig.version

            val librariesDir = File(state.config.minecraftPath, "libraries")
            val gameDir = File(state.config.minecraftPath, "servers/$serverName")
            val assetsDir = File(state.config.minecraftPath, "assets")
            val minecraftDir = File(state.config.minecraftPath, "versions/${version.id}")
            val minecraftNativesDir = File(minecraftDir, "natives")

            if (!minecraftDir.exists() && !minecraftDir.mkdirs())
                throw MinecraftException("Failed to create Minecraft directory: ${minecraftDir.absolutePath}")
            File(minecraftDir, "${version.id}.json").apply {
                if (!exists() || force)
                    writeText(jsonPretty.encodeToString(version))
            }

            replacements["library_directory"] = librariesDir.absolutePath
            replacements["game_libraries_directory"] = librariesDir.absolutePath
            replacements["game_directory"] = gameDir.absolutePath
            replacements["assets_root"] = assetsDir.absolutePath
            replacements["natives_directory"] = minecraftNativesDir.absolutePath
            val freeSpace: Long = File(state.config.minecraftPath).usableSpace
            if (freeSpace > 0 && freeSpace < 1024L * 64L) {
                throw MinecraftException(
                    "Insufficient space ${state.config.minecraftPath}($freeSpace)"
                )
            }
            installJava(force)
            installLibraries(librariesDir, force)
            installMinecraft(minecraftDir, force)
            unpackNatives(minecraftNativesDir, force)
            installAssets(assetsDir, gameDir, force)
            installFiles(gameDir, force)
            generateServerConfig(gameDir)
            launchGame()
        } catch (e: MinecraftException) {
            println(e)
            error(
                { Text(text = "Loading error") },
                { Text(text = e.message ?: "Unknown error occurred while loading Minecraft!") }
            )
            progress.value = 0f
            working = false
        }
    }

    private suspend fun buildMinecraftVersion() {
        val minecraft = data.minecraft
        if (minecraft == null) {
            throw MinecraftException(
                "Minecraft data is not found for server $serverName!\n" +
                        "Please check your server configuration or try again later.")
        }
        val response = client.get(minecraft.url)
        if (!response.status.isSuccess()) {
            throw MinecraftException(
                "Failed to load Minecraft data for server $serverName!\n" +
                    "Please check your server configuration or try again later.")
        }
        version = json.decodeFromString(response.bodyAsText())
        version.inherit(state.launcherService, client, json)
    }

    private fun constructClassPath(): String {
        println("Constructing classpath...")
        val result = StringBuilder()
        val classPath = version.getClassPath(state.os, features,
            File(state.config.minecraftPath))
        val separator = System.getProperty("path.separator")
        classPath.forEach { file ->
            if (!file.isFile)
                throw Exception("Classpath is not found: $file")
            if (result.isNotEmpty())
                result.append(separator)
            result.append(file.absolutePath)
        }

        return result.toString()
    }

    private suspend fun installJava(force: Boolean) {
        println("Loading Java...")
        val javaList = getJavaList()
        if (javaList == null) {
            throw MinecraftException(
                "Failed to load Java list!\nPlease check your internet connection or try again later."
            )
        }
        val javaVersion = version.javaVersion
        if (javaVersion == null) {
            throw MinecraftException("Java version is not specified in Minecraft version ${version.id}!")
        }
        val platformSpecificJava = javaList.filter {
            it.key.startsWith("${state.os.name.lowercase()}-", true)
        }.map {
            Pair(it.key.substring(state.os.name.length + 1), it.value)
        }.toMap()
        val javas = when (JNA.arch()) {
            Arch.Arm -> platformSpecificJava["arm"] ?: platformSpecificJava["arm32"]
                ?: platformSpecificJava["arm64"] ?: platformSpecificJava["aarch64"] ?: emptyMap()
            Arch.Arm64 -> platformSpecificJava["arm64"]
                ?: platformSpecificJava["aarch64"] ?: platformSpecificJava["arm"] ?: emptyMap()
            Arch.X64 -> platformSpecificJava["x64"]
                ?: platformSpecificJava["amd64"] ?: platformSpecificJava["x86_64"] ?: emptyMap()
            Arch.X86 -> platformSpecificJava["x86"]
                ?: platformSpecificJava["i386"] ?: platformSpecificJava["i686"] ?: emptyMap()
            null -> throw MinecraftException("Unsupported architecture: ${JNA.arch() ?: "unknown"}")
        }
        val java = javas[javaVersion.component]?.firstOrNull()
        if (java == null) {
            throw MinecraftException(
                "Java version ${javaVersion.component} is not supported for ${state.os.name} (${JNA.arch})!"
            )
        }
        java.tryInstallJava(
            File("${state.config.minecraftPath}/java", javaVersion.component),
            force,
            { progress.value = it * 0.05f }
        )
        println("Java installed!")
    }
    private suspend fun installLibraries(librariesDir: File, force: Boolean) {
        println("Installing libraries...")
        version.libraries.forEachIndexed { index, library ->
            library.tryInstallLibrary(librariesDir, force, {
                progress.value = 0.05f + (it + index) * 0.15f / version.libraries.size
            })
        }
        println("Libraries installed!")
    }
    private suspend fun installMinecraft(minecraftDir: File, force: Boolean) {
        val minecraftJarFile = File(minecraftDir, "${version.id}.jar")
        replacements["primary_jar"] = minecraftJarFile.absolutePath
        println("Installing Minecraft...")
        val client = version.downloads["client"]
        if (client == null)
            throw MinecraftException("Minecraft client is not found in recipe!")

        if (!minecraftJarFile.exists() || force) {
            val response = download(client, { progress.value = 0.2f + it * 0.1f })
            if (response == null || !response.status.isSuccess())
                throw MinecraftException("Failed to download Minecraft client!")

            if (!minecraftDir.exists()) minecraftDir.mkdirs()
            minecraftJarFile.writeBytes(response.readRawBytes())
        }
        println("Minecraft installed!")
    }
    @Throws(IOException::class)
    private fun unpackNatives(nativeDir: File, force: Boolean) {
        println("Unpacking natives...")
        val libraries = version.activeLibraries(state.os, features)
        if (force)
            nativeDir.delete()

        for (library in libraries) {
            if (!library.isNative(state.os, JNA.arch() ?: Arch.X64)) continue

            val file = File(state.config.minecraftPath, "libraries/" + library.artifactPath)
            if (!file.isFile()) {
                throw IOException("Required archive doesn't exist: " + file.absolutePath)
            }

            val zip: ZipFile?
            try {
                zip = ZipFile(file)
            } catch (var18: IOException) {
                throw IOException("Error opening ZIP archive: " + file.absolutePath, var18)
            }

            try {
                val extractRules = library.extract
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry: ZipEntry = entries.nextElement()
                    if (entry.isDirectory || entry.name.startsWith("META-INF/")) continue
                    if (extractRules != null && !extractRules.shouldExtract(entry.name)) continue
                    val filename = File(entry.name).name
                    val targetFile = File(nativeDir, filename)
                    if (!force && targetFile.isFile) continue
                    FileUtil.createFolder(targetFile.parentFile)
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(targetFile).use {
                            output -> input.copyTo(output)
                        }
                    }
                }
            } finally {
                zip.close()
            }
        }
        println("Natives unpacked!")
    }
    private suspend fun installAssets(assetsDir: File, gameDir: File, force: Boolean) {
        println("Assets installing...")
        val indexesDir = File(assetsDir, "indexes")
        val objectsDir = File(assetsDir, "objects")
        indexesDir.mkdirs()
        objectsDir.mkdirs()
        val assetIndex = version.assetIndex
        if (assetIndex != null) {
            val response = download(
                DownloadInfo(
                    sha1 = assetIndex.sha1,
                    size = assetIndex.size,
                    url = assetIndex.url
                )
            )
            if (response == null || !response.status.isSuccess())
                throw MinecraftException("Failed to download asset index: ${assetIndex.id}")

            File(indexesDir, "${assetIndex.id}.json").apply {
                if (force || !exists()) {
                    writeBytes(response.readRawBytes())
                }
            }
        }
        val file = File(indexesDir, "${version.assets}.json")
        val list = json.decodeFromString<AssetList>(file.readText())
        val size = list.objects.values.sumOf { it.size }
        var downloaded = 0f
        list.objects.values.forEach { asset ->
            val oldDownloaded = downloaded
            asset.tryInstall(
                objectsDir,
                force,
                {
                    downloaded = oldDownloaded + it * asset.size
                    progress.value = 0.3f + (downloaded / size) * 0.4f
                }
            )
        }
        println("Assets installed!")
        val localAssets = reconstructAssets(
            assetsDir,
            indexesDir,
            objectsDir,
            gameDir,
            force
        )
        replacements["game_assets"] = localAssets.absolutePath
    }

    @Throws(IOException::class)
    private fun reconstructAssets(assetsDir: File, indexesDir: File, objectsDir: File, gameDir: File, force: Boolean): File {
        var assetVersion: String? = version.assetIndex?.id
        if (assetVersion == null) {
            println("Asset version is unknown")
            assetVersion = "unknown"
        }
        val indexFile = File(indexesDir, "$assetVersion.json")
        var virtualRoot = File(File(assetsDir, "virtual"), assetVersion)
        if (!indexFile.isFile()) {
            println("No assets index file $virtualRoot; can't reconstruct assets",)
        } else {
            val index: AssetList
            try {
                index = json.decodeFromString(indexFile.readText())
            } catch (_: java.lang.Exception) {
                println("Couldn't read index file")
                return virtualRoot
            }

            if (index.mapToResources) {
                virtualRoot = File(gameDir, "resources")
            }

            if (index.virtual || index.mapToResources) {
                println("Reconstructing virtual assets folder at $virtualRoot")
                virtualRoot.mkdirs()
                index.objects.forEach { key, value ->
                    val target = File(virtualRoot, key)
                    val original = File(
                        File(
                            objectsDir,
                            value.hash.substring(0, 2)),
                        value.hash
                    )
                    if (!original.isFile()) {
                        println("Skipped reconstructing: $original")
                    } else if (force || !target.isFile()) {
                        original.copyTo(target)
                        println("$original -> $target")
                    }
                }
                File(virtualRoot, ".lastused").writeText(Date().toInstant().toString())
            }
        }
        return virtualRoot
    }
    private suspend fun installFiles(gameDir: File, force: Boolean) {
        if (force) gameDir.deleteRecursively()
        if (!gameDir.exists() && !gameDir.mkdirs())
            throw MinecraftException("Failed to create game directory: ${gameDir.absolutePath}")
        println("Installing files...")
        val allSize = data.files.sumOf { file -> file.size }
        var loaded = 0L
        data.files.forEach {
            val file = File(gameDir, it.path)
            file.parentFile.mkdirs()
            if (!checkHashes(file, it)) {
                // Download new version
                val oldLoaded = loaded
                val response = client.get(
                    it.downloadUrl
                ) {
                    onDownload { bytesSentTotal, _ ->
                        loaded = oldLoaded + bytesSentTotal
                        progress.value = 0.7f + loaded / allSize * 0.3f
                    }
                }
                if (response.status.isSuccess()) {
                    file.writeBytes(response.readRawBytes())
                    loaded = oldLoaded + it.size
                    progress.value = 0.7f + loaded / allSize * 0.3f
                } else {
                    throw MinecraftException("Failed to load file ${file.absolutePath}!")
                }
            }
        }
        println("Files installed!")
    }
    private fun generateServerConfig(gameDir: File) {
        val serversFile = File(gameDir, "servers.dat")
        serversFile.parentFile.mkdirs()
        val nbt = Nbt {
            variant = NbtVariant.Java
            compression = NbtCompression.None
        }
        var tag: NbtCompound = if (serversFile.exists()) {
            serversFile.inputStream().use { input ->
                nbt.decodeFromStream(input)
            }
        }
        else buildNbtCompound {}
        val servers = tag[""]?.nbtCompound["servers"]?.nbtList
        if (servers == null || servers.all { it.nbtCompound["ip"]?.nbtString?.value != data.address })
            tag = buildNbtCompound {
                putNbtCompound("") {
                    putNbtList("servers") {
                        addNbtCompound {
                            put("acceptTextures", true)
                            put("hidden", false)
                            put("ip", data.address)
                            put("name", serverName)
                        }
                        if (servers != null)
                            for (tag in servers)
                                add(tag.nbtCompound)
                    }
                }
            }



        serversFile.outputStream().use { output ->
            nbt.encodeToStream(tag, output)
        }
    }
    private fun launchGame(
    ) {
        replacements["classpath"] = constructClassPath()
        println("Launching game of $serverName")

        val command = mutableListOf<String>()
        val hooks = mutableListOf<(ProcessBuilder) -> Unit>()

        command.add("-Xms${min(state.config.ram, 2048)}M")
        command.add("-Xmx${state.config.ram}M")
        command.add("-Dfile.encoding=UTF-8")
        addOptimizedArguments(command)

        command.addAll(
            version.activeArguments(
                ArgumentType.Jvm, state.os, features
            )?.flatMap { arg -> arg.split(" ") } ?: emptyList()
        )
        command.add("-Dfml.ignoreInvalidMinecraftCertificates=true")
        command.add("-Dfml.ignorePatchDiscrepancies=true")
        command.add("-Djava.net.useSystemProxies=true")
        if (state.os == OS.Windows)
            command.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump")
        else
            command.add("-Xdock:name=Minecraft")

        command.addAll(
            version.activeArguments(
                ArgumentType.Game, state.os, features
            )?.flatMap { arg -> arg.split(" ") } ?: emptyList()
        )

        // GPU selection
        when(state.os) {
            OS.Windows -> {
                val build = JNAWindows.buildNumber
                if (build == null) {
                    println("Couldn't find current Windows build. Is JNA enabled? Setting GPU performance is disabled")
                }
                else if (build < GPU_PREFERENCE_WINDOWS_BUILD) {
                    println(
                        "Current Windows build ($build) doesn't support setting GPU preference through registry",
                    )
                }
                else hooks.add({ setWindowsGpuPreference(it) })
                command.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump")
            }
            OS.Linux -> hooks.add({
                it.environment()["DRI_PRIME"] = "1" // Force use of discrete GPU on Linux})
            })
            OS.OSX, OS.Unknown -> {
                // MacOS doesn't have GPU selection, so nothing to do
                println("MacOS doesn't support GPU selection through JNA, so nothing to do here.")
            }
        }
        command.forEachIndexed { index, text ->
            command[index] = text.replace(replacer) { match ->
                replacements[match.groupValues[1]] ?: match.value
            }
        }
        command.add(0, findJavaExecutable())
        command.add(version.mainClass)
        // After load

        state.discord.details = state.translation(
            "discord.playing.details",
            "Playing on server %s"
        ).format(serverName)
        state.discord.state = state.translation(
            "discord.playing.state",
            "Playing..."
        )
        state.minimize()
        progress.value = 0f
        val process = command.runCommandWithoutTimeout(hooks = hooks)
        if (process != null) {
            thread {
                runBlocking {
                    while (process.isAlive)
                        delay(1000)
                    working = false
                    state.discord.details = state.translation(
                        "discord.main.details",
                        "Looking up for servers..."
                    )
                    state.discord.state = state.translation(
                        "discord.main.state",
                        "Looking..."
                    )
                    println("Process finished with exit code: ${process.exitValue()}")
                }
            }
        }
        else {
            working = false
            state.discord.details = state.translation(
                "discord.main.details",
                "Looking up for servers..."
            )
            state.discord.state = state.translation(
                "discord.main.state",
                "Looking..."
            )
            println("Process start failed!")
        }
    }
    private fun findJavaExecutable(): String {
        val path = File("${state.config.minecraftPath}/java", version.javaVersion!!.component)
        if (!path.exists() || !path.isDirectory) {
            throw MinecraftException("Java directory not found: ${path.absolutePath}")
        }
        val javaExecutable = if (state.os == OS.Windows) {
            "bin\\java.exe"
        } else {
            "bin/java"
        }
        val javaPath = File(path, javaExecutable)
        if (!javaPath.exists() || !javaPath.isFile) {
            // Bruteforce search for java executable
            val javaFiles = path.walk().filter { it.isFile && it.name.startsWith("java") && it.extension == "exe" }.toList()
            if (javaFiles.isEmpty()) {
                throw MinecraftException("Java executable not found in: ${path.absolutePath}")
            }
            if (javaFiles.size > 1) {
                println("Found multiple Java executables in ${path.absolutePath}, using the first one: ${javaFiles.first()}")
            }
            return javaFiles.first().absolutePath
        }
        return javaPath.absolutePath
    }

    private fun addOptimizedArguments(args: MutableList<String>) {
        val jreMajorVersion: Int = version.javaVersion?.majorVersion ?: 8

        // I want Kotlin's when {}
        // Is enough power and Java 15+ => ZGC
        // ZGC requires A LOT of heap on start
        val availableProcessors = getAvailableProcessors();
        val supportsZgc = state.os != OS.Windows || (JNAWindows.buildNumber ?: 0) >= ZGC_WINDOWS_BUILD
        if (supportsZgc && jreMajorVersion >= 15 && availableProcessors >= 8 && state.config.ram >= 8192) {
            addZGCOptimizedArguments(args)
            return
        }

        // Is enough power and Java 8+ => G1
        // Java 11+ => G1 for all PCs
        if (jreMajorVersion >= 11 || (jreMajorVersion >= 8 && availableProcessors >= 4)) {
            addG1OptimizedArguments(args)
            return
        }

        // Junk PCs or old Java => CMS
        addCMSOptimizedArguments(args, availableProcessors)
    }
    private fun getAvailableProcessors(): Int {
        return try {
            ManagementFactory.getOperatingSystemMXBean().availableProcessors
        } catch (_: Throwable) { 1 }
    }

    private fun addZGCOptimizedArguments(args: MutableList<String>) {
        // https://github.com/Obydux/MC-ZGC-Flags
        args.add("-XX:+UnlockExperimentalVMOptions");
        args.add("-XX:+UseZGC"); // enable ZGC
        args.add("-XX:-ZUncommit"); // Unstable feature, disable
        args.add("-XX:ZCollectionInterval=5");
        args.add("-XX:ZAllocationSpikeTolerance=2.0");
        args.add("-XX:+AlwaysPreTouch"); // AlwaysPreTouch gets the memory setup and reserved at process start ensuring
        // it is contiguous, improving the efficiency of it more. This improves
        // the operating systems memory access speed. Mandatory to use Transparent Huge Pages
        args.add("-XX:+ParallelRefProcEnabled"); // Optimizes the GC process to use multiple threads for weak reference checking
        args.add("-XX:+DisableExplicitGC"); // Disable System.gc() calls
    }
    private fun addG1OptimizedArguments(args: MutableList<String>) {
        args.add("-XX:+UnlockExperimentalVMOptions"); // to unlock G1NewSizePercent
        args.add("-XX:+UseG1GC"); // enable G1
        args.add("-XX:G1NewSizePercent=20"); // from Mojang launcher
        args.add("-XX:G1ReservePercent=20"); // from Mojang launcher
        args.add("-XX:MaxGCPauseMillis=50"); // from Mojang launcher
        args.add("-XX:G1HeapRegionSize=32M"); // from Mojang launcher
        args.add("-XX:+DisableExplicitGC"); // Disable System.gc() calls
        args.add("-XX:+AlwaysPreTouch");
        args.add("-XX:+ParallelRefProcEnabled");
    }
    private fun addCMSOptimizedArguments(args: MutableList<String>, availableProcessors: Int) {
        args.add("-XX:+DisableExplicitGC"); // Disable System.gc() calls
        args.add("-XX:+UseConcMarkSweepGC"); // enable CMS
        args.add("-XX:-UseAdaptiveSizePolicy");
        args.add("-XX:+CMSParallelRemarkEnabled");
        args.add("-XX:+CMSClassUnloadingEnabled");
        args.add("-XX:+UseCMSInitiatingOccupancyOnly");
        args.add("-XX:ConcGCThreads=${max(1, availableProcessors / 2)}"); // we don't have Parallel anymore
    }

    private fun checkHashes(file: File, it: ServerFile): Boolean {
        if (!file.exists()) return false
        return when (it.hashType) {
            "Json"-> {
                val jsonHash = Json.decodeFromString<JsonHash>(
                    Base64.getDecoder().decode(it.hash).toString(Charsets.UTF_8)
                )
                val paths = pathsFromJson(file.readText())
                val hashPaths = jsonHash.paths.mapNotNull { path ->
                    if (paths[path] != null) path to paths[path] else null
                }.associate { pair -> pair }

                hashByAlgorithm(
                    Json.encodeToString(hashPaths).toByteArray(),
                    it.hashAlgorithm
                ) == jsonHash.hash
            }
            else -> {
                hashByAlgorithm(file.readBytes(), it.hashAlgorithm) == it.hash
            }
        }
    }

    private fun hashByAlgorithm(content: ByteArray, hashAlgorithm: String): String {
        val md = MessageDigest.getInstance( when(hashAlgorithm) {
            "MD5"-> "MD5"
            "SHA1"-> "SHA-1"
            "SHA256"-> "SHA-256"
            else ->  "SHA-512"
        })
        val digest = md.digest(content)
        return digest.joinToString("") { "%02x".format(it) }

    }

    fun setWindowsGpuPreference(
        process: ProcessBuilder,
    ) {
        val buildOpt = JNAWindows.buildNumber
        if (buildOpt == null) {
            println("Couldn't find current Windows build. Is JNA enabled? Setting GPU performance is disabled")
            return
        }
        val command: MutableList<String?> = process.command()
        if (command.isEmpty()) {
            println("Process command is empty, wat?")
            return
        }
        val path = Paths.get(command[0]!!)
        if (!path.isAbsolute) {
            println("JRE executable is not absolute ($path), setting GPU performance is disabled")
            return
        }
        val registryOpt = JNAWindows.registry
        if (registryOpt == null) {
            println("Registry is not available")
            return
        }
        val reg = registryOpt
        val expectedValue: String = format(
            "GpuPreference=%d;",
            if (state.config.useDiscreteGPU) 2 else 1
        )
        val currentValue: String?
        try {
            currentValue = reg.getString(
                WinReg.HKEY_CURRENT_USER,
                "Software\\Microsoft\\DirectX\\UserGpuPreferences",
                path.toString()
            )
        } catch (e: JNAException) {
            println("Couldn't fetch current GPU preference. Setting it was skipped.")
            println(e)
            return
        }
        if (expectedValue == currentValue) {
            if (state.config.debug)
                println("Skipping setting GPU Preference. Current value is matched expected one for $path: $currentValue")
            return
        }
        println("Setting GpuPreference value for $path: $expectedValue")
        try {
            reg.setString(
                WinReg.HKEY_CURRENT_USER,
                "Software\\Microsoft\\DirectX\\UserGpuPreferences",
                path.toString(),
                expectedValue
            )
        } catch (e: JNAException) {
            println("Couldn't set current GPU preference")
            println(e)
        }
    }

}

@Serializable
private data class JsonHash(
    val paths: List<String>,
    val hash: String,
)
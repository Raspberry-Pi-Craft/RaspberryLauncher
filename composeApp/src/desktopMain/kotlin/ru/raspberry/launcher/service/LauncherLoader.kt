package ru.raspberry.launcher.service

import androidx.compose.runtime.MutableState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.AppConfig
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.auth.AccountRepository
import ru.raspberry.launcher.models.dtos.ApiInfoDto
import ru.raspberry.launcher.models.dtos.LauncherInfoDto
import ru.raspberry.launcher.windows.MainWindowScreens
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.system.exitProcess

class LauncherLoader(private val text: MutableState<String>, private val state: WindowData<MainWindowScreens>) {
    private val supportedApis = listOf(
        "v1"
    )
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json)
        }
        headers {
            set(HttpHeaders.UserAgent, "Raspberry Launcher")
        }
    }
    val progress: Float
        get () = value

    private var value = 0f
    suspend fun start() = coroutineScope {
        try {
            value = 0.1f
            text.value = state.translation("loading.launcher","Checking for updates...")
            var response = client.get("${state.config.host}/api/info/launcher")
            if (!response.status.isSuccess()) {
                value = 0.25f
                if (response.status == HttpStatusCode.NotFound)
                    text.value = state.translation("loading.launcher.error.no_info","No info about update!")
                        .format(response.status, response.bodyAsText())
                else
                    text.value = state.translation("loading.launcher.error","Error: %s - %s")
                        .format(response.status, response.bodyAsText())
            } else {
                value = 0.25f
                val info = response.body<LauncherInfoDto>()
                state.launcherInfo = info
                if (info.version != AppConfig.version) {
                    text.value = state.translation("loading.launcher.success.old", "Loading update...")
                    val osName = System.getProperty("os.name")
                    val command: String
                    val format: String
                    if (osName.startsWith("Windows")) {
                        format = ".msi"
                        command = "msiexec /i RaspberryLauncher.msi /passive /norestart /log install.log"
                    } else if (osName.startsWith("Mac OS")) {
                        format = ".pkg"
                        val appDirectory = File(System.getProperty("user.dir"))
                        command = "sudo installer -pkg RaspberryLauncher.pkg -target ${appDirectory.parent}"
                    } else if (osName.startsWith("Linux")) {
                        format = ".deb"
                        command = "sudo apt install ./RaspberryLauncher.deb -y"
                    }
                    else {
                        value = 1f
                        text.value = state.translation(
                            "loading.launcher.error.os",
                            "Error: Unsupported OS"
                        )
                        // Wait 2 seconds and close
                        delay(2000)
                        state.close()
                        return@coroutineScope
                    }
                    val response = client.get("${info.downloadUrl}$format") {
                        onDownload { bytesSentTotal, contentLength ->
                            value = if (contentLength != null)
                                    (1f + 3f * bytesSentTotal / contentLength) / 4f
                            else
                                    (262144f + bytesSentTotal) / (1048576f + bytesSentTotal)

                        }
                    }
                    val dir = File("${state.config.launcherDataPath}/update")
                    dir.mkdirs()
                    val file = dir.resolve("RaspberryLauncher$format")
                    file.writeBytes(response.readRawBytes())
                    command.runCommand(dir)
                    exitProcess(0)
                }
                else
                    text.value = state.translation("loading.launcher.update.actual", "Launcher already updated!")
            }


            value = 0.5f
            text.value = state.translation("loading.api","Loading api info...")
            response = client.get("${state.config.host}/api/info")
            if (!response.status.isSuccess()) {
                value = 0.6f
                text.value = state.translation("loading.api.error","Error: %s - %s")
                    .format(response.status, response.bodyAsText())
            } else {
                val info = response.body<ApiInfoDto>()
                if (info.apiVersion !in supportedApis) {
                    value = 1f
                    text.value = state.translation("loading.api.error.version",
                        "Error: API version not supported!")
                    // Wait 2 seconds and close
                    delay(2000)
                    state.close()
                }
                else {
                    value = 0.6f
                    text.value = state.translation("loading.api.success",
                        "API info loaded successfully")
                }
            }


            value = 0.7f
            text.value = state.translation("loading.account", "Loading accounts...")
            val repo = AccountRepository(
                config = state.config,
            )
            val meta = repo.getMeta()
            value = 0.8f
            if (meta.isEmpty()) {
                state.config.activeAccountId = -1
                text.value = state.translation("loading.account.zero",  "No accounts found...")
            }
            else {
                if (state.config.activeAccountId == -1) {
                    state.activeAccount = repo.getByMeta(meta.firstOrNull())
                } else {
                    if (meta.size < state.config.activeAccountId) {
                        state.activeAccount = null
                    } else {
                        state.activeAccount = repo.getByMeta(meta[state.config.activeAccountId])
                    }
                }
            }
            value = 0.87f
            val account = state.activeAccount
            if (account == null) {
                text.value = state.translation("loading.account.zero.active",
                    "Active account not found!")
                state.config.activeAccountId = -1
            }
            else {
                value = 0.94f
                if (state.minecraftService.isTokenValid(account)) {
                    text.value = state.translation("loading.account.active",
                        "Active account: %s").format(account.username)
                    val result = state.launcherService.auth()
                    when (result) {
                        LauncherServiceV1.LauncherAuthResult.Success -> text.value = state.translation(
                            "loading.account.auth.success", "Auth successful!")
                        LauncherServiceV1.LauncherAuthResult.NoAccount -> text.value = state.translation(
                            "loading.account.auth.no_account", "Account not found!")
                        LauncherServiceV1.LauncherAuthResult.ConnectionFailed -> text.value = state.translation(
                        "loading.account.auth.connection_failed", "Connection failed!")
                        LauncherServiceV1.LauncherAuthResult.InvalidCredentials -> text.value = state.translation(
                        "loading.account.auth.invalid_credentials", "Invalid credentials!")
                        LauncherServiceV1.LauncherAuthResult.FailedToVerify -> text.value = state.translation(
                        "loading.account.auth.failed_to_verify", "Failed to verify account!")
                    }
                }
                else {
                    text.value = state.translation(
                        "loading.account.invalid",
                        "Active account token verification error!"
                    )
                    state.config.activeAccountId = -1
                    AccountRepository(state.config).remove(account)
                }
                delay(1000)
            }

            text.value = state.translation(
                "loading.complete",
                "Startup complete!"
            )
            value = 1f
            Files.createDirectories(Path(state.config.minecraftPath))
            // Wait 2 seconds and jump to main screen
            delay(2000)
            state.loaded.value = true
            state.changeScreen(MainWindowScreens.Main)

        }
        catch (e: Exception) {
            value = 1f
            text.value = state.translation(
                "loading.error",
                "Error: %s"
            ).format(e.message ?: "Unknown error")
            e.printStackTrace()
            // Wait 2 seconds and close
            delay(2000)
            state.close()
        }
    }
    private fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(60, TimeUnit.MINUTES)
    }
}

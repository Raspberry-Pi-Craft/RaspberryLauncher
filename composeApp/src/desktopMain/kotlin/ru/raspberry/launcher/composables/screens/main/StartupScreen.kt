package ru.raspberry.launcher.composables.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.raspberry
import ru.raspberry.launcher.Language
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.auth.AccountRepository
import ru.raspberry.launcher.service.LauncherServiceV1
import ru.raspberry.launcher.windows.MainWindowScreens
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.Locale
import kotlin.io.path.Path
import kotlin.system.exitProcess

class Startup(private val text: MutableState<String>, private val state: WindowData<MainWindowScreens>) {
    private val client: HttpClient = HttpClient.newHttpClient()
    private val progress: Float
        get () = loadingStep.toFloat() / loadingMax.toFloat()

    private var loadingStep: Int = 0
    private val loadingMax: Int = 7
    suspend fun start() = coroutineScope {
        Locale.setDefault(Language.Russian.locale)

        text.value = "Loading configuration..."
        Files.createDirectories(Path(state.config.launcherDataPath))
        Files.createDirectories(Path(state.config.minecraftPath))

        try {
            loadingStep++
            text.value = "Loading api info..."

            val request = HttpRequest.newBuilder(
                URI("${state.config.host}/api/info"),
            ).GET().build()
            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            if (response.statusCode() != 200) {
                loadingStep++
                text.value = "Error: ${response.statusCode()} - ${response.body()}"
            } else {
                loadingStep++
                text.value = "API info loaded successfully"
                System.out.println("Response: ${response.body()}")
            }


            loadingStep++
            text.value = "Loading accounts..."
            val repo = AccountRepository(
                config = state.config,
            )
            val meta = repo.getMeta()
            if (meta.isEmpty()) {
                loadingStep++
                state.config.activeAccountId = -1
                text.value = "No accounts found..."
            }
            else {
                loadingStep++
                text.value = "Accounts: ${meta.size}"
                if (state.config.activeAccountId == -1) {
                    loadingStep++
                    state.activeAccount = repo.getByMeta(meta.firstOrNull())
                } else {
                    loadingStep++
                    if (meta.size < state.config.activeAccountId) {
                        state.activeAccount = null
                    } else {
                        state.activeAccount = repo.getByMeta(meta[state.config.activeAccountId])
                    }
                }
            }
            val account = state.activeAccount
            if (account == null) {
                text.value = "Active account not found, skipping..."
                state.config.activeAccountId = -1
            }
            else {
                if (state.minecraftService.isTokenValid(account)) {
                    text.value = "Active account: ${account.username}"
                    val result = state.launcherService.auth()
                    when (result) {
                        LauncherServiceV1.LauncherAuthResult.Success -> text.value = "Auth successful!"
                        LauncherServiceV1.LauncherAuthResult.NoAccount -> text.value = "Account not found!"
                        LauncherServiceV1.LauncherAuthResult.ConnectionFailed -> text.value = "Connection failed!"
                        LauncherServiceV1.LauncherAuthResult.InvalidCredentials -> text.value = "Invalid credentials!"
                        LauncherServiceV1.LauncherAuthResult.FailedToVerify -> text.value = "Failed to verify account!"
                    }
                }
                else {
                    text.value = "Active account with invalid token, skipping..."
                    state.config.activeAccountId = -1
                    AccountRepository(state.config).remove(account)
                }
                delay(1000)
            }

            text.value = "Startup complete!"
            loadingStep = loadingMax // Set progress to 100%
            // Wait 2 seconds and jump to main screen
            delay(2000)
            state.loaded.value = true
            state.changeScreen(MainWindowScreens.Main)

        }
        catch (e: Exception) {
            loadingStep = loadingMax // Set progress to 100%
            text.value = "Error: ${e.message ?: "Unknown error"}"
            e.printStackTrace()
            // Wait 2 seconds and cross
            delay(2000)
            exitProcess(-1)
        }
    }

    fun progress(): Float {
        return progress
    }
}

@Preview
@Composable
fun WindowScope.StartupScreen(state: WindowData<MainWindowScreens>) {
    val coroutineScope = rememberCoroutineScope()
    val text = mutableStateOf("Loading...")
    val startup = Startup(text, state)
    coroutineScope.launch {
        startup.start()
    }

    AppTheme(
        theme = state.theme,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader<MainWindowScreens, Unit>(
                windowData = state
            )
        }
    ) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painterResource(Res.drawable.raspberry),
                "Raspberry Launcher",
                modifier = Modifier
                    .weight(1f),
            )
            LinearProgressIndicator(
                modifier = Modifier,
                progress = { startup.progress() },
            )
            Text(
                text = text.value,
                modifier = Modifier
                    .weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
package ru.raspberry.launcher.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.raspberry
import ru.raspberry.launcher.GoodJson
import ru.raspberry.launcher.Language
import ru.raspberry.launcher.composables.screens.main.MainScreen
import ru.raspberry.launcher.composables.screens.main.StartupScreen
import ru.raspberry.launcher.models.Config
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.Theme
import ru.raspberry.launcher.tools.jna.JNA
import ru.raspberry.launcher.tools.roundCorners
import java.io.File
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.minutes

enum class MainWindowScreens {
    Startup,
    Main
}

@Composable
fun MainWindow(
    close: () -> Unit
) {
    val windowState = rememberWindowState(
        width = 640.dp,
        height = 360.dp,
        position = WindowPosition(Alignment.Center),
    )
    var dropContext by remember { mutableStateOf(false) }
    val currentMainWindowScreens = remember { mutableStateOf(MainWindowScreens.Startup) }
    val state = remember {
        println(System.getProperty("compose.application.resources.dir"))
        var config = Config()
        val configFile = File("config.json")
        if (configFile.exists())
            config = GoodJson.decodeFromString<Config>(configFile.readText())
        else
            configFile.writeText(GoodJson.encodeToString(config))

        val themePath = File(config.launcherDataPath).resolve("themes")
        val themeFiles = themePath.listFiles()
        val themes = themeFiles?.mapNotNull { file ->
            if (file == null) null else GoodJson.decodeFromString<Theme>(file.readText(Charsets.UTF_8))
        }?.associateBy { theme -> theme.name } ?: emptyMap()

        val languagePath = File(config.launcherDataPath).resolve("languages")
        val languageFiles = languagePath.listFiles()
        val languages = languageFiles?.mapNotNull { file ->
            if (file == null)
                null
            else {
                val lang = GoodJson.decodeFromString<Language>(file.readText(Charsets.UTF_8))
                lang.id = file.nameWithoutExtension
                lang
            }
        }?.associateBy { language -> language.id } ?: emptyMap()

        val os = System.getProperty("os.name")
        WindowData(
            currentMainWindowScreens,
            windowState,
            close,
            {
                if (windowState.placement == WindowPlacement.Maximized)
                    windowState.placement = WindowPlacement.Floating
                else
                    windowState.placement = WindowPlacement.Maximized
            },
            {
                windowState.isMinimized = !windowState.isMinimized
            },
            config = config,
            themes = themes,
            languages = languages,
            recompose = { dropContext = true },
            os = JNA.currentOs ?: if (os.startsWith("Linux")) OS.Linux
            else if (os.startsWith("Mac") || os.startsWith("mac")) OS.OSX
            else if (os.startsWith("Windows")) OS.Windows
            else OS.Unknown
        )
    }
    println("Current OS: ${state.os} Arch: ${JNA.arch}")
    remember {
        thread {
            runBlocking {
                while (true) {
                    state.launcherService.refresh()
                    println("Refreshing launcher service...")
                    delay(30.minutes)
                }
            }
        }
    }
    Window(
        state = windowState,
        onCloseRequest = close,
        title = state.translation("app_name", "Raspberry Launcher"),
        icon = painterResource(Res.drawable.raspberry),
        undecorated = true
    ) {
        roundCorners(window)
        if (dropContext) {
            dropContext = false
            return@Window
        }
        when (currentMainWindowScreens.value) {
            MainWindowScreens.Startup -> StartupScreen(state)
            MainWindowScreens.Main -> MainScreen(state)
        }
    }
}
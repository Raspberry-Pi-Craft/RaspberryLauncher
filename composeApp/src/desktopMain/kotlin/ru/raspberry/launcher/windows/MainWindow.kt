package ru.raspberry.launcher.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.raspberry
import ru.raspberry.launcher.composables.screens.main.MainScreen
import ru.raspberry.launcher.composables.screens.main.StartupScreen
import ru.raspberry.launcher.models.Config
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.Theme
import ru.raspberry.launcher.tools.roundCorners
import java.io.File

enum class MainWindowScreens {
    Startup,
    Main
}

@Composable
fun MainWindow(close:() -> Unit) {
    val windowState = rememberWindowState(
        width = 640.dp,
        height = 360.dp,
        position = WindowPosition(Alignment.Center),
    )

    Window(
        state = windowState,
        onCloseRequest = close,
        title = "Raspberry Launcher",
        icon = painterResource(Res.drawable.raspberry),
        undecorated = true
    ) {
        roundCorners(window)
        val currentMainWindowScreens = remember { mutableStateOf(MainWindowScreens.Startup) }
        val state = remember {
            println(System.getProperty("compose.application.resources.dir"))
            var config = Config()
            val configFile = File("config.json")
            if (configFile.exists())
                config = Json.decodeFromString<Config>(configFile.readText())
            else
                configFile.writeText(Json.encodeToString( config))

            val themePath = File(config.launcherDataPath).resolve("themes")
            val themeFiles = themePath.listFiles()
            val themes = themeFiles?.mapNotNull { file ->
                if (file == null) null else Json.decodeFromString<Theme>(file.readText(Charsets.UTF_8))
            }?.associateBy { theme -> theme.name } ?: emptyMap()


            val state = WindowData(
                currentMainWindowScreens,
                windowState,
                title = "Raspberry Launcher",
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
                themes = themes
            )
            state
        }
        when (currentMainWindowScreens.value) {
            MainWindowScreens.Startup -> StartupScreen(state)
            MainWindowScreens.Main -> MainScreen(state)
        }
    }
}
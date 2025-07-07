package ru.raspberry.launcher.windows.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.composables.screens.settings.AboutSettingsScreen
import ru.raspberry.launcher.composables.screens.settings.LauncherSettingsScreen
import ru.raspberry.launcher.composables.screens.settings.MinecraftSettingsScreen
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens

enum class SettingsScreens(
    val displayName: String
) {
    Minecraft("Minecraft"),
    Launcher("Launcher"),
    About("About"),
}

@Composable
fun SettingsDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val currentScreen = remember { mutableStateOf(SettingsScreens.Minecraft) }
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = currentScreen,
            close = close,
            title = "Settings",
        )
    }
    DialogWindow(
        onCloseRequest = close,
        state = windowState,
        undecorated = true,
        resizable = false
    ) {
        roundCorners(window)
        AppTheme(
            theme = state.theme,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppHeader(
                    dialogData = dialogData
                )
            }
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(8.dp)
                ) {
                    for (screen in SettingsScreens.entries) {
                        Button(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 4.dp)
                                .weight(1f),
                            onClick = {
                                currentScreen.value = screen
                            }
                        ) { Text(screen.displayName) }
                    }
                }
                when (currentScreen.value) {
                    SettingsScreens.Minecraft -> MinecraftSettingsScreen(state)
                    SettingsScreens.Launcher -> LauncherSettingsScreen(state)
                    SettingsScreens.About -> AboutSettingsScreen(state)
                }
            }
        }
    }
}
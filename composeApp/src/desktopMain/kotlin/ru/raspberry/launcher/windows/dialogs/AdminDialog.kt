package ru.raspberry.launcher.windows.dialogs

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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
import ru.raspberry.launcher.composables.screens.admin.LauncherAdminScreen
import ru.raspberry.launcher.composables.screens.admin.ServersAdminScreen
import ru.raspberry.launcher.composables.screens.admin.UsersAdminScreen
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens

enum class AdminScreens(
    val defaultName: String,
    val translationKey: String
) {
    Launcher("Launcher", "admin.launcher"),
    Users("Users", "admin.users"),
    Servers("Servers", "admin.servers"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val currentScreen = remember { mutableStateOf(AdminScreens.Launcher) }
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = currentScreen,
            close = close,
            title = state.translation("admin", "Admin"),
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
                    dialogData = dialogData,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                )
            },
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            NavigationBar (
                modifier = Modifier
                    .height(50.dp)
            ) {
                for (screen in AdminScreens.entries) {
                    NavigationBarItem(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                            .weight(1f),
                        onClick = {
                            currentScreen.value = screen
                        },
                        selected = currentScreen.value == screen,
                        label = {
                            Text(state.translation(screen.translationKey, screen.defaultName))
                        },
                        icon = {}
                    )
                }
            }
            when (currentScreen.value) {
                AdminScreens.Launcher -> LauncherAdminScreen(state)
                AdminScreens.Users -> UsersAdminScreen(state)
                AdminScreens.Servers -> ServersAdminScreen(state)
            }
        }
    }
}
package ru.raspberry.launcher.windows.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveApproval(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
    serverName: String,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = state.translation("admin.remove_server.title", "Remove Server?"),
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
           Text(
               text = buildString {
                   appendLine(
                       state.translation(
                           "admin.remove_server.message",
                           "Are you sure you want to remove the server?"
                       )
                   )
                   appendLine("Server Name: $serverName")
               },
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
           )
            Row {
                Button(
                    onClick = {
                        changeDialog(DialogType.None, mapOf())
                    }
                ) {
                    Text(
                        text = state.translation("admin.remove_server.cancel", "Cancel")
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        runBlocking {
                            val response = state.launcherService.removeServer(serverName)
                            if (response.status.isSuccess())
                                changeDialog(DialogType.None, mapOf())
                            else
                                changeDialog(DialogType.GameError, mapOf())
                        }
                    }
                ) {
                    Text(
                        text = state.translation("admin.remove_server.confirm", "Confirm")
                    )
                }
            }
        }
    }
}
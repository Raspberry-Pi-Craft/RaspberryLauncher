package ru.raspberry.launcher.windows.dialogs.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.admin
import raspberrylauncher.composeapp.generated.resources.banned
import raspberrylauncher.composeapp.generated.resources.close
import ru.raspberry.launcher.composables.components.Accordion
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.components.Spinner
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.users.UserInfo
import ru.raspberry.launcher.models.users.UserSelector
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ToolsDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(null),
            close = close,
            title = "Users Management",
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
            },
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Accordion(
                    title = { Text("Importer") },
                    modifier = Modifier.fillMaxWidth(),
                ) {

                }
            }
        }
    }
}

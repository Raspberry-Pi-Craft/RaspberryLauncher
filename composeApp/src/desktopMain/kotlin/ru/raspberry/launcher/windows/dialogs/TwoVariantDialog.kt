package ru.raspberry.launcher.windows.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
fun TwoVariantDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    title: String,
    text: @Composable () -> Unit,
    textRight: @Composable RowScope.() -> Unit,
    actionRight: () -> Unit,
    textLeft: @Composable RowScope.() -> Unit,
    actionLeft: () -> Unit,
    reload: () -> Unit,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(300.dp, 200.dp)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = title,
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
            tonalElevation = 12.dp,
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                text()
                Spacer(Modifier.weight(1f))
                Row {
                    Button(
                        onClick = actionLeft,
                        content = textLeft
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = actionRight,
                        content = textRight
                    )
                }
            }
        }
    }
}
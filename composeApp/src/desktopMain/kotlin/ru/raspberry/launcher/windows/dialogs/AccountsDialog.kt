package ru.raspberry.launcher.windows.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.composables.screens.main.imageCache
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.auth.AccountRepository
import ru.raspberry.launcher.models.auth.AuthSystem
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens



@Composable
fun AccountsDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    var update by remember { mutableStateOf(false) }
    val windowState = rememberDialogState(position = WindowPosition(Alignment.Center))
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = "Accounts"
        )
    }
    DialogWindow(
        onCloseRequest = close,
        state = rememberDialogState(position = WindowPosition(Alignment.Center)),
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
            val scrollState = rememberScrollState()
            val repository = remember {
                AccountRepository(config = state.config)
            }
            val metas = repository.getMeta()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .scrollable(
                        state = scrollState,
                        orientation = Orientation.Vertical
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(metas.size) { index ->
                    val account = metas[index]
                    key(update) {
                        Button(
                            onClick = {
                                state.config.activeAccountId = index
                                state.config.save()
                                update = !update
                            },
                            enabled = state.config.activeAccountId != index,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val authSystemIcon = painterResource(account.authSystem.drawableResource)
                                Image(
                                    painter = authSystemIcon,
                                    contentDescription = "${account.authSystem.displayName} Icon",
                                    modifier = Modifier.size(40.dp)
                                )
                                val coroutine = rememberCoroutineScope()
                                var painter by remember {
                                    val image = imageCache.getOrDefault(account.skinUrl, null)
                                    mutableStateOf(
                                        if (image == null) authSystemIcon else
                                            BitmapPainter(
                                                image = image,
                                                srcOffset = IntOffset(8, 8),
                                                srcSize = IntSize(8, 8),
                                                filterQuality = FilterQuality.None
                                            )
                                    )
                                }
                                remember {
                                    coroutine.launch {
                                        val url = account.skinUrl
                                        val response = state.client.get(
                                            urlString = account.skinUrl
                                        )
                                        val bytes: ByteArray = response.readRawBytes()
                                        val image = bytes.decodeToImageBitmap()
                                        imageCache.put(url, image)
                                        painter = BitmapPainter(
                                            image = image,
                                            srcOffset = IntOffset(8, 8),
                                            srcSize = IntSize(8, 8),
                                            filterQuality = FilterQuality.None
                                        )
                                    }
                                }
                                Image(
                                    painter = painter,
                                    contentDescription = "${account.username} Account Icon",
                                    modifier = Modifier.size(60.dp)
                                )
                                Text(
                                    text = account.username,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    maxLines = 1,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                items(AuthSystem.entries) { system ->
                    Button(
                        onClick = {
                            changeDialog(DialogType.Auth, mapOf("authSystem" to system))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(system.drawableResource),
                                contentDescription = "${system.displayName} Icon",
                                modifier = Modifier.size(60.dp)
                            )
                            Text(
                                text = "Login with ${system.displayName}",
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
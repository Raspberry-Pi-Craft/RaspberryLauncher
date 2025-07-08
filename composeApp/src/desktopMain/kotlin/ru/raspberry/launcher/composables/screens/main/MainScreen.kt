package ru.raspberry.launcher.composables.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.account
import raspberrylauncher.composeapp.generated.resources.edit
import raspberrylauncher.composeapp.generated.resources.raspberry
import raspberrylauncher.composeapp.generated.resources.raspberry_with_bg
import raspberrylauncher.composeapp.generated.resources.settings
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.models.ServerData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.auth.AuthSystem
import ru.raspberry.launcher.service.GameLoader
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.windows.MainWindowScreens
import ru.raspberry.launcher.windows.dialogs.AccountsDialog
import ru.raspberry.launcher.windows.dialogs.AdminDialog
import ru.raspberry.launcher.windows.dialogs.AuthDialog
import ru.raspberry.launcher.windows.dialogs.SettingsDialog

enum class DialogType {
    Admin,
    Accounts,
    Auth,
    Settings,
    GameError,
    None
}

val imageCache = mutableMapOf<String, ImageBitmap>()

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Preview
@Composable
fun WindowScope.MainScreen(state: WindowData<MainWindowScreens>) {
    remember {
        state.resize( width = 960.dp, height = 540.dp)
        state.move(WindowPosition(Alignment.Center))
    }
    var dialogType by remember { mutableStateOf(DialogType.None) }
    val errorDialogTitle = remember { mutableStateOf<@Composable () -> Unit>({}) }
    val errorDialogText = remember { mutableStateOf<@Composable () -> Unit>({ }) }

    AppTheme(
        theme = state.theme,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader<MainWindowScreens, Unit>(
                windowData = state,
                customActions = {
                    if (state.adminMode) {
                        IconButton(
                            onClick = { dialogType = DialogType.Admin },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.edit),
                                modifier = Modifier.size(24.dp),
                                contentDescription = "Open Admin Panel"
                            )
                        }
                    }
                    IconButton(
                        onClick = { dialogType = DialogType.Accounts },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.account),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Account"
                        )
                    }
                    IconButton(
                        onClick = { dialogType = DialogType.Settings },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.settings),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Settings"
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) {
        val coroutine = rememberCoroutineScope()
        Column {
            val loader = remember { GameLoader(state) }
            Row(
                modifier = Modifier.weight(1f),
            ) { // Content
                val serverDataCache = remember { mutableMapOf<Pair<String, String>, ServerData>() }
                var selectedServerName by remember { mutableStateOf<String?>(null) }
                var serverDataLoaded by remember { mutableStateOf(false) }
                fun selectServer(name: String) {
                    selectedServerName = name
                    serverDataLoaded = false
                    coroutine.launch {
                        val name = selectedServerName
                        if (name.isNullOrBlank()) return@launch
                        val data = state.launcherService.getServerData(name, state.config.language)
                        if (data == null) return@launch
                        serverDataCache.put(
                            Pair(name, state.config.language),
                            data
                        )
                        serverDataLoaded = true
                    }
                }

                val servers = remember {
                    runBlocking {
                        val names = state.launcherService.getServerNames()
                        names
                    }
                }
                val scrollState = rememberScrollState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(3f)
                        .scrollable(scrollState, Orientation.Vertical)
                ) {
                    items(servers) { name ->
                        Button(
                            onClick = {
                                selectServer(name)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                        ) {
                            Text(text = name)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedServerName != null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(2f)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                    ) {
                        // Display
                        if (serverDataLoaded) {
                            val data = serverDataCache[Pair(selectedServerName, state.config.language)]
                            if (data != null) {
                                Text(
                                    text = data.serverName,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val serverImage = painterResource(Res.drawable.raspberry_with_bg)
                                var painter by remember {
                                    val image = imageCache.getOrDefault(data.imageUrl, null)
                                    mutableStateOf(
                                        if (image == null) serverImage else
                                            BitmapPainter(
                                                image = image,
                                                srcOffset = IntOffset(8, 8),
                                                srcSize = IntSize(8, 8),
                                                filterQuality = FilterQuality.None
                                            )
                                    )
                                }
                                Image(
                                    painter = painter,
                                    contentDescription = "${data.serverName} Icon",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                                val scrollState = rememberScrollState()
                                Text(
                                    text = data.description,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .scrollable(scrollState, Orientation.Vertical)
                                )
                                Button(
                                    onClick = {
                                        coroutine.launch {
                                            loader.startGame(
                                                serverName = data.serverName,
                                                errorTitle = errorDialogTitle,
                                                errorText = errorDialogText
                                            )
                                        }
                                    }
                                ) {
                                    Text(text = state.translation("play", "Play"))
                                }

                                return@Column
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(32.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (loader.progress > 0) {
                LinearProgressIndicator(
                    progress = { loader.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                )
            }
        }
        var authSystem = AuthSystem.ELY_BY
        var authCompleted = mutableStateOf(false)
        when (dialogType) {
            DialogType.Accounts -> {
                AccountsDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                        authSystem = data["authSystem"] as AuthSystem
                        authCompleted = data["authCompleted"] as? MutableState<Boolean> ?: mutableStateOf(false)
                    })
                )
            }

            DialogType.Auth -> {
                AuthDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                    authSystem = authSystem,
                    authCompleted = authCompleted
                )
            }

            DialogType.Settings ->
                SettingsDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                )
            DialogType.Admin ->
                AdminDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                )

            DialogType.GameError -> {
                AlertDialog(
                    onDismissRequest = { dialogType = DialogType.None },
                    confirmButton = { dialogType = DialogType.None },
                    dismissButton = { dialogType = DialogType.None },
                    icon = {
                        Image(
                            painterResource(Res.drawable.raspberry),
                            contentDescription = "Error Window Icon"
                        )
                    },
                    title = errorDialogTitle.value,
                    text = errorDialogText.value,
                    tonalElevation = 8.dp
                )
            }
            DialogType.None -> Unit
        }
    }
}

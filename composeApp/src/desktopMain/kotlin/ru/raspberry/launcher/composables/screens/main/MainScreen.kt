package ru.raspberry.launcher.composables.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.*
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.form.RedirectForm
import ru.raspberry.launcher.composables.form.ServerForm
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.server.BasicServerData
import ru.raspberry.launcher.models.users.auth.AuthSystem
import ru.raspberry.launcher.service.GameLoader
import ru.raspberry.launcher.service.repositories.RedirectRepository
import ru.raspberry.launcher.service.repositories.ServerRepository
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.windows.MainWindowScreens
import ru.raspberry.launcher.windows.dialogs.AccountsDialog
import ru.raspberry.launcher.windows.dialogs.AuthDialog
import ru.raspberry.launcher.windows.dialogs.SettingsDialog
import ru.raspberry.launcher.windows.dialogs.admin.CRUDAbstractDialog
import kotlin.collections.mutableMapOf

enum class DialogType {
    None,
    Accounts,
    Auth,
    Settings,
    GameError,
    Redirects,
    Users,
    Servers
}
private var client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json)
    }
    headers {
        set(HttpHeaders.UserAgent, "Raspberry Launcher")
    }
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
    var errorDialogTitle by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var errorDialogText by remember { mutableStateOf<@Composable () -> Unit>({ }) }
    var selectedServerName by remember { mutableStateOf<String?>(null) }

    AppTheme(
        theme = state.theme,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader<MainWindowScreens, Unit>(
                windowData = state,
                customActions = {
                    if (state.adminMode) {
                        var adminExpanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { adminExpanded = !adminExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.edit),
                                modifier = Modifier.size(24.dp),
                                contentDescription = "Open Admin Panel"
                            )
                        }
                        DropdownMenu(
                            expanded = adminExpanded,
                            onDismissRequest = { adminExpanded = false },
                            modifier = Modifier.width(200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = "Servers") },
                                onClick = {
                                    dialogType = DialogType.Servers
                                    adminExpanded = false
                                }
                            )
//                            TODO: Create user admin panel
//                            DropdownMenuItem(
//                                text = { Text(text = "Users") },
//                                onClick = {
//                                    dialogType = DialogType.Users
//                                    adminExpanded = false
//                                }
//                            )
                            DropdownMenuItem(
                                text = { Text(text = "Redirects") },
                                onClick = {
                                    dialogType = DialogType.Redirects
                                    adminExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(
                                    text = "${if (state.config.debug) "Disable" else "Enable"} Debug"
                                ) },
                                onClick = {
                                    state.config.debug = !state.config.debug
                                    state.config.save()
                                    adminExpanded = false
                                }
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
                    IconButton(
                        onClick = { state.recompose() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.reload),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Reload"
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
                val serverDataCache = remember { mutableMapOf<Pair<String, String>, BasicServerData>() }
                var serverDataLoaded by remember { mutableStateOf(false) }
                fun selectServer(name: String) {
                    selectedServerName = name
                    serverDataLoaded = false
                    coroutine.launch {
                        val name = selectedServerName
                        if (name.isNullOrBlank()) {
                            errorDialogTitle = {
                                Text(text = "Server loading error!")
                            }
                            errorDialogText = {
                                Text(text = "Server name is empty or null!")
                            }
                            dialogType = DialogType.GameError
                            return@launch
                        }
                        var data = state.launcherService.getServerInfo(name, state.config.language)
                        if (data == null) {
                            errorDialogTitle = {
                                Text(text = "Server loading error!")
                            }
                            errorDialogText = {
                                Text(text = "Server data is null for server: $name")
                            }
                            dialogType = DialogType.GameError
                            return@launch
                        }
                        if (data.description == null)
                            data = state.launcherService.getServerInfo(name)
                        if (data == null) {
                            errorDialogTitle = {
                                Text(text = "Server loading error!")
                            }
                            errorDialogText = {
                                Text(text = "Server data is null for server: $name")
                            }
                            dialogType = DialogType.GameError
                            return@launch
                        }
                        serverDataCache.put(
                            Pair(name, state.config.language),
                            data
                        )
                        serverDataLoaded = true
                    }
                }

                val servers = remember {
                    runBlocking {
                        state.launcherService.listServers().toMutableList()
                    }
                }
                servers.removeIf {
                    runBlocking {
                        val data = state.launcherService.getServerInfo(it, state.config.language)
                        if (data == null) true
                        else {
                            serverDataCache[Pair(it, state.config.language)] = data
                            false
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(4f)
                ) {
                    items(servers) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .clickable {
                                    selectServer(name)
                                }
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, modifier = Modifier.padding(start = 8.dp))
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
                    Divider()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Display
                        if (serverDataLoaded) {
                            val data = serverDataCache[Pair(selectedServerName, state.config.language)]
                            if (data != null) {
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
                                remember {
                                    coroutine.launch {
                                        if (data.imageUrl == null) return@launch
                                        val response = client.get(urlString = data.imageUrl)
                                        val bytes: ByteArray = response.readRawBytes()
                                        val image = bytes.decodeToImageBitmap()
                                        imageCache.put(data.imageUrl, image)
                                        painter = BitmapPainter(
                                            image = image,
                                            filterQuality = FilterQuality.None
                                        )
                                    }
                                }
                                Image(
                                    painter = painter,
                                    contentDescription = "${data.name} Icon",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                                Text(
                                    text = data.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 25.sp,
                                    textAlign = TextAlign.Center,
                                )
                                Divider(thickness = 2.dp)
                                val descriptionScrollState = rememberScrollState()
                                Text(
                                    text = data.description ?: "",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(descriptionScrollState)
                                )
                                Button(
                                    onClick = {
                                        coroutine.launch {
                                            loader.start(
                                                serverName = data.name,
                                                error = { title, text ->
                                                    errorDialogTitle = title
                                                    errorDialogText = text
                                                    dialogType = DialogType.GameError
                                                },
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
            LinearProgressIndicator(
                progress = { loader.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )
        }
        var authSystem = AuthSystem.ELY_BY
        var authCompleted = mutableStateOf(false)
        when (dialogType) {
            DialogType.None -> Unit
            DialogType.Accounts ->
                AccountsDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                        authSystem = data["authSystem"] as AuthSystem
                        authCompleted = data["authCompleted"] as? MutableState<Boolean> ?: mutableStateOf(false)
                    })
                )
            DialogType.Auth ->
                AuthDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                    authSystem = authSystem,
                    authCompleted = authCompleted
                )
            DialogType.Settings ->
                SettingsDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                )
            DialogType.GameError ->
                AlertDialog(
                    onDismissRequest = { dialogType = DialogType.None },
                    confirmButton = {},
                    dismissButton = {
                        Button(
                            onClick = { dialogType = DialogType.None },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "OK")
                        }
                    },
                    icon = {
                        Image(
                            painterResource(Res.drawable.raspberry),
                            contentDescription = "Error Window Icon"
                        )
                    },
                    title = errorDialogTitle,
                    text = errorDialogText,
                    tonalElevation = 8.dp
                )
            DialogType.Redirects ->
                CRUDAbstractDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                    title = state.translation("admin.redirects", "Redirects"),
                    titlePresenter = {
                        key, data ->
                        "$key -> ${data?.url ?: "New Redirect"}"
                    },
                    repository = RedirectRepository(state.launcherService),
                    form = RedirectForm(RedirectRepository(state.launcherService))
                )
            DialogType.Users -> {}
            DialogType.Servers ->
                CRUDAbstractDialog(
                    state = state,
                    close = { dialogType = DialogType.None },
                    changeDialog = ({ type, data ->
                        dialogType = type
                    }),
                    title = state.translation("admin.servers", "Servers"),
                    titlePresenter = {
                            key, data -> key
                    },
                    repository = ServerRepository(state.launcherService),
                    form = ServerForm(state)
                )
        }
    }
}

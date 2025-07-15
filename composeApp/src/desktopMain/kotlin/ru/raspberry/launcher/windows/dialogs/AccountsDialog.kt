package ru.raspberry.launcher.windows.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.composables.screens.main.imageCache
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.users.auth.AccountRepository
import ru.raspberry.launcher.models.users.auth.AuthSystem
import ru.raspberry.launcher.service.LauncherServiceV1
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    var update by remember { mutableStateOf(false) }
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(400.dp, 350.dp)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = state.translation("accounts", "Accounts")
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
            val scrollState = rememberScrollState()
            val repository = remember {
                AccountRepository(config = state.config)
            }
            val metas = repository.getMeta()
            val coroutine = rememberCoroutineScope()
            var error by remember { mutableStateOf<String?>(null) }
            AnimatedVisibility(
                visible = error != null,
            ) {
                Text(
                    text = error ?: "",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error
                )
            }
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
                    val meta = metas[index]
                    key(update) {
                        Button(
                            onClick = {
                                val account = repository.getByMeta(meta)
                                coroutine.launch {
                                    if (account != null) {
                                        if (!state.minecraftService.isTokenValid(account)) {
                                            val authCompleted = mutableStateOf(false)
                                            changeDialog(DialogType.Auth, mapOf(
                                                "authCompleted" to authCompleted
                                            ))
                                            while (!authCompleted.value) {
                                                delay(100)
                                            }
                                        }
                                        state.activeAccount = account
                                        val result = state.launcherService.auth()
                                        when (result) {
                                            LauncherServiceV1.LauncherAuthResult.Success -> {
                                                state.config.activeAccountId = index
                                                state.config.save()
                                                update = !update
                                            }
                                            LauncherServiceV1.LauncherAuthResult.NoAccount -> {
                                                error = state.translation(
                                                    "accounts.error.no_account",
                                                    "Account not found!"
                                                )
                                                state.activeAccount = null
                                                async { delay(1000); error = null }
                                            }
                                            LauncherServiceV1.LauncherAuthResult.ConnectionFailed -> {
                                                error = state.translation(
                                                    "accounts.error.connection_failed",
                                                    "Connection failed!"
                                                )
                                                state.activeAccount = null
                                                async { delay(1000); error = null }
                                            }
                                            LauncherServiceV1.LauncherAuthResult.InvalidCredentials -> {
                                                error = state.translation(
                                                    "accounts.error.invalid_credentials",
                                                    "Invalid credentials!"
                                                )
                                                state.activeAccount = null
                                                async { delay(1000); error = null }
                                            }
                                            LauncherServiceV1.LauncherAuthResult.FailedToVerify -> {
                                                error = state.translation(
                                                    "accounts.error.failed_to_verify",
                                                    "Failed to verify account!"
                                                )
                                                state.activeAccount = null
                                                async { delay(1000); error = null }
                                            }
                                        }
                                    }
                                    else {
                                        error = state.translation(
                                            "accounts.error.no_account",
                                            "Account not found!"
                                        )
                                        async { delay(1000); error = null }
                                    }
                                }
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
                                val authSystemIcon = painterResource(meta.authSystem.drawableResource)
                                Image(
                                    painter = authSystemIcon,
                                    contentDescription = "${meta.authSystem.displayName} Icon",
                                    modifier = Modifier.size(40.dp)
                                )
                                var painter by remember {
                                    val image = imageCache.getOrDefault(meta.skinUrl, null)
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
                                        val image = state.minecraftService.getSkin(meta)
                                        imageCache.put(meta.skinUrl, image)
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
                                    contentDescription = "${meta.username} Account Icon",
                                    modifier = Modifier.size(60.dp)
                                )
                                Text(
                                    text = meta.username,
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
                                text = state.translation("accounts.login", "Login with %s")
                                    .format(system.displayName),
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
package ru.raspberry.launcher.windows.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.service.AccountRepository
import ru.raspberry.launcher.models.users.auth.AuthSystem
import ru.raspberry.launcher.service.MinecraftApiService
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
    authSystem: AuthSystem,
    authCompleted: MutableState<Boolean>,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = state.translation("auth", "Authentication in %s")
                .format(authSystem.displayName)
        )
    }
    val repository = remember {
        AccountRepository(state)
    }
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    val totpRequested: MutableState<Boolean> = remember {
        mutableStateOf(false)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .scrollable(scrollState, Orientation.Vertical),
            ) {
                AnimatedVisibility(
                    visible = errorMessage != null,
                ) {
                    Text(
                        text = errorMessage ?: "",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextField(
                    value = username,
                    onValueChange = {
                        username = it
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = {
                        Text(
                            text = state.translation("auth.username", "Username")
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = {
                        Text(
                            text = state.translation("auth.password", "Password")
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                if (totpRequested.value) {
                    TextField(
                        value = totp,
                        onValueChange = {
                            totp = it
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        label = {
                            Text(
                                text = state.translation("auth.totp", "TOTP")
                            )
                        }
                    )
                }
                else {
                    Spacer(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            when (state.minecraftService.auth(
                                repository = repository,
                                authSystem = authSystem,
                                username = username,
                                password = password,
                                if (totpRequested.value) totp else null
                            )) {
                                MinecraftApiService.MinecraftAuthResult.RequestedTOTP -> {
                                    totpRequested.value = true
                                    errorMessage = state.translation("auth.error.totp.enter",
                                        "TOTP is required for this account.")
                                    async { delay(1000); errorMessage = null }
                                }

                                MinecraftApiService.MinecraftAuthResult.InvalidTOTP -> {
                                    errorMessage = state.translation("auth.error.totp.invalid",
                                        "Invalid TOTP!")
                                    async { delay(1000); errorMessage = null }
                                }

                                MinecraftApiService.MinecraftAuthResult.InvalidCredentials -> {
                                    errorMessage = state.translation("auth.error.credentials.invalid",
                                        "Invalid username or password!")
                                    async { delay(1000); errorMessage = null }
                                }

                                MinecraftApiService.MinecraftAuthResult.UnexpectedError -> {
                                    errorMessage = state.translation("auth.error.unexpected",
                                        "Unexpected error!")
                                    async { delay(1000); errorMessage = null }
                                }

                                MinecraftApiService.MinecraftAuthResult.Successful -> {
                                    authCompleted.value = true
                                    changeDialog(DialogType.Accounts, mapOf())
                                    totpRequested.value = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(end = 8.dp, start = 16.dp),
                    content = {
                        Text(
                            text = state.translation("auth.login", "Login"),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
        }
    }
}
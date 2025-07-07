package ru.raspberry.launcher.windows.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.auth.AccountRepository
import ru.raspberry.launcher.models.auth.AuthSystem
import ru.raspberry.launcher.service.MinecraftApiService

@Composable
fun AuthDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
    authSystem: AuthSystem
) {
    val windowState = rememberDialogState(position = WindowPosition(Alignment.Center))
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = "Authentication ${authSystem.displayName}"
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
            Column {
                val repository = remember {
                    AccountRepository(config = state.config)
                }
                val coroutineScope = rememberCoroutineScope()
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var totp by remember { mutableStateOf("") }
                val totpRequested: MutableState<Boolean> = remember {
                    mutableStateOf(false)
                }
                var errorMessage by remember { mutableStateOf("") }
                Text(
                    text = errorMessage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(0.5f)
                        .padding(10.dp),
                    color = Color.Red
                )
                TextField(
                    value = username,
                    onValueChange = {
                        username = it
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = {
                        Text(
                            text = "Username",
                            modifier = Modifier.fillMaxSize()
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
                        .fillMaxSize()
                        .weight(1f)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = {
                        Text(
                            text = "Password",
                            modifier = Modifier.fillMaxSize()
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
                            .fillMaxSize()
                            .weight(1f)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        label = {
                            Text(
                                text = "TOTP",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                                    errorMessage = "TOTP is required for this account."
                                    async { delay(1000); errorMessage = "" }
                                }

                                MinecraftApiService.MinecraftAuthResult.InvalidTOTP -> {
                                    errorMessage = "Invalid TOTP."
                                    async { delay(1000); errorMessage = "" }
                                }

                                MinecraftApiService.MinecraftAuthResult.InvalidCredentials -> {
                                    errorMessage = "Invalid username or password."
                                    async { delay(1000); errorMessage = "" }
                                }

                                MinecraftApiService.MinecraftAuthResult.UnexpectedError -> {
                                    errorMessage = "Unexpected error!"
                                    async { delay(1000); errorMessage = "" }
                                }

                                MinecraftApiService.MinecraftAuthResult.Successful ->
                                    changeDialog(DialogType.Accounts, mapOf())
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(20.dp),
                    content = {
                        Text("Login")
                    },
                )
            }
        }
    }
}
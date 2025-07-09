package ru.raspberry.launcher.error

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogWindow
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.raspberry
import kotlin.system.exitProcess

@Composable
fun BetterErrorHandling(
    content: @Composable () -> Unit) {
    val errorState = remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(Unit) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            errorState.value = throwable
        }
    }

    if (errorState.value != null) {
        DialogWindow(
            onCloseRequest = { exitProcess(0) },
            title = "Unexpected error occurred!",
            icon = painterResource(Res.drawable.raspberry),
        ) {
            Text(
                text = errorState.value?.message ?: "Unknown error",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize(),
                softWrap = true
            )
        }
    }
    else content()
}
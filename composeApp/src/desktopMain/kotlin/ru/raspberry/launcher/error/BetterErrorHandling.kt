package ru.raspberry.launcher.error

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            throwable.printStackTrace()
        }
    }

    if (errorState.value != null) {
        DialogWindow(
            onCloseRequest = { exitProcess(0) },
            title = "Unexpected error occurred!",
            icon = painterResource(Res.drawable.raspberry),
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = errorState.value?.stackTraceToString() ?: "Unknown error",
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(scrollState),
                softWrap = true
            )
        }
    }
    else content()
}
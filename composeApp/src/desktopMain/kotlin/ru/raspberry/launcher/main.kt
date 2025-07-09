package ru.raspberry.launcher


import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.error.BetterErrorHandling
import ru.raspberry.launcher.theme.Theme
import ru.raspberry.launcher.windows.MainWindow

@OptIn(ExperimentalSerializationApi::class)
val GoodJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    allowComments = true
    allowTrailingComma = true
}

fun main() = application {
    BetterErrorHandling {
        MainWindow(
            close = ::exitApplication
        )
    }
}


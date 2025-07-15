package ru.raspberry.launcher


import androidx.compose.ui.window.application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.error.BetterErrorHandling
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


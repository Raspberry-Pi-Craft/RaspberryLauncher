package ru.raspberry.launcher


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.windows.MainWindow

@OptIn(ExperimentalSerializationApi::class)
val GoodJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    allowComments = true
    allowTrailingComma = true
}

fun main() = application {
    MainWindow(
        close = ::exitApplication
    )
}
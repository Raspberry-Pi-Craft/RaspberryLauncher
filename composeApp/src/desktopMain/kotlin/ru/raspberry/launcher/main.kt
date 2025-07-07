package ru.raspberry.launcher


import androidx.compose.ui.window.application
import ru.raspberry.launcher.windows.MainWindow


fun main() = application {
    MainWindow(
        close = ::exitApplication,
    )
}
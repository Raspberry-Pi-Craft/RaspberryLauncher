package ru.raspberry.launcher


import androidx.compose.ui.window.application
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.windows.MainWindow
import java.awt.Button
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.Label

@OptIn(ExperimentalSerializationApi::class)
val GoodJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    allowComments = true
    allowTrailingComma = true
}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Dialog(Frame(), e.message ?: "Error").apply {
            layout = FlowLayout()
            val label = Label(e.message)
            add(label)
            val button = Button("OK").apply {
                addActionListener { dispose() }
            }
            add(button)
            setSize(300,300)
            isVisible = true
        }
    }
    application {
        MainWindow(
            close = ::exitApplication
        )
    }
}
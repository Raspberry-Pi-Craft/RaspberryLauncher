package ru.raspberry.launcher.windows

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.raspberry
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.util.MultiOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
) {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val dialogState = rememberDialogState()
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = dialogState,
            currentScreen = mutableStateOf(Unit),
            close = close,
            title = state.translation("log", "Log"),
        )
    }
    Window(
        onCloseRequest = close,
        state = windowState,
        undecorated = true,
        resizable = true,
        icon = painterResource(Res.drawable.raspberry),
        title = state.translation("log", "Log"),
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
            val logChannel = Channel<String>(capacity = Channel.UNLIMITED) // буфер неограниченный
            val logLines = remember { mutableStateListOf<String>() }
            val listState = rememberLazyListState()

            remember {
                val originalOut = System.out
                val byteBuffer = mutableListOf<Byte>()
                System.setOut(PrintStream(object : OutputStream() {
                    override fun write(b: Int) {
                        byteBuffer.add(b.toByte())
                        if (b.toChar() == '\n') {
                            // преобразуем весь буфер в строку UTF-8
                            val line = byteBuffer.toByteArray().toString(Charsets.UTF_8)
                            byteBuffer.clear()

                            // отправляем в канал
                            logChannel.trySend(line)

                            // дублируем в оригинальный вывод
                            originalOut.print(line)
                        }
                    }
                }, true, Charsets.UTF_8))
            }

            LaunchedEffect(Unit) {
                logChannel.receiveAsFlow().collect { line ->
                    logLines.add(line)
                    if (logLines.size > 5000) logLines.removeFirst() // ограничиваем размер
                    listState.scrollToItem(logLines.size - 1) // автоскролл
                }
            }
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    items(logLines) { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
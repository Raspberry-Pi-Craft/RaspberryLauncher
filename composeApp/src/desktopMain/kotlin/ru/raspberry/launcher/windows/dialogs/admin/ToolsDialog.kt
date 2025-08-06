package ru.raspberry.launcher.windows.dialogs.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.raspberry.launcher.composables.components.Accordion
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.tools.servers.deprecated.DeprecatedServerData
import ru.raspberry.launcher.models.server.Server
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.models.tools.servers.ServerData
import ru.raspberry.launcher.service.repositories.ServerRepository
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens
import java.io.File


@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    decodeEnumsCaseInsensitive = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ToolsDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(null),
            close = close,
            title = "Users Management",
        )
    }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Importer(state)
                Migrator(state)
            }
        }
    }
}

@Composable
private fun Importer(state: WindowData<MainWindowScreens>) {
    Accordion(
        title = { Text("Import Server") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val coroutine = rememberCoroutineScope()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val result = remember { mutableStateOf<String?>(null) }
            AnimatedVisibility(visible = result.value != null) {
                Text(
                    text = result.value ?: "",
                    maxLines = 100
                )
            }

            var update by remember { mutableStateOf(false) }
            var path by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.height(70.dp)
            ) {
                TextField(
                    value = path,
                    onValueChange = { path = it },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = { Text(text = "Config Path") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                key(update) {
                    Button(
                        onClick = {
                            runBlocking {
                                val file = FileKit.openFilePicker()
                                if (file == null) return@runBlocking
                                path = file.path
                                update = !update
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(150.dp)
                            .padding(8.dp)
                    ) {
                        Text(text = state.translation("select", "Select..."))
                    }
                }
            }
            var oldFormat by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Old Format:", modifier = Modifier)
                Checkbox(
                    checked = oldFormat,
                    onCheckedChange = { oldFormat = it },
                    modifier = Modifier.padding(32.dp, 0.dp)
                )
            }
            Button(
                onClick = {
                    coroutine.launch {
                        importServer(state, File(path), oldFormat, result)
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Import")
            }
        }
    }
}
private suspend fun importServer(
    state: WindowData<MainWindowScreens>,
    file: File,
    oldFormat: Boolean = false,
    text: MutableState<String?>
) {
    if (!file.exists() || !file.isFile)
        throw IllegalArgumentException("Invalid config file: ${file.path}")

    var creation: Triple<String, String, String>
    var changes: List<Pair<ServerChanges, String>>
    if (oldFormat) {
        val data = json.decodeFromString<DeprecatedServerData>(file.readText())
        creation = data.generateRoot()
        changes = data.generateChanges()
    } else {
        val data = json.decodeFromString<ServerData>(file.readText())
        creation = data.generateRoot()
        changes = data.generateChanges()
    }
    text.value = ""
    val repository = ServerRepository(state.launcherService)
    try {
        repository.add(creation)
        text.value += "Server ${creation.first} created!"
        changes.forEach {
            repository.edit(creation.first, it)
            text.value += "\nLocale ${it.second} applied!"
        }
        text.value = "Server ${creation.first} import completed!"
    }
    catch (e: Exception) {
        repository.remove(creation.first) // Clean up if something goes wrong
        text.value = "Error: ${e.message ?: e.toString()}"
        e.printStackTrace()
    }
}

@Composable
private fun Migrator(state: WindowData<MainWindowScreens>) {
    Accordion(
        title = { Text("Try Migrate Server") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val coroutine = rememberCoroutineScope()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

        }
    }
}
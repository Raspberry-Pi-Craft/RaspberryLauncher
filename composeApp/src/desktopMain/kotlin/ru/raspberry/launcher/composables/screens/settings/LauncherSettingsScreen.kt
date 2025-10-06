package ru.raspberry.launcher.composables.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.raspberry.launcher.composables.components.Spinner
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.windows.MainWindowScreens

@Preview
@Composable
fun LauncherSettingsScreen(state: WindowData<MainWindowScreens>) {
    var update by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.height(70.dp)
            ) {
                TextField(
                    value = state.config.launcherDataPath,
                    onValueChange = {
                        state.config.launcherDataPath = it
                        state.config.save()
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = {
                        Text(
                            text = state.translation("settings.launcher.path", "Launcher Data Path")
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                key(update) {
                    Button(
                        onClick = {
                            runBlocking {
                                val file = FileKit.openDirectoryPicker()
                                if (file == null) return@runBlocking
                                state.config.launcherDataPath = file.path
                                state.config.save()
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
            Spinner(
                label = {
                    Text(state.translation("settings.launcher.language", "Language"))
                },
                options = state.languages.values.toList(),
                selectedOption = state.language,
                onOptionSelected = { selected ->
                    state.config.language = selected.id
                    state.config.save()
                    state.recompose()
                },
                toText = { it?.name ?: "No languages selected" },
                modifier = Modifier.fillMaxWidth().padding(4.dp),
            )
            Spinner(
                label = {
                    Text(state.translation("settings.launcher.theme", "Theme"))
                },
                options = state.themes.values.toList(),
                selectedOption = state.themes.getOrDefault(
                    state.config.theme,
                    state.themes.values.firstOrNull()
                ),
                onOptionSelected = { selected ->
                    state.config.theme = selected?.name ?: "Dark"
                    state.config.save()
                    state.recompose()
                },
                toText = { it?.name ?: "No themes selected" },
                modifier = Modifier.fillMaxWidth().padding(4.dp),
            )
            Row(
                modifier = Modifier.height(70.dp)
            ) {
                Text(
                    text = state.translation("settings.launcher.auto_update", "Auto Update Launcher"),
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(8.dp),
                )
                Checkbox(
                    checked = state.config.autoCheckForUpdates,
                    onCheckedChange = {
                        state.config.autoCheckForUpdates = it
                        state.config.save()
                        update = !update
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp),
                )
            }
            Row(
                modifier = Modifier.height(70.dp)
            ) {
                Text(
                    text = state.translation("settings.launcher.rich_presence", "Discord Rich Presence"),
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(8.dp),
                )
                Checkbox(
                    checked = state.config.richPresence,
                    onCheckedChange = {
                        if (it) state.discord.enableRichPresence()
                        else state.discord.disableRichPresence()
                        state.config.richPresence = it
                        state.config.save()
                        update = !update
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp),
                )
            }
        }
    }

}

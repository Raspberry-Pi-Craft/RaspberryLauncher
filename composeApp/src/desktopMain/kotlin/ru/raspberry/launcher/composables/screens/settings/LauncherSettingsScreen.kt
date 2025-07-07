package ru.raspberry.launcher.composables.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.language
import raspberrylauncher.composeapp.generated.resources.theme
import ru.raspberry.launcher.Language
import ru.raspberry.launcher.composables.components.Spinner
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.theme.Theme
import ru.raspberry.launcher.windows.MainWindowScreens
import java.util.Locale

@Preview
@Composable
fun LauncherSettingsScreen(state: WindowData<MainWindowScreens>) {
    var update by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
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
                        text = "Launcher Data Path",
                        modifier = Modifier.fillMaxSize()
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
                        .width(120.dp)
                        .padding(8.dp)
                ) {
                    Text(text = "Select...")
                }
            }
        }
        Spinner(
            label = stringResource(Res.string.language),
            options = Language.entries,
            selectedOption = Language.entries.indexOf(
                Language.entries.find {
                    language -> Locale.getDefault() == language.locale
                }
            ),
            toText = { it?.displayName ?: "No languages selected" },
            onOptionSelected = { selected ->
                Locale.setDefault(selected.locale)
                update = !update
            },
            modifier = Modifier.fillMaxWidth().padding(4.dp),
        )
        Spinner(
            label = stringResource(Res.string.theme),
            options = state.themes.values.toList(),
            selectedOption = state.themes.keys.toList().indexOf(state.config.theme),
            toText = { it?.name ?: "No themes selected"},
            onOptionSelected = { selected ->
                state.config.theme = selected.name
                state.config.save()
                update = !update
            },
            modifier = Modifier.fillMaxWidth().padding(4.dp),
        )
    }

}

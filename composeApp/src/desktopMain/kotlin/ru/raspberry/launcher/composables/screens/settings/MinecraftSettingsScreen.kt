package ru.raspberry.launcher.composables.screens.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.windows.MainWindowScreens

@Preview
@Composable
fun MinecraftSettingsScreen(
    state: WindowData<MainWindowScreens>
) {
    var update by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .scrollable(scrollState, Orientation.Vertical),
    ) {
        Row(
            modifier = Modifier.height(70.dp)
        ) {
            TextField(
                value = state.config.minecraftPath,
                onValueChange = {
                    state.config.minecraftPath = it
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
                        text = "Minecraft Path",
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
                            state.config.minecraftPath = file.path
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
        TextField(
            value = state.config.ram.toString(),
            onValueChange = {
                state.config.ram = it.toInt()
                state.config.save()
                update = !update
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            label = {
                Text(
                    text = "Minecraft Memory Limit",
                    modifier = Modifier.fillMaxSize()
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
        Row (
            modifier = Modifier
                .height(70.dp)
        ) {
            TextField(
                value = state.config.minecraftWindowWidth.toString(),
                onValueChange = {
                    val value = it.toIntOrNull()
                    if (value == null) return@TextField
                    state.config.minecraftWindowWidth = value
                    state.config.save()
                    update = !update
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                label = {
                    Text(
                        text = "Minecraft Window Width",
                        modifier = Modifier.fillMaxSize()
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
            TextField(
                value = state.config.minecraftWindowHeight.toString(),
                onValueChange = {
                    val value = it.toIntOrNull()
                    if (value == null) return@TextField
                    state.config.minecraftWindowHeight = value
                    state.config.save()
                    update = !update
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                label = {
                    Text(
                        text = "Minecraft Window Height",
                        modifier = Modifier.fillMaxSize()
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
        }
        Row(
            modifier = Modifier.height(70.dp)
        ) {
            Text(
                text = "Use Discrete GPU",
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(8.dp),
            )
            Checkbox(
                checked = state.config.useDiscreteGPU,
                onCheckedChange = {
                    state.config.useDiscreteGPU = it
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
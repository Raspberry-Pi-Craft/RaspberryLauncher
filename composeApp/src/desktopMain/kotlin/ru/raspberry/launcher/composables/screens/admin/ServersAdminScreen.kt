package ru.raspberry.launcher.composables.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.chevron_right
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.java.JavaData
import ru.raspberry.launcher.windows.MainWindowScreens


@Preview
@Composable
fun ServersAdminScreen(state: WindowData<MainWindowScreens>) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
    ) {
        val coroutine = rememberCoroutineScope()
        var serverName by remember { mutableStateOf("") }
        var serverAddress by remember { mutableStateOf("") }
        var serverImageUrl by remember { mutableStateOf("") }
        var javaName by remember { mutableStateOf("") }
        var javaVersion by remember { mutableStateOf("") }

        var windowsJavaUrl by remember { mutableStateOf("") }
        var windowsExecutablePath by remember { mutableStateOf("") }
        var linuxJavaUrl by remember { mutableStateOf("") }
        var linuxExecutablePath by remember { mutableStateOf("") }
        var macosJavaUrl by remember { mutableStateOf("") }
        var macosExecutablePath by remember { mutableStateOf("") }

        var message by remember { mutableStateOf<String?>(null) }
        var errorMsg by remember { mutableStateOf(true) }
        AnimatedVisibility(
            visible = message != null,
        ) {
            Text(
                text = message ?: "",
                color = if (errorMsg) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
            )
        }
        TextField(
            value = serverName,
            onValueChange = { serverName = it },
            label = {
                Text("Server Name")
            },
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
        )
        TextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            label = {
                Text("Server Address")
            },
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
        )
        TextField(
            value = serverImageUrl,
            onValueChange = { serverImageUrl = it },
            label = {
                Text("Server Image URL")
            },
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
        )
        var expanded by remember { mutableStateOf(false) }
        val degrees by animateFloatAsState(if (expanded) -90f else 90f)
        Row(modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { expanded = expanded.not() }
            .fillMaxWidth()
            .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Java",
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                painter = painterResource(Res.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier.rotate(degrees).size(24.dp),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntSize.VisibilityThreshold
                )
            ),
            exit = shrinkVertically()
        ) {
            Column {
                TextField(
                    value = javaName,
                    onValueChange = { javaName = it},
                    label = {
                        Text("Java Name")
                    },
                    modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                )
                TextField(
                    value = javaVersion,
                    onValueChange = { javaVersion = it},
                    label = {
                        Text("Java Version")
                    },
                    modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                )

                var windowsExpanded by remember { mutableStateOf(false) }
                val windowsDegrees by animateFloatAsState(if (windowsExpanded) -90f else 90f)
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable { windowsExpanded = windowsExpanded.not() }
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Windows",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.chevron_right),
                        contentDescription = null,
                        modifier = Modifier.rotate(windowsDegrees).size(24.dp),
                    )
                }
                AnimatedVisibility(
                    visible = windowsExpanded,
                    enter = expandVertically(
                        spring(
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        )
                    ),
                    exit = shrinkVertically()
                ) {
                    Column {
                        TextField(
                            value = windowsJavaUrl,
                            onValueChange = { windowsJavaUrl = it },
                            label = {
                                Text("Java Download Url")
                            },
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        )
                        TextField(
                            value = windowsExecutablePath,
                            onValueChange = { windowsExecutablePath = it },
                            label = {
                                Text("Executable Path")
                            },
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        )
                    }
                }
                var linuxExpanded by remember { mutableStateOf(false) }
                val linuxDegrees by animateFloatAsState(if (linuxExpanded) -90f else 90f)
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable { linuxExpanded = linuxExpanded.not() }
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Linux",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.chevron_right),
                        contentDescription = null,
                        modifier = Modifier.rotate(linuxDegrees).size(24.dp),
                    )
                }
                AnimatedVisibility(
                    visible = linuxExpanded,
                    enter = expandVertically(
                        spring(
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        )
                    ),
                    exit = shrinkVertically()
                ) {
                    Column {
                        TextField(
                            value = linuxJavaUrl,
                            onValueChange = { linuxJavaUrl = it },
                            label = {
                                Text("Java Download Url")
                            },
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        )
                        TextField(
                            value = linuxExecutablePath,
                            onValueChange = { linuxExecutablePath = it },
                            label = {
                                Text("Executable Path")
                            },
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        )
                    }
                }
                var macosExpanded by remember { mutableStateOf(false) }
                val macosDegrees by animateFloatAsState(if (macosExpanded) -90f else 90f)
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable { macosExpanded = macosExpanded.not() }
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Windows",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.chevron_right),
                        contentDescription = null,
                        modifier = Modifier.rotate(macosDegrees).size(24.dp),
                    )
                }
                AnimatedVisibility(
                    visible = macosExpanded,
                    enter = expandVertically(
                        spring(
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        )
                    ),
                    exit = shrinkVertically()
                ) {
                    Column {
                        TextField(
                            value = macosJavaUrl,
                            onValueChange = { macosJavaUrl = it },
                            label = {
                                Text("Java Download Url")
                            },
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        )
                        TextField(
                            value = macosExecutablePath,
                            onValueChange = { macosExecutablePath = it },
                            label = {
                                Text("Executable Path")
                            },
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                coroutine.launch {
                    val response = state.launcherService.createServer(
                        serverName = serverName,
                        serverAddress = serverAddress,
                        imageUrl = serverImageUrl.ifEmpty { null },
                        java = JavaData(
                            name = javaName,
                            version = javaVersion,
                            windowsDownloadUrl = windowsJavaUrl,
                            windowsExecutablePath = windowsExecutablePath,
                            linuxDownloadUrl = linuxJavaUrl,
                            linuxExecutablePath = linuxExecutablePath,
                            macosDownloadUrl = macosJavaUrl,
                            macosExecutablePath = macosExecutablePath,
                        )
                    )
                    errorMsg = !response.status.isSuccess()
                    message = response.bodyAsText()
                    async {
                        delay(2000)
                        message = null
                        delay(2000)
                        if (!errorMsg) state.recompose()
                    }
                }
            },
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
        ) {
            Text("Create Server")
        }
    }
}
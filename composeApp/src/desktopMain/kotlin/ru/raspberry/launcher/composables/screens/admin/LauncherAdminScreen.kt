package ru.raspberry.launcher.composables.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.raspberry.launcher.AppConfig
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.dtos.LauncherInfo
import ru.raspberry.launcher.windows.MainWindowScreens
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun LauncherAdminScreen(state: WindowData<MainWindowScreens>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        var version by remember { mutableStateOf(state.launcherInfo?.version ?: AppConfig.version) }
        var downloadUrl by remember { mutableStateOf(state.launcherInfo?.downloadUrl ?: "") }
        val coroutine = rememberCoroutineScope()
        var message by remember { mutableStateOf<String?>(null) }
        var errorMsg by remember { mutableStateOf(true) }
        AnimatedVisibility(
            visible = message != null,
        ) {
            Text(
                text = message ?: "",
                color = if (errorMsg) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        TextField(
            value = version,
            onValueChange = {
                version = it
            },
            singleLine = true,
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            label = {
                Text(
                    text = state.translation("admin.launcher.version", "Version")
                )
            }
        )
        TextField(
            value = downloadUrl,
            onValueChange = {
                downloadUrl = it
            },
            singleLine = true,
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            label = {
                Text(
                    text = state.translation("admin.launcher.url", "Download URL")
                )
            }
        )
        Button(
            onClick = {
                if (downloadUrl.isEmpty()) {
                    errorMsg = true
                    message = "Empty download URL"
                    return@Button
                }
                val info = LauncherInfo(
                    version = version,
                    downloadUrl = downloadUrl,
                    lastUpdated = LocalDateTime.Formats.ISO.format(
                        Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    ),
                )
                coroutine.launch {
                    val response = state.launcherService.releaseNewLauncherVersion(info)
                    if (response.status.isSuccess()) {
                        state.launcherInfo = info
                        message = response.bodyAsText()
                        errorMsg = false
                        async { delay(2000); message = null }
                    } else {
                        message = response.bodyAsText()
                        errorMsg = true
                        async { delay(2000); message = null }
                    }
                }
            },
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = state.translation("admin.launcher.create", "Create release")
            )
        }
        Spacer(modifier = Modifier.weight(2f))
    }
}
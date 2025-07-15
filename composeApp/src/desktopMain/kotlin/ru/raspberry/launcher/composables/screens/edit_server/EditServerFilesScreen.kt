package ru.raspberry.launcher.composables.screens.edit_server

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.windows.MainWindowScreens

private var client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json)
    }
    headers {
        set(HttpHeaders.UserAgent, "Raspberry Launcher")
    }
}
@Preview
@Composable
fun EditServerFilesScreen(state: WindowData<MainWindowScreens>, serverName: String) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
    ) {

    }
}
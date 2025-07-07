package ru.raspberry.launcher.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.auth.Account
import ru.raspberry.launcher.service.LauncherServiceV1
import ru.raspberry.launcher.service.MinecraftApiService
import ru.raspberry.launcher.theme.Theme


data class WindowData<S>(
    private val currentScreen: MutableState<S>,
    private val windowState: WindowState,
    val title: String,
    val close: () -> Unit,
    val maximize: () -> Unit,
    val minimize: () -> Unit,
    var config: Config = Config(),
    var activeAccount : Account? = null,
    val themes: Map<String, Theme>,
) {
    val theme: Theme
        get() = themes[config.theme] ?: Theme("Default", true)
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    val launcherService = LauncherServiceV1<S>(this)
    val minecraftService = MinecraftApiService<S>(this)

    fun changeScreen(screen: S) {
        currentScreen.value = screen
    }

    fun resize(size: DpSize) {
        windowState.size = size
    }
    fun changePlacement(placement: WindowPlacement) {
        windowState.placement = placement
    }
    fun resize(width: Dp, height: Dp) {
        resize(DpSize(width, height))
    }
    fun move(position: WindowPosition) {
        windowState.position = position
    }
    fun move(x: Dp, y: Dp) {
        move(WindowPosition(x, y))
    }
    val size = windowState.size
    val position = windowState.position
    val placement = windowState.placement
    var loaded = mutableStateOf(false)
}
package ru.raspberry.launcher.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import ru.raspberry.launcher.Language
import ru.raspberry.launcher.models.users.auth.Account
import ru.raspberry.launcher.models.dtos.LauncherInfo
import ru.raspberry.launcher.models.users.auth.AccountRepository
import ru.raspberry.launcher.service.DiscordIntegration
import ru.raspberry.launcher.service.LauncherServiceV1
import ru.raspberry.launcher.service.MinecraftApiService
import ru.raspberry.launcher.theme.Theme


data class WindowData<S>(
    private val currentScreen: MutableState<S>,
    private val windowState: WindowState,
    val discord: DiscordIntegration,
    val close: () -> Unit,
    val maximize: () -> Unit,
    val minimize: () -> Unit,
    var config: Config = Config(),
    var activeAccount: Account? = null,
    val themes: Map<String, Theme>,
    val languages: Map<String, Language>,
    val recompose: () -> Unit,
    var isAccountAdmin: Boolean = false,
    var adminMode: Boolean = false,
    val os: OS
) {
    var launcherInfo: LauncherInfo? = null

    val theme: Theme
        get() = themes[config.theme] ?: Theme("Default", true)
    val language: Language
        get() = languages[config.language] ?: Language("Unknown")

    fun translation(key: String, default: String = ""): String {
        return language.get(key, default)
    }
    val launcherService = LauncherServiceV1<S>(this)
    val minecraftService = MinecraftApiService(AccountRepository(config))

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
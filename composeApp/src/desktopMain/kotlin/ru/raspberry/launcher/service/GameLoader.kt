package ru.raspberry.launcher.service

import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.windows.MainWindowScreens

class GameLoader(
    private val state: WindowData<MainWindowScreens>
) {
    var step : Int = 0
    private var max : Int = 100
    val progress: Float
        get() = step.toFloat() / max.toFloat()

    suspend fun startGame() {

        // After load
        state.close()
    }
}
package ru.raspberry.launcher.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition

data class DialogData<T, P>(
    private val currentScreen: MutableState<T>,
    private val dialogState: androidx.compose.ui.window.DialogState,
    val title: String,
    val close: () -> Unit,
    private val parent: WindowData<P>,
) {
    fun changeScreen(screen: T) {
        currentScreen.value = screen
    }

    fun resize(size: DpSize) {
        dialogState.size = size
    }
    fun resize(width: Dp, height: Dp) {
        resize(DpSize(width, height))
    }
    fun move(position: WindowPosition) {
        dialogState.position = position
    }
    fun move(x: Dp, y: Dp) {
        move(WindowPosition(x, y))
    }
    val size = dialogState.size
    val position = dialogState.position
    var loaded = mutableStateOf(false)
}
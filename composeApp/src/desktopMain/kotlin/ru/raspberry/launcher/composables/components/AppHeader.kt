package ru.raspberry.launcher.composables.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.isTraySupported
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.close
import raspberrylauncher.composeapp.generated.resources.maximize
import raspberrylauncher.composeapp.generated.resources.minimize
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun<T, P> WindowScope.AppHeader(
    customActions: @Composable () -> Unit = {},
    windowData: WindowData<T>? = null,
    dialogData: DialogData<T, P>? = null
) {
    WindowDraggableArea {
        TopAppBar(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            title = {
                Text(windowData?.title ?: dialogData?.title ?: "No Title")
            },
            actions = {
                customActions()

                if (isTraySupported && windowData?.minimize != null) {
                    IconButton(
                        onClick = windowData.minimize,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.minimize),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Minimize"
                        )
                    }
                }
                if (windowData?.maximize != null) {
                    IconButton(
                        onClick = windowData.maximize,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.maximize),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Maximize"
                        )
                    }
                }
                if (windowData?.close != null || dialogData?.close != null) {
                    IconButton(
                        onClick = windowData?.close ?: dialogData?.close ?: {},
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Close"
                        )
                    }
                }
            },
            elevation = 0.dp
        )
    }
}
package ru.raspberry.launcher.composables.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Colors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun<T, P> WindowScope.AppHeader(
    customActions: @Composable () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    windowData: WindowData<T>? = null,
    dialogData: DialogData<T, P>? = null,
) {
    WindowDraggableArea {
        TopAppBar(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            title = {
                Text(windowData?.translation("app_name", "Window") ?: dialogData?.title ?: "No Title")
            },
            colors = colors,
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
            }
        )
    }
}
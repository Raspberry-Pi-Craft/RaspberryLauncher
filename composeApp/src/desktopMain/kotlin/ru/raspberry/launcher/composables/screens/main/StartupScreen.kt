package ru.raspberry.launcher.composables.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.raspberry
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.service.LauncherLoader
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.windows.MainWindowScreens

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun WindowScope.StartupScreen(state: WindowData<MainWindowScreens>) {
    val coroutineScope = rememberCoroutineScope()
    val text = mutableStateOf("Loading...")
    val launcherLoader = LauncherLoader(text, state)
    coroutineScope.launch {
        launcherLoader.start()
    }

    AppTheme(
        theme = state.theme,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader<MainWindowScreens, Unit>(
                windowData = state
            )
        }
    ) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painterResource(Res.drawable.raspberry),
                "Raspberry Launcher",
                modifier = Modifier
                    .weight(1f),
            )
            LinearProgressIndicator(
                modifier = Modifier,
                progress = { launcherLoader.progress },
            )
            Text(
                text = text.value,
                modifier = Modifier
                    .weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
package ru.raspberry.launcher.composables.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.windows.MainWindowScreens

@Preview
@Composable
fun UsersAdminScreen(state: WindowData<MainWindowScreens>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {

    }
}
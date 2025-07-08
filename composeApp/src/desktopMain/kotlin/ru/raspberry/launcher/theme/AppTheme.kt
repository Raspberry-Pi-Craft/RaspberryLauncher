package ru.raspberry.launcher.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun AppTheme(
    theme: Theme,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.colorScheme
    ) {
        Surface(
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation
        ) {
            Column {
                topBar()
                content()
            }
        }
    }
}
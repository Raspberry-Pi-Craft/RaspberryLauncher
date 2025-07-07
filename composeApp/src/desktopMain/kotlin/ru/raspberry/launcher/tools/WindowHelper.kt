package ru.raspberry.launcher.tools

import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.geom.RoundRectangle2D

fun roundCorners(window: ComposeDialog, arcw: Double = 20.0, arch: Double = 20.0) {
    window.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            window.shape = RoundRectangle2D.Double(
                0.0,
                0.0,
                window.width.toDouble(),
                window.height.toDouble(),
                arcw,
                arch
            )
        }
    })
}
fun roundCorners(window: ComposeWindow, arcw: Double = 20.0, arch: Double = 20.0) {
    window.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            window.shape = RoundRectangle2D.Double(
                0.0,
                0.0,
                window.width.toDouble(),
                window.height.toDouble(),
                arcw,
                arch
            )
        }
    })
}
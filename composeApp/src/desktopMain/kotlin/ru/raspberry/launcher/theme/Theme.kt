package ru.raspberry.launcher.theme

import androidx.compose.material.Colors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.String

@Serializable
data class Theme(
    val name: String,
    val isDark: Boolean,
    val primary: String? = null,
    val onPrimary: String? = null,
    val primaryContainer: String? = null,
    val onPrimaryContainer: String? = null,
    val inversePrimary: String? = null,
    val secondary: String? = null,
    val onSecondary: String? = null,
    val secondaryContainer: String? = null,
    val onSecondaryContainer: String? = null,
    val tertiary: String? = null,
    val onTertiary: String? = null,
    val tertiaryContainer: String? = null,
    val onTertiaryContainer: String? = null,
    val background: String? = null,
    val onBackground: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val surfaceVariant: String? = null,
    val onSurfaceVariant: String? = null,
    val surfaceTint: String? = null,
    val inverseSurface: String? = null,
    val inverseOnSurface: String? = null,
    val error: String? = null,
    val onError: String? = null,
    val errorContainer: String? = null,
    val onErrorContainer: String? = null,
    val outline: String? = null,
    val outlineVariant: String? = null,
    val scrim: String? = null,
    val surfaceBright: String? = null,
    val surfaceDim: String? = null,
    val surfaceContainer: String? = null,
    val surfaceContainerHigh: String? = null,
    val surfaceContainerHighest: String? = null,
    val surfaceContainerLow: String? = null,
    val surfaceContainerLowest: String? = null
) {
    @Transient
    private val dark = darkColorScheme()
    @Transient
    private val light = lightColorScheme()

    val primaryColor get() = parseColor(primary, {it.primary})
    val onPrimaryColor get() = parseColor(onPrimary, {it.onPrimary})
    val primaryContainerColor get() = parseColor(primaryContainer, {it.primaryContainer})
    val onPrimaryContainerColor get() = parseColor(onPrimaryContainer, {it.onPrimaryContainer})
    val inversePrimaryColor get() = parseColor(inversePrimary, {it.inversePrimary})
    val secondaryColor get() = parseColor(secondary, {it.secondary})
    val onSecondaryColor get() = parseColor(onSecondary, {it.onSecondary})
    val secondaryContainerColor get() = parseColor(secondaryContainer, {it.secondaryContainer})
    val onSecondaryContainerColor get() = parseColor(onSecondaryContainer, {it.onSecondaryContainer})
    val tertiaryColor get() = parseColor(tertiary, {it.tertiary})
    val onTertiaryColor get() = parseColor(onTertiary, {it.onTertiary})
    val tertiaryContainerColor get() = parseColor(tertiaryContainer, {it.tertiaryContainer})
    val onTertiaryContainerColor get() = parseColor(onTertiaryContainer, {it.onTertiaryContainer})
    val backgroundColor get() = parseColor(background, {it.background})
    val onBackgroundColor get() = parseColor(onBackground, {it.onBackground})
    val surfaceColor get() = parseColor(surface, {it.surface})
    val onSurfaceColor get() = parseColor(onSurface, {it.onSurface})
    val surfaceVariantColor get() = parseColor(surfaceVariant, {it.surfaceVariant})
    val onSurfaceVariantColor get() = parseColor(onSurfaceVariant, {it.onSurfaceVariant})
    val surfaceTintColor get() = parseColor(surfaceTint, {it.surfaceTint})
    val inverseSurfaceColor get() = parseColor(inverseSurface, {it.inverseSurface})
    val inverseOnSurfaceColor get() = parseColor(inverseOnSurface, {it.inverseOnSurface})
    val errorColor get() = parseColor(error, {it.error})
    val onErrorColor get() = parseColor(onError, {it.onError})
    val errorContainerColor get() = parseColor(errorContainer, {it.errorContainer})
    val onErrorContainerColor get() = parseColor(onErrorContainer, {it.onErrorContainer})
    val outlineColor get() = parseColor(outline, {it.outline})
    val outlineVariantColor get() = parseColor(outlineVariant, {it.outlineVariant})
    val scrimColor get() = parseColor(scrim, {it.scrim})
    val surfaceBrightColor get() = parseColor(surfaceBright, {it.surfaceBright})
    val surfaceDimColor get() = parseColor(surfaceDim, {it.surfaceDim})
    val surfaceContainerColor get() = parseColor(surfaceContainer, {it.surfaceContainer})
    val surfaceContainerHighColor get() = parseColor(surfaceContainerHigh, {it.surfaceContainerHigh})
    val surfaceContainerHighestColor get() = parseColor(surfaceContainerHighest, {it.surfaceContainerHighest})
    val surfaceContainerLowColor get() = parseColor(surfaceContainerLow, {it.surfaceContainerLow})
    val surfaceContainerLowestColor get() = parseColor(surfaceContainerLowest, {it.surfaceContainerLowest})
    
    val colorScheme: ColorScheme
        get() = ColorScheme(
            primaryColor,
            onPrimaryColor,
            primaryContainerColor,
            onPrimaryContainerColor,
            inversePrimaryColor,
            secondaryColor,
            onSecondaryColor,
            secondaryContainerColor,
            onSecondaryContainerColor,
            tertiaryColor,
            onTertiaryColor,
            tertiaryContainerColor,
            onTertiaryContainerColor,
            backgroundColor,
            onBackgroundColor,
            surfaceColor,
            onSurfaceColor,
            surfaceVariantColor,
            onSurfaceVariantColor,
            surfaceTintColor,
            inverseSurfaceColor,
            inverseOnSurfaceColor,
            errorColor,
            onErrorColor,
            errorContainerColor,
            onErrorContainerColor,
            outlineColor,
            outlineVariantColor,
            scrimColor,
            surfaceBrightColor,
            surfaceDimColor,
            surfaceContainerColor,
            surfaceContainerHighColor,
            surfaceContainerHighestColor,
            surfaceContainerLowColor,
            surfaceContainerLowestColor,
            
        )

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseColor(string: String? = null, selector: (ColorScheme) -> Color): Color {
        return if (string != null)
            Color(
                string.replace("#", "").hexToLong()
            )
        else if (isDark)
            selector(dark)
        else
            selector(light)
    }
}

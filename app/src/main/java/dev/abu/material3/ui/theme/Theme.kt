package dev.abu.material3.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanDark,
    surface = OceanDark, // Make surface same as background for seamless look, or OceanSurface
    onPrimary = OceanDark,
    onSecondary = OceanDark,
    onTertiary = OceanDark,
    onBackground = OnOceanDark,
    onSurface = OnOceanDark,
)

@Composable
fun LisyoTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = OceanColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

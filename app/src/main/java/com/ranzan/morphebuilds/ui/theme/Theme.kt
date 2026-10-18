package com.ranzan.morphebuilds.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PureBlackColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = PureBlack,
    secondaryContainer = AccentGreenContainer,
    onSecondaryContainer = AccentGreenText,
    tertiary = AccentOrange,
    onTertiary = PureBlack,
    tertiaryContainer = AccentOrangeContainer,
    onTertiaryContainer = AccentOrangeText,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = PureBlack,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkBorder,
    outlineVariant = DarkBorderLight,
    error = AccentRed,
    onError = PureBlack,
    errorContainer = AccentRedContainer,
    onErrorContainer = AccentRedText
)

@Composable
fun MorpheBuildsTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = PureBlack.toArgb()
            window.navigationBarColor = PureBlack.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = PureBlackColorScheme,
        typography = Typography,
        content = content
    )
}
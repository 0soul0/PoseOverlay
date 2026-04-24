package com.example.poseoverlay.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GPhotosLightScheme = lightColorScheme(
    primary              = GBlue700,
    onPrimary            = Color.White,
    primaryContainer     = GBlueCont,
    onPrimaryContainer   = Color(0xFF001849),
    secondary            = Color(0xFF545F71),   // Google blue-gray
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary             = Color(0xFF6B5778),   // muted purple — used for Albums
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFF2DAFF),
    onTertiaryContainer  = Color(0xFF251431),
    error                = GRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    background           = GSurface1,
    onBackground         = GText900,
    surface              = GSurface0,
    onSurface            = GText900,
    surfaceVariant       = GSurface2,
    onSurfaceVariant     = GText600,
    outline              = GOutline,
    outlineVariant       = Color(0xFFECEEF0),
    scrim                = Color(0xFF000000),
    inverseSurface       = GDarkBg,
    inverseOnSurface     = GTextDk900,
    inversePrimary       = GBlue200,
)

private val GPhotosDarkScheme = darkColorScheme(
    primary              = GBlue200,
    onPrimary            = Color(0xFF00305F),
    primaryContainer     = GBlueContDk,
    onPrimaryContainer   = GBlueCont,
    secondary            = Color(0xFFBCC7DB),
    onSecondary          = Color(0xFF263141),
    secondaryContainer   = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary             = Color(0xFFD8BBE8),
    onTertiary           = Color(0xFF3B2948),
    tertiaryContainer    = Color(0xFF533F5F),
    onTertiaryContainer  = Color(0xFFF2DAFF),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = GDarkBg,
    onBackground         = GTextDk900,
    surface              = GDarkSurf,
    onSurface            = GTextDk900,
    surfaceVariant       = GDarkSurfV,
    onSurfaceVariant     = GTextDk600,
    outline              = GDarkOutline,
    outlineVariant       = Color(0xFF444749),
    scrim                = Color(0xFF000000),
    inverseSurface       = GSurface1,
    inverseOnSurface     = GText900,
    inversePrimary       = GBlue700,
)

@Composable
fun PoseOverlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Google Photos respects Material You on Android 12+ — keep true
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> GPhotosDarkScheme
        else      -> GPhotosLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Google Photos blends status bar into background
            window.statusBarColor     = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val ctrl = WindowCompat.getInsetsController(window, view)
            ctrl.isAppearanceLightStatusBars     = !darkTheme
            ctrl.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = GPhotosShapes,
        content     = content
    )
}
package com.example.project_for_vtb.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = BankPrimaryDark,
    secondary = BankSecondary,
    tertiary = IncomeColor,
    background = BankSurfaceDark,
    surface = BankSurfaceDark,
    onPrimary = BankOnSurfaceDark,
    onSecondary = BankOnSurfaceDark,
    onTertiary = BankOnSurfaceDark,
    onBackground = BankOnSurfaceDark,
    onSurface = BankOnSurfaceDark,
    error = BankError,
    onError = BankOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = BankPrimary,
    secondary = BankSecondary,
    tertiary = BankPrimaryLight,
    background = BankSurface,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = BankOnSurface,
    onSurface = BankOnSurface,
    error = BankError,
    onError = Color.White
)

@Composable
fun Project_for_VTBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color отключен для банковской цветовой схемы
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
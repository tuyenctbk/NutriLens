package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NutriPrimaryDark,
    onPrimary = NutriOnPrimaryDark,
    primaryContainer = NutriPrimaryContainerDark,
    onPrimaryContainer = NutriOnPrimaryContainerDark,
    secondary = NutriSecondaryDark,
    onSecondary = NutriOnSecondaryDark,
    secondaryContainer = NutriSecondaryContainerDark,
    onSecondaryContainer = NutriOnSecondaryContainerDark,
    tertiary = NutriTertiaryDark,
    onTertiary = NutriOnTertiaryDark,
    tertiaryContainer = NutriTertiaryContainerDark,
    onTertiaryContainer = NutriOnTertiaryContainerDark,
    background = NutriBackgroundDark,
    onBackground = NutriOnBackgroundDark,
    surface = NutriSurfaceDark,
    onSurface = NutriOnSurfaceDark,
    surfaceVariant = NutriSurfaceVariantDark,
    onSurfaceVariant = NutriOnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = NutriPrimaryLight,
    onPrimary = NutriOnPrimaryLight,
    primaryContainer = NutriPrimaryContainerLight,
    onPrimaryContainer = NutriOnPrimaryContainerLight,
    secondary = NutriSecondaryLight,
    onSecondary = NutriOnSecondaryLight,
    secondaryContainer = NutriSecondaryContainerLight,
    onSecondaryContainer = NutriOnSecondaryContainerLight,
    tertiary = NutriTertiaryLight,
    onTertiary = NutriOnTertiaryLight,
    tertiaryContainer = NutriTertiaryContainerLight,
    onTertiaryContainer = NutriOnTertiaryContainerLight,
    background = NutriBackgroundLight,
    onBackground = NutriOnBackgroundLight,
    surface = NutriSurfaceLight,
    onSurface = NutriOnSurfaceLight,
    surfaceVariant = NutriSurfaceVariantLight,
    onSurfaceVariant = NutriOnSurfaceVariantLight,
)

@Composable
fun NutriLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

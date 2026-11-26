package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NutriLens Palette - Deep Botanical Emerald, Organic Cream, Mint, Amber & Alert Crimson
val NutriPrimaryLight = Color(0xFF006C4C)
val NutriOnPrimaryLight = Color(0xFFFFFFFF)
val NutriPrimaryContainerLight = Color(0xFF8CF5C6)
val NutriOnPrimaryContainerLight = Color(0xFF002114)

val NutriSecondaryLight = Color(0xFF476355)
val NutriOnSecondaryLight = Color(0xFFFFFFFF)
val NutriSecondaryContainerLight = Color(0xFFCAE8D6)
val NutriOnSecondaryContainerLight = Color(0xFF052014)

val NutriTertiaryLight = Color(0xFF396374)
val NutriOnTertiaryLight = Color(0xFFFFFFFF)
val NutriTertiaryContainerLight = Color(0xFFBDE8FC)
val NutriOnTertiaryContainerLight = Color(0xFF001F2A)

val NutriBackgroundLight = Color(0xFFF6FAF6)
val NutriOnBackgroundLight = Color(0xFF171D19)
val NutriSurfaceLight = Color(0xFFF6FAF6)
val NutriOnSurfaceLight = Color(0xFF171D19)
val NutriSurfaceVariantLight = Color(0xFFDBE5DD)
val NutriOnSurfaceVariantLight = Color(0xFF404943)
val NutriSurfaceContainerLowLight = Color(0xFFF0F5F0)
val NutriSurfaceContainerLight = Color(0xFFEAF0EB)
val NutriSurfaceContainerHighLight = Color(0xFFE4EBE5)
val NutriOutlineVariantLight = Color(0xFFC0CAC2)

// Dark Theme
val NutriPrimaryDark = Color(0xFF6FDBAB)
val NutriOnPrimaryDark = Color(0xFF003825)
val NutriPrimaryContainerDark = Color(0xFF005238)
val NutriOnPrimaryContainerDark = Color(0xFF8CF5C6)

val NutriSecondaryDark = Color(0xFFB0CCBB)
val NutriOnSecondaryDark = Color(0xFF1C3528)
val NutriSecondaryContainerDark = Color(0xFF324B3E)
val NutriOnSecondaryContainerDark = Color(0xFFCAE8D6)

val NutriTertiaryDark = Color(0xFFA2CCDF)
val NutriOnTertiaryDark = Color(0xFF043544)
val NutriTertiaryContainerDark = Color(0xFF214B5C)
val NutriOnTertiaryContainerDark = Color(0xFFBDE8FC)

val NutriBackgroundDark = Color(0xFF0F1411)
val NutriOnBackgroundDark = Color(0xFFE0E4DF)
val NutriSurfaceDark = Color(0xFF0F1411)
val NutriOnSurfaceDark = Color(0xFFE0E4DF)
val NutriSurfaceVariantDark = Color(0xFF3D4741)
val NutriOnSurfaceVariantDark = Color(0xFFBEC9C1)
val NutriSurfaceContainerLowDark = Color(0xFF161C19)
val NutriSurfaceContainerDark = Color(0xFF1B221E)
val NutriSurfaceContainerHighDark = Color(0xFF232C27)
val NutriOutlineVariantDark = Color(0xFF3E4942)

// Food Scores & Alerts
val NutriScoreA = Color(0xFF038141)
val NutriScoreB = Color(0xFF85BB2F)
val NutriScoreC = Color(0xFFFECB02)
val NutriScoreD = Color(0xFFEE8100)
val NutriScoreE = Color(0xFFE63E11)

val Nova1 = Color(0xFF008947)
val Nova2 = Color(0xFFF5A623)
val Nova3 = Color(0xFFE66E00)
val Nova4 = Color(0xFFD82222)

val AllergenDanger = Color(0xFFB71C1C)
val AllergenWarning = Color(0xFFE65100)
val SafeGreen = Color(0xFF1B5E20)
val PalmOilWarning = Color(0xFFD84315)

// Dark-mode-aware semantic surface & foreground pairs for badges and banners
@Composable
fun semanticDangerColors(): Triple<Color, Color, Color> = if (isSystemInDarkTheme()) {
    Triple(Color(0xFF3B1518), Color(0xFFFF8A80), Color(0xFF8E2429))
} else {
    Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFEF9A9A))
}

@Composable
fun semanticCautionColors(): Triple<Color, Color, Color> = if (isSystemInDarkTheme()) {
    Triple(Color(0xFF3A260D), Color(0xFFFFB74D), Color(0xFF8D5611))
} else {
    Triple(Color(0xFFFFF8E1), Color(0xFFE65100), Color(0xFFFFCC80))
}

@Composable
fun semanticSafeColors(): Triple<Color, Color, Color> = if (isSystemInDarkTheme()) {
    Triple(Color(0xFF112E1F), Color(0xFF69F0AE), Color(0xFF1E6B45))
} else {
    Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFFA5D6A7))
}

@Composable
fun semanticNeutralColors(): Triple<Color, Color, Color> = if (isSystemInDarkTheme()) {
    Triple(Color(0xFF262D29), Color(0xFFB0BEB6), Color(0xFF45524B))
} else {
    Triple(Color(0xFFEEEEEE), Color(0xFF616161), Color(0xFFBDBDBD))
}

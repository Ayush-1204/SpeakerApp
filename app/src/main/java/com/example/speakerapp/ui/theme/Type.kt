package com.example.speakerapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun speakerTypography(compact: Boolean): Typography {
    val displayLargeSize = if (compact) 52.sp else 60.sp
    val displayLargeLine = if (compact) 56.sp else 64.sp
    val headlineLargeSize = if (compact) 42.sp else 48.sp
    val headlineLargeLine = if (compact) 46.sp else 52.sp
    val titleLargeSize = if (compact) 18.sp else 20.sp
    val titleMediumSize = if (compact) 15.sp else 16.sp
    val labelSmallSize = if (compact) 9.sp else 10.sp
    val bodyLargeSize = if (compact) 15.sp else 16.sp
    val bodyMediumSize = if (compact) 13.sp else 14.sp

    return Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = displayLargeSize,
        lineHeight = displayLargeLine,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = headlineLargeSize,
        lineHeight = headlineLargeLine,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = titleLargeSize,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = titleMediumSize,
        lineHeight = 22.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = labelSmallSize,
        lineHeight = 12.sp,
        letterSpacing = 1.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = bodyLargeSize,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = bodyMediumSize,
        lineHeight = 20.sp
    )
)
}

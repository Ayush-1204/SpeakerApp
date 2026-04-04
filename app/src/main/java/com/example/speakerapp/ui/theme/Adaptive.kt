package com.example.speakerapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class AdaptiveUi(
    val isCompact: Boolean,
    val cornerSmall: Dp,
    val cornerMedium: Dp,
    val cornerLarge: Dp,
    val cornerExtraLarge: Dp,
    val horizontalScreenPadding: Dp,
    val elementVerticalSpacing: Dp,
    val navPillHeight: Dp,
    val navIconBubbleWidth: Dp,
    val navIconBubbleHeight: Dp
)

val LocalAdaptiveUi = staticCompositionLocalOf {
    AdaptiveUi(
        isCompact = false,
        cornerSmall = 10.dp,
        cornerMedium = 16.dp,
        cornerLarge = 24.dp,
        cornerExtraLarge = 30.dp,
        horizontalScreenPadding = 24.dp,
        elementVerticalSpacing = 16.dp,
        navPillHeight = 80.dp,
        navIconBubbleWidth = 56.dp,
        navIconBubbleHeight = 32.dp
    )
}

object AppTheme {
    val adaptive: AdaptiveUi
        @Composable
        @ReadOnlyComposable
        get() = LocalAdaptiveUi.current
}

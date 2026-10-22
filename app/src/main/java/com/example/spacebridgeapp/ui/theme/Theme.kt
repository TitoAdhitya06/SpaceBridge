package com.example.spacebridgeapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

data class SpaceBridgeColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderStrong: Color,
    val overlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val primary: Color,
    val primary50: Color,
    val primary100: Color,
    val primary500: Color,
    val primary600: Color,
    val primary700: Color,
    val primary800: Color,
    val focusRing: Color,
    val success: Color,
    val success50: Color,
    val success500: Color,
    val success600: Color,
    val info: Color,
    val info50: Color,
    val info500: Color,
    val info600: Color,
    val danger: Color,
    val danger50: Color,
    val danger500: Color,
    val danger600: Color,
    val warning: Color,
    val warning50: Color,
    val warning500: Color,
    val warning700: Color,
    val primaryLight: Color = primary500,
    val primaryIndigo: Color = primary600,
    val primaryContainer: Color = primary50,
    val onPrimary: Color = textOnAccent,
    val onSecondary: Color = Color.White,
    val cosmicCyan: Color = primary500,
    val cosmicCyanLight: Color = primary500,
    val nebulaViolet: Color = primary600,
    val nebulaVioletLight: Color = primary500,
    val surfaceContainerLowest: Color = surface,
    val surfaceContainerLow: Color = surfaceMuted,
    val surfaceContainer: Color = surfaceMuted,
    val surfaceContainerHigh: Color = surfaceMuted,
    val surfaceContainerHighest: Color = borderStrong,
    val errorContainer: Color = danger50,
    val statusError: Color = danger600,
    val onError: Color = Color.White,
    val onErrorContainer: Color = danger600,
    val statusSuccess: Color = success600,
    val statusWarning: Color = warning500,
    val textMuted: Color = textTertiary,
    val ghostBorder: Color = border,
)

val LightSpaceBridgeColors =
    SpaceBridgeColorScheme(
        isDark = false,
        background = BgColor,
        surface = SurfaceColor,
        surfaceMuted = SurfaceMuted,
        surfaceHover = SurfaceHover,
        border = BorderColor,
        borderStrong = BorderStrong,
        overlay = OverlayColor,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary,
        textDisabled = TextDisabled,
        textOnAccent = TextOnAccent,
        primary = Primary600,
        primary50 = Primary50,
        primary100 = Primary100,
        primary500 = Primary500,
        primary600 = Primary600,
        primary700 = Primary700,
        primary800 = Primary800,
        focusRing = FocusRing,
        success = Success600,
        success50 = Success50,
        success500 = Success500,
        success600 = Success600,
        info = Info600,
        info50 = Info50,
        info500 = Info500,
        info600 = Info600,
        danger = Danger600,
        danger50 = Danger50,
        danger500 = Danger500,
        danger600 = Danger600,
        warning = Warning500,
        warning50 = Warning50,
        warning500 = Warning500,
        warning700 = Warning700,
    )

val DarkSpaceBridgeColors =
    SpaceBridgeColorScheme(
        isDark = true,
        background = DarkBgColor,
        surface = DarkSurfaceColor,
        surfaceMuted = DarkSurfaceMuted,
        surfaceHover = DarkSurfaceHover,
        border = DarkBorderColor,
        borderStrong = DarkBorderStrong,
        overlay = DarkOverlayColor,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        textTertiary = DarkTextTertiary,
        textDisabled = DarkTextDisabled,
        textOnAccent = DarkTextOnAccent,
        primary = DarkPrimary,
        primary50 = DarkPrimary50,
        primary100 = DarkPrimary50,
        primary500 = DarkPrimary,
        primary600 = DarkPrimaryHover,
        primary700 = DarkPrimaryPressed,
        primary800 = DarkPrimaryPressed,
        focusRing = FocusRing,
        success = DarkSuccess,
        success50 = DarkPrimary50,
        success500 = DarkSuccess,
        success600 = DarkSuccess,
        info = DarkInfo,
        info50 = DarkPrimary50,
        info500 = DarkInfo,
        info600 = DarkInfo,
        danger = DarkDanger,
        danger50 = DarkPrimary50,
        danger500 = DarkDanger,
        danger600 = DarkDanger,
        warning = DarkWarning,
        warning50 = DarkPrimary50,
        warning500 = DarkWarning,
        warning700 = DarkWarning,
    )

val LocalSpaceBridgeColors = staticCompositionLocalOf { LightSpaceBridgeColors }

object SpaceBridgeTheme {
    val colors: SpaceBridgeColorScheme
        @Composable
        get() = LocalSpaceBridgeColors.current

    val isDark: Boolean
        @Composable
        get() = LocalSpaceBridgeColors.current.isDark
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpaceBridgeAppTheme(
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appColors = if (isDarkTheme) DarkSpaceBridgeColors else LightSpaceBridgeColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
            view.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        }
    }

    val materialColorScheme =
        if (isDarkTheme) {
            darkColorScheme(
                primary = appColors.primary,
                onPrimary = appColors.textOnAccent,
                primaryContainer = appColors.primary50,
                onPrimaryContainer = appColors.textPrimary,
                secondary = appColors.info,
                onSecondary = Color.White,
                tertiary = appColors.warning,
                background = appColors.background,
                onBackground = appColors.textPrimary,
                surface = appColors.surface,
                onSurface = appColors.textPrimary,
                surfaceVariant = appColors.surfaceMuted,
                onSurfaceVariant = appColors.textSecondary,
                outline = appColors.border,
                outlineVariant = appColors.borderStrong,
                error = appColors.danger,
                onError = Color.White,
            )
        } else {
            lightColorScheme(
                primary = appColors.primary600,
                onPrimary = appColors.textOnAccent,
                primaryContainer = appColors.primary50,
                onPrimaryContainer = appColors.primary700,
                secondary = appColors.info600,
                onSecondary = Color.White,
                tertiary = appColors.warning500,
                background = appColors.background,
                onBackground = appColors.textPrimary,
                surface = appColors.surface,
                onSurface = appColors.textPrimary,
                surfaceVariant = appColors.surfaceMuted,
                onSurfaceVariant = appColors.textSecondary,
                outline = appColors.border,
                outlineVariant = appColors.borderStrong,
                error = appColors.danger600,
                onError = Color.White,
            )
        }

    @Suppress("DEPRECATION")
    CompositionLocalProvider(
        LocalSpaceBridgeColors provides appColors,
        LocalOverscrollFactory provides null,
        LocalOverscrollConfiguration provides null,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content,
        )
    }
}

@Preview(name = "Light Mode - Warm Paper", showBackground = true)
@Composable
private fun LightThemePreview() {
    SpaceBridgeAppTheme(isDarkTheme = false) {
        ThemePreviewContent()
    }
}

@Preview(name = "Dark Mode - Warm Charcoal", showBackground = true)
@Composable
private fun DarkThemePreview() {
    SpaceBridgeAppTheme(isDarkTheme = true) {
        ThemePreviewContent()
    }
}

@Composable
private fun ThemePreviewContent() {
    val colors = SpaceBridgeTheme.colors
    Surface(
        color = colors.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "SpaceBridge v3",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = "Palet warm paper, bebas ungu dan bebas gradien.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            // Kartu Permukaan
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kartu Catatan Utama",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Latar kartu memakai SurfaceColor dengan border halus tanpa bayangan hitam pekat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }

            // Status Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeSample(label = "Aktif", bg = colors.success50, text = colors.success600)
                BadgeSample(label = "Info", bg = colors.info50, text = colors.info600)
                BadgeSample(label = "Peringatan", bg = colors.warning50, text = colors.warning700)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Utama
            Button(
                onClick = {},
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.textOnAccent,
                    ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    text = "Aksi Utama",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun BadgeSample(
    label: String,
    bg: Color,
    text: Color,
) {
    Box(
        modifier =
            Modifier
                .background(bg, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = text,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

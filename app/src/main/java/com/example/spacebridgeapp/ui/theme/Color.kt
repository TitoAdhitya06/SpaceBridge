package com.example.spacebridgeapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet Terang (Light Mode Warm Paper - DESIGN.md Bagian 2)
val BgColor = Color(0xFFF8F4EC)
val SurfaceColor = Color(0xFFFFFDF9)
val SurfaceMuted = Color(0xFFF2ECE1)
val SurfaceHover = Color(0xFFF5EFE4)
val BorderColor = Color(0xFFE8E0D2)
val BorderStrong = Color(0xFFD3C7B3)
val OverlayColor = Color(0x732E2A25)

// Teks Terang
val TextPrimary = Color(0xFF2E2A25)
val TextSecondary = Color(0xFF5C554C)
val TextTertiary = Color(0xFF756D61)
val TextDisabled = Color(0xFFA69E90)
val TextOnAccent = Color(0xFFFFFDF9)

// Aksen Oranye
val Primary50 = Color(0xFFFDF1E8)
val Primary100 = Color(0xFFFADDC6)
val Primary500 = Color(0xFFE2702F)
val Primary600 = Color(0xFFBE531A)
val Primary700 = Color(0xFFA54715)
val Primary800 = Color(0xFF8C3C12)
val FocusRing = Color(0x59E2702F)

// Info (Biru, hue 195°-215°)
val Info50 = Color(0xFFE8F1FA)
val Info500 = Color(0xFF4A8AC9)
val Info600 = Color(0xFF2F6FB0)
val Info700 = Color(0xFF255A91)
val Info800 = Color(0xFF1D4A78)

// Sukses (Hijau)
val Success50 = Color(0xFFE6F4EC)
val Success500 = Color(0xFF3A9B63)
val Success600 = Color(0xFF27774A)
val Success700 = Color(0xFF1F6340)
val Success800 = Color(0xFF18502F)

// Bahaya (Merah)
val Danger50 = Color(0xFFFBEAE8)
val Danger500 = Color(0xFFD9534A)
val Danger600 = Color(0xFFC23B32)
val Danger700 = Color(0xFFA5302A)
val Danger800 = Color(0xFF8A2722)

// Peringatan (Amber)
val Warning50 = Color(0xFFFDF4E3)
val Warning500 = Color(0xFFE9A23B)
val Warning700 = Color(0xFF8F5D12)

// Palet Gelap (Dark Mode Warm Charcoal - DESIGN.md Bagian 2)
val DarkBgColor = Color(0xFF1C1915)
val DarkSurfaceColor = Color(0xFF26221D)
val DarkSurfaceMuted = Color(0xFF302B25)
val DarkSurfaceHover = Color(0xFF3A342D)
val DarkBorderColor = Color(0xFF3D3730)
val DarkBorderStrong = Color(0xFF4D453D)
val DarkOverlayColor = Color(0x73000000)

val DarkTextPrimary = Color(0xFFF2ECE3)
val DarkTextSecondary = Color(0xFFC4BBAE)
val DarkTextTertiary = Color(0xFF968D80)
val DarkTextDisabled = Color(0xFF6B6357)
val DarkTextOnAccent = Color(0xFF1C1915)

val DarkPrimary = Color(0xFFE8793A)
val DarkPrimaryHover = Color(0xFFF08C52)
val DarkPrimaryPressed = Color(0xFFD96A2C)
val DarkPrimary50 = Color(0xFF332014)

val DarkInfo = Color(0xFF6AA5DE)
val DarkSuccess = Color(0xFF5CB987)
val DarkDanger = Color(0xFFE8675D)
val DarkWarning = Color(0xFFE9B04F)

// Kompatibilitas transisi untuk kode lama yang belum dimigrasi (dialihkan ke palet warm tanpa warna ungu)
@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val CosmicCyan: Color
    @Composable get() = SpaceBridgeTheme.colors.primary500

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val CosmicCyanLight: Color
    @Composable get() = SpaceBridgeTheme.colors.primary500

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val NebulaViolet: Color
    @Composable get() = SpaceBridgeTheme.colors.primary600

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val NebulaVioletLight: Color
    @Composable get() = SpaceBridgeTheme.colors.primary500

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val DeepVoid: Color
    @Composable get() = SpaceBridgeTheme.colors.background

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val DarkDeepVoid: Color = DarkBgColor

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val PrimaryIndigo: Color
    @Composable get() = SpaceBridgeTheme.colors.primary600

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val PrimaryLight: Color
    @Composable get() = SpaceBridgeTheme.colors.primary500

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val PrimaryContainer: Color
    @Composable get() = SpaceBridgeTheme.colors.primary50

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val GhostBorder: Color
    @Composable get() = SpaceBridgeTheme.colors.border

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val SurfaceContainerLowest: Color
    @Composable get() = SpaceBridgeTheme.colors.surface

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val SurfaceContainerLow: Color
    @Composable get() = SpaceBridgeTheme.colors.surfaceMuted

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val SurfaceContainer: Color
    @Composable get() = SpaceBridgeTheme.colors.surfaceMuted

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val SurfaceContainerHigh: Color
    @Composable get() = SpaceBridgeTheme.colors.surfaceMuted

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val SurfaceContainerHighest: Color
    @Composable get() = SpaceBridgeTheme.colors.borderStrong

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val TextMuted: Color
    @Composable get() = SpaceBridgeTheme.colors.textTertiary

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val StatusError: Color
    @Composable get() = SpaceBridgeTheme.colors.danger600

@Deprecated("Warna kosmik dihapus dari DESIGN.md v3, dialihkan ke palet warm")
val StatusSuccess: Color
    @Composable get() = SpaceBridgeTheme.colors.success600

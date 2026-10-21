package com.gamepadpro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Neon palette from the design concept image. */
object PadColors {
    val BgDeep = Color(0xFF070B17)
    val BgMid = Color(0xFF0A1230)
    val BgGlow = Color(0xFF14275A)
    val Panel = Color(0xFF101A3D)
    val PanelBorder = Color(0xFF1D2C5C)
    val Neon = Color(0xFF1E6BFF)
    val Cyan = Color(0xFF49B4FF)
    val TextHi = Color(0xFFE8EEFF)
    val TextMu = Color(0xFF8C9BC7)
    val Ok = Color(0xFF2FD36B)
    val Warn = Color(0xFFFFB02E)
    val Danger = Color(0xFFFF5252)
    val Selected = Color(0xFFFFD84A)

    // ABXY face-button colors
    val BtnA = Color(0xFF2BD16A)
    val BtnB = Color(0xFFE5262B)
    val BtnX = Color(0xFF2E7BFF)
    val BtnY = Color(0xFFF5D000)

    fun forMapping(map: String?): Color = when (map) {
        "A" -> BtnA
        "B" -> BtnB
        "X" -> BtnX
        "Y" -> BtnY
        else -> Cyan
    }
}

private val DarkScheme = darkColorScheme(
    primary = PadColors.Neon,
    onPrimary = Color.White,
    secondary = PadColors.Cyan,
    background = PadColors.BgDeep,
    onBackground = PadColors.TextHi,
    surface = PadColors.Panel,
    onSurface = PadColors.TextHi,
    surfaceVariant = PadColors.PanelBorder,
    onSurfaceVariant = PadColors.TextMu,
    error = PadColors.Danger
)

@Composable
fun GamePadProTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}

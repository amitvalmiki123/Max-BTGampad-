package com.gamepadpro.app.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadpro.app.ui.theme.PadColors

/**
 * Round face button (A/B/X/Y) or pill button (LB/RB/LT/RT/BACK/START).
 *
 * Handles its OWN pointers via [awaitEachGesture], so many buttons can be
 * held at the same time with different fingers — e.g. X + A together
 * (pure multi-touch, no combo logic).
 *
 * @param heightDp used to scale the label font.
 * @param interactive false in edit mode (drag is handled by the parent).
 */
@Composable
fun ButtonControl(
    label: String,
    color: Color,
    pill: Boolean,
    heightDp: Dp,
    opacity: Float,
    vibration: Boolean,
    interactive: Boolean,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val currentOnPressedChange by androidx.compose.runtime.rememberUpdatedState(onPressedChange)
    val currentVibration by androidx.compose.runtime.rememberUpdatedState(vibration)

    val shape = if (pill) RoundedCornerShape(percent = 50) else CircleShape
    val fill = if (pressed) {
        Brush.radialGradient(listOf(color, color.copy(alpha = 0.75f)))
    } else if (pill) {
        Brush.verticalGradient(listOf(Color(0xFF26335F), Color(0xFF141D3F)))
    } else {
        Brush.radialGradient(listOf(Color(0xFF1A2552), Color(0xFF0A1030)))
    }
    val borderColor = if (pill) Color(0xFF2B3C78) else color
    val textColor = when {
        pressed -> Color.White
        pill -> PadColors.TextHi
        else -> color
    }

    val touch = if (interactive) {
        Modifier.pointerInput(label) {
            awaitEachGesture {
                awaitFirstDown()
                pressed = true
                if (vibration) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPressedChange(true)
                waitForUpOrCancellation()
                pressed = false
                onPressedChange(false)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(opacity)
            .graphicsLayer {
                val s = if (pressed) 0.92f else 1f
                scaleX = s
                scaleY = s
            }
            .background(fill, shape)
            .border(2.dp, borderColor, shape)
            .then(touch),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (heightDp.value * if (pill) 0.36f else 0.42f).sp
        )
    }
}

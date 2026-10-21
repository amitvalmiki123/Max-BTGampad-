package com.gamepadpro.app.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gamepadpro.app.ui.theme.PadColors
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Analog stick. The knob follows the finger within a radius of 30% of the
 * base width; output = offset / radius in -1..1 (dead zone applied by the
 * caller). Releasing resets to 0. A quick tap (little movement) fires
 * [onClick] -> L3 / R3.
 *
 * Handles its own pointers so both sticks + buttons work simultaneously.
 */
@Composable
fun StickControl(
    sizeDp: Dp,
    opacity: Float,
    interactive: Boolean,
    onMove: (x: Float, y: Float) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var knob by remember { mutableStateOf(Offset.Zero) } // px offset from center
    val onMoveState = androidx.compose.runtime.rememberUpdatedState(onMove)
    val onClickState = androidx.compose.runtime.rememberUpdatedState(onClick)

    val touch = if (interactive) {
        Modifier.pointerInput(Unit) {
            val travel = size.width * 0.30f
            val center = Offset(size.width / 2f, size.height / 2f)

            fun handle(pos: Offset): Offset {
                var d = pos - center
                val len = hypot(d.x, d.y)
                if (len > travel && len > 0f) d = d * (travel / len)
                knob = d
                if (travel > 0f) onMoveState.value(d.x / travel, d.y / travel)
                return d
            }

            awaitEachGesture {
                val down = awaitFirstDown()
                val downTime = down.uptimeMillis
                var maxDist = 0f
                handle(down.position).let { d -> maxDist = maxOf(maxDist, hypot(d.x, d.y)) }
                var upTime = downTime
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        upTime = change.uptimeMillis
                        break
                    }
                    change.consume()
                    val d = handle(change.position)
                    maxDist = maxOf(maxDist, hypot(d.x, d.y))
                }
                val wasTap = (upTime - downTime) < 250L && travel > 0f && maxDist / travel < 0.18f
                knob = Offset.Zero
                onMoveState.value(0f, 0f)
                if (wasTap) onClickState.value()
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(opacity)
            .background(
                Brush.radialGradient(listOf(Color(0xFF0A1230), Color(0xFF141F45))),
                CircleShape
            )
            .border(3.dp, PadColors.PanelBorder, CircleShape)
            .then(touch),
        contentAlignment = Alignment.Center
    ) {
        // direction ticks
        Box(
            modifier = Modifier
                .size(sizeDp * 0.86f)
                .border(1.dp, PadColors.PanelBorder.copy(alpha = 0.5f), CircleShape)
        )
        // knob with neon ring
        Box(
            modifier = Modifier
                .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(sizeDp * 0.62f)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF222D5A), Color(0xFF0B1230))),
                    CircleShape
                )
                .border(3.dp, PadColors.Neon, CircleShape)
        )
    }
}

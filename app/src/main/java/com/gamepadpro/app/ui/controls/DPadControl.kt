package com.gamepadpro.app.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadpro.app.ui.theme.PadColors

private data class Dirs(val up: Boolean, val down: Boolean, val left: Boolean, val right: Boolean)

/**
 * D-pad (hat switch). Direction comes from the finger offset relative to the
 * center; past a 14%-of-size threshold the direction turns on. Diagonals
 * work by sliding the finger to a corner.
 */
@Composable
fun DPadControl(
    sizeDp: Dp,
    opacity: Float,
    interactive: Boolean,
    onDirection: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var dirs by remember { mutableStateOf(Dirs(false, false, false, false)) }
    val onDirectionState = androidx.compose.runtime.rememberUpdatedState(onDirection)

    fun setDirs(d: Dirs) {
        if (d != dirs) {
            dirs = d
            onDirectionState.value(d.up, d.down, d.left, d.right)
        }
    }

    val touch = if (interactive) {
        Modifier.pointerInput(Unit) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val threshold = size.width * 0.14f

            fun handle(pos: Offset) {
                val dx = pos.x - center.x
                val dy = pos.y - center.y
                setDirs(
                    Dirs(
                        up = dy < -threshold,
                        down = dy > threshold,
                        left = dx < -threshold,
                        right = dx > threshold
                    )
                )
            }

            awaitEachGesture {
                val down = awaitFirstDown()
                handle(down.position)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    handle(change.position)
                }
                setDirs(Dirs(false, false, false, false))
            }
        }
    } else {
        Modifier
    }

    val armColor = Color(0xFF18234F)
    val armOn = PadColors.Neon
    val shapeV = RoundedCornerShape(10.dp)
    val shapeH = RoundedCornerShape(10.dp)
    val arrowSize = (sizeDp.value * 0.14f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(opacity)
            .then(touch),
        contentAlignment = Alignment.Center
    ) {
        // vertical arm
        Box(
            modifier = Modifier
                .fillMaxWidth(0.34f)
                .fillMaxHeight()
                .background(armColor, shapeV)
                .border(1.dp, PadColors.PanelBorder, shapeV)
        )
        // horizontal arm
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
                .background(armColor, shapeH)
                .border(1.dp, PadColors.PanelBorder, shapeH)
        )
        // arrows
        Text(
            "▲",
            color = if (dirs.up) Color.White else PadColors.Cyan,
            fontSize = arrowSize, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = sizeDp * 0.05f)
                .background(if (dirs.up) armOn else Color.Transparent, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp)
        )
        Text(
            "▼",
            color = if (dirs.down) Color.White else PadColors.Cyan,
            fontSize = arrowSize, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = sizeDp * 0.05f)
                .background(if (dirs.down) armOn else Color.Transparent, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp)
        )
        Text(
            "◀",
            color = if (dirs.left) Color.White else PadColors.Cyan,
            fontSize = arrowSize, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = sizeDp * 0.05f)
                .background(if (dirs.left) armOn else Color.Transparent, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp)
        )
        Text(
            "▶",
            color = if (dirs.right) Color.White else PadColors.Cyan,
            fontSize = arrowSize, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = sizeDp * 0.05f)
                .background(if (dirs.right) armOn else Color.Transparent, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp)
        )
    }
}

package com.gamepadpro.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadpro.app.hid.ConnState
import com.gamepadpro.app.ui.theme.PadColors

/** Short human label for the current HID connection state. */
fun ConnState.label(): String = when (this) {
    is ConnState.Idle -> "Not started"
    is ConnState.Registering -> "Starting gamepad…"
    is ConnState.Registered -> "Ready — pair / connect from PC or TV"
    is ConnState.Connecting -> "Connecting to $deviceName…"
    is ConnState.Connected -> "Connected · $deviceName"
    is ConnState.Error -> message
}

fun ConnState.dotColor(): Color = when (this) {
    is ConnState.Connected -> PadColors.Ok
    is ConnState.Connecting, is ConnState.Registering -> PadColors.Warn
    is ConnState.Registered -> PadColors.Cyan
    is ConnState.Error -> PadColors.Danger
    is ConnState.Idle -> PadColors.TextMu
}

/** Small pill showing connection status (green dot + text), like the design concept. */
@Composable
fun StatusChip(connState: ConnState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PadColors.Panel.copy(alpha = 0.88f), RoundedCornerShape(50))
            .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(connState.dotColor(), CircleShape)
        )
        Text(
            text = connState.label(),
            color = PadColors.TextHi,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

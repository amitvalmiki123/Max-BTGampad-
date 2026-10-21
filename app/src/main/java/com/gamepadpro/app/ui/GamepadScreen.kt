package com.gamepadpro.app.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamepadpro.app.hid.ConnState
import com.gamepadpro.app.hid.GamepadState
import com.gamepadpro.app.hid.GamepadState.Companion.toHex
import com.gamepadpro.app.hid.HidGamepad
import com.gamepadpro.app.layout.ControlSpec
import com.gamepadpro.app.layout.ControlType
import com.gamepadpro.app.layout.DefaultLayout
import com.gamepadpro.app.layout.Settings
import com.gamepadpro.app.ui.controls.ButtonControl
import com.gamepadpro.app.ui.controls.DPadControl
import com.gamepadpro.app.ui.controls.StickControl
import com.gamepadpro.app.ui.theme.PadColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Full-screen landscape gamepad: PLAY mode + PPSSPP-style EDIT mode.
 * Every control handles its own pointers, so multi-touch combos like
 * X + A (two fingers) work naturally — byte 0 becomes 0x05.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadScreen(
    specs: List<ControlSpec>,
    onSpecsChange: (List<ControlSpec>) -> Unit,
    onResetLayout: () -> Unit,
    gamepadState: GamepadState,
    hid: HidGamepad,
    connState: ConnState,
    settings: Settings,
    startInEditMode: Boolean,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var editMode by remember { mutableStateOf(startInEditMode) }
    val selectedIdState = remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(true) }
    val sentCount by hid.sentReports.collectAsState()
    val sendOk by hid.lastSendOk.collectAsState()
    var reportHex by remember { mutableStateOf(GamepadState.neutralReport().toHex()) }
    var pressedLabel by remember { mutableStateOf("—") }

    val specsState by rememberUpdatedState(specs)
    val onSpecsChangeState by rememberUpdatedState(onSpecsChange)
    val deadZoneState by rememberUpdatedState(settings.deadZone)

    fun push() {
        val report = gamepadState.toReport()
        hid.send(report)
        reportHex = report.toHex()
        val names = gamepadState.pressedNames().sorted().joinToString(" ")
        val hat = buildString {
            if (gamepadState.up) append("↑")
            if (gamepadState.down) append("↓")
            if (gamepadState.left) append("←")
            if (gamepadState.right) append("→")
        }
        pressedLabel = listOf(names, hat).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "—" }
    }

    fun stickClick(axis: String?) {
        val name = if (axis == "R") "R3" else "L3"
        gamepadState.press(name)
        push()
        scope.launch {
            delay(130)
            gamepadState.release(name)
            push()
        }
    }

    // entering edit mode: stop all output, no HID reports while editing
    LaunchedEffect(editMode) {
        if (editMode) {
            gamepadState.reset()
            hid.sendNeutral()
            reportHex = GamepadState.neutralReport().toHex()
            pressedLabel = "—"
        }
    }

    // leaving the screen: neutral report, no stuck buttons
    DisposableEffect(Unit) {
        onDispose {
            gamepadState.reset()
            hid.sendNeutral()
        }
    }

    // full-screen toggle (immersive system bars)
    val view = LocalView.current
    LaunchedEffect(fullscreen) {
        val activity = view.context as? Activity ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(activity.window, view)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PadColors.BgGlow, PadColors.BgMid, PadColors.BgDeep)
                )
            )
    ) {
        if (editMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 20f
                val stepY = size.height / 20f
                val c = Color.White.copy(alpha = 0.05f)
                var x = stepX
                while (x < size.width) {
                    drawLine(c, Offset(x, 0f), Offset(x, size.height))
                    x += stepX
                }
                var y = stepY
                while (y < size.height) {
                    drawLine(c, Offset(0f, y), Offset(size.width, y))
                    y += stepY
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val containerWPx = with(density) { maxWidth.toPx() }
            val containerHPx = with(density) { maxHeight.toPx() }

            for (spec in specs) {
                val (baseW, baseH) = spec.baseSize
                val wDp = maxHeight * (baseW * spec.scale / 100f)
                val hDp = maxHeight * (baseH * spec.scale / 100f)
                val xOff = maxWidth * (spec.x / 100f) - wDp / 2
                val yOff = maxHeight * (spec.y / 100f) - hDp / 2
                val isSelected = editMode && selectedIdState.value == spec.id

                val outline = when {
                    !editMode -> Modifier
                    isSelected -> Modifier.border(2.dp, PadColors.Selected, RoundedCornerShape(10.dp))
                    else -> Modifier.drawBehind {
                        drawRect(
                            color = PadColors.Cyan.copy(alpha = 0.55f),
                            topLeft = Offset(3f, 3f),
                            size = size.copy(width = size.width - 6f, height = size.height - 6f),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )
                    }
                }

                val editTouch = if (editMode) {
                    Modifier.pointerInput(spec.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            selectedIdState.value = spec.id
                            var last = down.position
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val delta = change.position - last
                                last = change.position
                                if (delta != Offset.Zero && containerWPx > 0f && containerHPx > 0f) {
                                    change.consume()
                                    val current = specsState.firstOrNull { it.id == spec.id } ?: spec
                                    val newX = (current.x + delta.x / containerWPx * 100f)
                                        .roundToInt().coerceIn(3, 97).toFloat()
                                    val newY = (current.y + delta.y / containerHPx * 100f)
                                        .roundToInt().coerceIn(3, 97).toFloat()
                                    if (newX != current.x || newY != current.y) {
                                        onSpecsChangeState(
                                            specsState.map {
                                                if (it.id == spec.id) it.copy(x = newX, y = newY) else it
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .offset(x = xOff, y = yOff)
                        .size(wDp, hDp)
                        .then(outline)
                        .then(editTouch)
                ) {
                    when (spec.type) {
                        ControlType.BUTTON -> ButtonControl(
                            label = spec.map ?: "?",
                            color = PadColors.forMapping(spec.map),
                            pill = false,
                            heightDp = hDp,
                            opacity = spec.opacity,
                            vibration = settings.vibration,
                            interactive = !editMode,
                            onPressedChange = { pressed ->
                                val name = spec.map
                                if (name != null) {
                                    if (pressed) gamepadState.press(name) else gamepadState.release(name)
                                    push()
                                }
                            }
                        )
                        ControlType.PILL -> ButtonControl(
                            label = spec.map ?: "?",
                            color = PadColors.Neon,
                            pill = true,
                            heightDp = hDp,
                            opacity = spec.opacity,
                            vibration = settings.vibration,
                            interactive = !editMode,
                            onPressedChange = { pressed ->
                                val name = spec.map
                                if (name != null) {
                                    if (pressed) gamepadState.press(name) else gamepadState.release(name)
                                    push()
                                }
                            }
                        )
                        ControlType.STICK -> StickControl(
                            sizeDp = wDp,
                            opacity = spec.opacity,
                            interactive = !editMode,
                            onMove = { x, y ->
                                val (ax, ay) = applyDeadZone(x, y, deadZoneState)
                                if (spec.axis == "R") {
                                    gamepadState.rx = ax
                                    gamepadState.ry = ay
                                } else {
                                    gamepadState.lx = ax
                                    gamepadState.ly = ay
                                }
                                push()
                            },
                            onClick = { stickClick(spec.axis) }
                        )
                        ControlType.DPAD -> DPadControl(
                            sizeDp = wDp,
                            opacity = spec.opacity,
                            interactive = !editMode,
                            onDirection = { u, d, l, r ->
                                gamepadState.up = u
                                gamepadState.down = d
                                gamepadState.left = l
                                gamepadState.right = r
                                push()
                            }
                        )
                    }
                }
            }
        }

        // ---- top overlays ----
        if (editMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .background(PadColors.Panel.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
                    .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("✎ Edit Layout", color = PadColors.TextHi, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                TextButton(onClick = {
                    onResetLayout()
                    selectedIdState.value = null
                }) { Text("Reset") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Full screen", color = PadColors.TextMu, fontSize = 12.sp)
                    Switch(checked = fullscreen, onCheckedChange = { fullscreen = it })
                }
                Button(
                    onClick = {
                        editMode = false
                        selectedIdState.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PadColors.Neon)
                ) { Text("Done") }
            }
            Text(
                text = "Drag any control to move it · tap a control to change size / opacity / mapping",
                color = PadColors.TextMu,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
            )
        } else {
            StatusChip(
                connState = connState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
            )
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                Button(
                    onClick = { menuOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PadColors.Panel.copy(alpha = 0.88f)
                    )
                ) { Text("☰", color = PadColors.TextHi) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("✎  Edit layout") },
                        onClick = { menuOpen = false; editMode = true }
                    )
                    DropdownMenuItem(
                        text = { Text("⚙  Settings") },
                        onClick = { menuOpen = false; onOpenSettings() }
                    )
                    DropdownMenuItem(
                        text = { Text("✕  Disconnect") },
                        onClick = { menuOpen = false; hid.disconnect() }
                    )
                    DropdownMenuItem(
                        text = { Text("⌂  Home") },
                        onClick = { menuOpen = false; onOpenHome() }
                    )
                }
            }
        }

        // ---- debug overlay (Settings se ON hota hai): X+A test ke liye ----
        if (settings.debugOverlay && !editMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Pressed: $pressedLabel", color = PadColors.Cyan, fontSize = 11.sp)
                Text(
                    "Report: $reportHex",
                    color = PadColors.TextHi,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "HID sent: $sentCount" + when (sendOk) {
                        true -> " (ok)"
                        false -> " (FAIL — phone se report nahi ja raha)"
                        null -> ""
                    },
                    color = if (sendOk == false) PadColors.Danger else PadColors.Ok,
                    fontSize = 11.sp
                )
            }
        }

        // ---- edit panel for the selected control ----
        val selected = if (editMode) specs.firstOrNull { it.id == selectedIdState.value } else null
        if (selected != null) {
            EditPanel(
                spec = selected,
                onChange = { updated ->
                    onSpecsChange(specs.map { if (it.id == updated.id) updated else it })
                },
                onResetThis = {
                    DefaultLayout.byId(selected.id)?.let { def ->
                        onSpecsChange(specs.map { if (it.id == def.id) def else it })
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .widthIn(max = 620.dp)
                    .fillMaxWidth(0.72f)
            )
        }
    }
}

private fun applyDeadZone(x: Float, y: Float, deadZone: Float): Pair<Float, Float> {
    val mag = hypot(x, y)
    if (mag == 0f || mag < deadZone) return 0f to 0f
    val scaled = ((mag - deadZone) / (1f - deadZone)).coerceIn(0f, 1f)
    return (x / mag * scaled) to (y / mag * scaled)
}

@Composable
private fun EditPanel(
    spec: ControlSpec,
    onChange: (ControlSpec) -> Unit,
    onResetThis: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(PadColors.Panel.copy(alpha = 0.96f), RoundedCornerShape(16.dp))
            .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            spec.displayName,
            color = PadColors.Selected,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size", color = PadColors.TextMu, fontSize = 12.sp, modifier = Modifier.size(width = 64.dp, height = 20.dp))
            Slider(
                value = spec.scale,
                onValueChange = { onChange(spec.copy(scale = it)) },
                valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f)
            )
            Text("%.2fx".format(spec.scale), color = PadColors.TextHi, fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity", color = PadColors.TextMu, fontSize = 12.sp, modifier = Modifier.size(width = 64.dp, height = 20.dp))
            Slider(
                value = spec.opacity,
                onValueChange = { onChange(spec.copy(opacity = it)) },
                valueRange = 0.3f..1.0f,
                modifier = Modifier.weight(1f)
            )
            Text("${(spec.opacity * 100).roundToInt()}%", color = PadColors.TextHi, fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (spec.type) {
                ControlType.BUTTON, ControlType.PILL -> {
                    Text("Mapping", color = PadColors.TextMu, fontSize = 12.sp)
                    Box {
                        Button(
                            onClick = { mapMenuOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PadColors.Neon)
                        ) { Text("${spec.map ?: "?"}  ▾") }
                        DropdownMenu(expanded = mapMenuOpen, onDismissRequest = { mapMenuOpen = false }) {
                            for (name in GamepadState.MAPPABLE) {
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        mapMenuOpen = false
                                        onChange(spec.copy(map = name))
                                    }
                                )
                            }
                        }
                    }
                }
                ControlType.STICK -> {
                    Text("Axis", color = PadColors.TextMu, fontSize = 12.sp)
                    for (axis in listOf("L", "R")) {
                        val active = spec.axis == axis
                        Button(
                            onClick = { onChange(spec.copy(axis = axis)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) PadColors.Neon else PadColors.PanelBorder
                            )
                        ) { Text(if (axis == "L") "Left (L)" else "Right (R)") }
                    }
                    Text("Tap stick = ${if (spec.axis == "R") "R3" else "L3"}", color = PadColors.TextMu, fontSize = 11.sp)
                }
                ControlType.DPAD -> {
                    Text("D-Pad → hat switch (fixed)", color = PadColors.TextMu, fontSize = 12.sp)
                }
            }
            TextButton(onClick = onResetThis) { Text("Reset this control") }
        }
    }
}

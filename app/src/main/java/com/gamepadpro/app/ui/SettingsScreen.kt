package com.gamepadpro.app.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadpro.app.hid.HidGamepad
import com.gamepadpro.app.layout.Settings
import com.gamepadpro.app.ui.theme.PadColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    hid: HidGamepad,
    settings: Settings,
    onBack: () -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
    onDeadZoneChange: (Float) -> Unit,
    onDebugOverlayChange: (Boolean) -> Unit,
    onEditLayout: () -> Unit,
    onResetLayout: () -> Unit
) {
    var refreshTick by remember { mutableIntStateOf(0) }
    val btOn = remember(refreshTick) { hid.isBluetoothEnabled() }
    var resetDone by remember { mutableStateOf(false) }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshTick++ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PadColors.BgGlow, PadColors.BgMid, PadColors.BgDeep))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("←", color = PadColors.Cyan, fontSize = 18.sp) }
                Text("Settings", color = PadColors.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            SettingsCard(title = "Bluetooth", subtitle = if (btOn) "Device Connection · ON" else "Device Connection · OFF") {
                if (btOn) {
                    Switch(checked = true, onCheckedChange = null)
                } else {
                    Button(
                        onClick = {
                            runCatching {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadColors.Neon)
                    ) { Text("Turn ON") }
                }
            }

            SettingsCard(title = "Vibration", subtitle = "Haptic Feedback — button press par halki vibration") {
                Switch(checked = settings.vibration, onCheckedChange = onVibrationChange)
            }

            SettingsCard(title = "Auto Reconnect", subtitle = "Connect to last device — last PC/TV se apne-aap jud jao") {
                Switch(checked = settings.autoReconnect, onCheckedChange = onAutoReconnectChange)
            }

            SettingsCard(title = "Debug Overlay", subtitle = "Pressed buttons + HID report bytes (X+A test: 05 dikhna chahiye)") {
                Switch(checked = settings.debugOverlay, onCheckedChange = onDebugOverlayChange)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .background(PadColors.Panel, RoundedCornerShape(14.dp))
                    .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sensitivity", color = PadColors.TextHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Joystick dead zone (Trigger)", color = PadColors.TextMu, fontSize = 11.sp)
                    }
                    Text("${(settings.deadZone * 100).roundToInt()}%", color = PadColors.Cyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.deadZone,
                    onValueChange = onDeadZoneChange,
                    valueRange = 0f..0.30f
                )
            }

            SettingsCard(title = "Gamepad Layout", subtitle = "Customize Button Mapping — buttons ghisakao, size/opacity badlo") {
                Button(
                    onClick = onEditLayout,
                    colors = ButtonDefaults.buttonColors(containerColor = PadColors.Neon)
                ) { Text("✎ Edit") }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onResetLayout()
                    resetDone = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset layout to default", color = PadColors.Danger) }
            if (resetDone) {
                Text("✓ Layout reset ho gaya", color = PadColors.Ok, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PadColors.Panel, RoundedCornerShape(14.dp))
                    .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text("About", color = PadColors.TextHi, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("App Version 1.0.1", color = PadColors.TextMu, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Jaankari (Limitations):\n" +
                        "• Android 9 (API 28) ya usse upar chahiye, aur phone ka Bluetooth HID Device mode support karna chahiye. Kuch brands ke phones me ye band hota hai — tab app saaf error dikhayega.\n" +
                        "• Windows isse ek generic (DirectInput) gamepad maanta hai. Jo games sirf Xbox/XInput controller maangte hain, unke liye Steam Input use karo (game ko Steam me add karke controller config ON karo) ya x360ce jaisa tool.\n" +
                        "• Ek time par ek hi PC/TV connect hoga.\n" +
                        "• X + A ek saath chalane ke liye dono buttons ko do ungliyon se ek saath dabao — multi-touch hi kaafi hai, koi combo button nahi hai.",
                    color = PadColors.TextMu, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(PadColors.Panel, RoundedCornerShape(14.dp))
            .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFFE8EEFF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = PadColors.TextMu, fontSize = 11.sp)
        }
        action()
    }
}

package com.gamepadpro.app.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadpro.app.hid.ConnState
import com.gamepadpro.app.hid.HidGamepad
import com.gamepadpro.app.ui.theme.PadColors

@Composable
fun HomeScreen(
    hid: HidGamepad,
    connState: ConnState,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onRetryHid: () -> Unit,
    onPairNew: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenSettings: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit
) {
    var refreshTick by remember { mutableIntStateOf(0) }
    val bonded = remember(connState, refreshTick, hasPermissions) {
        if (hasPermissions) hid.bondedDevices() else emptyList()
    }
    val btOn = remember(connState, refreshTick) { hid.isBluetoothEnabled() }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTick++
        onRetryHid()
    }

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
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🎮  GamePad Pro", color = PadColors.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Bluetooth HID Controller", color = PadColors.TextMu, fontSize = 12.sp)
                }
                TextButton(onClick = onOpenSettings) {
                    Text("⚙ Settings", color = PadColors.Cyan)
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(Color(0xFF0B1230), CircleShape)
                    .border(3.dp, PadColors.Neon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎮", fontSize = 48.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Turn Your Android Into a Gamepad",
                color = PadColors.TextHi,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Connect via Bluetooth and start playing on your PC / TV.\nNo app needed on the PC — phone khud ek real gamepad ban jata hai.",
                color = PadColors.TextMu,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))
            StatusChip(connState)

            when {
                !btOn -> {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            runCatching {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadColors.Warn)
                    ) { Text("Turn ON Bluetooth", color = Color.Black) }
                }
                !hasPermissions -> {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onRequestPermissions) { Text("Allow Bluetooth permission") }
                }
                connState is ConnState.Error -> {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRetryHid,
                        colors = ButtonDefaults.buttonColors(containerColor = PadColors.Danger)
                    ) { Text("Retry") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPairNew,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PadColors.Neon),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("＋  Pair New Device", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenGamepad,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("🎮  Open Gamepad", color = PadColors.Cyan, fontSize = 15.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paired devices (tap to connect)", color = PadColors.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                TextButton(onClick = { refreshTick++ }) { Text("↻ Refresh", color = PadColors.Cyan) }
            }

            if (bonded.isEmpty()) {
                Text(
                    "No paired PC/TV found yet.\nPehle upar \"Pair New Device\" se apne PC/TV me is phone ko pair karo.",
                    color = PadColors.TextMu,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val connectedAddress = (connState as? ConnState.Connected)?.address
                for (device in bonded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(PadColors.Panel, RoundedCornerShape(12.dp))
                            .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(12.dp))
                            .clickable { onConnectDevice(device) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    if (device.address == connectedAddress) PadColors.Ok else PadColors.TextMu,
                                    CircleShape
                                )
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(hid.deviceLabel(device), color = PadColors.TextHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (device.address == connectedAddress) "Connected" else device.address,
                                color = PadColors.TextMu, fontSize = 11.sp
                            )
                        }
                        Text("🎮", fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

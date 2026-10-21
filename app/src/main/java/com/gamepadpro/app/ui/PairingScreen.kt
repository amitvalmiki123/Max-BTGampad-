package com.gamepadpro.app.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadpro.app.hid.ConnState
import com.gamepadpro.app.hid.HidGamepad
import com.gamepadpro.app.ui.theme.PadColors

@Composable
fun PairingScreen(
    hid: HidGamepad,
    connState: ConnState,
    onBack: () -> Unit,
    onOpenGamepad: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val bonded = remember(connState, refreshTick) { hid.bondedDevices() }

    val pulse = rememberInfiniteTransition(label = "bt-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale"
    )

    fun makeDiscoverable() {
        runCatching {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            context.startActivity(intent)
        }
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
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("← Back", color = PadColors.Cyan) }
            }

            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .size(104.dp)
                    .background(Color(0xFF0B1230), CircleShape)
                    .border(3.dp, PadColors.Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("BT", color = PadColors.Cyan, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                if (connState is ConnState.Connected) "Connected!" else "Searching for devices…",
                color = PadColors.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "Make sure your PC / TV is in pairing mode and Bluetooth is ON.",
                color = PadColors.TextMu, fontSize = 13.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            StatusChip(connState)

            if (connState is ConnState.Connected) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onOpenGamepad,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PadColors.Ok),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("🎮  Open Gamepad — Start Playing", color = Color.Black, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PadColors.Panel, RoundedCornerShape(14.dp))
                    .border(1.dp, PadColors.PanelBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("Pair kaise karein (pehli baar)", color = PadColors.TextHi, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "1) Neeche wala button dabao — phone 5 minute ke liye \"GamePad Pro\" naam se dikhega.\n" +
                        "2) PC par: Settings → Bluetooth & devices → Add device → Bluetooth → \"GamePad Pro\" (ya apne phone ka naam) select karo.\n" +
                        "   TV par: Settings → Remotes & Accessories → Pair accessory.\n" +
                        "3) Pair ho jane ke baad neeche ki list me apna PC/TV tap karke connect karo. Connection PC ki taraf se bhi auto ho sakta hai.",
                    color = PadColors.TextMu, fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = ::makeDiscoverable,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PadColors.Neon),
                shape = RoundedCornerShape(14.dp)
            ) { Text("📡  Phone do dhundne do — Make Discoverable (5 min)", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available devices (paired)", color = PadColors.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                TextButton(onClick = { refreshTick++ }) { Text("↻ Refresh", color = PadColors.Cyan) }
            }

            if (bonded.isEmpty()) {
                Text(
                    "No device yet — PC/TV par ek baar pair karo, fir yahan dikhega.",
                    color = PadColors.TextMu, fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 10.dp)
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖥", fontSize = 18.sp)
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(hid.deviceLabel(device), color = PadColors.TextHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (device.address == connectedAddress) "Connected" else device.address,
                                color = PadColors.TextMu, fontSize = 11.sp
                            )
                        }
                        if (device.address == connectedAddress) {
                            Text("✓", color = PadColors.Ok, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        } else {
                            Button(onClick = { onConnectDevice(device) }) { Text("Connect") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onBack) { Text("Cancel", color = PadColors.TextMu) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

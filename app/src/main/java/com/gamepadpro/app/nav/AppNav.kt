package com.gamepadpro.app.nav

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gamepadpro.app.hid.ConnState
import com.gamepadpro.app.hid.GamepadState
import com.gamepadpro.app.hid.HidGamepad
import com.gamepadpro.app.layout.ControlSpec
import com.gamepadpro.app.layout.DefaultLayout
import com.gamepadpro.app.layout.LayoutRepository
import com.gamepadpro.app.layout.Settings
import com.gamepadpro.app.layout.SettingsRepository
import com.gamepadpro.app.ui.GamepadScreen
import com.gamepadpro.app.ui.HomeScreen
import com.gamepadpro.app.ui.PairingScreen
import com.gamepadpro.app.ui.SettingsScreen
import com.gamepadpro.app.ui.theme.PadColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { HOME, PAIRING, GAMEPAD, SETTINGS }

private fun hasBluetoothPermissions(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
    val advertise = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
    return connect == PackageManager.PERMISSION_GRANTED &&
        advertise == PackageManager.PERMISSION_GRANTED
}

private val bluetoothPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        emptyArray()
    }

@Composable
fun AppNav(
    hid: HidGamepad,
    gamepadState: GamepadState,
    layoutRepo: LayoutRepository,
    settingsRepo: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(Screen.HOME) }
    var gamepadEditStart by remember { mutableStateOf(false) }

    val connState by hid.state.collectAsState()
    val settings by settingsRepo.settings.collectAsState(initial = Settings())

    // ---- layout: edit locally for instant dragging, save debounced ----
    val storedLayout by layoutRepo.layout.collectAsState(initial = null)
    var specs by remember { mutableStateOf(DefaultLayout.controls) }
    var layoutLoaded by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(storedLayout) {
        val stored = storedLayout
        if (!layoutLoaded && stored != null) {
            specs = stored
            layoutLoaded = true
        }
    }

    fun updateSpecs(newSpecs: List<ControlSpec>) {
        specs = newSpecs
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(250)
            layoutRepo.save(newSpecs)
        }
    }

    fun resetLayout() {
        specs = DefaultLayout.controls
        saveJob?.cancel()
        scope.launch { layoutRepo.reset() }
    }

    // ---- Bluetooth runtime permissions, then start HID ----
    var hasPermissions by remember { mutableStateOf(hasBluetoothPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = hasBluetoothPermissions(context)
        if (hasPermissions) hid.start()
    }

    LaunchedEffect(Unit) {
        if (hasPermissions) {
            hid.start()
        } else if (bluetoothPermissions.isNotEmpty()) {
            permissionLauncher.launch(bluetoothPermissions)
        }
    }

    fun retryHid() {
        hasPermissions = hasBluetoothPermissions(context)
        if (hasPermissions) {
            hid.start()
        } else if (bluetoothPermissions.isNotEmpty()) {
            permissionLauncher.launch(bluetoothPermissions)
        }
    }

    // ---- remember last host + auto reconnect ----
    var autoConnectTried by remember { mutableStateOf(false) }
    LaunchedEffect(connState, settings.lastHostAddress, settings.autoReconnect) {
        when (val s = connState) {
            is ConnState.Connected -> {
                if (settings.lastHostAddress != s.address) {
                    scope.launch { settingsRepo.setLastHostAddress(s.address) }
                }
            }
            is ConnState.Registered -> {
                if (!autoConnectTried && settings.autoReconnect && settings.lastHostAddress.isNotEmpty()) {
                    autoConnectTried = true
                    hid.bondedDevice(settings.lastHostAddress)?.let { hid.connect(it) }
                }
            }
            else -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PadColors.BgDeep) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                hid = hid,
                connState = connState,
                hasPermissions = hasPermissions,
                onRequestPermissions = { retryHid() },
                onRetryHid = { retryHid() },
                onPairNew = { screen = Screen.PAIRING },
                onOpenGamepad = {
                    gamepadEditStart = false
                    screen = Screen.GAMEPAD
                },
                onOpenSettings = { screen = Screen.SETTINGS },
                onConnectDevice = { device ->
                    retryHid()
                    hid.connect(device)
                    gamepadEditStart = false
                    screen = Screen.GAMEPAD
                }
            )

            Screen.PAIRING -> PairingScreen(
                hid = hid,
                connState = connState,
                onBack = { screen = Screen.HOME },
                onOpenGamepad = {
                    gamepadEditStart = false
                    screen = Screen.GAMEPAD
                },
                onConnectDevice = { device ->
                    retryHid()
                    hid.connect(device)
                    gamepadEditStart = false
                    screen = Screen.GAMEPAD
                }
            )

            Screen.GAMEPAD -> GamepadScreen(
                specs = specs,
                onSpecsChange = { updateSpecs(it) },
                onResetLayout = { resetLayout() },
                gamepadState = gamepadState,
                hid = hid,
                connState = connState,
                settings = settings,
                startInEditMode = gamepadEditStart,
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenHome = { screen = Screen.HOME }
            )

            Screen.SETTINGS -> SettingsScreen(
                hid = hid,
                settings = settings,
                onBack = { screen = Screen.HOME },
                onVibrationChange = { scope.launch { settingsRepo.setVibration(it) } },
                onAutoReconnectChange = { scope.launch { settingsRepo.setAutoReconnect(it) } },
                onDeadZoneChange = { scope.launch { settingsRepo.setDeadZone(it) } },
                onDebugOverlayChange = { scope.launch { settingsRepo.setDebugOverlay(it) } },
                onEditLayout = {
                    gamepadEditStart = true
                    screen = Screen.GAMEPAD
                },
                onResetLayout = { resetLayout() }
            )
        }
    }
}

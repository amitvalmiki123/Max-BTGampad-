package com.gamepadpro.app.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Connection lifecycle of the HID gamepad. */
sealed interface ConnState {
    data object Idle : ConnState
    data object Registering : ConnState
    /** HID app registered — PC/TV can now pair & connect to this phone. */
    data object Registered : ConnState
    data class Connecting(val deviceName: String) : ConnState
    data class Connected(val deviceName: String, val address: String) : ConnState
    data class Error(val message: String) : ConnState
}

/**
 * Wrapper around [BluetoothHidDevice] (API 28+).
 * Registers this phone as a Bluetooth HID game pad so a PC / laptop / TV
 * sees it as a standard gamepad — no PC-side app needed.
 */
class HidGamepad(context: Context) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _state = MutableStateFlow<ConnState>(ConnState.Idle)
    val state: StateFlow<ConnState> = _state.asStateFlow()

    private val _sentReports = MutableStateFlow(0L)
    /** Total reports successfully handed to the Bluetooth stack (diagnostics). */
    val sentReports: StateFlow<Long> = _sentReports.asStateFlow()

    private val _lastSendOk = MutableStateFlow<Boolean?>(null)
    /** Result of the most recent sendReport call; null = nothing sent yet. */
    val lastSendOk: StateFlow<Boolean?> = _lastSendOk.asStateFlow()

    /** Latest report; re-sent by the keepalive and used to answer GET_REPORT. */
    @Volatile
    private var lastReport: ByteArray = GamepadState.neutralReport()

    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private var started = false
    private var closing = false

    // --- report throttling: max ~125 reports/sec, always send latest ---
    private val handler = Handler(Looper.getMainLooper())
    private var lastSentAt = 0L
    private var pending: ByteArray? = null
    private val flushPending = Runnable {
        val report = pending
        pending = null
        if (report != null) rawSend(report)
    }

    /**
     * Keepalive: while connected, re-send the latest report every 100 ms.
     * HID reports carry absolute state, so re-sending self-heals a change
     * report the host missed (some Windows stacks are not yet listening on
     * the interrupt channel right after the HID connection opens).
     */
    private val heartbeat = object : Runnable {
        override fun run() {
            if (_state.value is ConnState.Connected) {
                rawSend(lastReport)
                handler.postDelayed(this, HEARTBEAT_MS)
            }
        }
    }

    private fun startHeartbeat() {
        handler.removeCallbacks(heartbeat)
        handler.postDelayed(heartbeat, HEARTBEAT_MS)
    }

    private fun stopHeartbeat() {
        handler.removeCallbacks(heartbeat)
    }

    fun isBluetoothSupported(): Boolean = adapter != null

    fun isBluetoothEnabled(): Boolean = try {
        adapter?.isEnabled == true
    } catch (e: SecurityException) {
        false
    }

    fun deviceLabel(device: BluetoothDevice): String =
        runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: device.address

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BluetoothDevice> = try {
        adapter?.bondedDevices?.sortedBy { deviceLabel(it).lowercase() } ?: emptyList()
    } catch (e: SecurityException) {
        emptyList()
    }

    fun bondedDevice(address: String): BluetoothDevice? =
        bondedDevices().firstOrNull { it.address == address }

    /** Start the HID device profile and register the gamepad app. Safe to retry. */
    fun start() {
        if (adapter == null) {
            _state.value = ConnState.Error("This phone has no Bluetooth adapter.")
            return
        }
        if (!isBluetoothEnabled()) {
            _state.value = ConnState.Error("Bluetooth is OFF. Turn Bluetooth ON, then tap Retry.")
            return
        }
        if (started && hid != null) return
        started = true
        closing = false
        _state.value = ConnState.Registering
        val ok = try {
            adapter.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE)
        } catch (e: SecurityException) {
            _state.value = ConnState.Error("Bluetooth permission denied. Allow \"Nearby devices\" permission.")
            false
        }
        if (!ok) {
            started = false
            if (_state.value !is ConnState.Error) {
                _state.value = ConnState.Error(
                    "This phone does not support Bluetooth HID device mode."
                )
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hid = proxy as BluetoothHidDevice
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hid = null
            host = null
            started = false
            if (!closing) _state.value = ConnState.Idle
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val device = hid ?: return
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "GamePad Pro",
            "Android Bluetooth gamepad",
            "GamePad Pro",
            BluetoothHidDevice.SUBCLASS2_GAMEPAD,
            HidDescriptor.BYTES
        )
        try {
            val ok = device.registerApp(
                sdp, null, null,
                ContextCompat.getMainExecutor(appContext),
                hidCallback
            )
            if (!ok) {
                _state.value = ConnState.Error(
                    "This phone does not support Bluetooth HID device mode."
                )
            }
        } catch (e: SecurityException) {
            _state.value = ConnState.Error("Bluetooth permission denied. Allow \"Nearby devices\" permission.")
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                if (_state.value !is ConnState.Connected) _state.value = ConnState.Registered
            } else if (!closing && _state.value !is ConnState.Connected) {
                _state.value = ConnState.Error(
                    "HID gamepad could not be registered. Some phones block HID device mode, " +
                        "or another gamepad/HID app is already active — close it and tap Retry."
                )
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, connectionState: Int) {
            when (connectionState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    _state.value = ConnState.Connected(deviceLabel(device), device.address)
                    rawSend(GamepadState.neutralReport())
                    startHeartbeat()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _state.value = ConnState.Connecting(deviceLabel(device))
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    stopHeartbeat()
                    if (host?.address == device.address) host = null
                    if (_state.value is ConnState.Connected || _state.value is ConnState.Connecting) {
                        _state.value = ConnState.Registered
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            // The host (Windows) may request the current state via GET_REPORT
            // while setting the device up; reply with the latest input report.
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                try {
                    hid?.replyReport(
                        device, type,
                        if (id.toInt() == 0) REPORT_ID.toByte() else id,
                        lastReport
                    )
                } catch (e: Exception) {
                    // best effort only
                }
            }
        }
    }

    /** Connect to an already bonded (paired) PC / TV. */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        val device2 = hid
        if (device2 == null) {
            _state.value = ConnState.Error("HID not ready yet — wait for \"Ready\" then try again.")
            return
        }
        try {
            _state.value = ConnState.Connecting(deviceLabel(device))
            val ok = device2.connect(device)
            if (!ok) {
                _state.value = ConnState.Error(
                    "Could not connect to ${deviceLabel(device)}. On the PC/TV, remove this phone " +
                        "from Bluetooth devices and pair again."
                )
            }
        } catch (e: SecurityException) {
            _state.value = ConnState.Error("Bluetooth permission denied.")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopHeartbeat()
        rawSend(GamepadState.neutralReport())
        try {
            host?.let { hid?.disconnect(it) }
        } catch (e: SecurityException) {
            // ignore — we are disconnecting anyway
        }
        host = null
        if (hid != null) _state.value = ConnState.Registered
    }

    /** Queue a report. Sent immediately, coalesced to ~125/sec max. */
    fun send(report: ByteArray) {
        lastReport = report
        if (_state.value !is ConnState.Connected) return
        val now = System.currentTimeMillis()
        val elapsed = now - lastSentAt
        if (elapsed >= MIN_INTERVAL_MS) {
            handler.removeCallbacks(flushPending)
            pending = null
            rawSend(report)
        } else {
            pending = report
            handler.removeCallbacks(flushPending)
            handler.postDelayed(flushPending, MIN_INTERVAL_MS - elapsed)
        }
    }

    /** Send an all-neutral report right now (no stuck buttons on exit/background). */
    fun sendNeutral() {
        handler.removeCallbacks(flushPending)
        pending = null
        lastReport = GamepadState.neutralReport()
        if (_state.value is ConnState.Connected) rawSend(lastReport)
    }

    @SuppressLint("MissingPermission")
    private fun rawSend(report: ByteArray) {
        val device = hid ?: return
        val target = host ?: return
        try {
            val ok = device.sendReport(target, REPORT_ID, report)
            _lastSendOk.value = ok
            if (ok) {
                _sentReports.value += 1
                lastSentAt = System.currentTimeMillis()
            }
        } catch (e: SecurityException) {
            _lastSendOk.value = false
            _state.value = ConnState.Error("Bluetooth permission denied.")
        }
    }

    /** Unregister and release the profile proxy. Call from Activity.onDestroy. */
    @SuppressLint("MissingPermission")
    fun close() {
        closing = true
        stopHeartbeat()
        sendNeutral()
        try {
            hid?.unregisterApp()
        } catch (e: SecurityException) {
            // ignore
        }
        try {
            hid?.let { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        } catch (e: SecurityException) {
            // ignore
        }
        hid = null
        host = null
        started = false
    }

    companion object {
        const val REPORT_ID = 1
        private const val MIN_INTERVAL_MS = 8L // ~125 reports/sec
        private const val HEARTBEAT_MS = 100L
    }
}

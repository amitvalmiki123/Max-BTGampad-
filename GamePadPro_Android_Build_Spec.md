# GamePad Pro: Android Bluetooth HID Gamepad App (Build Spec for an AI coding tool)

## 0. How to use this file
Paste this whole file into an AI coding tool (Claude Code, Cursor, Android Studio Gemini, etc.) with this instruction:

> Build the complete Android project described in this spec. Create all files, make it compile with Gradle, and give me the exact commands to produce a debug APK. Follow the steps in section 11 in order and do not skip the acceptance tests in section 12.

Also attach these two reference files if the tool accepts attachments:
- `gamepad-custom.html` (working web prototype of the customizable layout, edit mode and HID report preview; copy its default layout and logic)
- the neon UI concept image (visual style reference)

---

## 1. Product summary
An Android app that turns the phone into a **Bluetooth HID gamepad**. The phone pairs with a PC, laptop or TV as a standard gamepad (no PC-side app needed). The app shows a full-screen **landscape**, PPSSPP-style customizable touch controller.

Key requirements:
1. Phone appears to the host as a Bluetooth HID **gamepad** (uses `BluetoothHidDevice`).
2. Full-screen landscape controller UI with a neon dark theme.
3. **Customization:** move, resize, change opacity, and **custom button mapping** for every control.
4. **X + A behavior: multi-touch only.** There is NO combo button and NO auto-combo. Pressing X and A together must work by two fingers at the same time, and both bits must be set in the same report.
5. Layout saved locally and restored on next launch. Reset to default available.

Non-goals (do not build): accounts, ads, analytics, network calls, macros, combo buttons, gyro.

## 2. Tech stack
- Kotlin, Jetpack Compose (Material 3), single Activity.
- `minSdk 28` (BluetoothHidDevice needs API 28+), `targetSdk 34` or latest, `compileSdk` latest stable.
- Jetpack DataStore (Preferences) to store layout JSON; `kotlinx.serialization` for JSON.
- Coroutines + StateFlow. No other third-party libraries.
- Package: `com.gamepadpro.app`.

## 3. Project structure
```
app/src/main/
  AndroidManifest.xml
  java/com/gamepadpro/app/
    MainActivity.kt            // hosts navigation, immersive landscape
    nav/AppNav.kt              // Home, Pairing, Gamepad, Settings
    hid/HidGamepad.kt          // BluetoothHidDevice wrapper
    hid/HidDescriptor.kt       // report descriptor bytes
    hid/GamepadState.kt        // pressed buttons, axes, hat -> 9-byte report
    layout/ControlSpec.kt      // data model + default layout
    layout/LayoutRepository.kt // DataStore save/load/reset
    ui/HomeScreen.kt
    ui/PairingScreen.kt
    ui/GamepadScreen.kt        // play mode + edit mode
    ui/controls/               // ButtonControl, StickControl, DPadControl
    ui/theme/Theme.kt
```

## 4. Manifest and permissions
```xml
<uses-feature android:name="android.hardware.bluetooth" android:required="true"/>
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"/>
<uses-permission android:name="android.permission.VIBRATE"/>
```
- `MainActivity`: `android:screenOrientation="sensorLandscape"` for the Gamepad screen (Home and Settings may also be landscape-locked for simplicity), `android:configChanges="orientation|screenSize|keyboardHidden"`.
- Request `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_SCAN` at runtime on API 31+ before starting HID.
- Gamepad screen: hide system bars (immersive sticky) and keep screen on (`FLAG_KEEP_SCREEN_ON`).

## 5. HID report format (must match exactly)
One input report, **Report ID 1**, payload is **9 bytes**:

| Byte | Content |
|---|---|
| 0 | Buttons low byte (bits 0-7): A, B, X, Y, LB, RB, BACK, START |
| 1 | Buttons high byte (bits 8-15): GUIDE, L3, R3, then 5 unused bits |
| 2 | Hat switch: 0=Up, 1=Up-Right, 2=Right, 3=Down-Right, 4=Down, 5=Down-Left, 6=Left, 7=Up-Left, 8=neutral |
| 3 | Left stick X, signed -127..127 |
| 4 | Left stick Y, signed -127..127 (down is positive) |
| 5 | Right stick X, signed -127..127 |
| 6 | Right stick Y, signed -127..127 |
| 7 | Left trigger LT, 0..255 (0 or 255 for a button, digital) |
| 8 | Right trigger RT, 0..255 |

Button bit order: `A=0, B=1, X=2, Y=3, LB=4, RB=5, BACK=6, START=7, GUIDE=8, L3=9, R3=10`.

### HID descriptor (`HidDescriptor.kt`)
```kotlin
object HidDescriptor {
    val BYTES: ByteArray = intArrayOf(
        0x05,0x01,        // Usage Page (Generic Desktop)
        0x09,0x05,        // Usage (Game Pad)
        0xA1,0x01,        // Collection (Application)
        0x85,0x01,        //   Report ID (1)
        0x05,0x09,        //   Usage Page (Button)
        0x19,0x01,        //   Usage Min (1)
        0x29,0x10,        //   Usage Max (16)
        0x15,0x00,        //   Logical Min (0)
        0x25,0x01,        //   Logical Max (1)
        0x75,0x01,        //   Report Size (1)
        0x95,0x10,        //   Report Count (16)
        0x81,0x02,        //   Input (Data,Var,Abs)
        0x05,0x01,        //   Usage Page (Generic Desktop)
        0x09,0x39,        //   Usage (Hat switch)
        0x15,0x00,        //   Logical Min (0)
        0x25,0x07,        //   Logical Max (7)
        0x35,0x00,        //   Physical Min (0)
        0x46,0x3B,0x01,   //   Physical Max (315)
        0x65,0x14,        //   Unit (Degrees)
        0x75,0x04,        //   Report Size (4)
        0x95,0x01,        //   Report Count (1)
        0x81,0x42,        //   Input (Data,Var,Abs,Null state)
        0x65,0x00,        //   Unit (None)
        0x75,0x04,        //   Report Size (4)
        0x95,0x01,        //   Report Count (1)
        0x81,0x03,        //   Input (Const) padding
        0x05,0x01,        //   Usage Page (Generic Desktop)
        0x09,0x30,        //   Usage (X)
        0x09,0x31,        //   Usage (Y)
        0x09,0x33,        //   Usage (Rx)
        0x09,0x34,        //   Usage (Ry)
        0x15,0x81,        //   Logical Min (-127)
        0x25,0x7F,        //   Logical Max (127)
        0x75,0x08,        //   Report Size (8)
        0x95,0x04,        //   Report Count (4)
        0x81,0x02,        //   Input (Data,Var,Abs)
        0x09,0x32,        //   Usage (Z)  -> LT
        0x09,0x35,        //   Usage (Rz) -> RT
        0x15,0x00,        //   Logical Min (0)
        0x26,0xFF,0x00,   //   Logical Max (255)
        0x75,0x08,        //   Report Size (8)
        0x95,0x02,        //   Report Count (2)
        0x81,0x02,        //   Input (Data,Var,Abs)
        0xC0              // End Collection
    ).map { it.toByte() }.toByteArray()
}
```

### `GamepadState.kt`
```kotlin
class GamepadState {
    val pressed = mutableSetOf<String>()   // "A","B","X","Y","LB","RB","LT","RT","L3","R3","BACK","START","GUIDE"
    var lx = 0f; var ly = 0f; var rx = 0f; var ry = 0f   // -1f..1f
    var up = false; var down = false; var left = false; var right = false

    private val bit = mapOf("A" to 0,"B" to 1,"X" to 2,"Y" to 3,"LB" to 4,"RB" to 5,
        "BACK" to 6,"START" to 7,"GUIDE" to 8,"L3" to 9,"R3" to 10)

    fun toReport(): ByteArray {
        var m = 0
        for ((k, b) in bit) if (k in pressed) m = m or (1 shl b)
        val hat = when {
            up && right -> 1; right && down -> 3; down && left -> 5; left && up -> 7
            up -> 0; right -> 2; down -> 4; left -> 6; else -> 8
        }
        fun ax(v: Float) = (v.coerceIn(-1f, 1f) * 127).toInt().toByte()
        return byteArrayOf(
            (m and 0xFF).toByte(), ((m shr 8) and 0xFF).toByte(), hat.toByte(),
            ax(lx), ax(ly), ax(rx), ax(ry),
            if ("LT" in pressed) 0xFF.toByte() else 0,
            if ("RT" in pressed) 0xFF.toByte() else 0
        )
    }
}
```
Important: when the same mapped button is held by two controls, keep it pressed until both are released (use a count per mapped name, not a plain set).

## 6. Bluetooth HID wrapper (`HidGamepad.kt`)
Responsibilities:
- `getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)` to get `BluetoothHidDevice`.
- `registerApp(sdp, null, null, executor, callback)` with:
  - `BluetoothHidDeviceAppSdpSettings("GamePad Pro", "Android gamepad", "GamePad Pro", BluetoothHidDevice.SUBCLASS2_GAMEPAD, HidDescriptor.BYTES)`
- Callback `onAppStatusChanged(device, registered)` and `onConnectionStateChanged(device, state)`; expose a `StateFlow<ConnState>` (`Idle`, `Registered`, `Connecting`, `Connected(deviceName)`, `Error(msg)`).
- Pairing flow:
  1. After `Registered`, fire `ACTION_REQUEST_DISCOVERABLE` (300 s) so the PC can find the phone.
  2. The user also can pick an already bonded host from `adapter.bondedDevices` and call `hid.connect(device)`.
  3. Auto reconnect: remember last host MAC in DataStore and call `connect()` on start if the setting is ON.
- `fun send(report: ByteArray)` calls `hid.sendReport(host, 1, report)` only when connected.
- **Send policy:** send a report immediately on every state change, and coalesce so no more than about 125 reports per second go out (drop intermediate stick moves, always send the latest state). Also send a neutral report on disconnect or when the app goes to background.
- Handle `SecurityException`; if the phone does not support the HID Device profile (`getProfileProxy` returns false or `registerApp` fails), show a clear message: "This phone does not support Bluetooth HID device mode."
- Unregister the app and close the proxy when the service or Activity is destroyed.

Optional but recommended: run HID in a **foreground service** so the connection survives short app switches.

## 7. Layout data model (`ControlSpec.kt`)
```kotlin
@Serializable
data class ControlSpec(
    val id: String,
    val type: ControlType,      // STICK, DPAD, BUTTON, PILL
    val map: String? = null,    // for BUTTON/PILL: one of the mapping names
    val axis: String? = null,   // for STICK: "L" or "R"
    val x: Float,               // center, 0..100 (% of screen width)
    val y: Float,               // center, 0..100 (% of screen height)
    val scale: Float = 1f,      // 0.5..2.0
    val opacity: Float = 1f     // 0.3..1.0
)
```
Allowed mappings (dropdown): `A,B,X,Y,LB,RB,LT,RT,L3,R3,BACK,START,GUIDE`. **Do not add any combo mapping like "X+A".**

Base sizes in units of screen height (multiply by `scale`): stick 36, dpad 28, button 15 (circle), pill 16 wide x 9 tall.

### Default layout (landscape, x/y in %)
```
ls  STICK axis=L           x=20 y=46
rs  STICK axis=R           x=63 y=74
dp  DPAD                   x=15 y=82
y   BUTTON map=Y           x=86 y=40   (yellow)
x   BUTTON map=X           x=77 y=58   (blue)
b   BUTTON map=B           x=95 y=58   (red)
a   BUTTON map=A           x=86 y=76   (green)
lb  PILL   map=LB          x=10 y=9
lt  PILL   map=LT          x=25 y=9
rb  PILL   map=RB          x=90 y=9
rt  PILL   map=RT          x=75 y=9
bk  PILL   map=BACK        x=40 y=92
st  PILL   map=START       x=60 y=92
```
`LayoutRepository` stores the list as JSON in DataStore, exposes `Flow<List<ControlSpec>>`, `save(list)`, `reset()`.

## 8. UI and behavior

### Screens
1. **Home:** logo, text "Turn your Android into a gamepad", primary button "Pair new device", list "Connected devices" (tap to open gamepad), gear icon for Settings.
2. **Pairing:** pulsing Bluetooth icon, "Searching for devices...", help text "Make sure your PC / TV is in pairing mode and Bluetooth is ON.", list "Available devices" (bonded hosts and newly paired), Cancel button.
3. **Gamepad (full-screen landscape):** play mode and edit mode (below).
4. **Settings:** toggles: Bluetooth, Vibration (haptic on press), Auto reconnect; rows: Gamepad layout (opens edit mode), Sensitivity (stick dead zone 0-30%, default 8%), About (version).

### Visual style (from the concept image)
Dark navy gradient background (`#070B17` to `#14275A`), neon blue accent `#1E6BFF`, cyan `#49B4FF`, success green `#2FD36B`. Buttons are dark glossy circles with colored outline and letter: A green `#2BD16A`, B red `#E5262B`, X blue `#2E7BFF`, Y yellow `#F5D000`. Sticks have a neon blue ring on the knob. Status chip at top: green dot, "Connected" and host name. Rounded corners, no heavy shadows.

### Play mode (Gamepad screen)
- Each control handles **its own pointers** so many fingers work at once: use `Modifier.pointerInput` with `awaitEachGesture` per control (do not put one gesture detector on a parent that consumes events).
- Button: down sets pressed (and haptic tick if enabled), up/cancel releases.
- Stick: knob follows the finger within a radius of 30% of the base width; output = offset / radius, with dead zone; releasing resets to 0.
- D-pad: direction from the finger offset relative to center (threshold 14% of size); diagonals allowed; sets up/down/left/right flags (hat).
- Every state change calls `HidGamepad.send(state.toReport())`.
- Small status chip at top center; a small "menu" icon opens: Edit layout, Settings, Disconnect.

### Edit mode (PPSSPP style)
- Entered from the menu. Show a faint grid, dashed outline on every control, toolbar with **Done**, **Reset**, **Full screen** toggle.
- Drag a control to move it (clamp x,y to 3..97%, keep inside safe area, optional 1% snapping).
- Tap a control to select it (yellow outline); a bottom or center panel shows:
  - **Size** slider 0.5 to 2.0
  - **Opacity** slider 0.3 to 1.0
  - **Mapping** dropdown (for BUTTON/PILL) or **Axis** L/R (for STICK)
  - **Reset this control**
- Controls do not send HID reports in edit mode.
- Save to DataStore on every change (debounced) and on Done.

## 9. Multi-touch rule for X + A
- Never merge or special-case X and A. They are two independent controls.
- Pressing both with two fingers must produce one report where bit 0 (A) and bit 2 (X) are both 1: byte 0 = `0x05`.
- Add a debug overlay (toggle in Settings, off by default) that shows the pressed names and the 9 report bytes in hex, like the prototype does.

## 10. Known limitations to tell the user in the About screen
- Needs Android 9 (API 28) or newer, and a phone whose Bluetooth stack supports the HID Device profile. Some OEM builds disable it.
- Windows usually treats this as a generic DirectInput gamepad. Games that need XInput may require Steam Input (add as non-Steam game controller config), DS4Windows-like mapping tools, or x360ce.
- Only one host at a time.

## 11. Build steps for the AI (do in order)
1. Create the Gradle project (Kotlin DSL, Compose, serialization plugin, DataStore). Confirm it builds.
2. Add theme, navigation and the four screens with static UI.
3. Implement `ControlSpec`, default layout, `LayoutRepository`.
4. Implement the three control composables and the Gamepad screen in play mode with a **local-only** debug overlay showing the report bytes. Verify multi-touch X + A shows `05`.
5. Implement edit mode (move, size, opacity, mapping, reset, persistence).
6. Implement `HidDescriptor`, `GamepadState`, `HidGamepad` (permissions, register, discoverable, connect, send, throttle).
7. Wire pairing UI, status chip, auto reconnect, vibration, settings.
8. Add the foreground service (optional), error messages for unsupported phones.
9. Run lint, fix warnings. Produce the APK: `./gradlew assembleDebug` and report the path `app/build/outputs/apk/debug/app-debug.apk`.
10. Write a short README with install steps and how to pair with Windows, Android TV and Linux.

## 12. Acceptance tests
1. App opens in landscape, full screen, with the default layout.
2. Dragging any control moves it; size, opacity and mapping changes apply live; restart keeps the layout; Reset restores the default.
3. Changing the A button mapping to B makes it output B.
4. Touching X and A together (two fingers) shows both pressed and report byte 0 = `0x05`; releasing one keeps the other pressed.
5. Both sticks can be moved at the same time as two buttons.
6. On a Windows PC: pair the phone in Bluetooth settings, open `joy.cpl` (Game Controllers) and confirm that buttons, hat, both sticks and both triggers respond.
7. Disconnecting or leaving the app sends a neutral report (no stuck buttons).
8. On a phone without HID Device support, the app shows the clear error message and does not crash.

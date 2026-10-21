# 🎮 GamePad Pro — Android Bluetooth HID Gamepad

Apne Android phone ko ek **real Bluetooth gamepad** banao. Phone PC / laptop / TV ke
saath standard **Bluetooth HID gamepad** ki tarah pair hota hai — **PC par koi app
install nahi karna**. Fir mobile ki touchscreen se PC game control karo.

Design bilkul repo me diye gaye concept/layout jaisa hai: dark navy neon theme,
landscape full-screen controller, A (green) B (red) X (blue) Y (yellow), do neon-ring
sticks, D-pad, LT/LB/RT/RB pills, BACK/START, upar “Connected” status chip.

## APK kaise milega (sabse asan)

1. GitHub repo me **Actions** tab kholo → latest **“Build Debug APK”** run kholo.
2. Neeche **Artifacts** me `GamePadPro-debug-apk` download karo, zip kholo, `app-debug.apk` phone me install karo.
   (Play Protect “unknown app” puche toh *Install anyway* karo — ye tumhara khud ka debug build hai.)

### Android Studio se khud build karna ho toh
```bash
# Android Studio me is folder ko kholo, ya command line se:
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest   # HID report unit tests (X+A = 0x05)
```
Requirements: JDK 17, Android SDK 34. Phone: **Android 9 (API 28) ya upar**.

## PC / Laptop se connect kaise karein (Windows)

1. Phone me app kholo → Bluetooth ON rakho → jab permission mange to **Allow** (Nearby devices) karo.
2. Home screen par status **“Ready — pair / connect from PC or TV”** aana chahiye.
3. **Pair New Device** dabao → **“Make Discoverable (5 min)”** dabao → Allow karo.
4. PC par: **Settings → Bluetooth & devices → Add device → Bluetooth** → list me
   **“GamePad Pro”** (ya apne phone ka naam) select karke pair karo.
5. Pair hone ke baad app me neeche list me apna PC dikhega → usse tap karo → **Connect**.
   Upar green dot ke saath **Connected · <PC ka naam>** aate hi gamepad screen khul jayegi.
6. Test karne ke liye PC par `Win + R` → `joy.cpl` → gamepad select karke dekho —
   buttons, D-pad (POV hat), dono sticks aur triggers (Z / Rz) hilne chahiye.

**Dobara use:** next time party pair nahi karna padega. App kholo, Home ki list me PC tap
karo (ya *Auto Reconnect* ON rakho toh apne-aap jud jayega).

## TV se connect (Android TV / Google TV)

1. Upar wale steps 1–3 phone par karo (discoverable ON).
2. TV par: **Settings → Remotes & Accessories → Pair accessory** → phone select karo.
3. Fir app ki list me TV tap karke connect karo.

Linux: Bluetooth settings me phone pair karo; gamepad `/dev/input/js0` ki tarah dikhega (`jstest` se test kar sakte ho).

## Gamepad screen — controls

- **Buttons (multi-touch):** har button alag finger se chalta hai. **X + A ek saath** chalane
  ke liye dono ko **do ungliyon se ek saath dabao** — koi alag “X+A” combo button nahi hai
  (buzz, spec ke hisaab se). Test: Settings → *Debug Overlay* ON karo, X+A dabao → report ki
  pehli byte **`05`** dikhni chahiye.
- **Sticks:** ungil ghumao, chhodne par center par wapis. Stick ko **halka tap** = **L3 / R3** click.
- **D-Pad:** direction par ungil rakho/slide karo, diagonal ke liye kone ki taraf slide karo.
- **LT/RT:** digital trigger (0 ya 255 full).
- Upar-right **☰ menu**: Edit layout, Settings, Disconnect, Home.

## Layout customize (PPSSPP style edit mode)

Menu → **Edit layout** (ya Settings → Gamepad Layout → Edit):

- Kisi bhi control ko **drag** karke kahin bhi rakho (1% snap).
- Control tap karke select karo → **Size**, **Opacity**, **Mapping** (A/B/X/Y/LB/RB/LT/RT/L3/R3/BACK/START/GUIDE) badlo; stick ki **Axis L/R** badlo.
- **Reset** = pura default layout wapis; panel me **Reset this control** = sirf wahi control.
- Layout phone me save rehta hai, app dobara kholne par wahi milega.

## Jaankari / Limitations (dhyaan se padho)

- **Apne phone par HID support hona chahiye.** Kuch brands/models ke Android builds me
  Bluetooth *HID Device* profile band hota hai. Tab app saaf message dikhayega:
  *“This phone does not support Bluetooth HID device mode.”* Us phone par ye tareeka
  possible hi nahi (bina root/custom ROM ke).
- **Windows isse generic (DirectInput) gamepad** ki tarah dekhta hai. Bahut se games
  (Steam wale, emulators like PPSSPP/RetroArch, racing games ke settings) me seedha chalega,
  bas game ki controller settings me ek baar mapping confirm kar lena.
  Jo games sirf **Xbox/XInput** controller mante hain, unke liye:
  game ko **Steam me add karke Steam Input ON** karo (sabse asan), ya `x360ce` jaisa tool use karo.
- Ek time me **ek hi host** (PC/TV) connect hoga.
- Connection tab tak rehta hai jab tak app screen par khula hai (gamepad haath me hi rehta hai,
  isiliye screen-on rakha gaya hai). App background me jaane par sab buttons auto-release ho jate
  hain (koi button “ataka” nahi rahega).

## "Connected" dikh raha hai par buttons kaam nahi kar rahe?

Ye sabse common problem hai, aur aksar cause **purana Bluetooth pairing cache** hota hai —
agar phone PC se pehle (bina app ke) pair hua tha, toh Windows phone ko gamepad ki tarah
register nahi karta. Step-by-step fix:

1. **Pehle confirm karo report ja raha hai ya nahi:** app me Settings → *Debug Overlay* ON karo,
   gamepad screen par koi button dabao. Neeche *Report:* ke bytes badalne chahiye aur
   **"HID sent:" wala number badhta rehna chahiye** (v1.0.1+). Number badh raha hai matlab
   phone theek se bhej raha hai — problem PC side par hai (agla step).
2. **Purana pair dono taraf se hatao:** PC par Settings → Bluetooth & devices → apna phone →
   **Remove device**. Phone par Settings → Bluetooth → apna PC → **Forget/Unpair**.
3. **App khula rakh ke dobara pair karo:** app me status "Ready" ho, *Make Discoverable* dabao,
   PC se pair karo, fir app ki list se **Connect** karo.
4. **`joy.cpl` me sahi controller chuno:** hamara gamepad list me **phone ke Bluetooth naam** se
   aayega. Agar list me koi aur controller dikhta hai (jaise *"Gamepad by momo5502"* — ye kisi
   dusre PC software ka virtual controller hai), toh wo **hamara gamepad nahi hai**; us software
   ko band kar do varna games usi khaali controller ko use karenge. Hamare pad ko select karke
   **Properties → Test** me buttons check karo.
5. **Device Manager check:** Device Manager → *Human Interface Devices* me
   "HID-compliant game controller" banna chahiye. Nahi bana toh step 2–3 (re-pair) hi fix hai.

## Problem aaye toh (Troubleshooting)

| Problem | Solution |
|---|---|
| PC ki list me phone nahi dikh raha | App me Discoverable phir se dabao; phone ka Bluetooth ON aur app ko permission di ho; PC par “Add device” list refresh karo. |
| Pair ho gaya par Connect fail | PC aur phone dono taraf se purana pair **Remove/Forget** karke dobara pair karo (HID profile mismatch ka sabse common fix yahi hai). |
| “HID gamepad could not be registered” | Dusri koi gamepad/HID wali app khuli ho toh band karo; Bluetooth off/on karke Retry dabao. Fir bhi aaye to phone HID Device mode support nahi karta. |
| `joy.cpl` me chal raha hai par game me nahi | Game XInput maang raha hai → Steam Input ya x360ce use karo (upar dekho). |
| Buttons ulte-seedhe lag rahe hain | Edit layout me jis button ko jo kaam chahiye, uski Mapping badal do. |
| Stick apne aap hilta/drift karta hai | Settings → Sensitivity (dead zone) thoda badha do (8% default). |

## Technical details

- Kotlin + Jetpack Compose (Material 3), single Activity, package `com.gamepadpro.app`
- `BluetoothHidDevice` (API 28+), SDP subclass *Game Pad*, Report ID 1, 9-byte report:
  16 buttons + hat + X/Y/Rx/Ry (−127..127) + Z/Rz triggers (0..255) — `GamePadPro_Android_Build_Spec.md` ke exact hisaab se.
- Reports ~125/sec par coalesce hote hain; layout Settings DataStore me save hoti hai.
- Project structure spec ke section 3 jaisa hi hai (`hid/`, `layout/`, `ui/`, `nav/`).
- Reference files: `GamePadPro_Android_Build_Spec.md` (build spec), `gamepad-custom.html`
  (web layout prototype), `Neon Bluetooth Gamepad UI Concept.png` (design).

App Version 1.0.1

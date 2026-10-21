package com.gamepadpro.app.hid

/**
 * Mutable gamepad state -> 9-byte HID input report (see [HidDescriptor]).
 *
 * Buttons are tracked with a reference count per mapped name, so if two
 * on-screen controls are mapped to the same button (or two fingers hold it),
 * the button stays pressed until every holder releases it.
 *
 * X + A rule: buttons are fully independent. Holding X and A with two
 * fingers sets bit 0 (A) and bit 2 (X) in the same report -> byte 0 = 0x05.
 * There is deliberately NO "X+A" combo mapping anywhere in this app.
 */
class GamepadState {

    private val counts = mutableMapOf<String, Int>()

    /** Stick axes, -1f..1f. Y down is positive (standard HID). */
    @Volatile var lx = 0f
    @Volatile var ly = 0f
    @Volatile var rx = 0f
    @Volatile var ry = 0f

    /** D-pad flags -> hat switch. */
    @Volatile var up = false
    @Volatile var down = false
    @Volatile var left = false
    @Volatile var right = false

    @Synchronized
    fun press(name: String) {
        counts[name] = (counts[name] ?: 0) + 1
    }

    @Synchronized
    fun release(name: String) {
        val c = (counts[name] ?: 0) - 1
        if (c <= 0) counts.remove(name) else counts[name] = c
    }

    @Synchronized
    fun isPressed(name: String): Boolean = counts.containsKey(name)

    @Synchronized
    fun pressedNames(): Set<String> = counts.keys.toSet()

    @Synchronized
    fun reset() {
        counts.clear()
        lx = 0f; ly = 0f; rx = 0f; ry = 0f
        up = false; down = false; left = false; right = false
    }

    @Synchronized
    fun toReport(): ByteArray {
        var mask = 0
        for ((name, bit) in BITS) {
            if (counts.containsKey(name)) mask = mask or (1 shl bit)
        }
        val hat = when {
            up && right -> 1
            right && down -> 3
            down && left -> 5
            left && up -> 7
            up -> 0
            right -> 2
            down -> 4
            left -> 6
            else -> 8 // neutral (null state)
        }
        fun axis(v: Float): Byte = (v.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        return byteArrayOf(
            (mask and 0xFF).toByte(),
            ((mask shr 8) and 0xFF).toByte(),
            hat.toByte(),
            axis(lx), axis(ly), axis(rx), axis(ry),
            if (counts.containsKey("LT")) 0xFF.toByte() else 0,
            if (counts.containsKey("RT")) 0xFF.toByte() else 0
        )
    }

    companion object {
        /** Button name -> bit index in the 16-bit button field. */
        val BITS: Map<String, Int> = mapOf(
            "A" to 0, "B" to 1, "X" to 2, "Y" to 3,
            "LB" to 4, "RB" to 5, "BACK" to 6, "START" to 7,
            "GUIDE" to 8, "L3" to 9, "R3" to 10
        )

        /** All names a control can be mapped to (LT/RT are analog trigger bytes). */
        val MAPPABLE: List<String> = listOf(
            "A", "B", "X", "Y", "LB", "RB", "LT", "RT",
            "L3", "R3", "BACK", "START", "GUIDE"
        )

        fun neutralReport(): ByteArray = GamepadState().toReport()

        fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
    }
}

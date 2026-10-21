package com.gamepadpro.app

import com.gamepadpro.app.hid.GamepadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the 9-byte HID report (spec acceptance tests 3, 4, 7). */
class GamepadStateTest {

    @Test
    fun reportIs9BytesAndNeutralByDefault() {
        val report = GamepadState().toReport()
        assertEquals(9, report.size)
        // neutral: no buttons, hat = 8 (null), axes 0, triggers 0
        assertEquals(0.toByte(), report[0])
        assertEquals(0.toByte(), report[1])
        assertEquals(8.toByte(), report[2])
        assertEquals(0.toByte(), report[3])
        assertEquals(0.toByte(), report[7])
        assertEquals(0.toByte(), report[8])
    }

    @Test
    fun xAndATogetherGive0x05() {
        // Acceptance test 4: X + A by two fingers -> byte 0 = 0x05 (A=bit0, X=bit2)
        val state = GamepadState()
        state.press("X")
        state.press("A")
        val report = state.toReport()
        assertEquals(0x05.toByte(), report[0])
        // releasing one keeps the other
        state.release("X")
        assertEquals(0x01.toByte(), state.toReport()[0])
        assertTrue(state.isPressed("A"))
    }

    @Test
    fun sameButtonHeldTwiceStaysPressedUntilBothRelease() {
        val state = GamepadState()
        state.press("A")
        state.press("A") // e.g. two controls mapped to A
        state.release("A")
        assertTrue(state.isPressed("A"))
        state.release("A")
        assertTrue(!state.isPressed("A"))
    }

    @Test
    fun buttonBitOrderMatchesSpec() {
        val expected = mapOf(
            "A" to (0 to 1), "B" to (0 to 2), "X" to (0 to 4), "Y" to (0 to 8),
            "LB" to (0 to 16), "RB" to (0 to 32), "BACK" to (0 to 64), "START" to (0 to 128),
            "GUIDE" to (1 to 1), "L3" to (1 to 2), "R3" to (1 to 4)
        )
        for ((name, idxAndMask) in expected) {
            val state = GamepadState()
            state.press(name)
            val (byteIndex, mask) = idxAndMask
            assertEquals("$name bit", mask, state.toReport()[byteIndex].toInt() and 0xFF)
        }
    }

    @Test
    fun triggersAre255WhenPressed() {
        val state = GamepadState()
        state.press("LT")
        state.press("RT")
        val report = state.toReport()
        assertEquals(0xFF.toByte(), report[7])
        assertEquals(0xFF.toByte(), report[8])
    }

    @Test
    fun hatDiagonals() {
        val state = GamepadState()
        state.up = true; state.right = true
        assertEquals(1.toByte(), state.toReport()[2]) // up-right
        state.right = false
        assertEquals(0.toByte(), state.toReport()[2]) // up
        state.up = false; state.down = true; state.left = true
        assertEquals(5.toByte(), state.toReport()[2]) // down-left
    }

    @Test
    fun axesClampAndScale() {
        val state = GamepadState()
        state.lx = 1f
        state.ly = -1f
        state.rx = 2f // out of range -> clamped
        val report = state.toReport()
        assertEquals(127.toByte(), report[3])
        assertEquals((-127).toByte(), report[4])
        assertEquals(127.toByte(), report[5])
    }

    @Test
    fun resetClearsEverything() {
        val state = GamepadState()
        state.press("A")
        state.lx = 1f
        state.up = true
        state.reset()
        val report = state.toReport()
        assertEquals(0.toByte(), report[0])
        assertEquals(8.toByte(), report[2])
        assertEquals(0.toByte(), report[3])
    }
}

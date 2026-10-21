package com.gamepadpro.app.hid

/**
 * HID report descriptor for a standard game pad.
 * One input report, Report ID 1, payload 9 bytes:
 *  byte 0: buttons 1-8   (A B X Y LB RB BACK START)
 *  byte 1: buttons 9-16  (GUIDE L3 R3, rest unused)
 *  byte 2: hat switch (low nibble, 8 = neutral via null state)
 *  byte 3: X  (left stick X,  -127..127)
 *  byte 4: Y  (left stick Y,  -127..127, down positive)
 *  byte 5: Rx (right stick X, -127..127)
 *  byte 6: Ry (right stick Y, -127..127)
 *  byte 7: Z  (LT, 0..255)
 *  byte 8: Rz (RT, 0..255)
 */
object HidDescriptor {
    val BYTES: ByteArray = intArrayOf(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x05,        // Usage (Game Pad)
        0xA1, 0x01,        // Collection (Application)
        0x85, 0x01,        //   Report ID (1)
        0x05, 0x09,        //   Usage Page (Button)
        0x19, 0x01,        //   Usage Min (1)
        0x29, 0x10,        //   Usage Max (16)
        0x15, 0x00,        //   Logical Min (0)
        0x25, 0x01,        //   Logical Max (1)
        0x75, 0x01,        //   Report Size (1)
        0x95, 0x10,        //   Report Count (16)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0x05, 0x01,        //   Usage Page (Generic Desktop)
        0x09, 0x39,        //   Usage (Hat switch)
        0x15, 0x00,        //   Logical Min (0)
        0x25, 0x07,        //   Logical Max (7)
        0x35, 0x00,        //   Physical Min (0)
        0x46, 0x3B, 0x01,  //   Physical Max (315)
        0x65, 0x14,        //   Unit (Degrees)
        0x75, 0x04,        //   Report Size (4)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x42,        //   Input (Data,Var,Abs,Null state)
        0x65, 0x00,        //   Unit (None)
        0x75, 0x04,        //   Report Size (4)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x03,        //   Input (Const) padding
        0x05, 0x01,        //   Usage Page (Generic Desktop)
        0x09, 0x30,        //   Usage (X)
        0x09, 0x31,        //   Usage (Y)
        0x09, 0x33,        //   Usage (Rx)
        0x09, 0x34,        //   Usage (Ry)
        0x15, 0x81,        //   Logical Min (-127)
        0x25, 0x7F,        //   Logical Max (127)
        0x75, 0x08,        //   Report Size (8)
        0x95, 0x04,        //   Report Count (4)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0x09, 0x32,        //   Usage (Z)  -> LT
        0x09, 0x35,        //   Usage (Rz) -> RT
        0x15, 0x00,        //   Logical Min (0)
        0x26, 0xFF, 0x00,  //   Logical Max (255)
        0x75, 0x08,        //   Report Size (8)
        0x95, 0x02,        //   Report Count (2)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0xC0               // End Collection
    ).map { it.toByte() }.toByteArray()
}

package com.gamepadpro.app.layout

import kotlinx.serialization.Serializable

@Serializable
enum class ControlType { STICK, DPAD, BUTTON, PILL }

/**
 * One on-screen control.
 * [x]/[y] = center position in % of screen width/height.
 * [scale] 0.5..2.0, [opacity] 0.3..1.0.
 * [map] for BUTTON/PILL = output name (A, B, X, ...). No combo mappings exist.
 * [axis] for STICK = "L" or "R".
 */
@Serializable
data class ControlSpec(
    val id: String,
    val type: ControlType,
    val map: String? = null,
    val axis: String? = null,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val opacity: Float = 1f
) {
    /** Base size (width to height) in % of screen HEIGHT, before [scale]. */
    val baseSize: Pair<Float, Float>
        get() = when (type) {
            ControlType.STICK -> 36f to 36f
            ControlType.DPAD -> 28f to 28f
            ControlType.BUTTON -> 15f to 15f
            ControlType.PILL -> 16f to 9f
        }

    val displayName: String
        get() = when (type) {
            ControlType.STICK -> if (axis == "R") "Right Stick" else "Left Stick"
            ControlType.DPAD -> "D-Pad"
            else -> map ?: id.uppercase()
        }
}

/** Default landscape layout (matches the design concept). x/y in %. */
object DefaultLayout {
    val controls: List<ControlSpec> = listOf(
        ControlSpec("ls", ControlType.STICK, axis = "L", x = 20f, y = 46f),
        ControlSpec("rs", ControlType.STICK, axis = "R", x = 63f, y = 74f),
        ControlSpec("dp", ControlType.DPAD, x = 15f, y = 82f),
        ControlSpec("y", ControlType.BUTTON, map = "Y", x = 86f, y = 40f),
        ControlSpec("x", ControlType.BUTTON, map = "X", x = 77f, y = 58f),
        ControlSpec("b", ControlType.BUTTON, map = "B", x = 95f, y = 58f),
        ControlSpec("a", ControlType.BUTTON, map = "A", x = 86f, y = 76f),
        ControlSpec("lb", ControlType.PILL, map = "LB", x = 10f, y = 9f),
        ControlSpec("lt", ControlType.PILL, map = "LT", x = 25f, y = 9f),
        ControlSpec("rb", ControlType.PILL, map = "RB", x = 90f, y = 9f),
        ControlSpec("rt", ControlType.PILL, map = "RT", x = 75f, y = 9f),
        ControlSpec("bk", ControlType.PILL, map = "BACK", x = 40f, y = 92f),
        ControlSpec("st", ControlType.PILL, map = "START", x = 60f, y = 92f)
    )

    fun byId(id: String): ControlSpec? = controls.firstOrNull { it.id == id }
}

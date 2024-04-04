/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.input.key

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType

internal data class NativePointerEvent(
    /**
     * X position in points (scaled pixels that depend on the scale factor of the current display).
     *
     * If the event contains multiple pointers, it represents the center of all pointers.
     */
    val x: Double,
    /**
     * Y position in points (scaled pixels that depend on the scale factor of the current display)
     *
     * If the event contains multiple pointers, it represents the center of all pointers.
     */
    val y: Double,
    val kind: PointerEventType,
    /**
     * Scroll delta along the X axis
     */
    val deltaX: Double = 0.0,
    /**
     * Scroll delta along the Y axis
     */
    val deltaY: Double = 0.0,
    val pressedButtons: MouseButtons = MouseButtons.NONE,
    /**
     * Timestamp in milliseconds
     */
    val timestamp: Long = 0,
    val pointers: List<PointerEventRecord> = listOf(
        PointerEventRecord(0, x, y, pressedButtons.has(MouseButtons.LEFT))
    ),
)

/**
 * Represents pointer such as mouse cursor, or touch/stylus press.
 * There can be multiple pointers on the screen at the same time.
 */
internal data class PointerEventRecord(
    /**
     * Unique id associated with the pointer. Used to distinguish between multiple pointers that can exist
     * at the same time (i.e. multiple pressed touches).
     *
     * If there is only on pointer in the system (for example, one mouse), it should always
     * have the same id across multiple events.
     */
    val id: Long,

    /**
     * X position in points (scaled pixels that depend on the scale factor of the current display)
     */
    val x: Double,
    /**
     * Y position in points (scaled pixels that depend on the scale factor of the current display)
     */
    val y: Double,

    /**
     * `true` if the pointer event is considered "pressed." For example, finger
     *  touching the screen or a mouse button is pressed [pressed] would be `true`.
     *  During the up event, pointer is considered not pressed.
     */
    val pressed: Boolean,

    /**
     * The device type associated with the pointer, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     */
    val device: PointerType = PointerType.Mouse,

    /**
     * Pressure of the pointer. 0.0 - no pressure, 1.0 - average pressure
     */
    val pressure: Double = 1.0,
)

internal fun NativePointerEvent.getScrollDelta(): Offset {
    return this.takeIf {
        it.kind == PointerEventType.Scroll
    }?.let {
        Offset(it.deltaX.toFloat(), it.deltaY.toFloat())
    } ?: Offset.Zero
}

internal value class MouseButtons(val value: Int) {
    companion object {
        val NONE = MouseButtons(0)
        val LEFT = MouseButtons(1)
        val RIGHT = MouseButtons(2)
        val MIDDLE = MouseButtons(4)
        val BUTTON_4 = MouseButtons(8)
        val BUTTON_5 = MouseButtons(16)
        val BUTTON_6 = MouseButtons(32)
        val BUTTON_7 = MouseButtons(64)
        val BUTTON_8 = MouseButtons(128)
    }

    fun has(value: MouseButtons): Boolean {
        return value.value and this.value != 0
    }

    override fun toString(): String {
        val result = mutableListOf<String>().apply {
            if (has(LEFT)) {
                add("LEFT")
            }
            if (has(RIGHT)) {
                add("RIGHT")
            }
            if (has(MIDDLE)) {
                add("MIDDLE")
            }
            if (has(BUTTON_4)) {
                add("BUTTON_4")
            }
            if (has(BUTTON_5)) {
                add("BUTTON_5")
            }
            if (has(BUTTON_6)) {
                add("BUTTON_6")
            }
            if (has(BUTTON_7)) {
                add("BUTTON_7")
            }
            if (has(BUTTON_8)) {
                add("BUTTON_8")
            }
        }
        return if (result.isNotEmpty()) result.toString() else ""
    }
}
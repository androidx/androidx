/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.ui.input.indirect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerId

/**
 * An IndirectPointerEvent represents a pointer input event, where the pointer positions do not
 * correspond to a position on the screen. Instead, the position of the pointer corresponds to the
 * position on the input device, such as a touchpad.
 *
 * Since IndirectPointerEvents do not have a position on the screen, they cannot be dispatched
 * through hit-testing and instead they are dispatched through the focus tree, similar to key input.
 * Only the focused component and any of its parents will receive an IndirectPointerEvent.
 *
 * This event differs from a [PointerEvent] as it does not necessitate an existence of a pointer. If
 * an event were to have an associated pointer, they will be routed to through [PointerEvent].
 */
sealed interface IndirectPointerEvent {
    /** The list of individual pointer changes in this event. */
    val changes: List<IndirectPointerInputChange>

    /** The reason the [IndirectPointerEvent] was sent. */
    val type: IndirectPointerEventType

    /** Main coordinate axis to use for movement. */
    val primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis
}

// Work around for Kotlin cross module sealed interfaces.
internal interface PlatformIndirectPointerEvent : IndirectPointerEvent

/** Indicates the reason that the [IndirectPointerEvent] was sent. */
@kotlin.jvm.JvmInline
value class IndirectPointerEventType private constructor(internal val value: Int) {
    companion object {

        /** An unknown reason for the event. */
        val Unknown = IndirectPointerEventType(0)

        /** A pressed gesture as started. */
        val Press = IndirectPointerEventType(1)

        /** A pressed gesture has finished. */
        val Release = IndirectPointerEventType(2)

        /** A change has happened during a press gesture. */
        val Move = IndirectPointerEventType(3)
    }

    override fun toString(): String =
        when (this) {
            Press -> "Press"
            Release -> "Release"
            Move -> "Move"
            else -> "Unknown"
        }
}

/**
 * The primary axis for motion from an [IndirectPointerEvent]. Indirect input devices such as
 * touchpads that do not move a cursor on screen may define a primary axis for motion (such as
 * scrolling). This facilitates the translation of a 2D input gesture into a 1D scroll on the
 * screen. For example, an input device might be wide horizontally but narrow vertically. In such a
 * case, it would designate X as its primary axis of motion. This means horizontal scrolling on the
 * input device would cause a horizontal list to scroll horizontally, and a vertical list to scroll
 * vertically - even though the direction of motion on the input device is horizontal in both cases.
 */
@kotlin.jvm.JvmInline
value class IndirectPointerEventPrimaryDirectionalMotionAxis
private constructor(internal val value: Int) {
    companion object {

        /** No coordinate axes specified for movement. */
        val None = IndirectPointerEventPrimaryDirectionalMotionAxis(0)

        /** X coordinate axis specified as the primary movement axis. */
        val X = IndirectPointerEventPrimaryDirectionalMotionAxis(1)

        /** Y coordinate axis specified as the primary movement axis. */
        val Y = IndirectPointerEventPrimaryDirectionalMotionAxis(2)
    }
}

/**
 * Represents a single pointer input change for an indirect pointer event. The coordinate space does
 * NOT map to the screen space but to the coordinate space of the device sending the data (thus the
 * name indirect pointer change).
 *
 * @param id The unique identifier for the pointer.
 * @param uptimeMillis The time at which the event occurred.
 * @param position The position of the pointer on the input device (not screen).
 * @param pressed Whether the pointer is down or up.
 * @param pressure The pressure of the pointer.
 * @param previousUptimeMillis The time at which the previous event occurred.
 * @param previousPosition The position of the pointer on the input device (not screen) at the
 *   previous event.
 * @param previousPressed Whether the pointer was down or up at the previous event.
 */
class IndirectPointerInputChange(
    val id: PointerId,
    val uptimeMillis: Long,
    val position: Offset,
    @get:Suppress("GetterSetterNames") val pressed: Boolean,
    val pressure: Float,
    val previousUptimeMillis: Long,
    val previousPosition: Offset,
    @get:Suppress("GetterSetterNames") val previousPressed: Boolean,
) {
    /** Indicates whether the change was consumed or not. */
    var isConsumed: Boolean = false
        private set

    /** Consumes the change event, claiming it for the caller. */
    fun consume() {
        isConsumed = true
    }

    override fun toString(): String {
        return "IndirectPointerInputChange(id=$id, " +
            "uptimeMillis=$uptimeMillis, " +
            "position=$position, " +
            "pressed=$pressed, " +
            "pressure=$pressure, " +
            "previousUptimeMillis=$previousUptimeMillis, " +
            "previousPosition=$previousPosition, " +
            "previousPressed=$previousPressed, " +
            "isConsumed=$isConsumed)"
    }
}

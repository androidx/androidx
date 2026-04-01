/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastMap
import kotlin.jvm.JvmInline

/**
 * Represents pointer such as mouse cursor, or touch/stylus press.
 * There can be multiple pointers on the screen at the same time.
 */
@InternalComposeUiApi
class ComposeScenePointer(
    /**
     * Unique id associated with the pointer. Used to distinguish between multiple pointers that can exist
     * at the same time (i.e. multiple pressed touches).
     */
    val id: PointerId,

    /**
     * The [Offset] of the pointer.
     */
    val position: Offset,

    /**
     * `true` if the pointer event is considered "pressed". For example,
     * a finger touches the screen or any mouse button is pressed.
     *  During the up event, pointer is considered not pressed.
     */
    val pressed: Boolean,

    /**
     * The device type associated with the pointer, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     */
    val type: PointerType = PointerType.Mouse,

    /**
     * Pressure of the pointer. 0.0 - no pressure, 1.0 - average pressure
     */
    val pressure: Float = 1.0f,

    /**
     * High-frequency pointer moves in between the current event and the last event.
     * can be used for extra accuracy when touchscreen rate exceeds framerate.
     *
     * Can be empty, if a platform doesn't provide any.
     *
     * For example, on iOS this list is populated using the data of.
     * https://developer.apple.com/documentation/uikit/uievent/1613808-coalescedtouchesfortouch?language=objc
     */
    val historical: List<HistoricalChange> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ComposeScenePointer

        if (position != other.position) return false
        if (pressed != other.pressed) return false
        if (type != other.type) return false
        if (id != other.id) return false
        if (pressure != other.pressure) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + pressed.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + pressure.hashCode()
        return result
    }

    override fun toString(): String {
        return "Pointer(position=$position, pressed=$pressed, type=$type, id=$id, pressure=$pressure)"
    }
}

internal fun PointerInputEvent(
    eventType: PointerEventType,
    pointers: List<ComposeScenePointer>,
    timeMillis: Long,
    nativeEvent: Any?,
    scrollDelta: Offset,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers,
    changedButton: PointerButton?,
    scaleGestureFactor: Float,
    panGestureOffset: Offset,
) = PointerInputEvent(
    eventType = eventType,
    uptime = timeMillis,
    pointers = pointers.fastMap {
        PointerInputEventData(
            id = it.id,
            uptime = timeMillis,
            positionOnScreen = it.position,
            position = it.position,
            down = it.pressed,
            pressure = it.pressure,
            type = it.type,
            activeHover = it.type == PointerType.Mouse,
            historical = it.historical,
            scrollDelta = scrollDelta,
            originalEventPosition = it.position,
            scaleGestureFactor = scaleGestureFactor,
            panGestureOffset = panGestureOffset,
        )
    },
    buttons = buttons,
    keyboardModifiers = keyboardModifiers,
    nativeEvent = nativeEvent,
    button = changedButton
)

/**
 * The result of processing a pointer event.
 *
 * @property value is a bitmask indicating the properties of the result.
 * Use [anyMovementConsumed] and [anyChangeConsumed] to read those properties.
 *
 * The value is the same as in [androidx.compose.ui.input.pointer.ProcessResult],
 * but we can't just reuse it because it's internal.
 */
@InternalComposeUiApi
@JvmInline
value class PointerEventResult internal constructor(internal val value: Int) {

    constructor(
        anyMovementConsumed: Boolean = false,
        anyChangeConsumed: Boolean = false,
        dispatchedToAPointerInputModifier: Boolean = false,
    ) : this(
        value =
            dispatchedToAPointerInputModifier.toInt() or
            (anyMovementConsumed.toInt() shl 1) or
            (anyChangeConsumed.toInt() shl 2)
    )

    internal val dispatchedToAPointerInputModifier
        inline get() = (value and 0x1) != 0

    /** It's true when [PointerInputChange] was consumed and Pointer's position was changed */
    internal val anyMovementConsumed
        inline get() = (value and 0x2) != 0

    /** It's true when any [PointerInputChange] was consumed. */
    internal val anyChangeConsumed
        inline get() = (value and 0x4) != 0
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Boolean.toInt() = if (this) 1 else 0

// TODO: Rewrite to vararg after https://youtrack.jetbrains.com/issue/KT-33565/Allow-vararg-parameter-of-inline-class-type
internal fun PointerEventResult.merging(
    result1: PointerEventResult,
    result2: PointerEventResult? = null,
    result3: PointerEventResult? = null,
    result4: PointerEventResult? = null,
    result5: PointerEventResult? = null,
    result6: PointerEventResult? = null,
    result7: PointerEventResult? = null,
) = PointerEventResult(
    value = this.value or result1.value or (result2?.value ?: 0) or (result3?.value ?: 0) or
        (result4?.value ?: 0) or (result5?.value ?: 0) or (result6?.value ?: 0) or (result7?.value ?: 0)
)

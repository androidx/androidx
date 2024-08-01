/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.CLASSIFICATION_AMBIGUOUS_GESTURE
import android.view.MotionEvent.CLASSIFICATION_DEEP_PRESS
import android.view.MotionEvent.CLASSIFICATION_NONE
import android.view.MotionEvent.CLASSIFICATION_PINCH
import android.view.MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
import androidx.annotation.IntDef
import androidx.collection.LongSparseArray
import androidx.compose.ui.util.fastForEach

internal actual typealias NativePointerButtons = Int

internal actual typealias NativePointerKeyboardModifiers = Int

/**
 * Restricts Ints to `MotionEvent`'s classification types. See the
 * [Android documentation on MotionEvent.getClassification()]
 * (https://developer.android.com/reference/android/view/MotionEvent#getClassification()) for more
 * details.
 */
@IntDef(
    CLASSIFICATION_NONE,
    CLASSIFICATION_AMBIGUOUS_GESTURE,
    CLASSIFICATION_DEEP_PRESS,
    CLASSIFICATION_TWO_FINGER_SWIPE,
    CLASSIFICATION_PINCH
)
@Retention(AnnotationRetention.SOURCE) // Only for compile-time checks
internal annotation class MotionEventClassification

/** Describes a pointer input change event that has occurred at a particular point in time. */
actual class PointerEvent
internal actual constructor(
    /** The changes. */
    actual val changes: List<PointerInputChange>,
    internal val internalPointerEvent: InternalPointerEvent?
) {
    internal val motionEvent: MotionEvent?
        get() = internalPointerEvent?.motionEvent

    /** Returns `MotionEvent`'s classification. */
    @get:MotionEventClassification
    val classification: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                motionEvent?.classification ?: CLASSIFICATION_NONE
            } else {
                CLASSIFICATION_NONE // Return NONE for versions lower than Android Q
            }

    /** @param changes The changes. */
    actual constructor(changes: List<PointerInputChange>) : this(changes, null)

    actual val buttons = PointerButtons(motionEvent?.buttonState ?: 0)

    actual val keyboardModifiers = PointerKeyboardModifiers(motionEvent?.metaState ?: 0)

    actual var type: PointerEventType = calculatePointerEventType()
        internal set

    private fun calculatePointerEventType(): PointerEventType {
        val motionEvent = motionEvent
        if (motionEvent != null) {
            return when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> PointerEventType.Press
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> PointerEventType.Release
                MotionEvent.ACTION_HOVER_MOVE,
                MotionEvent.ACTION_MOVE -> PointerEventType.Move
                MotionEvent.ACTION_HOVER_ENTER -> PointerEventType.Enter
                MotionEvent.ACTION_HOVER_EXIT -> PointerEventType.Exit
                ACTION_SCROLL -> PointerEventType.Scroll
                else -> PointerEventType.Unknown
            }
        }
        // Used for testing.
        changes.fastForEach {
            if (it.changedToUpIgnoreConsumed()) {
                return PointerEventType.Release
            }
            if (it.changedToDownIgnoreConsumed()) {
                return PointerEventType.Press
            }
        }
        return PointerEventType.Move
    }

    // only because PointerEvent was a data class
    fun component1(): List<PointerInputChange> = changes

    // only because PointerEvent was a data class
    fun copy(changes: List<PointerInputChange>, motionEvent: MotionEvent?): PointerEvent =
        when (motionEvent) {
            null -> PointerEvent(changes, null)
            this.motionEvent -> PointerEvent(changes, internalPointerEvent)
            else -> {
                val changesArray = LongSparseArray<PointerInputChange>(changes.size)
                val pointerEventData = ArrayList<PointerInputEventData>(changes.size)
                changes.fastForEach { change ->
                    changesArray.put(change.id.value, change)
                    pointerEventData +=
                        PointerInputEventData(
                            change.id,
                            change.uptimeMillis,
                            change.position,
                            change.position,
                            change.pressed,
                            change.pressure,
                            change.type,
                            this.internalPointerEvent?.activeHoverEvent(change.id) == true
                        )
                }

                val pointerInputEvent =
                    PointerInputEvent(motionEvent.eventTime, pointerEventData, motionEvent)
                val event = InternalPointerEvent(changesArray, pointerInputEvent)
                PointerEvent(changes, event)
            }
        }
}

internal actual fun EmptyPointerKeyboardModifiers() = PointerKeyboardModifiers(0)

actual val PointerButtons.isPrimaryPressed: Boolean
    get() = packedValue and (MotionEvent.BUTTON_PRIMARY or MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

actual val PointerButtons.isSecondaryPressed: Boolean
    get() =
        packedValue and (MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_STYLUS_SECONDARY) != 0

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = packedValue and MotionEvent.BUTTON_TERTIARY != 0

actual val PointerButtons.isBackPressed: Boolean
    get() = packedValue and MotionEvent.BUTTON_BACK != 0

actual val PointerButtons.isForwardPressed: Boolean
    get() = packedValue and MotionEvent.BUTTON_FORWARD != 0

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    when (buttonIndex) {
        0 -> isPrimaryPressed
        1 -> isSecondaryPressed
        2,
        3,
        4 -> packedValue and (1 shl buttonIndex) != 0
        else -> packedValue and (1 shl (buttonIndex + 2)) != 0
    }

actual val PointerButtons.areAnyPressed: Boolean
    get() = packedValue != 0

actual fun PointerButtons.indexOfFirstPressed(): Int {
    if (packedValue == 0) {
        return -1
    }
    var index = 0
    // shift stylus primary and secondary to primary and secondary
    var shifted = ((packedValue and 0x60) ushr 5) or (packedValue and 0x60.inv())
    while (shifted and 1 == 0) {
        index++
        shifted = shifted ushr 1
    }
    return index
}

actual fun PointerButtons.indexOfLastPressed(): Int {
    // shift stylus primary and secondary to primary and secondary
    var shifted = ((packedValue and 0x60) ushr 5) or (packedValue and 0x60.inv())
    var index = -1
    while (shifted != 0) {
        index++
        shifted = shifted ushr 1
    }
    return index
}

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = (packedValue and KeyEvent.META_CTRL_ON) != 0

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = (packedValue and KeyEvent.META_META_ON) != 0

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = (packedValue and KeyEvent.META_ALT_ON) != 0

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = false

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = (packedValue and KeyEvent.META_SYM_ON) != 0

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = (packedValue and KeyEvent.META_SHIFT_ON) != 0

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = (packedValue and KeyEvent.META_FUNCTION_ON) != 0

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = (packedValue and KeyEvent.META_CAPS_LOCK_ON) != 0

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = (packedValue and KeyEvent.META_SCROLL_LOCK_ON) != 0

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = (packedValue and KeyEvent.META_NUM_LOCK_ON) != 0

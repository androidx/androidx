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

package androidx.compose.ui.hapticfeedback

import android.view.View
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/** Android implementation for [HapticFeedback] */
internal class PlatformHapticFeedback(private val view: View) : HapticFeedback {

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val constant =
            when (hapticFeedbackType) {
                HapticFeedbackType.Confirm -> HapticFeedbackConstantsCompat.CONFIRM
                HapticFeedbackType.ContextClick -> HapticFeedbackConstantsCompat.CONTEXT_CLICK
                HapticFeedbackType.GestureEnd -> HapticFeedbackConstantsCompat.GESTURE_END
                HapticFeedbackType.GestureThresholdActivate ->
                    HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE
                HapticFeedbackType.KeyboardTap -> HapticFeedbackConstantsCompat.KEYBOARD_TAP
                HapticFeedbackType.LongPress -> HapticFeedbackConstantsCompat.LONG_PRESS
                HapticFeedbackType.Reject -> HapticFeedbackConstantsCompat.REJECT
                HapticFeedbackType.SegmentFrequentTick ->
                    HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                HapticFeedbackType.SegmentTick -> HapticFeedbackConstantsCompat.SEGMENT_TICK
                HapticFeedbackType.TextHandleMove -> HapticFeedbackConstantsCompat.TEXT_HANDLE_MOVE
                HapticFeedbackType.ToggleOff -> HapticFeedbackConstantsCompat.TOGGLE_OFF
                HapticFeedbackType.ToggleOn -> HapticFeedbackConstantsCompat.TOGGLE_ON
                HapticFeedbackType.VirtualKey -> HapticFeedbackConstantsCompat.VIRTUAL_KEY
                else -> HapticFeedbackConstantsCompat.NO_HAPTICS
            }
        ViewCompat.performHapticFeedback(view, constant)
    }
}

internal actual object PlatformHapticFeedbackType {
    actual val Confirm: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.CONFIRM)
    actual val ContextClick: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.CONTEXT_CLICK)
    actual val GestureEnd: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.GESTURE_END)
    actual val GestureThresholdActivate: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE)
    actual val KeyboardTap: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.KEYBOARD_TAP)
    actual val LongPress: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.LONG_PRESS)
    actual val Reject: HapticFeedbackType = HapticFeedbackType(HapticFeedbackConstantsCompat.REJECT)
    actual val SegmentFrequentTick: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK)
    actual val SegmentTick: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.SEGMENT_TICK)
    actual val TextHandleMove: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.TEXT_HANDLE_MOVE)
    actual val ToggleOff: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.TOGGLE_OFF)
    actual val ToggleOn: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.TOGGLE_ON)
    actual val VirtualKey: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstantsCompat.VIRTUAL_KEY)
}

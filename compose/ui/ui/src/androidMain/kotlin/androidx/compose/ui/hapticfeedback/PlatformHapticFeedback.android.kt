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

import android.view.HapticFeedbackConstants
import android.view.View

/** Android implementation for [HapticFeedback] */
internal class PlatformHapticFeedback(private val view: View) : HapticFeedback {

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        when (hapticFeedbackType) {
            HapticFeedbackType.Confirm ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            HapticFeedbackType.ContextClick ->
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            HapticFeedbackType.GestureEnd ->
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
            HapticFeedbackType.GestureThresholdActivate ->
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
            HapticFeedbackType.KeyboardTap ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            HapticFeedbackType.LongPress ->
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            HapticFeedbackType.Reject -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            HapticFeedbackType.SegmentFrequentTick ->
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            HapticFeedbackType.SegmentTick ->
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
            HapticFeedbackType.TextHandleMove ->
                view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
            HapticFeedbackType.ToggleOff ->
                view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
            HapticFeedbackType.ToggleOn ->
                view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
            HapticFeedbackType.VirtualKey ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

internal actual object PlatformHapticFeedbackType {
    actual val Confirm: HapticFeedbackType = HapticFeedbackType(HapticFeedbackConstants.CONFIRM)
    actual val ContextClick: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.CONTEXT_CLICK)
    actual val GestureEnd: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.GESTURE_END)
    actual val GestureThresholdActivate: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
    actual val KeyboardTap: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.KEYBOARD_TAP)
    actual val LongPress: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.LONG_PRESS)
    actual val Reject: HapticFeedbackType = HapticFeedbackType(HapticFeedbackConstants.REJECT)
    actual val SegmentFrequentTick: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    actual val SegmentTick: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.SEGMENT_TICK)
    actual val TextHandleMove: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
    actual val ToggleOff: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.TOGGLE_OFF)
    actual val ToggleOn: HapticFeedbackType = HapticFeedbackType(HapticFeedbackConstants.TOGGLE_ON)
    actual val VirtualKey: HapticFeedbackType =
        HapticFeedbackType(HapticFeedbackConstants.VIRTUAL_KEY)
}

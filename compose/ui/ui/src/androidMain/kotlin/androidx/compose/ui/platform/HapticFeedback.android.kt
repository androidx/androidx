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

package androidx.compose.ui.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Provide a default implementation of HapticFeedback to call through to the view's
 * [performHapticFeedback] with the associated HapticFeedbackConstant.
 *
 * @param view The current view, used for forwarding haptic feedback requests.
 */
internal class DefaultHapticFeedback(private val view: View) : HapticFeedback {
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

/** Provide a no-op implementation of HapticFeedback */
internal class NoHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // No-op
    }
}

/** Contains defaults for haptics functionality */
internal object HapticDefaults {
    /**
     * Returns whether the device supports premium haptic feedback.
     *
     * @param context The current context for access to the Vibrator via System Service.
     */
    fun isPremiumVibratorEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrator = context.getSystemService(Vibrator::class.java)

            // NB whilst the 'areAllPrimitivesSupported' API needs R (API 30), we need S (API
            // 31) so that PRIMITIVE_THUD is available.
            if (
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_THUD
                )
            ) {
                return true
            }
        }

        return false
    }
}

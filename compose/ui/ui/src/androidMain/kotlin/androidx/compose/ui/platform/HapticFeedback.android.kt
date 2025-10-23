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
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * Provide a default implementation of HapticFeedback to call through to the view's
 * [performHapticFeedback] with the associated HapticFeedbackConstant.
 *
 * @param view The current view, used for forwarding haptic feedback requests.
 */
internal class DefaultHapticFeedback(private val view: View) : HapticFeedback {
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
                    VibrationEffect.Composition.PRIMITIVE_THUD,
                )
            ) {
                return true
            }
        }

        return false
    }
}

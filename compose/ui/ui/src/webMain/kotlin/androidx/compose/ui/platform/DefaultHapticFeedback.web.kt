/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.JsNumber
import kotlin.js.js
import kotlin.js.toJsArray
import kotlin.js.toJsNumber

@OptIn(ExperimentalWasmJsInterop::class)
internal class WebHapticFeedback : HapticFeedback {

    // on Android these values are configured
    // see config_longPressVibePattern in https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/res/values/config.xml
    // We don't have a high-level browser API right now. So we hardcode the patterns here.
    // TODO: to eventually avoid the hardcoded values, follow the new browser API proposal https://github.com/WICG/web-haptics
    companion object {
        private val ConfirmVibrationPattern = vibrationPatternOf(18, 32, 36)
        private val RejectVibrationPattern = vibrationPatternOf(18, 28, 18, 28, 18)
        private val SinglePulseVibrationPattern = vibrationPatternOf(12)
        private val SoftTickVibrationPattern = vibrationPatternOf(6)
        private val LongPressVibrationPattern = vibrationPatternOf(0, 30)
        private val VirtualKeyVibrationPattern = vibrationPatternOf(0, 20)

        fun webHapticFeedbackOrDefault(): HapticFeedback =  if (isVibrationSupported()) WebHapticFeedback() else DefaultHapticFeedback
    }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val pattern = vibrationPatternFor(hapticFeedbackType) ?: return
        vibrate(pattern)
    }


    @OptIn(ExperimentalWasmJsInterop::class)
    private fun vibrationPatternFor(hapticFeedbackType: HapticFeedbackType): JsArray<JsNumber>? {
        return when (hapticFeedbackType) {
            HapticFeedbackType.Confirm -> ConfirmVibrationPattern
            HapticFeedbackType.ContextClick -> SinglePulseVibrationPattern
            HapticFeedbackType.GestureEnd -> SinglePulseVibrationPattern
            HapticFeedbackType.GestureThresholdActivate -> SinglePulseVibrationPattern
            HapticFeedbackType.KeyboardTap -> SoftTickVibrationPattern
            HapticFeedbackType.LongPress -> LongPressVibrationPattern
            HapticFeedbackType.Reject -> RejectVibrationPattern
            HapticFeedbackType.SegmentFrequentTick -> SoftTickVibrationPattern
            HapticFeedbackType.SegmentTick -> SoftTickVibrationPattern
            HapticFeedbackType.TextHandleMove -> ConfirmVibrationPattern
            HapticFeedbackType.ToggleOff -> SinglePulseVibrationPattern
            HapticFeedbackType.ToggleOn -> SinglePulseVibrationPattern
            HapticFeedbackType.VirtualKey -> VirtualKeyVibrationPattern
            else -> null
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun vibrationPatternOf(vararg durations: Int): JsArray<JsNumber> =
    durations.map { it.toJsNumber() }.toJsArray()

//language=javascript
@OptIn(ExperimentalWasmJsInterop::class)
private fun vibrate(pattern: JsArray<JsNumber>) {
    // Assuming the API support has been checked in advance, we can safely call it
    js("window.navigator.vibrate(pattern)")
}

private fun isVibrationSupported(): Boolean = js(
    //language=javascript
    """
        typeof window !== 'undefined' &&
        window.navigator != null &&
        typeof window.navigator.vibrate === 'function'
    """
)
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

package androidx.core.haptics.device

import android.os.Build
import androidx.core.haptics.signal.CompositionSignal
import androidx.core.haptics.signal.HapticSignal
import androidx.core.haptics.signal.PredefinedEffectSignal
import androidx.core.haptics.signal.WaveformSignal
import java.util.Objects

/** A [HapticDeviceProfile] describes the vibrator hardware capabilities for a device. */
public class HapticDeviceProfile
@JvmOverloads
constructor(

    /**
     * Hint for whether the device supports controlling the vibration strength.
     *
     * The actual amplitude control support also depends on the minimum SDK level required for the
     * platform APIs that accept vibration amplitudes.
     */
    amplitudeControlSupportHint: Boolean = false,

    /**
     * Hint for the predefined effects with hardware optimization confirmed by the device vibrator.
     *
     * The actual optimized effects also depends on the minimum SDK level required for the platform
     * APIs that accept predefined effects.
     */
    hardwareOptimizedPredefinedEffectsHint: Set<PredefinedEffectSignal> = emptySet(),

    /** The vibrator capabilities related to the support for [CompositionSignal]s. */
    public val compositionProfile: HapticCompositionProfile = HapticCompositionProfile(),
) {
    /**
     * Whether the device supports controlling the vibration strength.
     *
     * Devices with amplitude control support are able to vibrate with different strengths. This
     * means that waveform signals like step waveforms, defined by constant signals at different
     * amplitudes, can be played on the device.
     *
     * Devices without amplitude control are only able to turn the vibrator motor on and off at a
     * fixed strength. This means only on/off vibration patterns can be played on the device.
     */
    public val isAmplitudeControlSupported: Boolean

    /**
     * The predefined effects with hardware optimization confirmed by the device vibrator.
     *
     * Predefined effects are always supported by the Android platform, but not all devices provide
     * optimized hardware implementation for these haptic effects.
     *
     * The set contains only the effects that have confirmed hardware implementation reported by the
     * device vibrator. The set will be empty if the device hardware does not report support for any
     * predefined haptic effect, or if the required APIs are not available in this SDK level.
     */
    public val hardwareOptimizedPredefinedEffects: Set<PredefinedEffectSignal>

    init {
        // No amplitude waveform is supported before Android O.
        isAmplitudeControlSupported =
            amplitudeControlSupportHint && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val availablePredefinedEffects = PredefinedEffectSignal.getSdkAvailableEffects()
        hardwareOptimizedPredefinedEffects =
            hardwareOptimizedPredefinedEffectsHint
                .filter { availablePredefinedEffects.contains(it) }
                .toSet()
    }

    /**
     * Returns true if the vibrator has the necessary capabilities to play the given haptic signal.
     *
     * Note that playing an unsupported [HapticSignal] on a device vibrator might lead to unexpected
     * results. Some haptic signals may be ignored, e.g. [CompositionSignal] on lower SDKs or
     * devices without the primitives support. Other signals might play a different vibration than
     * the one intended, e.g. [WaveformSignal] with partial amplitudes on a device without amplitude
     * control will play at a fixed default vibration strength.
     *
     * This method will always return true for [PredefinedEffectSignal], as the platform will play a
     * device-specific predefined vibration even without hardware support for these effects. If
     * hardware support is required for your use case then check
     * [hardwareOptimizedPredefinedEffects] directly.
     *
     * @param signal The haptic signal to check for support
     * @return true if the device vibrator can play the given haptic signal as intended, false
     *   otherwise.
     */
    public fun supports(signal: HapticSignal): Boolean = signal.isSupportedBy(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticDeviceProfile) return false
        if (isAmplitudeControlSupported != other.isAmplitudeControlSupported) return false
        if (hardwareOptimizedPredefinedEffects != other.hardwareOptimizedPredefinedEffects) {
            return false
        }
        if (compositionProfile != other.compositionProfile) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            isAmplitudeControlSupported,
            hardwareOptimizedPredefinedEffects,
            compositionProfile,
        )
    }

    override fun toString(): String {
        return "HapticDeviceProfile(" +
            "isAmplitudeControlSupported=$isAmplitudeControlSupported," +
            "hardwareOptimizedPredefinedEffects=" +
            "${
                hardwareOptimizedPredefinedEffects.joinToString(
                    prefix = "[",
                    postfix = "]",
                    transform = { PredefinedEffectSignal.typeToString(it.type) },
                )
            }," +
            "compositionProfile=$compositionProfile)"
    }
}

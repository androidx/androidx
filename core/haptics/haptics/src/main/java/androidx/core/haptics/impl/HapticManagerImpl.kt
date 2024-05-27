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

package androidx.core.haptics.impl

import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.haptics.HapticAttributes
import androidx.core.haptics.HapticManager
import androidx.core.haptics.VibratorWrapper
import androidx.core.haptics.device.HapticCompositionProfile
import androidx.core.haptics.device.HapticDeviceProfile
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import androidx.core.haptics.signal.HapticSignal
import androidx.core.haptics.signal.PredefinedEffectSignal

/** [HapticManager] implementation for the [Vibrator] service. */
internal class HapticManagerImpl internal constructor(private val vibrator: VibratorWrapper) :
    HapticManager {

    init {
        require(vibrator.hasVibrator()) {
            "Haptic manager cannot be created for a device without vibrator"
        }
    }

    private val deviceProfileDelegate = lazy {
        val supportedPrimitiveTypes = getSupportedPrimitiveTypes()
        HapticDeviceProfile(
            vibrator.hasAmplitudeControl(),
            getHardwareSupportedPredefinedEffects(),
            HapticCompositionProfile(
                supportedPrimitiveTypes,
                getPrimitiveDurations(supportedPrimitiveTypes),
            ),
        )
    }
    override val deviceProfile: HapticDeviceProfile by deviceProfileDelegate

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun play(signal: HapticSignal, attrs: HapticAttributes) {
        signal.toVibration()?.let { vibration -> vibrator.vibrate(vibration, attrs.toAttributes()) }
    }

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun cancel() {
        vibrator.cancel()
    }

    private fun getHardwareSupportedPredefinedEffects(): Set<PredefinedEffectSignal> {
        val predefinedEffects = PredefinedEffectSignal.getSdkAvailableEffects()
        val effectSupportArray =
            vibrator.areEffectsSupported(predefinedEffects.map { it.type }.toIntArray())

        if (effectSupportArray == null) {
            return emptySet()
        }

        return effectSupportArray
            .mapIndexed { index, effectSupport ->
                if (effectSupport == VibratorWrapper.EffectSupport.YES) {
                    predefinedEffects[index]
                } else {
                    null
                }
            }
            .filterNotNull()
            .toSet()
    }

    private fun getSupportedPrimitiveTypes(): Set<Int> {
        val primitiveTypes = PrimitiveAtom.getSdkAvailablePrimitiveTypes()
        val primitiveSupportArray = vibrator.arePrimitivesSupported(primitiveTypes.toIntArray())

        if (primitiveSupportArray == null) {
            return emptySet()
        }

        return primitiveSupportArray
            .mapIndexed { index, isSupported ->
                if (isSupported) {
                    primitiveTypes[index]
                } else {
                    null
                }
            }
            .filterNotNull()
            .toSet()
    }

    private fun getPrimitiveDurations(supportedPrimitiveTypes: Set<Int>): Map<Int, Long>? {
        val primitiveTypes = supportedPrimitiveTypes.toIntArray()
        val primitiveDurationArray = vibrator.getPrimitivesDurations(primitiveTypes)

        if (primitiveDurationArray == null) {
            return null
        }

        return primitiveDurationArray
            .mapIndexed { index, durationMillis ->
                primitiveTypes[index] to durationMillis.toLong()
            }
            .toMap()
    }
}

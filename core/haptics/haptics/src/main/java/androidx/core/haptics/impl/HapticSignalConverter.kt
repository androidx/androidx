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

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.haptics.PatternVibrationWrapper
import androidx.core.haptics.VibrationEffectWrapper
import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.signal.CompositionSignal
import androidx.core.haptics.signal.CompositionSignal.OffAtom
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import androidx.core.haptics.signal.PredefinedEffectSignal
import androidx.core.haptics.signal.WaveformSignal
import androidx.core.haptics.signal.WaveformSignal.ConstantVibrationAtom
import androidx.core.haptics.signal.WaveformSignal.ConstantVibrationAtom.Companion.DEFAULT_AMPLITUDE
import kotlin.math.roundToInt

private const val VIBRATION_DEFAULT_AMPLITUDE: Int = -1 // VibrationEffect.DEFAULT_AMPLITUDE
private const val VIBRATION_MAX_AMPLITUDE: Int = 255 // VibrationEffect.MAX_AMPLITUDE

/** Returns true if amplitude is 0, 1 or [DEFAULT_AMPLITUDE]. */
internal fun ConstantVibrationAtom.hasPatternAmplitude(): Boolean =
    (amplitude == 0f) || (amplitude == 1f) || (amplitude == DEFAULT_AMPLITUDE)

/** Returns the amplitude value in [0,255] or [android.os.VibrationEffect.DEFAULT_AMPLITUDE]. */
internal fun ConstantVibrationAtom.getAmplitudeInt(): Int =
    if (amplitude == DEFAULT_AMPLITUDE) {
        VIBRATION_DEFAULT_AMPLITUDE
    } else {
        (amplitude * VIBRATION_MAX_AMPLITUDE).roundToInt()
    }

/**
 * Helper class to convert haptic signals to platform types based on SDK support available.
 */
internal object HapticSignalConverter {

    internal fun toVibration(effect: PredefinedEffectSignal): VibrationWrapper? =
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.toVibrationEffect(effect)
        } else {
            ApiImpl.toPatternVibration(effect)
        }

    internal fun toVibration(
        initialWaveform: WaveformSignal?,
        repeatingWaveform: WaveformSignal?
    ): VibrationWrapper? =
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.toVibrationEffect(initialWaveform, repeatingWaveform)
        } else {
            ApiImpl.toPatternVibration(initialWaveform, repeatingWaveform)
        }

    internal fun toVibration(composition: CompositionSignal): VibrationWrapper? =
        if (Build.VERSION.SDK_INT >= 31) {
            Api31Impl.toVibrationEffect(composition)
        } else if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.toVibrationEffect(composition)
        } else {
            null
        }

    /** Version-specific static inner class. */
    @RequiresApi(31)
    private object Api31Impl {
        @JvmStatic
        @DoNotInline
        fun toVibrationEffect(composition: CompositionSignal): VibrationEffectWrapper? =
            if (composition.minSdk() <= 31) {
                // Use same API to create composition from API 30, but allow constants from API 31.
                Api30Impl.createComposition(composition)
            } else {
                null
            }
    }

    /** Version-specific static inner class. */
    @RequiresApi(30)
    private object Api30Impl {
        @JvmStatic
        @DoNotInline
        fun toVibrationEffect(composition: CompositionSignal): VibrationEffectWrapper? =
            if (composition.minSdk() <= 30) {
                createComposition(composition)
            } else {
                null
            }

        @JvmStatic
        @DoNotInline
        fun createComposition(composition: CompositionSignal): VibrationEffectWrapper? {
            val platformComposition = VibrationEffect.startComposition()
            var delayMs = 0

            composition.atoms.forEach { atom ->
                when (atom) {
                    is PrimitiveAtom -> {
                        platformComposition.addPrimitive(atom.type, atom.amplitudeScale, delayMs)
                        delayMs = 0
                    }

                    is OffAtom -> {
                        delayMs += atom.durationMillis.toInt()
                    }

                    else -> {
                        // Unsupported composition atom
                        return@createComposition null
                    }
                }
            }

            return VibrationEffectWrapper(platformComposition.compose())
        }
    }

    /** Version-specific static inner class. */
    @RequiresApi(29)
    private object Api29Impl {
        @JvmStatic
        @DoNotInline
        fun toVibrationEffect(effect: PredefinedEffectSignal): VibrationEffectWrapper? =
            if (effect.minSdk() <= 29) {
                VibrationEffectWrapper(VibrationEffect.createPredefined(effect.type))
            } else {
                null
            }
    }

    /** Version-specific static inner class. */
    @RequiresApi(26)
    private object Api26Impl {

        @JvmStatic
        @DoNotInline
        fun toVibrationEffect(
            initialWaveform: WaveformSignal? = null,
            repeatingWaveform: WaveformSignal? = null,
        ): VibrationEffectWrapper? {
            if (initialWaveform?.atoms?.any { it !is ConstantVibrationAtom } == true ||
                repeatingWaveform?.atoms?.any { it !is ConstantVibrationAtom } == true) {
                // Unsupported waveform atoms
                return null
            }

            val initialAtoms =
                initialWaveform?.atoms?.filterIsInstance<ConstantVibrationAtom>().orEmpty()
            val repeatingAtoms =
                repeatingWaveform?.atoms?.filterIsInstance<ConstantVibrationAtom>().orEmpty()
            val allAtoms = initialAtoms + repeatingAtoms

            val timings = allAtoms.map { it.durationMillis }.toLongArray()
            val amplitudes = allAtoms.map { it.getAmplitudeInt() }.toIntArray()
            val repeatIndex = if (repeatingAtoms.isNotEmpty()) initialAtoms.size else -1

            if (timings.isEmpty() || timings.sum() == 0L) {
                // Empty or zero duration waveforms not supported by VibrationEffect.createWaveform
                return null
            }

            return VibrationEffectWrapper(
                VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)
            )
        }
    }

    /** Version-specific static inner class. */
    private object ApiImpl {

        @JvmStatic
        fun toPatternVibration(effect: PredefinedEffectSignal): PatternVibrationWrapper? =
            // Fallback patterns for predefined effects in SDK < 29.
            when (effect.type) {
                PredefinedEffectSignal.TICK ->
                    PatternVibrationWrapper(longArrayOf(0, 10), repeatIndex = -1)
                PredefinedEffectSignal.CLICK ->
                    PatternVibrationWrapper(longArrayOf(0, 20), repeatIndex = -1)
                PredefinedEffectSignal.HEAVY_CLICK ->
                    PatternVibrationWrapper(longArrayOf(0, 30), repeatIndex = -1)
                PredefinedEffectSignal.DOUBLE_CLICK ->
                    PatternVibrationWrapper(longArrayOf(0, 30, 100, 30), repeatIndex = -1)
                else ->
                    null
            }

        @JvmStatic
        fun toPatternVibration(
            initialWaveform: WaveformSignal? = null,
            repeatingWaveform: WaveformSignal? = null,
        ): PatternVibrationWrapper? {
            if (initialWaveform?.atoms?.any { it !is ConstantVibrationAtom } == true ||
                repeatingWaveform?.atoms?.any { it !is ConstantVibrationAtom } == true) {
                // Unsupported waveform entries
                return null
            }

            val initialAtoms =
                initialWaveform?.atoms?.filterIsInstance<ConstantVibrationAtom>().orEmpty()
                    .toMutableList()
            val repeatingAtoms =
                repeatingWaveform?.atoms?.filterIsInstance<ConstantVibrationAtom>().orEmpty()

            if (!initialAtoms.all { it.hasPatternAmplitude() } ||
                !repeatingAtoms.all { it.hasPatternAmplitude() }) {
                // Not possible to represent all amplitudes by an on-off pattern.
                return null
            }

            val allAtoms = initialAtoms + repeatingAtoms
            val timings = mutableListOf<Long>()
            var currentIsOff = true // Vibration pattern starts with an off entry.
            var currentTiming = 0L
            var repeatIndex = -1

            for ((index, atom) in allAtoms.withIndex()) {
                if (index == initialAtoms.size) { // This is the first repeating atom.
                    if (currentTiming > 0) {
                        // Make sure not to merge the last initial atom to the first repeating one.
                        timings.add(currentTiming)
                        currentTiming = 0
                        currentIsOff = !currentIsOff
                    }
                    // Mark the start of the repetition before adding the first repeating atom.
                    repeatIndex = timings.size
                }
                val atomIsOff = atom.amplitude == 0f
                if (currentIsOff == atomIsOff) {
                    // Merge timings of same on/off state.
                    currentTiming += atom.durationMillis
                } else {
                    // Start new timing with different on/off state.
                    timings.add(currentTiming)
                    currentTiming = atom.durationMillis
                    currentIsOff = atomIsOff
                }
            }

            if (currentTiming > 0) {
                // Add last timing entry to the pattern.
                timings.add(currentTiming)
            }

            if (timings.isEmpty() || timings.sum() == 0L) {
                // Empty or zero duration waveforms are not supported by pattern vibrations.
                return null
            }

            return PatternVibrationWrapper(timings.toLongArray(), repeatIndex)
        }
    }
}

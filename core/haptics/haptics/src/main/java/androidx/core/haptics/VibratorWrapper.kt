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

package androidx.core.haptics

import androidx.annotation.RequiresPermission

/**
 * Internal wrapper for [android.os.Vibrator] to enable fake implementations for testing.
 */
internal interface VibratorWrapper {
    /** Check whether the hardware has a vibrator. */
    fun hasVibrator(): Boolean

    /** Check whether the vibrator has amplitude control. */
    fun hasAmplitudeControl(): Boolean

    /** Check whether the hardware supports each predefined effect type. */
    fun areEffectsSupported(effects: IntArray): Array<EffectSupport>?

    /** Check whether the hardware supports each primitive effect type. */
    fun arePrimitivesSupported(primitives: IntArray): BooleanArray?

    /** Vibrate with a given vibration effect or pattern. */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun vibrate(vibration: VibrationWrapper)

    /** Cancel any ongoing vibration from this app and turns the vibrator off. */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun cancel()

    /** Represents constants from [android.os.Vibrator.VIBRATION_EFFECT_SUPPORT_*]. */
    enum class EffectSupport {
        UNKNOWN, YES, NO
    }
}

/**
 * Represents different API levels of support for [android.os.Vibrator.vibrate] parameters.
 */
internal sealed interface VibrationWrapper

/**
 * Represents vibrations defined by on-off patterns.
 */
internal data class PatternVibrationWrapper(
    val timings: LongArray,
    val repeatIndex: Int,
) : VibrationWrapper {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatternVibrationWrapper

        if (!timings.contentEquals(other.timings)) return false
        if (repeatIndex != other.repeatIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timings.contentHashCode()
        result = 31 * result + repeatIndex.hashCode()
        return result
    }
}

/**
 * Represents vibrations defined by an instance of [android.os.VibrationEffect].
 */
internal data class VibrationEffectWrapper(
    val vibrationEffect: Any,
) : VibrationWrapper

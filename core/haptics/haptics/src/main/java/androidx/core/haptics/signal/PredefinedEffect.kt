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

package androidx.core.haptics.signal

/**
 * A [PredefinedEffect] describes a haptic effect to be played by a vibrator.
 *
 * Predefined effects represent common vibration effects that should be identical, regardless of
 * the app they come from, in order to provide a cohesive experience for users across the entire
 * device.
 *
 * They also may be custom tailored to the device hardware in order to provide a better
 * experience than you could otherwise build using the generic building blocks.
 *
 * This will fallback to a generic pattern if one exists and there is no hardware-specific
 * implementation of the effect available.
 */
class PredefinedEffect private constructor(

    /** The id of the effect to be played. */
    internal val effectId: Int
) {

    companion object {

        /**
         * A standard tick effect.
         *
         * This effect is less strong than the [PredefinedClick].
         */
        @JvmField
        val PredefinedTick = PredefinedEffect(2) // VibrationEffect.EFFECT_TICK

        /**
         * A standard click effect.
         *
         * Use this effect as a baseline, as it's the most common type of click effect.
         */
        @JvmField
        val PredefinedClick = PredefinedEffect(0) // VibrationEffect.EFFECT_CLICK

        /**
         * A heavy click effect.
         *
         * This effect is stronger than the [PredefinedClick].
         */
        @JvmField
        val PredefinedHeavyClick = PredefinedEffect(5) // VibrationEffect.EFFECT_HEAVY_CLICK

        /**
         * A double-click effect.
         */
        @JvmField
        val PredefinedDoubleClick = PredefinedEffect(1) // VibrationEffect.EFFECT_DOUBLE_CLICK
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PredefinedEffect) return false
        if (effectId != other.effectId) return false
        return true
    }

    override fun hashCode(): Int {
        return effectId.hashCode()
    }
}

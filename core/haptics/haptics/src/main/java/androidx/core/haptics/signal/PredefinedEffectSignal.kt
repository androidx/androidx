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

import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.device.HapticDeviceProfile
import androidx.core.haptics.impl.HapticSignalConverter

/**
 * A predefined haptic effect that represents common vibration effects, like clicks and ticks.
 *
 * Predefined haptic effects should be identical, regardless of the app they come from, in order to
 * provide a cohesive experience for users across the entire device. The predefined effects are
 * based on the [android.os.VibrationEffect.createPredefined] platform API.
 *
 * @sample androidx.core.haptics.samples.PlaySystemStandardClick
 */
public class PredefinedEffectSignal
private constructor(

    /** The type of haptic effect to be played. */
    @Type internal val type: Int,

    /** The minimum SDK level where this effect type is available in the platform. */
    private val minSdk: Int,
) : FiniteSignal() {

    /** Typedef for the [type] attribute. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TICK,
        CLICK,
        HEAVY_CLICK,
        DOUBLE_CLICK,
    )
    public annotation class Type

    public companion object {
        internal const val TICK = 2 // VibrationEffect.EFFECT_TICK
        internal const val CLICK = 0 // VibrationEffect.EFFECT_CLICK
        internal const val HEAVY_CLICK = 5 // VibrationEffect.EFFECT_HEAVY_CLICK
        internal const val DOUBLE_CLICK = 1 // VibrationEffect.EFFECT_DOUBLE_CLICK

        private val Tick = PredefinedEffectSignal(TICK, Build.VERSION_CODES.Q)
        private val Click = PredefinedEffectSignal(CLICK, Build.VERSION_CODES.Q)
        private val HeavyClick = PredefinedEffectSignal(HEAVY_CLICK, Build.VERSION_CODES.Q)
        private val DoubleClick = PredefinedEffectSignal(DOUBLE_CLICK, Build.VERSION_CODES.Q)

        internal val ALL_EFFECTS = listOf(Tick, Click, HeavyClick, DoubleClick)

        /** Returns all [PredefinedEffectSignal] types available at the current SDK level. */
        @JvmStatic
        internal fun getSdkAvailableEffects(): List<PredefinedEffectSignal> =
            ALL_EFFECTS.filter { it.minSdk <= Build.VERSION.SDK_INT }.toList()

        @JvmStatic
        internal fun typeToString(@Type type: Int): String {
            return when (type) {
                TICK -> "Tick"
                CLICK -> "Click"
                HEAVY_CLICK -> "HeavyClick"
                DOUBLE_CLICK -> "DoubleClick"
                else -> type.toString()
            }
        }

        /**
         * A standard tick effect.
         *
         * This effect is less strong than the [predefinedClick].
         */
        @JvmStatic public fun predefinedTick(): PredefinedEffectSignal = Tick

        /**
         * A standard click effect.
         *
         * Use this effect as a baseline, as it's the most common type of click effect.
         */
        @JvmStatic public fun predefinedClick(): PredefinedEffectSignal = Click

        /**
         * A heavy click effect.
         *
         * This effect is stronger than the [predefinedClick].
         */
        @JvmStatic public fun predefinedHeavyClick(): PredefinedEffectSignal = HeavyClick

        /** A double-click effect. */
        @JvmStatic public fun predefinedDoubleClick(): PredefinedEffectSignal = DoubleClick
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PredefinedEffectSignal) return false
        if (type != other.type) return false
        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return "PredefinedEffectSignal(type=${typeToString(type)})"
    }

    /** Returns the minimum SDK level required by the effect type. */
    internal fun minSdk(): Int = minSdk

    override fun toVibration(): VibrationWrapper? = HapticSignalConverter.toVibration(this)

    override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean = true
}

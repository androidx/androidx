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
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.device.HapticDeviceProfile
import androidx.core.haptics.impl.HapticSignalConverter
import java.util.Objects
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinDuration

/**
 * A composition of haptic elements that can be played as one single haptic effect.
 *
 * Composition signals may be defined as a composition of scalable primitive effects, which are
 * tailored to the device hardware. The composition signal is based on the
 * [android.os.VibrationEffect.Composition] platform API.
 *
 * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
 */
public class CompositionSignal(

    /** The composition signal atoms that describes the haptic elements to be played in sequence. */
    public val atoms: List<Atom>,
) : FiniteSignal() {
    init {
        require(atoms.isNotEmpty()) { "Haptic signals cannot be empty" }
    }

    public companion object {

        /**
         * Returns a [CompositionSignal] with given atoms.
         *
         * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
         * @param atoms The [CompositionSignal.Atom] instances that define the [CompositionSignal].
         */
        @JvmStatic
        public fun compositionOf(vararg atoms: Atom): CompositionSignal =
            CompositionSignal(atoms.toList())

        /**
         * Returns a [CompositionSignal.Atom] for a very short low frequency tick effect.
         *
         * This effect should produce a light crisp sensation intended to be used repetitively for
         * dynamic feedback.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun lowTick(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.LowTick.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for a very short light tick effect.
         *
         * This effect should produce a light crisp sensation stronger than the [lowTick], and is
         * also intended to be used repetitively for dynamic feedback.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun tick(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.Tick.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for a click effect.
         *
         * This effect should produce a sharp, crisp click sensation.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun click(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.Click.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for an effect with increasing strength.
         *
         * This effect simulates quick upward movement against gravity.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun quickRise(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.QuickRise.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for a longer effect with increasing strength.
         *
         * This effect simulates slow upward movement against gravity and is longer than the
         * [quickRise].
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun slowRise(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.SlowRise.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for an effect with decreasing strength.
         *
         * This effect simulates quick downwards movement against gravity.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun quickFall(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.QuickFall.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for a spin effect.
         *
         * This effect simulates spinning momentum.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun spin(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f
        ): PrimitiveAtom = PrimitiveAtom.Spin.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] for a thud effect.
         *
         * This effect simulates downwards movement with gravity, often followed by extra energy of
         * hitting and reverberation to augment physicality.
         *
         * @param amplitudeScale The amplitude scale for the new [PrimitiveAtom]
         */
        @JvmOverloads
        @JvmStatic
        public fun thud(
            @FloatRange(from = 0.0, to = 1.0) amplitudeScale: Float = 1f,
        ): PrimitiveAtom = PrimitiveAtom.Thud.withAmplitudeScale(amplitudeScale)

        /**
         * Returns a [CompositionSignal.Atom] to turn the vibrator off for the specified duration.
         *
         * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
         * @param duration The duration the vibrator should be turned off.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        public fun off(duration: java.time.Duration): OffAtom = OffAtom(duration.toKotlinDuration())

        /**
         * Returns a [CompositionSignal.Atom] to turn the vibrator off for the specified duration.
         *
         * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
         * @param durationMillis The duration the vibrator should be turned off, in milliseconds.
         */
        @JvmStatic
        public fun off(durationMillis: Long): OffAtom = OffAtom(durationMillis.milliseconds)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompositionSignal) return false
        if (atoms != other.atoms) return false
        return true
    }

    override fun hashCode(): Int {
        return atoms.hashCode()
    }

    override fun toString(): String {
        return "CompositionSignal(${atoms.joinToString()})"
    }

    /** Returns the minimum SDK level required by the atoms of this signal. */
    internal fun minSdk(): Int = atoms.maxOf { it.minSdk() }

    override fun toVibration(): VibrationWrapper? = HapticSignalConverter.toVibration(this)

    override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean =
        atoms.all { it.isSupportedBy(deviceProfile) }

    /**
     * A [CompositionSignal.Atom] is a building block for creating a [CompositionSignal].
     *
     * Composition signal atoms describe basic haptic elements to be played in sequence as a single
     * haptic effect. They can describe haptic effects tailored to the device hardware, like click
     * and tick effects, or then can represent pauses in the effect composition.
     *
     * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
     */
    public abstract class Atom internal constructor() {

        /** The minimum SDK level where this atom is available in the platform. */
        internal abstract fun minSdk(): Int

        /** Returns true if the device vibrator can play this atom as intended, false otherwise. */
        internal abstract fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean
    }

    /**
     * A [PrimitiveAtom] plays a haptic effect with the specified vibration strength scale.
     *
     * Composition primitives are haptic effects tailored to the device hardware with configurable
     * vibration strength. They can be used as building blocks to create more complex haptic
     * effects. The primitive atoms are based on the
     * [android.os.VibrationEffect.Composition.addPrimitive] platform API.
     *
     * A primitive effect will always be played with a non-zero vibration amplitude, but the actual
     * vibration strength can be scaled by values in the range [0f..1f]. Zero [amplitudeScale]
     * implies the vibrator will play it at the minimum strength required for the effect to be
     * perceived on the device. The maximum [amplitudeScale] value of 1 implies the vibrator will
     * play it at the maximum strength that preserves the effect's intended design. For instance, a
     * [Click] effect with [amplitudeScale] of 1 will usually feel stronger than a [Tick] with same
     * amplitude scale.
     *
     * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
     */
    public class PrimitiveAtom
    private constructor(

        /** The type of haptic effect to be played. */
        @Type public val type: Int,

        /** The minimum SDK level where this effect type is available in the platform. */
        private val minSdk: Int,

        /**
         * The scale for the vibration strength.
         *
         * A primitive effect will always be played with a non-zero vibration strength. Zero values
         * here represent minimum effect strength that can still be perceived on the device, and
         * maximum values represent the maximum strength the effect can be played.
         */
        @FloatRange(from = 0.0, to = 1.0) public val amplitudeScale: Float = 1f,
    ) : Atom() {
        init {
            require(amplitudeScale in 0.0..1.0) {
                "Primitive amplitude scale must be in [0,1]: $amplitudeScale"
            }
        }

        /** Typedef for the [type] attribute. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            LOW_TICK,
            TICK,
            CLICK,
            SLOW_RISE,
            QUICK_RISE,
            QUICK_FALL,
            SPIN,
            THUD,
        )
        public annotation class Type

        public companion object {

            @JvmStatic
            internal fun typeToString(@Type type: Int): String {
                return when (type) {
                    LOW_TICK -> "LowTick"
                    TICK -> "Tick"
                    CLICK -> "Click"
                    SLOW_RISE -> "SlowRise"
                    QUICK_RISE -> "QuickRise"
                    QUICK_FALL -> "QuickFall"
                    SPIN -> "Spin"
                    THUD -> "Thud"
                    else -> type.toString()
                }
            }

            /**
             * A very short low frequency tick effect.
             *
             * This effect should produce a light crisp sensation intended to be used repetitively
             * for dynamic feedback.
             */
            public const val LOW_TICK: Int = 8 // VibrationEffect.Composition.PRIMITIVE_LOW_TICK

            /**
             * A very short light tick effect.
             *
             * This effect should produce a light crisp sensation stronger than the [LowTick], and
             * is also intended to be used repetitively for dynamic feedback.
             */
            public const val TICK: Int = 7 // VibrationEffect.Composition.PRIMITIVE_TICK

            /**
             * A click effect.
             *
             * This effect should produce a sharp, crisp click sensation.
             */
            public const val CLICK: Int = 1 // VibrationEffect.Composition.PRIMITIVE_CLICK

            /**
             * An effect with increasing strength.
             *
             * This effect simulates quick upward movement against gravity.
             */
            public const val QUICK_RISE: Int = 4 // VibrationEffect.Composition.PRIMITIVE_QUICK_RISE

            /**
             * A longer effect with increasing strength.
             *
             * This effect simulates slow upward movement against gravity and is longer than the
             * [QuickRise].
             */
            public const val SLOW_RISE: Int = 5 // VibrationEffect.Composition.PRIMITIVE_SLOW_RISE

            /**
             * An effect with decreasing strength.
             *
             * This effect simulates quick downwards movement against gravity.
             */
            public const val QUICK_FALL: Int = 6 // VibrationEffect.Composition.PRIMITIVE_QUICK_FALL

            /**
             * A spin effect.
             *
             * This effect simulates spinning momentum.
             */
            public const val SPIN: Int = 3 // VibrationEffect.Composition.PRIMITIVE_SPIN

            /**
             * A thud effect.
             *
             * This effect simulates downwards movement with gravity, often followed by extra energy
             * of hitting and reverberation to augment physicality.
             */
            public const val THUD: Int = 2 // VibrationEffect.Composition.PRIMITIVE_THUD

            internal val LowTick = PrimitiveAtom(LOW_TICK, Build.VERSION_CODES.S)
            internal val Tick = PrimitiveAtom(TICK, Build.VERSION_CODES.R)
            internal val Click = PrimitiveAtom(CLICK, Build.VERSION_CODES.R)
            internal val QuickRise = PrimitiveAtom(QUICK_RISE, Build.VERSION_CODES.R)
            internal val SlowRise = PrimitiveAtom(SLOW_RISE, Build.VERSION_CODES.R)
            internal val QuickFall = PrimitiveAtom(QUICK_FALL, Build.VERSION_CODES.R)
            internal val Spin = PrimitiveAtom(SPIN, Build.VERSION_CODES.S)
            internal val Thud = PrimitiveAtom(THUD, Build.VERSION_CODES.S)

            internal val ALL_PRIMITIVES =
                listOf(LowTick, Tick, Click, SlowRise, QuickRise, QuickFall, Spin, Thud)

            @JvmStatic
            internal fun getSdkAvailablePrimitiveTypes(): List<Int> =
                ALL_PRIMITIVES.filter { it.minSdk <= Build.VERSION.SDK_INT }.map { it.type }
        }

        /**
         * Returns a [PrimitiveAtom] with same effect type and new [amplitudeScale].
         *
         * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
         * @param newAmplitudeScale The amplitude scale for the new [PrimitiveAtom]
         * @return A new [PrimitiveAtom] with the same effect type and the new amplitude scale.
         */
        public fun withAmplitudeScale(
            @FloatRange(from = 0.0, to = 1.0) newAmplitudeScale: Float,
        ): PrimitiveAtom =
            if (amplitudeScale == newAmplitudeScale) {
                this
            } else {
                PrimitiveAtom(type, minSdk, newAmplitudeScale)
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrimitiveAtom) return false
            if (type != other.type) return false
            if (minSdk != other.minSdk) return false
            if (amplitudeScale != other.amplitudeScale) return false
            return true
        }

        override fun hashCode(): Int = Objects.hash(type, amplitudeScale)

        override fun toString(): String {
            return "PrimitiveAtom(type=${typeToString(type)}, amplitude=$amplitudeScale)"
        }

        override fun minSdk(): Int = minSdk

        override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean =
            deviceProfile.compositionProfile.supportedPrimitiveTypes.contains(type)
    }

    /**
     * A [OffAtom] turns off the vibrator for the specified duration.
     *
     * @sample androidx.core.haptics.samples.CompositionSignalOfScaledEffectsAndOff
     */
    public class OffAtom internal constructor(duration: Duration) : Atom() {
        /** The duration for the vibrator to be turned off, in milliseconds. */
        public val durationMillis: Long

        init {
            require(duration.isFinite() && !duration.isNegative()) {
                "Composition signal off atom duration must be finite and non-negative: $duration"
            }
            durationMillis = duration.inWholeMilliseconds
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is OffAtom) return false
            if (durationMillis != other.durationMillis) return false
            return true
        }

        override fun hashCode(): Int {
            return durationMillis.hashCode()
        }

        override fun toString(): String {
            return "OffAtom(durationMillis=$durationMillis)"
        }

        override fun minSdk(): Int = Build.VERSION_CODES.R

        override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean = true
    }
}

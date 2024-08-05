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
import androidx.annotation.RequiresApi
import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.device.HapticDeviceProfile
import androidx.core.haptics.impl.HapticSignalConverter
import androidx.core.haptics.signal.WaveformSignal.ConstantVibrationAtom.Companion.DEFAULT_AMPLITUDE
import java.util.Objects
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinDuration

/**
 * A haptic signal where the vibration parameters change over time.
 *
 * Waveform signals may be used to describe step waveforms, defined by a sequence of constant
 * vibrations played at different strengths. They can also be combined to define a
 * [RepeatingWaveformSignal], which is an [InfiniteSignal] that repeats a waveform until the
 * vibration is canceled.
 *
 * @sample androidx.core.haptics.samples.AmplitudeWaveform
 * @sample androidx.core.haptics.samples.PatternThenRepeatAmplitudeWaveform
 */
public class WaveformSignal(

    /** The waveform signal atoms that describes the vibration parameters over time. */
    public val atoms: List<Atom>,
) : FiniteSignal() {
    init {
        require(atoms.isNotEmpty()) { "Haptic signals cannot be empty" }
    }

    public companion object {

        /**
         * Returns a [WaveformSignal] created with given waveform atoms.
         *
         * Use [on] and [off] to create atoms.
         *
         * @sample androidx.core.haptics.samples.AmplitudeWaveform
         * @param atoms The [WaveformSignal.Atom] instances that define the [WaveformSignal].
         */
        @JvmStatic
        public fun waveformOf(vararg atoms: Atom): WaveformSignal = WaveformSignal(atoms.toList())

        /**
         * Returns a [RepeatingWaveformSignal] created with given waveform atoms.
         *
         * Repeating waveforms should include any desired loop delay as an [off] atom at the end of
         * the atom list.
         *
         * @sample androidx.core.haptics.samples.RepeatingAmplitudeWaveform
         * @param atoms The [WaveformSignal.Atom] instances that define the
         *   [RepeatingWaveformSignal].
         */
        @JvmStatic
        public fun repeatingWaveformOf(vararg atoms: Atom): RepeatingWaveformSignal =
            waveformOf(*atoms).repeat()

        /**
         * Returns a [WaveformSignal.Atom] that turns off the vibrator for the specified duration.
         *
         * @sample androidx.core.haptics.samples.PatternWaveform
         * @param duration The duration the vibrator should be turned off.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        public fun off(duration: java.time.Duration): ConstantVibrationAtom =
            ConstantVibrationAtom(duration.toKotlinDuration(), amplitude = 0f)

        /**
         * Returns a [WaveformSignal.Atom] that turns off the vibrator for the specified duration.
         *
         * @sample androidx.core.haptics.samples.PatternWaveform
         * @param durationMillis The duration the vibrator should be turned off, in milliseconds.
         */
        @JvmStatic
        public fun off(durationMillis: Long): ConstantVibrationAtom =
            ConstantVibrationAtom(durationMillis.milliseconds, amplitude = 0f)

        /**
         * Returns a [WaveformSignal.Atom] that turns on the vibrator for the specified duration at
         * a device-specific default amplitude.
         *
         * @sample androidx.core.haptics.samples.PatternWaveform
         * @param duration The duration for the vibration.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        public fun on(duration: java.time.Duration): ConstantVibrationAtom =
            ConstantVibrationAtom(duration.toKotlinDuration(), DEFAULT_AMPLITUDE)

        /**
         * Returns a [WaveformSignal.Atom] that turns on the vibrator for the specified duration at
         * a device-specific default amplitude.
         *
         * @sample androidx.core.haptics.samples.PatternWaveform
         * @param durationMillis The duration for the vibration, in milliseconds.
         */
        @JvmStatic
        public fun on(durationMillis: Long): ConstantVibrationAtom =
            ConstantVibrationAtom(durationMillis.milliseconds, DEFAULT_AMPLITUDE)

        /**
         * Returns a [WaveformSignal.Atom] that turns on the vibrator for the specified duration at
         * the specified amplitude.
         *
         * @sample androidx.core.haptics.samples.AmplitudeWaveform
         * @param duration The duration for the vibration.
         * @param amplitude The vibration strength, with 1 representing maximum amplitude, and 0
         *   representing off - equivalent to calling [off].
         */
        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        public fun on(
            duration: java.time.Duration,
            @FloatRange(from = 0.0, to = 1.0) amplitude: Float
        ): ConstantVibrationAtom = ConstantVibrationAtom(duration.toKotlinDuration(), amplitude)

        /**
         * Returns a [WaveformSignal.Atom] that turns on the vibrator for the specified duration at
         * the specified amplitude.
         *
         * @sample androidx.core.haptics.samples.AmplitudeWaveform
         * @param durationMillis The duration for the vibration, in milliseconds.
         * @param amplitude The vibration strength, with 1 representing maximum amplitude, and 0
         *   representing off - equivalent to calling [off].
         */
        @JvmStatic
        public fun on(
            durationMillis: Long,
            @FloatRange(from = 0.0, to = 1.0) amplitude: Float
        ): ConstantVibrationAtom = ConstantVibrationAtom(durationMillis.milliseconds, amplitude)
    }

    /**
     * Returns a [RepeatingWaveformSignal] to play this waveform on repeat until it's canceled.
     *
     * @sample androidx.core.haptics.samples.PatternWaveformRepeat
     */
    public fun repeat(): RepeatingWaveformSignal =
        RepeatingWaveformSignal(initialWaveform = null, repeatingWaveform = this)

    /**
     * Returns a [RepeatingWaveformSignal] that starts with this waveform signal then plays the
     * given waveform signal on repeat until the vibration is canceled.
     *
     * @sample androidx.core.haptics.samples.PatternThenRepeatExistingWaveform
     * @param waveformToRepeat The waveform to be played on repeat after this waveform.
     */
    public fun thenRepeat(waveformToRepeat: WaveformSignal): RepeatingWaveformSignal =
        RepeatingWaveformSignal(initialWaveform = this, repeatingWaveform = waveformToRepeat)

    /**
     * Returns a [RepeatingWaveformSignal] that starts with this waveform signal then plays the
     * given waveform atoms on repeat until the vibration is canceled.
     *
     * @sample androidx.core.haptics.samples.PatternThenRepeatAmplitudeWaveform
     * @param atoms The [WaveformSignal.Atom] instances that define the repeating [WaveformSignal]
     *   to be played after this waveform.
     */
    public fun thenRepeat(vararg atoms: Atom): RepeatingWaveformSignal =
        thenRepeat(waveformOf(*atoms))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaveformSignal) return false
        if (atoms != other.atoms) return false
        return true
    }

    override fun hashCode(): Int {
        return atoms.hashCode()
    }

    override fun toString(): String {
        return "WaveformSignal(${atoms.joinToString()})"
    }

    override fun toVibration(): VibrationWrapper? =
        HapticSignalConverter.toVibration(initialWaveform = this, repeatingWaveform = null)

    override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean =
        atoms.all { it.isSupportedBy(deviceProfile) }

    /**
     * A [WaveformSignal.Atom] is a building block for creating a [WaveformSignal].
     *
     * Waveform signal atoms describe how vibration parameters change over time. They can describe a
     * constant vibration sustained for a fixed duration, for example, which can be used to create a
     * step waveform. They can also be used to describe simpler on-off vibration patterns.
     *
     * @sample androidx.core.haptics.samples.PatternWaveform
     * @sample androidx.core.haptics.samples.AmplitudeWaveform
     */
    public abstract class Atom internal constructor() {

        /** Returns true if the device vibrator can play this atom as intended, false otherwise. */
        internal abstract fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean
    }

    /**
     * A [ConstantVibrationAtom] plays a constant vibration for the specified period of time.
     *
     * Constant vibrations can be played in sequence to create custom waveform signals.
     *
     * The amplitude determines the strength of the vibration, defined as a value in the range
     * [0f..1f]. Zero amplitude implies the vibrator motor should be off. The amplitude can also be
     * defined by [DEFAULT_AMPLITUDE], which will vibrate constantly at a hardware-specific default
     * vibration strength.
     *
     * @sample androidx.core.haptics.samples.PatternWaveform
     * @sample androidx.core.haptics.samples.AmplitudeWaveform
     */
    public class ConstantVibrationAtom
    internal constructor(
        duration: Duration,

        /**
         * The vibration strength.
         *
         * Zero amplitude turns the vibrator off for the specified duration, and [DEFAULT_AMPLITUDE]
         * uses a hardware-specific default vibration strength.
         */
        public val amplitude: Float,
    ) : Atom() {
        /** The duration to sustain the constant vibration, in milliseconds. */
        public val durationMillis: Long

        init {
            require(duration.isFinite() && !duration.isNegative()) {
                "Constant vibration duration must be finite and non-negative: $duration"
            }
            require(amplitude in (0.0..1.0) || amplitude == DEFAULT_AMPLITUDE) {
                "Constant vibration amplitude must be in [0,1]: $amplitude"
            }
            durationMillis = duration.inWholeMilliseconds
        }

        public companion object {
            /**
             * The [amplitude] value that represents a hardware-specific default vibration strength.
             */
            public const val DEFAULT_AMPLITUDE: Float = -1f
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ConstantVibrationAtom) return false
            if (durationMillis != other.durationMillis) return false
            if (amplitude != other.amplitude) return false
            return true
        }

        override fun hashCode(): Int {
            return Objects.hash(durationMillis, amplitude)
        }

        override fun toString(): String {
            return "ConstantVibrationAtom(durationMillis=$durationMillis" +
                ", amplitude=${if (amplitude == DEFAULT_AMPLITUDE) "default" else amplitude})"
        }

        override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean =
            deviceProfile.isAmplitudeControlSupported || hasPatternAmplitude()

        /** Returns true if amplitude is 0, 1 or [DEFAULT_AMPLITUDE]. */
        internal fun hasPatternAmplitude(): Boolean =
            (amplitude == 0f) || (amplitude == 1f) || (amplitude == DEFAULT_AMPLITUDE)
    }
}

/**
 * A [RepeatingWaveformSignal] describes an infinite haptic signal where a waveform signal is played
 * on repeat until canceled.
 *
 * A repeating waveform signal has an optional initial [WaveformSignal] that plays once before the
 * repeating waveform signal is played on repeat until the vibration is canceled.
 *
 * @sample androidx.core.haptics.samples.RepeatingAmplitudeWaveform
 */
public class RepeatingWaveformSignal
internal constructor(

    /** The optional initial waveform signal to be played once at the beginning of the vibration. */
    public val initialWaveform: WaveformSignal?,

    /** The waveform signal to be repeated after the initial waveform. */
    public val repeatingWaveform: WaveformSignal,
) : InfiniteSignal() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepeatingWaveformSignal) return false
        if (initialWaveform != other.initialWaveform) return false
        if (repeatingWaveform != other.repeatingWaveform) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(initialWaveform, repeatingWaveform)
    }

    override fun toString(): String {
        return "RepeatingWaveformSignal(initial=$initialWaveform, repeating=$repeatingWaveform)"
    }

    override fun toVibration(): VibrationWrapper? =
        HapticSignalConverter.toVibration(
            initialWaveform = initialWaveform,
            repeatingWaveform = repeatingWaveform,
        )

    override fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean =
        initialWaveform?.isSupportedBy(deviceProfile) != false &&
            repeatingWaveform.isSupportedBy(deviceProfile)
}

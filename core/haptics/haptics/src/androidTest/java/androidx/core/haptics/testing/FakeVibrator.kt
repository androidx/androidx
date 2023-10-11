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

package androidx.core.haptics.testing

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi
import androidx.core.haptics.AttributesWrapper
import androidx.core.haptics.PatternVibrationWrapper
import androidx.core.haptics.VibrationEffectWrapper
import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.VibratorWrapper
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import androidx.core.haptics.signal.PredefinedEffectSignal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fake [VibratorWrapper] implementation for testing.
 */
internal sealed class FakeVibrator(
    private val amplitudeControlSupported: Boolean,
    private val effectsSupported: IntArray? = null,
    private val primitivesSupported: IntArray = intArrayOf(),
    private val primitivesDurations: Map<Int, Duration>? = null,
) : VibratorWrapper {
    private val requests: MutableList<VibratorRequest> = mutableListOf()
    private val vibrations: MutableList<AttributedVibration> = mutableListOf()

    override fun hasVibrator(): Boolean = true

    override fun hasAmplitudeControl(): Boolean = amplitudeControlSupported

    override fun areEffectsSupported(
        effects: IntArray,
    ): Array<VibratorWrapper.EffectSupport> =
        effects.map {
            when {
                effectsSupported == null -> VibratorWrapper.EffectSupport.UNKNOWN
                effectsSupported.contains(it) -> VibratorWrapper.EffectSupport.YES
                else -> VibratorWrapper.EffectSupport.NO
            }
        }.toTypedArray()

    override fun arePrimitivesSupported(primitives: IntArray): BooleanArray =
        primitives.map { primitivesSupported.contains(it) }.toBooleanArray()

    override fun getPrimitivesDurations(primitives: IntArray): IntArray? =
        primitivesDurations?.let {
            primitives.map {
                primitivesDurations[it]?.inWholeMilliseconds?.toInt() ?: 0
            }.toIntArray()
        }

    override fun vibrate(vibration: VibrationWrapper, attrs: AttributesWrapper?) {
        requests.add(PlayVibration(vibration))
        vibrations.add(AttributedVibration(vibration, attrs))
    }

    override fun cancel() {
        requests.add(CancelVibration)
    }

    /** Returns all requests sent to the [android.os.Vibrator], in order. */
    internal fun requests(): List<VibratorRequest> = requests

    /** Returns all vibrate requests sent to the [android.os.Vibrator], in order. */
    internal fun vibrations(): List<AttributedVibration> = vibrations
}

/**
 * Represents different vibrator requests to vibrate or cancel.
 */
internal sealed interface VibratorRequest

/**
 * Represents a request to cancel any vibration.
 */
internal object CancelVibration : VibratorRequest

/**
 * Represents a request to play a given vibration.
 */
internal data class PlayVibration(
    val vibration: VibrationWrapper,
) : VibratorRequest

/**
 * Represents a request to vibrate with different API levels of support.
 */
internal data class AttributedVibration(
    val vibration: VibrationWrapper,
    val attrs: AttributesWrapper?,
)

/**
 * Vibrator that has no vibrator motor available on device.
 */
internal class NoVibrator : FakeVibrator(
    amplitudeControlSupported = false,
) {
    override fun hasVibrator(): Boolean = false
}

/**
 * Vibrator that only supports on-off patterns.
 */
internal class PatternVibrator : FakeVibrator(
    amplitudeControlSupported = false,
)

/**
 * Vibrator that only supports amplitude control.
 */
internal class AmplitudeVibrator : FakeVibrator(
    amplitudeControlSupported = true,
)

/**
 * Vibrator that supports amplitude control and all predefined effects.
 */
internal class PredefinedEffectsAndAmplitudeVibrator : FakeVibrator(
    amplitudeControlSupported = true,
    effectsSupported = PredefinedEffectSignal.ALL_EFFECTS.map { it.type }.toIntArray()
)

/**
 * Vibrator that supports amplitude control, all predefined effects and given primitives.
 */
internal class PartialVibrator(
    primitivesSupported: IntArray,
    primitivesDurations: Map<Int, Duration>? = null,
) : FakeVibrator(
    amplitudeControlSupported = true,
    effectsSupported = PredefinedEffectSignal.ALL_EFFECTS.map { it.type }.toIntArray(),
    primitivesSupported,
    primitivesDurations,
)

/**
 * Vibrator that supports amplitude control and all predefined and primitive effects.
 */
internal class FullVibrator(
    fakePrimitiveDuration: Duration? = 20.milliseconds,
) : FakeVibrator(
    amplitudeControlSupported = true,
    effectsSupported = PredefinedEffectSignal.ALL_EFFECTS.map { it.type }.toIntArray(),
    primitivesSupported = PrimitiveAtom.ALL_PRIMITIVES.map { it.type }.toIntArray(),
    primitivesDurations = fakePrimitiveDuration?.let {
            PrimitiveAtom.ALL_PRIMITIVES.map { it.type }.associateWith { fakePrimitiveDuration }
        },
)

/** Helper to create [android.os.VibrationEffect.Composition] entries. */
internal data class CompositionPrimitive(
    val primitiveId: Int,
    val scale: Float,
    val delayMs: Int,
) {
    constructor(
        primitive: PrimitiveAtom,
        scale: Float = primitive.amplitudeScale,
        delay: Duration = Duration.ZERO,
    ) : this(primitive.type, scale, delay.inWholeMilliseconds.toInt())
}

/** Helper to create [VibrationWrapper] request for a on-off pattern. */
internal fun vibration(
    pattern: LongArray,
    repeat: Int = -1,
): VibrationWrapper =
    PatternVibrationWrapper(pattern, repeat)

/** Helper to create [VibrationWrapper] request for a predefined effect. */
@RequiresApi(Build.VERSION_CODES.Q)
internal fun vibration(effect: PredefinedEffectSignal): VibrationWrapper =
    VibrationEffectWrapper(VibrationEffect.createPredefined(effect.type))

/** Helper to create [VibrationWrapper] request for a waveform effect. */
@RequiresApi(Build.VERSION_CODES.O)
internal fun vibration(
    timings: LongArray,
    amplitudes: IntArray,
    repeat: Int = -1,
): VibrationWrapper =
    VibrationEffectWrapper(VibrationEffect.createWaveform(timings, amplitudes, repeat))

/** Helper to create [VibrationWrapper] request for a primitive composition effect. */
@RequiresApi(Build.VERSION_CODES.R)
internal fun vibration(vararg primitives: CompositionPrimitive): VibrationWrapper {
    return VibrationEffectWrapper(
        VibrationEffect.startComposition().apply {
            primitives.forEach { addPrimitive(it.primitiveId, it.scale, it.delayMs) }
        }.compose()
    )
}

/** Helper to create a request to cancel any vibration. */
internal fun cancelRequest(): VibratorRequest = CancelVibration

/** Helper to create a request to play given vibration. */
internal fun playRequest(vibration: VibrationWrapper): VibratorRequest = PlayVibration(vibration)

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

import androidx.core.haptics.signal.CompositionSignal
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import java.util.Objects

/**
 * A [HapticCompositionProfile] describes the vibrator capabilities related to [CompositionSignal].
 *
 * Composition signals are defined by one or more haptic effect primitives that are custom tailored
 * to the device hardware. This profile provides hardware capabilities related to these primitives,
 * like support checks and estimated duration within a composition signal.
 *
 * @property supportedPrimitiveTypes A set of [Int] values defined by
 *   [CompositionSignal.PrimitiveAtom.Type] representing composition primitive types supported.
 */
public class HapticCompositionProfile
@JvmOverloads
constructor(

    /**
     * Hint for the composition primitives that are supported by the vibrator hardware.
     *
     * The values must be one of [CompositionSignal.PrimitiveAtom.Type]. The actual support set also
     * depends on the minimum SDK level required for each primitive.
     */
    supportedPrimitiveTypesHint: Set<Int> = emptySet(),

    /**
     * Hint for the estimated duration of each type of composition primitive, if available.
     *
     * The keys must be one of [CompositionSignal.PrimitiveAtom.Type], and the values are durations
     * in milliseconds. This can be null if the device does not report the estimated duration of
     * primitives.
     *
     * The actual entries also depend on the minimum SDK level required for each primitive key.
     */
    primitiveDurationMillisMapHint: Map<Int, Long>? = null,
) {
    /**
     * Whether the device reports the estimated duration for supported composition primitives.
     *
     * If primitive durations are reported then [getPrimitiveDurationMillis] can be used for
     * supported primitive types to check individual [PrimitiveAtom] durations within a
     * [CompositionSignal].
     */
    public val isPrimitiveDurationReported: Boolean
        get() = _primitiveDurationMillisMap != null

    /**
     * The composition primitive types that are supported by the vibrator hardware.
     *
     * Composition signals are only supported by the Android platform if the device hardware
     * supports all primitive atom types used in the composition.
     *
     * The values will be one of [CompositionSignal.PrimitiveAtom.Type]. The set will be empty if
     * the device reports no supported primitive, or if the required APIs are not available in this
     * SDK level.
     */
    public val supportedPrimitiveTypes: Set<Int>

    private val _primitiveDurationMillisMap: Map<Int, Long>?

    init {
        val availablePrimitives = PrimitiveAtom.getSdkAvailablePrimitiveTypes()
        supportedPrimitiveTypes =
            supportedPrimitiveTypesHint.filter { availablePrimitives.contains(it) }.toSet()

        _primitiveDurationMillisMap =
            primitiveDurationMillisMapHint
                ?.filter { supportedPrimitiveTypes.contains(it.key) }
                ?.toMap()

        require(_primitiveDurationMillisMap?.let { supportedPrimitiveTypes == it.keys } ?: true) {
            "Composition primitive durations should be reported for all supported primitives." +
                " Device supports primitive types $supportedPrimitiveTypes but got" +
                " estimated durations for ${_primitiveDurationMillisMap?.keys}."
        }
    }

    /**
     * The estimated duration a [PrimitiveAtom] of give type will play in a [CompositionSignal].
     *
     * This will be zero if the primitive type is not supported or if the devices does not report
     * primitive durations, which can be checked via [isPrimitiveDurationReported].
     *
     * @param primitiveType The type of primitive queried.
     * @return The estimated duration the device will take to play a [PrimitiveAtom] of given type
     *   in a [CompositionSignal], or null if the primitive type is not supported or the hardware
     *   does not report primitive durations.
     */
    public fun getPrimitiveDurationMillis(@PrimitiveAtom.Type primitiveType: Int): Long =
        _primitiveDurationMillisMap?.get(primitiveType) ?: 0L

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticCompositionProfile) return false
        if (supportedPrimitiveTypes != other.supportedPrimitiveTypes) return false
        if (_primitiveDurationMillisMap != other._primitiveDurationMillisMap) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            supportedPrimitiveTypes,
            _primitiveDurationMillisMap,
        )
    }

    override fun toString(): String {
        return "HapticCompositionProfile(" +
            ", isPrimitiveDurationReported=$isPrimitiveDurationReported" +
            ", supportedPrimitiveTypes=" +
            "${
                supportedPrimitiveTypes.joinToString(
                    prefix = "[",
                    postfix = "]",
                    transform = { PrimitiveAtom.typeToString(it) },
                )
            }" +
            ", primitiveDurations=" +
            "${
                _primitiveDurationMillisMap?.entries?.joinToString(
                    prefix = "{",
                    postfix = "}",
                    transform = { "${PrimitiveAtom.typeToString(it.key)} to ${it.value}ms" },
                )
            })"
    }
}

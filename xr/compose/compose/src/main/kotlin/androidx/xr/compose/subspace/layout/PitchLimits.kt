/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.xr.compose.subspace.layout

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs a [PitchLimits] from the given [minimumPitch] and [maximumPitch] angles in degrees.
 *
 * @param minimumPitch The minimum allowed pitch angle in degrees. Must be in the range
 *   [-90.0, 90.0].
 * @param maximumPitch The maximum allowed pitch angle in degrees. Must be in the range
 *   [-90.0, 90.0].
 */
@ExperimentalRotateToLookAtUserApi
public fun PitchLimits(
    @FloatRange(from = -90.0, to = 90.0) minimumPitch: Float,
    @FloatRange(from = -90.0, to = 90.0) maximumPitch: Float,
): PitchLimits {
    require(minimumPitch in -90f..90f) {
        "minimumPitch ($minimumPitch) must be in the range [-90.0, 90.0]"
    }
    require(maximumPitch in -90f..90f) {
        "maximumPitch ($maximumPitch) must be in the range [-90.0, 90.0]"
    }
    require(minimumPitch <= maximumPitch) {
        "minimumPitch ($minimumPitch) cannot be greater than maximumPitch ($maximumPitch)"
    }
    return PitchLimits(packFloats(minimumPitch, maximumPitch))
}

/**
 * Represents the limits for pitch angles in degrees.
 *
 * To construct a [PitchLimits], call the factory function that accepts minimum and maximum pitch
 * angles:
 * ```
 * val limits = PitchLimits(minimumPitch = -15f, maximumPitch = 15f)
 * ```
 */
@Immutable
@JvmInline
@ExperimentalRotateToLookAtUserApi
public value class PitchLimits internal constructor(private val packedValue: Long) {
    /** The minimum allowed pitch angle in degrees. Must be in the range [-90.0, 90.0]. */
    @get:FloatRange(from = -90.0, to = 90.0)
    public val minimumPitch: Float
        get() = unpackFloat1(packedValue)

    /** The maximum allowed pitch angle in degrees. Must be in the range [-90.0, 90.0]. */
    @get:FloatRange(from = -90.0, to = 90.0)
    public val maximumPitch: Float
        get() = unpackFloat2(packedValue)

    public inline operator fun component1(): Float = minimumPitch

    public inline operator fun component2(): Float = maximumPitch

    /**
     * Returns a copy of this PitchLimits instance optionally overriding the minimumPitch or
     * maximumPitch parameter.
     */
    public fun copy(
        @FloatRange(from = -90.0, to = 90.0) minimumPitch: Float = this.minimumPitch,
        @FloatRange(from = -90.0, to = 90.0) maximumPitch: Float = this.maximumPitch,
    ): PitchLimits = PitchLimits(minimumPitch, maximumPitch)

    override fun toString(): String {
        return "PitchLimits(minimumPitch=$minimumPitch, maximumPitch=$maximumPitch)"
    }

    public companion object {
        /**
         * A [PitchLimits] that allows the full range of pitch angles from -90.0 to 90.0 degrees.
         */
        public val FullRange: PitchLimits = PitchLimits(-90f, 90f)
    }
}

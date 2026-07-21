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

package androidx.camera.common

import android.view.Surface

/**
 * Kotlin value class that represents fixed 0, 90, 180, 270 degree rotations with utility functions
 * for adding and subtracting discrete rotations from each other.
 *
 * A [DiscreteRotation] represents integer degrees in fixed 90 degree increments.
 */
@JvmInline
@Suppress("NOTHING_TO_INLINE", "ValueClassDefinition")
public value class DiscreteRotation @PublishedApi internal constructor(public val degrees: Int) {
    /** Add a [DiscreteRotation] from this, modding the result by 360. */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun plus(other: DiscreteRotation): DiscreteRotation =
        DiscreteRotation((this.degrees + other.degrees) % 360)

    /** Add [degrees] to this, modding the result by 360. */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun plus(degrees: Int): DiscreteRotation = this.plus(from(degrees))

    /** Subtract a [DiscreteRotation] from this, modding the result by 360. */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun minus(other: DiscreteRotation): DiscreteRotation =
        DiscreteRotation((this.degrees - other.degrees + 360) % 360)

    /** Subtract [degrees] from this, modding the result by 360. */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun minus(degrees: Int): DiscreteRotation = this.minus(from(degrees))

    override fun toString(): String = "$degrees°"

    public companion object {

        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_0: DiscreteRotation = DiscreteRotation(0)

        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_90: DiscreteRotation = DiscreteRotation(90)

        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_180: DiscreteRotation = DiscreteRotation(180)

        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_270: DiscreteRotation = DiscreteRotation(270)

        /** Convert integer [degrees] to a [DiscreteRotation]. */
        @JvmStatic
        @JvmName("from")
        public fun from(degrees: Int): DiscreteRotation {
            require(degrees == 0 || degrees == 90 || degrees == 180 || degrees == 270) {
                "Unexpected rotation: $degrees. Value must be one of 0, 90, 180, 270"
            }
            return DiscreteRotation(degrees)
        }

        /**
         * Round [degrees] to the nearest [DiscreteRotation] (0, 90, 180, 270).
         *
         * Negative values are rounded to the nearest positive [DiscreteRotation] (e.g. -90 rounds
         * to 270). Boundary values (e.g. 45) are rounded up to the nearest 90-degree increment
         * (e.g. 45 rounds to 90).
         */
        @JvmStatic
        @JvmName("round")
        public fun round(degrees: Int): DiscreteRotation =
            DiscreteRotation(((degrees % 360 + (360 + 45)) / 90) * 90 % 360)

        /**
         * Round [degrees] to the nearest [DiscreteRotation] (0, 90, 180, 270).
         *
         * Negative values are rounded to the nearest positive [DiscreteRotation] (e.g. -90 rounds
         * to 270). Boundary values (e.g. 45.0) are rounded up to the nearest 90-degree increment
         * (e.g. 45.0 rounds to 90).
         */
        @JvmStatic
        @JvmName("round")
        public fun round(degrees: Float): DiscreteRotation =
            DiscreteRotation((Math.round(degrees % 360 / 90) * 90 + 360) % 360)

        /**
         * Get a [DiscreteRotation] from [Surface] rotation values.
         *
         * Rotation values are relative to the device's "natural" rotation, [Surface.ROTATION_0].
         */
        @JvmStatic
        @JvmName("fromSurfaceRotation")
        public fun fromSurfaceRotation(@SurfaceRotation surfaceRotation: Int): DiscreteRotation =
            DiscreteRotation(
                when (surfaceRotation) {
                    Surface.ROTATION_0 -> 0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else ->
                        throw IllegalArgumentException(
                            "Unexpected Surface rotation $surfaceRotation!"
                        )
                }
            )
    }
}

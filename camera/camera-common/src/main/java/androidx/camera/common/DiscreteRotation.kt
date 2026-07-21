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
 * A value class representing a rotation constrained to 90-degree increments (0, 90, 180, or 270
 * degrees).
 *
 * This class provides utility functions for performing modulo-360 arithmetic on rotations (addition
 * and subtraction), as well as rounding arbitrary rotation values to the nearest 90-degree step.
 *
 * @property degrees The rotation value in degrees. Guaranteed to be one of 0, 90, 180, or 270.
 */
@JvmInline
@Suppress("NOTHING_TO_INLINE", "ValueClassDefinition")
public value class DiscreteRotation @PublishedApi internal constructor(public val degrees: Int) {
    /**
     * Adds another [DiscreteRotation] to this rotation.
     *
     * The result is normalized to the range `[0, 360)` degrees.
     *
     * @param other The [DiscreteRotation] to add.
     * @return The resulting [DiscreteRotation].
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun plus(other: DiscreteRotation): DiscreteRotation =
        DiscreteRotation((this.degrees + other.degrees) % 360)

    /**
     * Adds the specified number of [degrees] to this rotation.
     *
     * The result is normalized to the range `[0, 360)` degrees.
     *
     * @param degrees The degrees to add. Must be a valid discrete rotation (0, 90, 180, or 270).
     * @return The resulting [DiscreteRotation].
     * @throws IllegalArgumentException If [degrees] is not a valid discrete rotation (0, 90, 180,
     *   or 270).
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun plus(degrees: Int): DiscreteRotation = this.plus(from(degrees))

    /**
     * Subtracts another [DiscreteRotation] from this rotation.
     *
     * The result is normalized to the range `[0, 360)` degrees.
     *
     * @param other The [DiscreteRotation] to subtract.
     * @return The resulting [DiscreteRotation].
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun minus(other: DiscreteRotation): DiscreteRotation =
        DiscreteRotation((this.degrees - other.degrees + 360) % 360)

    /**
     * Subtracts the specified number of [degrees] from this rotation.
     *
     * The result is normalized to the range `[0, 360)` degrees.
     *
     * @param degrees The degrees to subtract. Must be a valid discrete rotation (0, 90, 180, or
     *   270).
     * @return The resulting [DiscreteRotation].
     * @throws IllegalArgumentException If [degrees] is not a valid discrete rotation (0, 90, 180,
     *   or 270).
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    public inline operator fun minus(degrees: Int): DiscreteRotation = this.minus(from(degrees))

    override fun toString(): String = "$degrees°"

    public companion object {

        /** A [DiscreteRotation] of 0 degrees. */
        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_0: DiscreteRotation = DiscreteRotation(0)

        /** A [DiscreteRotation] of 90 degrees. */
        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_90: DiscreteRotation = DiscreteRotation(90)

        /** A [DiscreteRotation] of 180 degrees. */
        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_180: DiscreteRotation = DiscreteRotation(180)

        /** A [DiscreteRotation] of 270 degrees. */
        @get:JvmSynthetic
        @Suppress("ValueClassUsageWithoutJvmName")
        public val ROTATION_270: DiscreteRotation = DiscreteRotation(270)

        /**
         * Creates a [DiscreteRotation] from the given integer [degrees].
         *
         * @param degrees The rotation in degrees. Must be one of 0, 90, 180, or 270.
         * @return A [DiscreteRotation] representing the specified degrees.
         * @throws IllegalArgumentException If [degrees] is not a valid discrete rotation (0, 90,
         *   180, or 270).
         */
        @JvmStatic
        @JvmName("from")
        public fun from(degrees: Int): DiscreteRotation {
            require(degrees == 0 || degrees == 90 || degrees == 180 || degrees == 270) {
                "Unexpected rotation: $degrees. Value must be one of 0, 90, 180, 270"
            }
            return DiscreteRotation(degrees)
        }

        /**
         * Rounds [degrees] to the nearest [DiscreteRotation] (0, 90, 180, or 270).
         *
         * Negative values are normalized and rounded to the nearest positive [DiscreteRotation]
         * (e.g., -90 rounds to 270, and -45 rounds to 0). Boundary values (e.g., 45) are rounded up
         * to the nearest 90-degree increment (e.g., 45 rounds to 90).
         *
         * @param degrees The rotation in degrees to round.
         * @return The rounded [DiscreteRotation].
         */
        @JvmStatic
        @JvmName("round")
        public fun round(degrees: Int): DiscreteRotation =
            DiscreteRotation(((degrees % 360 + (360 + 45)) / 90) * 90 % 360)

        /**
         * Rounds [degrees] to the nearest [DiscreteRotation] (0, 90, 180, or 270).
         *
         * Negative values are normalized and rounded to the nearest positive [DiscreteRotation]
         * (e.g., -90.0f rounds to 270, and -45.0f rounds to 0). Boundary values (e.g., 45.0f) are
         * rounded up to the nearest 90-degree increment (e.g., 45.0f rounds to 90).
         *
         * @param degrees The rotation in degrees to round.
         * @return The rounded [DiscreteRotation].
         */
        @JvmStatic
        @JvmName("round")
        public fun round(degrees: Float): DiscreteRotation =
            DiscreteRotation((Math.round(degrees % 360 / 90) * 90 + 360) % 360)

        /**
         * Creates a [DiscreteRotation] from a [Surface] rotation constant.
         *
         * Rotation values are relative to the device's natural orientation, [Surface.ROTATION_0].
         *
         * @param surfaceRotation A [Surface] rotation value. Must be one of the [SurfaceRotation]
         *   constants (e.g., [Surface.ROTATION_90]).
         * @return The corresponding [DiscreteRotation].
         * @throws IllegalArgumentException If the provided [surfaceRotation] is not valid.
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

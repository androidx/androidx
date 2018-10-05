/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.physics

private const val EPSILON_DEFAULT: Double = 0.001

/**
 * Structure that specifies maximum allowable magnitudes for distances,
 * durations, and velocity differences to be considered equal.
 */

/**
 * By default, the distance, time, and velocity
 * tolerances are all ±0.001; the constructor arguments override this.
 *
 * The arguments should all be positive values.
 */
class Tolerance(
    /**
     * The magnitude of the maximum distance between two points for them to be
     * considered within tolerance.
     *
     * The units for the distance tolerance must be the same as the units used
     * for the distances that are to be compared to this tolerance.
     */
    val distance: Double = EPSILON_DEFAULT,
    /**
     * The magnitude of the maximum duration between two times for them to be
     * considered within tolerance.
     *
     * The units for the time tolerance must be the same as the units used
     * for the times that are to be compared to this tolerance.
     */
    val time: Double = EPSILON_DEFAULT,
    /**
     * The magnitude of the maximum difference between two velocities for them to
     * be considered within tolerance.
     *
     * The units for the velocity tolerance must be the same as the units used
     * for the velocities that are to be compared to this tolerance.
     */
    val velocity: Double = EPSILON_DEFAULT

) {

    override fun toString() = "Tolerance(distance: ±$distance, time: ±$time, velocity: ±$velocity)"

    companion object {
        val DEFAULT_TOLERANCE: Tolerance = Tolerance()
    }
}

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

import androidx.ui.runtimeType

/**
 * The base class for all simulations.
 *
 * A simulation models an object, in a one-dimensional space, on which particular
 * forces are being applied, and exposes:
 *
 *  * The object's position, [x]
 *  * The object's velocity, [dx]
 *  * Whether the simulation is "done", [isDone]
 *
 * A simulation is generally "done" if the object has, to a given [tolerance],
 * come to a complete rest.
 *
 * The [x], [dx], and [isDone] functions take a time argument which specifies
 * the time for which they are to be evaluated. In principle, simulations can
 * be stateless, and thus can be queried with arbitrary times. In practice,
 * however, some simulations are not, and calling any of these functions will
 * advance the simulation to the given time.
 *
 * As a general rule, therefore, a simulation should only be queried using
 * times that are equal to or greater than all times previously used for that
 * simulation.
 *
 * Simulations do not specify units for distance, velocity, and time. Client
 * should establish a convention and use that convention consistently with all
 * related objects.
 */
abstract class Simulation(
    /**
     * How close to the actual end of the simulation a value at a particular time
     * must be before [isDone] considers the simulation to be "done".
     *
     * A simulation with an asymptotic curve would never technically be "done",
     * but once the difference from the value at a particular time and the
     * asymptote itself could not be seen, it would be pointless to continue. The
     * tolerance defines how to determine if the difference could not be seen.
     */
    val tolerance: Tolerance = Tolerance.DEFAULT_TOLERANCE
) {

    /** The position of the object in the simulation at the given time. */
    abstract fun x(timeInSeconds: Double): Double

    /** The velocity of the object in the simulation at the given time. */
    abstract fun dx(timeInSeconds: Double): Double

    /** Whether the simulation is "done" at the given time. */
    abstract fun isDone(timeInSeconds: Double): Boolean

    override fun toString() = runtimeType().toString()
}
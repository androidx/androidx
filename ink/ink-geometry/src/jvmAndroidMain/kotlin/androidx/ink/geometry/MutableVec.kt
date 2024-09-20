/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.UsedByNative
import kotlin.math.cos
import kotlin.math.sin

/**
 * A mutable two-dimensional vector, i.e. an (x, y) coordinate pair. It can be used to represent
 * either:
 * 1) A two-dimensional offset, i.e. the difference between two points
 * 2) A point in space, i.e. treating the vector as an offset from the origin
 *
 * This object is mutable and is not inherently thread-safe, so callers should apply their own
 * synchronization logic or use this object from a single thread. See [ImmutableVec] for an
 * immutable alternative.
 */
public class MutableVec(
    @set:UsedByNative override var x: Float,
    @set:UsedByNative override var y: Float,
) : Vec() {

    public constructor() : this(0F, 0F)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asImmutable(): ImmutableVec = ImmutableVec(x, y)

    /** Fills this [MutableVec] with the same values contained in [input]. */
    public fun populateFrom(input: Vec): MutableVec {
        x = input.x
        y = input.y
        return this
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is Vec && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Mutable${string(this)}"

    public companion object {
        @JvmStatic
        public fun fromDirectionAndMagnitude(
            @AngleRadiansFloat direction: Float,
            magnitude: Float,
        ): MutableVec {
            return MutableVec(magnitude * cos(direction), magnitude * sin(direction))
        }
    }
}

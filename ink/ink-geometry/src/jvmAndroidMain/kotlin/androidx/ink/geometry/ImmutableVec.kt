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
import kotlin.jvm.JvmSynthetic
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * An immutable two-dimensional vector, i.e. an (x, y) coordinate pair. It can be used to represent
 * either:
 * 1) A two-dimensional offset, i.e. the difference between two points
 * 2) A point in space, i.e. treating the vector as an offset from the origin
 *
 * This object is immutable, so it is inherently thread-safe. See [MutableVec] for a mutable
 * alternative.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class ImmutableVec(override val x: Float, override val y: Float) : Vec {

    /** Fills [output] with the x and y coordinates of this [ImmutableVec] */
    public fun fillMutable(output: MutableVec) {
        output.x = this.x
        output.y = this.y
    }

    /** Returns a [MutableVec] containing the same x and y coordinates as this [ImmutableVec] */
    public fun newMutable(): MutableVec {
        return MutableVec(x, y)
    }

    override val magnitude: Float = hypot(x, y)

    override val magnitudeSquared: Float = x * x + y * y

    override val asImmutable: ImmutableVec = this

    @JvmSynthetic
    override fun asImmutable(x: Float, y: Float): ImmutableVec {
        return if (x == this.x && y == this.y) this else ImmutableVec(x, y)
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is Vec && Vec.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Vec.hash(this)

    override fun toString(): String = "Immutable${Vec.string(this)}"

    public companion object {
        @JvmStatic
        public fun fromDirectionAndMagnitude(
            @AngleRadiansFloat direction: Float,
            magnitude: Float,
        ): ImmutableVec {
            return ImmutableVec(magnitude * cos(direction), magnitude * sin(direction))
        }
    }
}

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

/** Represents a location in 2-dimensional space. See [MutablePoint] for a mutable alternative. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class ImmutablePoint(override val x: Float, override val y: Float) : Point {
    /** Fills [output] with the x and y coordinates of this [ImmutablePoint] */
    public fun fillMutable(output: MutablePoint) {
        output.x = this.x
        output.y = this.y
    }

    /** Returns a [MutablePoint] containing the same x and y coordinates as this [ImmutablePoint] */
    public fun newMutable(): MutablePoint {
        return MutablePoint(x, y)
    }

    /** Return a copy of this object with modified x and y as provided. */
    @JvmSynthetic
    public fun copy(x: Float = this.x, y: Float = this.y): ImmutablePoint =
        if (x == this.x && y == this.y) this else ImmutablePoint(x, y)

    override fun equals(other: Any?): Boolean =
        other === this || (other is Point && Point.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Point.hash(this)

    override fun toString(): String = "Immutable${Point.string(this)}"
}

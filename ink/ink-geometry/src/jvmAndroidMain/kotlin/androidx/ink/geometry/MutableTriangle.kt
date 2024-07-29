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

/**
 * Represents a mutable triangle, defined by its three corners [p0], [p1] and [p2] in order. See
 * [ImmutableTriangle] for the immutable version.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class MutableTriangle(p0: Vec, p1: Vec, p2: Vec) : Triangle {

    private var _p0 = MutableVec(p0.x, p0.y)
    private var _p1 = MutableVec(p1.x, p1.y)
    private var _p2 = MutableVec(p2.x, p2.y)

    override var p0: Vec = _p0
        private set

    override var p1: Vec = _p1
        private set

    override var p2: Vec = _p2
        private set

    /** Constructs a degenerate [MutableTriangle] with [p0], [p1], and [p2] set to (0, 0). */
    public constructor() : this(MutableVec(0f, 0f), MutableVec(0f, 0f), MutableVec(0f, 0f))

    /** Sets [p0] equal to [value]. */
    public fun p0(value: Vec): MutableTriangle {
        _p0.x = value.x
        _p0.y = value.y
        return this
    }

    /** Sets [p0] to the location ([x], [y]). */
    public fun p0(x: Float, y: Float): MutableTriangle {
        _p0.x = x
        _p0.y = y
        return this
    }

    /** Sets [p1] equal to [value]. */
    public fun p1(value: Vec): MutableTriangle {
        _p1.x = value.x
        _p1.y = value.y
        return this
    }

    /** Sets [p1] to the location ([x], [y]). */
    public fun p1(x: Float, y: Float): MutableTriangle {
        _p1.x = x
        _p1.y = y
        return this
    }

    /** Sets [p2] equal to [value]. */
    public fun p2(value: Vec): MutableTriangle {
        _p2.x = value.x
        _p2.y = value.y
        return this
    }

    /** Sets [p2] to the location ([x], [y]). */
    public fun p2(x: Float, y: Float): MutableTriangle {
        _p2.x = x
        _p2.y = y
        return this
    }

    /** Copies the points from [input] to [this] [MutableTriangle]. */
    public fun populateFrom(input: Triangle): MutableTriangle {
        _p0.x = input.p0.x
        _p0.y = input.p0.y
        _p1.x = input.p1.x
        _p1.y = input.p1.y
        _p2.x = input.p2.x
        _p2.y = input.p2.y
        return this
    }

    override fun asImmutable(): ImmutableTriangle = ImmutableTriangle(this.p0, this.p1, this.p2)

    @JvmSynthetic
    override fun asImmutable(p0: Vec, p1: Vec, p2: Vec): ImmutableTriangle {
        return ImmutableTriangle(p0, p1, p2)
    }

    /**
     * Equality for [MutableTriangle] is defined using the order in which [p0], [p1] and [p2] are
     * defined. Rotated/flipped triangles with out-of-order vertices are not considered equal.
     */
    override fun equals(other: Any?): Boolean =
        other === this || (other is Triangle && Triangle.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Triangle.hash(this)

    override fun toString(): String = "Mutable${Triangle.string(this)}"
}

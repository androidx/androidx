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
 *
 * @constructor Create the [MutableTriangle] from three existing [MutableVec] instances. Note that
 *   these instances will become the internal state of this [MutableTriangle], so modifications made
 *   to them directly or through setters on this [MutableTriangle] will modify the input
 *   [MutableVec] instances too. This is to allow performance-critical code to avoid any unnecessary
 *   allocations. This can be tricky to manage, especially in multithreaded code, so when calling
 *   code is unable to guarantee ownership of the nested mutable data at a particular time, it may
 *   be safest to construct this with copies of the data to give this [MutableTriangle] exclusive
 *   ownership of those copies.
 */
public class MutableTriangle(
    override var p0: MutableVec,
    override var p1: MutableVec,
    override var p2: MutableVec,
) : Triangle() {

    /** Constructs a degenerate [MutableTriangle] with [p0], [p1], and [p2] set to (0, 0). */
    public constructor() : this(MutableVec(0f, 0f), MutableVec(0f, 0f), MutableVec(0f, 0f))

    /**
     * Fills this [MutableTriangle] with the values from [input].
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFrom(input: Triangle): MutableTriangle {
        p0.x = input.p0.x
        p0.y = input.p0.y
        p1.x = input.p1.x
        p1.y = input.p1.y
        p2.x = input.p2.x
        p2.y = input.p2.y
        return this
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toImmutable(): ImmutableTriangle = ImmutableTriangle(this.p0, this.p1, this.p2)

    /**
     * Equality for [MutableTriangle] is defined using the order in which [p0], [p1] and [p2] are
     * defined. Rotated/flipped triangles with out-of-order vertices are not considered equal.
     */
    override fun equals(other: Any?): Boolean =
        other === this || (other is Triangle && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Mutable${string(this)}"
}

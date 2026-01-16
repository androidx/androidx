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
 * Represents a mutable directed line segment between two points. See [ImmutableSegment] for the
 * immutable alternative.
 *
 * @constructor Create the [MutableSegment] from two existing [MutableVec] instances. Note that
 *   these instances will become the internal state of this [MutableSegment], so modifications made
 *   to them directly or through setters on this [MutableSegment] will modify the input [MutableVec]
 *   instances too. This is to allow performance-critical code to avoid any unnecessary allocations.
 *   This can be tricky to manage, especially in multithreaded code, so when calling code is unable
 *   to guarantee ownership of the nested mutable data at a particular time, it may be safest to
 *   construct this with copies of the data to give this [MutableSegment] exclusive ownership of
 *   those copies.
 */
public class MutableSegment(override var start: MutableVec, override var end: MutableVec) :
    Segment() {

    /** Constructs a degenerate [MutableSegment] with both [start] and [end] set to (0f, 0f) */
    public constructor() : this(MutableVec(0f, 0f), MutableVec(0f, 0f))

    /**
     * Fills this [MutableSegment] with the same values contained in [input].
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFrom(input: Segment): MutableSegment {
        this.start.x = input.start.x
        this.start.y = input.start.y
        this.end.x = input.end.x
        this.end.y = input.end.y
        return this
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toImmutable(): ImmutableSegment = ImmutableSegment(this.start, this.end)

    override fun equals(other: Any?): Boolean =
        other === this || (other is Segment && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Mutable${string(this)}"
}

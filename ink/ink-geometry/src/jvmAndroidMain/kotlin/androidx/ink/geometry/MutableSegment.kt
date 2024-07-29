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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class MutableSegment(start: Vec, end: Vec) : Segment {

    private var _start = MutableVec(start.x, start.y)
    private var _end = MutableVec(end.x, end.y)

    override var start: Vec = _start
        private set

    override var end: Vec = _end
        private set

    /** Constructs a degenerate [MutableSegment] with both [start] and [end] set to (0f, 0f) */
    public constructor() : this(MutableVec(0f, 0f), MutableVec(0f, 0f))

    /** Sets this segment’s [start] point. */
    public fun start(point: Vec): MutableSegment {
        this._start.x = point.x
        this._start.y = point.y
        return this
    }

    /** Sets this segment's [start] point to ([x], [y]). */
    public fun start(x: Float, y: Float): MutableSegment {
        this._start.x = x
        this._start.y = y
        return this
    }

    /** Sets this segment’s [end] point. */
    public fun end(point: Vec): MutableSegment {
        this._end.x = point.x
        this._end.y = point.y
        return this
    }

    /** Sets this segment's [end] point to ([x], [y]). */
    public fun end(x: Float, y: Float): MutableSegment {
        this._end.x = x
        this._end.y = y
        return this
    }

    /** Fills this [MutableSegment] with the same values contained in [input]. */
    public fun populateFrom(input: Segment): MutableSegment {
        this._start.x = input.start.x
        this._start.y = input.start.y
        this._end.x = input.end.x
        this._end.y = input.end.y
        return this
    }

    override val vec: ImmutableVec
        get() = ImmutableVec(end.x - start.x, end.y - start.y)

    override fun asImmutable(): ImmutableSegment = ImmutableSegment(this.start, this.end)

    @JvmSynthetic
    override fun asImmutable(start: Vec, end: Vec): ImmutableSegment {
        return ImmutableSegment(start, end)
    }

    override val midpoint: ImmutableVec
        get() = ImmutableVec((start.x + end.x) / 2, (start.y + end.y) / 2)

    override fun equals(other: Any?): Boolean =
        other === this || (other is Segment && Segment.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Segment.hash(this)

    override fun toString(): String = "Mutable${Segment.string(this)}"
}

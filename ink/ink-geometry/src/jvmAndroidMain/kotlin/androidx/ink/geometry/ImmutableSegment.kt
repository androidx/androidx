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
 * Represents a directed line segment between two points. See [MutableSegment] for mutable
 * alternative.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class ImmutableSegment(start: Vec, end: Vec) : Segment {

    @Suppress("Immutable") override val start: Vec = start.asImmutable
    @Suppress("Immutable") override val end: Vec = end.asImmutable

    /**
     * Caches the result of [vec] if it is called. This format is used to avoid unnecessary
     * allocations on construction, and avoid extra allocations if [vec] is called multiple times.
     * Although the Immutable lint is being suppressed, this object is still immutable as its
     * visible data cannot be modified.
     */
    @Suppress("Immutable") private var _vec: ImmutableVec? = null

    override val vec: ImmutableVec
        get() = _vec ?: ImmutableVec(end.x - start.x, end.y - start.y).also { _vec = it }

    override fun asImmutable(): ImmutableSegment = this

    @JvmSynthetic
    override fun asImmutable(start: Vec, end: Vec): ImmutableSegment {
        if (this.start === start && this.end === end) {
            return this
        }

        return ImmutableSegment(start, end)
    }

    /**
     * Caches the result of [midpoint] if it is called. This format is used to avoid unnecessary
     * allocations on construction, and avoid extra allocations if [midpoint] is called multiple
     * times. Although the Immutable lint is being suppressed, this object is still immutable as its
     * visible data cannot be modified.
     */
    @Suppress("Immutable") private var _midpoint: ImmutableVec? = null

    override val midpoint: ImmutableVec
        get() =
            _midpoint
                ?: ImmutableVec((start.x + end.x) / 2, (start.y + end.y) / 2).also {
                    _midpoint = it
                }

    override fun equals(other: Any?): Boolean =
        other === this || (other is Segment && Segment.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Segment.hash(this)

    override fun toString(): String = "Immutable${Segment.string(this)}"
}

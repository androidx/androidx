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
public class ImmutableSegment(start: Vec, end: Vec) : Segment() {

    override val start: ImmutableVec = start.toImmutable()
    override val end: ImmutableVec = end.toImmutable()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toImmutable(): ImmutableSegment = this

    override fun equals(other: Any?): Boolean =
        other === this || (other is Segment && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Immutable${string(this)}"
}

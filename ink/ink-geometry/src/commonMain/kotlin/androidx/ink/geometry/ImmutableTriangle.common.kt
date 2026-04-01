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
 * An immutable triangle, defined by its three corners [p0], [p1] and [p2] in order. This object is
 * immutable, so it is inherently thread-safe. See [MutableTriangle] for the mutable version.
 */
public class ImmutableTriangle(p0: Vec, p1: Vec, p2: Vec) : Triangle() {

    override val p0: ImmutableVec = p0.toImmutable()
    override val p1: ImmutableVec = p1.toImmutable()
    override val p2: ImmutableVec = p2.toImmutable()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toImmutable(): ImmutableTriangle = this

    /**
     * Equality for [ImmutableTriangle] is defined using the order in which [p0], [p1] and [p2] are
     * defined. Rotated/flipped triangles with out-of-order vertices are not considered equal.
     */
    override fun equals(other: Any?): Boolean =
        other === this || (other is Triangle && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode.
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Immutable${string(this)}"
}

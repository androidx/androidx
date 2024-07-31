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
 * Represents a location in 2-dimensional space. See [ImmutablePoint] for an immutable alternative.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class MutablePoint(
    override var x:
        Float, // TODO: b/355248266 - @set:UsedByNative("point_jni_helper.cc") must go in Proguard
    // config file instead.
    override var y:
        Float, // TODO: b/355248266 - @set:UsedByNative("point_jni_helper.cc") must go in Proguard
    // config file instead.
) : Point {

    /**
     * Constructs a [MutablePoint] without any initial data. This is useful when pre-allocating an
     * instance to be filled later.
     */
    public constructor() : this(0f, 0f)

    /** Construct an [ImmutablePoint] out of this [MutablePoint]. */
    public fun build(): ImmutablePoint = ImmutablePoint(x, y)

    override fun equals(other: Any?): Boolean =
        other === this || (other is Point && Point.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Point.hash(this)

    override fun toString(): String = "Mutable${Point.string(this)}"
}

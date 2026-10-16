/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.semantics

/**
 * Mutable area made of integer rectangles, backed by the platform region class.
 *
 * [SemanticsOwner] uses it to find the parts of each semantics node that nodes drawn on top don't
 * cover. Coordinates are pixels in any space, as long as all operations on related regions use the
 * same one; [SemanticsOwner] uses root coordinates.
 * - A rectangle covers `left <= x < right` and `top <= y < bottom`, so a rectangle whose `right` is
 *   not greater than `left`, or whose `bottom` is not greater than `top`, is empty.
 * - All operations change this region in place and don't allocate.
 */
internal interface SemanticsRegion {
    /**
     * Replaces the area of this region with the rectangle from ([left], [top]) to ([right],
     * [bottom]).
     */
    fun set(left: Int, top: Int, right: Int, bottom: Int)

    /**
     * Reduces this region to the area it shares with [region]. [region] doesn't change.
     *
     * @param region the region to intersect with. Must come from [SemanticsRegion], like this one.
     * @return `true` if the resulting area isn't empty
     */
    fun intersect(region: SemanticsRegion): Boolean

    /**
     * Removes the rectangle from ([left], [top]) to ([right], [bottom]) from this region.
     *
     * @return `true` if the resulting area isn't empty
     */
    fun difference(left: Int, top: Int, right: Int, bottom: Int): Boolean

    /** `true` when this region covers no area. */
    val isEmpty: Boolean

    /**
     * Computes the smallest rectangle that contains this region and stores its edges in
     * [boundsLeft], [boundsTop], [boundsRight] and [boundsBottom].
     *
     * Those values don't follow later changes to this region: call this again after [set],
     * [intersect] or [difference]. An empty region stores `0` for all edges.
     */
    fun updateBounds()

    /** Left edge of the bounds stored by the last [updateBounds] call, or `0` before the first. */
    val boundsLeft: Int

    /** Top edge of the bounds stored by the last [updateBounds] call, or `0` before the first. */
    val boundsTop: Int

    /** Right edge of the bounds stored by the last [updateBounds] call, or `0` before the first. */
    val boundsRight: Int

    /**
     * Bottom edge of the bounds stored by the last [updateBounds] call, or `0` before the first.
     */
    val boundsBottom: Int
}

/** Creates an empty [SemanticsRegion] backed by the platform region class. */
internal expect fun SemanticsRegion(): SemanticsRegion

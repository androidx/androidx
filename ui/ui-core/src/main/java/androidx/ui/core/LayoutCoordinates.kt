/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition

/**
 * A holder of the measured bounds for the layout (MeasureBox).
 */
// TODO(Andrey): Add Matrix transformation here when we would have this logic.
interface LayoutCoordinates {
    /**
     * The size of this layout in the local coordinates space.
     */
    val size: IntPxSize

    /**
     * The alignment lines provided for this layout, not including inherited lines.
     */
    val providedAlignmentLines: Set<AlignmentLine>

    /**
     * The coordinates of the parent layout. Null if there is no parent.
     */
    val parentCoordinates: LayoutCoordinates?

    /**
     * Returns false if the corresponding layout was detached from the hierarchy.
     */
    val isAttached: Boolean

    /**
     * Converts a global position into a local position within this layout.
     */
    fun globalToLocal(global: PxPosition): PxPosition

    /**
     * Converts a local position within this layout into a global one.
     */
    fun localToGlobal(local: PxPosition): PxPosition

    /**
     * Converts a local position within this layout into an offset from the root composable.
     */
    fun localToRoot(local: PxPosition): PxPosition

    /**
     * Converts a child layout position into a local position within this layout.
     */
    fun childToLocal(child: LayoutCoordinates, childLocal: PxPosition): PxPosition

    /**
     * Returns the child bounding box, discarding clipped rectangles, in local coordinates.
     */
    fun childBoundingBox(child: LayoutCoordinates): PxBounds

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or `null` if the line is not provided.
     */
    operator fun get(line: AlignmentLine): IntPx?
}

/**
 * The global position of this layout.
 */
inline val LayoutCoordinates.globalPosition: PxPosition get() = localToGlobal(PxPosition.Origin)

/**
 * The position of this layout inside the root composable.
 */
inline val LayoutCoordinates.positionInRoot: PxPosition get() = localToRoot(PxPosition.Origin)

/**
 * The boundaries of this layout inside the root composable.
 */
val LayoutCoordinates.boundsInRoot: PxBounds
    get() {
        return findRoot(this).childBoundingBox(this)
    }

/**
 * Returns the position of the top-left in the parent's content area or (0, 0)
 * for the root.
 */
val LayoutCoordinates.positionInParent: PxPosition
    get() = parentCoordinates?.childToLocal(this, PxPosition.Origin) ?: PxPosition.Origin

/**
 * Returns the bounding box of the child in the parent's content area, including any clipping
 * done with respect to the parent. For the root, the bounds is positioned at (0, 0) and sized
 * to the size of the root.
 */
val LayoutCoordinates.boundsInParent: PxBounds
    get() = parentCoordinates?.childBoundingBox(this)
        ?: PxBounds(0f, 0f, size.width.value.toFloat(), size.height.value.toFloat())

/**
 * The global boundaries of this layout inside.
 */
val LayoutCoordinates.globalBounds: PxBounds
    get() {
        val root = findRoot(this)
        val rootPosition = root.localToGlobal(PxPosition.Origin)
        val bounds = root.childBoundingBox(this)
        return PxBounds(
            left = bounds.left + rootPosition.x,
            top = bounds.top + rootPosition.y,
            right = bounds.right + rootPosition.x,
            bottom = bounds.bottom + rootPosition.y
        )
    }

private fun findRoot(layoutCoordinates: LayoutCoordinates): LayoutCoordinates {
    var root = layoutCoordinates
    var parent = root.parentCoordinates
    while (parent != null) {
        root = parent
        parent = root.parentCoordinates
    }
    return root
}
/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.internal.throwUnsupportedOperationException
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastMaxOf
import androidx.compose.ui.util.fastMinOf

/** A holder of the measured bounds for the [Layout]. */
@JvmDefaultWithCompatibility
interface LayoutCoordinates {
    /** The size of this layout in the local coordinates space. */
    val size: IntSize

    /** The alignment lines provided for this layout, not including inherited lines. */
    val providedAlignmentLines: Set<AlignmentLine>

    /** The coordinates of the parent layout. Null if there is no parent. */
    val parentLayoutCoordinates: LayoutCoordinates?

    /**
     * The coordinates of the parent layout modifier or parent layout if there is no parent layout
     * modifier, or `null` if there is no parent.
     */
    val parentCoordinates: LayoutCoordinates?

    /** Returns false if the corresponding layout was detached from the hierarchy. */
    val isAttached: Boolean

    /**
     * Indicates whether the corresponding Layout is expected to change its [Offset] in small
     * increments (such as when its parent is a `Scroll`).
     *
     * In those situations, the corresponding placed [LayoutCoordinates] will have their
     * [introducesMotionFrameOfReference] return `true`.
     *
     * Custom Layouts that are expected to have similar behaviors should place their children using
     * [Placeable.PlacementScope.withMotionFrameOfReferencePlacement].
     *
     * You may then use [localPositionOf] with `includeMotionFrameOfReference = false` to query a
     * Layout's position such that it excludes all [Offset] introduced by those Layouts.
     *
     * This is typically helpful when deciding when to animate an [approachLayout] using
     * [LookaheadScope] coordinates. As you probably don't want to trigger animations on small
     * positional increments.
     *
     * @see Placeable.PlacementScope.withMotionFrameOfReferencePlacement
     * @see localPositionOf
     */
    @Suppress("GetterSetterNames") // Preferred name
    val introducesMotionFrameOfReference: Boolean
        get() = false

    /**
     * Converts [relativeToScreen] relative to the device's screen's origin into an [Offset]
     * relative to this layout. Returns [Offset.Unspecified] if the conversion cannot be performed.
     */
    fun screenToLocal(relativeToScreen: Offset): Offset = Offset.Unspecified

    /**
     * Converts [relativeToLocal] position within this layout into an [Offset] relative to the
     * device's screen. Returns [Offset.Unspecified] if the conversion cannot be performed.
     */
    fun localToScreen(relativeToLocal: Offset): Offset = Offset.Unspecified

    /**
     * Converts [relativeToWindow] relative to the window's origin into an [Offset] relative to this
     * layout.
     */
    fun windowToLocal(relativeToWindow: Offset): Offset

    /**
     * Converts [relativeToLocal] position within this layout into an [Offset] relative to the
     * window's origin.
     */
    fun localToWindow(relativeToLocal: Offset): Offset

    /** Converts a local position within this layout into an offset from the root composable. */
    fun localToRoot(relativeToLocal: Offset): Offset

    /**
     * Converts an [relativeToSource] in [sourceCoordinates] space into local coordinates.
     * [sourceCoordinates] may be any [LayoutCoordinates] that belong to the same compose layout
     * hierarchy.
     *
     * By default, includes the [Offset] when [introducesMotionFrameOfReference] is `true`. But you
     * may exclude it from the calculation by using the overload that takes
     * `includeMotionFrameOfReference` and passing it as `false`.
     */
    fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset

    /**
     * Converts an [relativeToSource] in [sourceCoordinates] space into local coordinates.
     * [sourceCoordinates] may be any [LayoutCoordinates] that belong to the same compose layout
     * hierarchy.
     *
     * Use [includeMotionFrameOfReference] to decide whether to include the [Offset] of any
     * `LayoutCoordinate` that returns `true` in the [includeMotionFrameOfReference] flag.
     *
     * In other words, passing [includeMotionFrameOfReference] as `false`, returns a calculation
     * that excludes the [Offset] set from Layouts that place their children using
     * [Placeable.PlacementScope.withMotionFrameOfReferencePlacement].
     */
    fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset = Offset.Zero,
        includeMotionFrameOfReference: Boolean = true
    ): Offset {
        throw UnsupportedOperationException(
            "localPositionOf is not implemented on this LayoutCoordinates"
        )
    }

    /**
     * Returns the bounding box of [sourceCoordinates] in the local coordinates. If [clipBounds] is
     * `true`, any clipping that occurs between [sourceCoordinates] and this layout will affect the
     * returned bounds, and can even result in an empty rectangle if clipped regions do not overlap.
     * If [clipBounds] is false, the bounding box of [sourceCoordinates] will be converted to local
     * coordinates irrespective of any clipping applied between the layouts.
     *
     * When rotation or scaling is applied, the bounding box of the rotated or scaled value will be
     * computed in the local coordinates. For example, if a 40 pixels x 20 pixel layout is rotated
     * 90 degrees, the bounding box will be 20 pixels x 40 pixels in its parent's coordinates.
     */
    fun localBoundingBoxOf(sourceCoordinates: LayoutCoordinates, clipBounds: Boolean = true): Rect

    /**
     * Modifies [matrix] to be a transform to convert a coordinate in [sourceCoordinates] to a
     * coordinate in `this` [LayoutCoordinates].
     */
    @Suppress("DocumentExceptions")
    fun transformFrom(sourceCoordinates: LayoutCoordinates, matrix: Matrix) {
        throwUnsupportedOperationException(
            "transformFrom is not implemented on this LayoutCoordinates"
        )
    }

    /**
     * Takes a [matrix] which transforms some coordinate system `C` to local coordinates, and
     * updates the matrix to transform from `C` to screen coordinates instead.
     */
    @Suppress("DocumentExceptions")
    fun transformToScreen(matrix: Matrix) {
        throw UnsupportedOperationException(
            "transformToScreen is not implemented on this LayoutCoordinates"
        )
    }

    /**
     * Returns the position in pixels of an [alignment line][AlignmentLine], or
     * [AlignmentLine.Unspecified] if the line is not provided.
     */
    operator fun get(alignmentLine: AlignmentLine): Int
}

/** The position of this layout inside the root composable. */
fun LayoutCoordinates.positionInRoot(): Offset = localToRoot(Offset.Zero)

/** The position of this layout relative to the window. */
fun LayoutCoordinates.positionInWindow(): Offset = localToWindow(Offset.Zero)

/**
 * The position of this layout on the device's screen. Returns [Offset.Unspecified] if the
 * conversion cannot be performed.
 */
fun LayoutCoordinates.positionOnScreen(): Offset = localToScreen(Offset.Zero)

/** The boundaries of this layout inside the root composable. */
fun LayoutCoordinates.boundsInRoot(): Rect = findRootCoordinates().localBoundingBoxOf(this)

/** The boundaries of this layout relative to the window's origin. */
fun LayoutCoordinates.boundsInWindow(): Rect {
    val root = findRootCoordinates()
    val rootWidth = root.size.width.toFloat()
    val rootHeight = root.size.height.toFloat()

    val bounds = boundsInRoot()
    val boundsLeft = bounds.left.fastCoerceIn(0f, rootWidth)
    val boundsTop = bounds.top.fastCoerceIn(0f, rootHeight)
    val boundsRight = bounds.right.fastCoerceIn(0f, rootWidth)
    val boundsBottom = bounds.bottom.fastCoerceIn(0f, rootHeight)

    if (boundsLeft == boundsRight || boundsTop == boundsBottom) {
        return Rect.Zero
    }

    val topLeft = root.localToWindow(Offset(boundsLeft, boundsTop))
    val topRight = root.localToWindow(Offset(boundsRight, boundsTop))
    val bottomRight = root.localToWindow(Offset(boundsRight, boundsBottom))
    val bottomLeft = root.localToWindow(Offset(boundsLeft, boundsBottom))

    val topLeftX = topLeft.x
    val topRightX = topRight.x
    val bottomLeftX = bottomLeft.x
    val bottomRightX = bottomRight.x

    val left = fastMinOf(topLeftX, topRightX, bottomLeftX, bottomRightX)
    val right = fastMaxOf(topLeftX, topRightX, bottomLeftX, bottomRightX)

    val topLeftY = topLeft.y
    val topRightY = topRight.y
    val bottomLeftY = bottomLeft.y
    val bottomRightY = bottomRight.y

    val top = fastMinOf(topLeftY, topRightY, bottomLeftY, bottomRightY)
    val bottom = fastMaxOf(topLeftY, topRightY, bottomLeftY, bottomRightY)

    return Rect(left, top, right, bottom)
}

/** Returns the position of the top-left in the parent's content area or (0, 0) for the root. */
fun LayoutCoordinates.positionInParent(): Offset =
    parentLayoutCoordinates?.localPositionOf(this, Offset.Zero) ?: Offset.Zero

/**
 * Returns the bounding box of the child in the parent's content area, including any clipping done
 * with respect to the parent. For the root, the bounds is positioned at (0, 0) and sized to the
 * size of the root.
 */
fun LayoutCoordinates.boundsInParent(): Rect =
    parentLayoutCoordinates?.localBoundingBoxOf(this)
        ?: Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())

/**
 * Walks up the [LayoutCoordinates] hierarchy to find the [LayoutCoordinates] whose
 * [LayoutCoordinates.parentCoordinates] is `null` and returns it. If
 * [LayoutCoordinates.isAttached], this will have the size of the
 * [ComposeView][androidx.compose.ui.platform.ComposeView].
 */
fun LayoutCoordinates.findRootCoordinates(): LayoutCoordinates {
    var root = this
    var parent = root.parentLayoutCoordinates
    while (parent != null) {
        root = parent
        parent = root.parentLayoutCoordinates
    }
    var rootCoordinator = root as? NodeCoordinator ?: return root
    var parentCoordinator = rootCoordinator.wrappedBy
    while (parentCoordinator != null) {
        rootCoordinator = parentCoordinator
        parentCoordinator = parentCoordinator.wrappedBy
    }
    return rootCoordinator
}

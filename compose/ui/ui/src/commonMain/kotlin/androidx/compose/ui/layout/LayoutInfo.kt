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

import androidx.annotation.IntRange
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The public information about the layouts used internally as nodes in the Compose UI hierarchy.
 */
public interface LayoutInfo {

    /**
     * This returns a new List of [Modifier]s and the coordinates and any extra information that may
     * be useful. This is used for tooling to retrieve layout modifier and layer information.
     */
    public fun getModifierInfo(): List<ModifierInfo>

    /** The measured width of this layout and all of its modifiers. */
    public val width: Int

    /** The measured height of this layout and all of its modifiers. */
    public val height: Int

    /** Coordinates of just the contents of the layout, after being affected by all modifiers. */
    public val coordinates: LayoutCoordinates

    /** Whether or not this layout and all of its parents have been placed in the hierarchy. */
    public val isPlaced: Boolean

    /** Parent of this layout. */
    public val parentInfo: LayoutInfo?

    /** The density in use for this layout. */
    public val density: Density

    /** The layout direction in use for this layout. */
    public val layoutDirection: LayoutDirection

    /** The [ViewConfiguration] in use for this layout. */
    public val viewConfiguration: ViewConfiguration

    /** Returns true if this layout is currently a part of the layout tree. */
    public val isAttached: Boolean

    /** Unique and stable id representing this node to the semantics system. */
    public val semanticsId: Int

    /**
     * True if the node is deactivated. For example, the children of
     * [androidx.compose.ui.layout.SubcomposeLayout] which are retained to be reused in future are
     * considered deactivated.
     */
    public val isDeactivated: Boolean
        get() = false

    /** A layout node that is ignored by the layout system. */
    public val isVirtual: Boolean
        get() = false

    /** The zIndex of this layout node, defining the drawing order. */
    @ExperimentalComposeUiHostApi
    public val zIndex: Float
        get() = 0f

    /**
     * Orders this layout among siblings placed by the same parent during layout.
     *
     * Placed children receive sequential values starting from `0`, or [NotPlacedPlaceOrder] when
     * not placed ([isPlaced] is `false`).
     */
    @get:IntRange(from = 0)
    @ExperimentalComposeUiHostApi
    public val placeOrder: Int
        get() = NotPlacedPlaceOrder

    @ExperimentalComposeUiHostApi
    public companion object {
        /**
         * Marks a [LayoutInfo] as not placed in [placeOrder].
         *
         * [placeOrder] returns this value when [isPlaced] is `false`.
         */
        @Suppress("ConstPropertyName")
        @ExperimentalComposeUiHostApi
        public const val NotPlacedPlaceOrder: Int = Int.MAX_VALUE
    }
}

/**
 * Calculates the bounds of this layout in root coordinates, including all of its modifiers
 * (padding, size, border, and so on), writing the result into [rect]. Bounds are not clipped.
 *
 * These are the bounds the layout occupies in its parent, transformed to the root. If this layout
 * isn't attached, [rect] is set to `(0, 0, 0, 0)`.
 *
 * @param rect The [MutableRect] into which the bounds are written.
 */
@ExperimentalComposeUiHostApi
public fun LayoutInfo.calculateOuterBoundsInRoot(rect: MutableRect) {
    val layoutNode = this as? LayoutNode
    if (layoutNode == null || !layoutNode.isAttached) {
        rect.set(0f, 0f, 0f, 0f)
        return
    }
    layoutNode.outerCoordinator.calculateUnclippedBoundsInRoot(rect)
}

/** Writes the unclipped bounds of these attached coordinates in root coordinates into [rect]. */
internal fun LayoutCoordinates.calculateUnclippedBoundsInRoot(rect: MutableRect) {
    val rootCoordinates = findRootCoordinates()
    if (rootCoordinates is NodeCoordinator) {
        rootCoordinates.localBoundingBoxOf(this, rect, clipBounds = false)
    } else {
        val bounds = rootCoordinates.localBoundingBoxOf(this, clipBounds = false)
        rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }
}

/**
 * Whether this layout is drawn fully transparent by an ancestor: the layer of its outermost
 * modifier, or a layer of any ancestor layout, has an alpha of `0` or less.
 *
 * Layers inside this layout's own modifier chain are not considered, so `Modifier.alpha(0f)`
 * applied after other modifiers on this layout doesn't make it transparent.
 */
@ExperimentalComposeUiHostApi
public val LayoutInfo.isTransparent: Boolean
    get() = (this as? LayoutNode)?.isTransparent() ?: false

/** Used by tooling to examine the modifiers on a [LayoutInfo]. */
public class ModifierInfo(
    public val modifier: Modifier,
    public val coordinates: LayoutCoordinates,
    public val extra: Any? = null,
) {
    public override fun toString(): String {
        return "ModifierInfo($modifier, $coordinates, $extra)"
    }
}

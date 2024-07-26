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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OverlayLayout
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.OnUnplacedModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseDescendantsInDrawOrder

/**
 * Providing interop container as composition local, so platofrm interop can use it to add
 * native views to the hierarchy.
 */
internal val LocalInteropContainer = staticCompositionLocalOf<InteropContainer> {
    error("LocalInteropContainer not provided")
}

/**
 * An interface for container that controls interop views/components.
 */
internal interface InteropContainer {
    val root: InteropViewGroup
    var rootModifier: TrackInteropPlacementModifierNode?
    val interopViews: Set<InteropViewHolder>

    fun placeInteropView(interopView: InteropViewHolder)
    fun unplaceInteropView(interopView: InteropViewHolder)

    // TODO: Should be the same as [Owner.onInteropViewLayoutChange]
    fun changeInteropViewLayout(action: () -> Unit) {
        action()
    }
}

/**
 * Counts the number of interop components before the given native view in the container.
 *
 * @param interopView The native view to count interop components before.
 * @return The number of interop components before the given native view.
 */
internal fun InteropContainer.countInteropComponentsBelow(interopView: InteropViewHolder): Int {
    var componentsBefore = 0
    rootModifier?.traverseDescendantsInDrawOrder {
        if (it.interopView != interopView) {
            // It might be inside a Compose tree before adding in InteropContainer in case
            // if it was initiated out of scroll visible bounds for example.
            if (it.interopView in interopViews) {
                componentsBefore++
            }
            true
        } else {
            false
        }
    }
    return componentsBefore
}

/**
 * Wrapper of Compose content that might contain interop views. It adds a helper modifier to root
 * that allows traversing interop views in the tree with the right order.
 */
@Composable
internal fun InteropContainer.TrackInteropPlacementContainer(content: @Composable () -> Unit) {
    OverlayLayout(
        modifier = RootTrackInteropPlacementModifierElement { rootModifier = it },
        content = content
    )
}

/**
 * A helper modifier element to track interop views inside a [LayoutNode] hierarchy.
 *
 * @property onModifierNodeCreated An optional block of code that to receive the reference to
 * [TrackInteropPlacementModifierNode].
 */
private data class RootTrackInteropPlacementModifierElement(
    val onModifierNodeCreated: (TrackInteropPlacementModifierNode) -> Unit
) : ModifierNodeElement<TrackInteropPlacementModifierNode>() {
    override fun create() = TrackInteropPlacementModifierNode(
        interopView = null
    ).also {
        onModifierNodeCreated.invoke(it)
    }

    override fun update(node: TrackInteropPlacementModifierNode) {
    }
}

/**
 * Modifier to track interop view inside [LayoutNode] hierarchy.
 *
 * @param interopView The interop view that matches the current node.
 */
internal fun Modifier.trackInteropPlacement(interopView: InteropViewHolder): Modifier =
    this then TrackInteropPlacementModifierElement(interopView)

/**
 * A helper modifier element that tracks an interop view inside a [LayoutNode] hierarchy.
 *
 * @property interopView The native view associated with this modifier element.
 *
 * @see TrackInteropPlacementModifierNode
 * @see ModifierNodeElement
 */
private data class TrackInteropPlacementModifierElement(
    val interopView: InteropViewHolder,
) : ModifierNodeElement<TrackInteropPlacementModifierNode>() {
    override fun create() = TrackInteropPlacementModifierNode(
        interopView = interopView
    )

    override fun update(node: TrackInteropPlacementModifierNode) {
        node.interopView = interopView
    }
}

private const val TRAVERSAL_NODE_KEY =
    "androidx.compose.ui.node.TRACK_INTEROP_TRAVERSAL_NODE_KEY"

/**
 * A modifier node for tracking and traversing interop purposes.
 *
 * @property interopView the native view that matches the current node.
 *
 * @see TraversableNode
 */
internal class TrackInteropPlacementModifierNode(
    var interopView: InteropViewHolder?,
) : Modifier.Node(), TraversableNode, LayoutAwareModifierNode, OnUnplacedModifierNode {
    override val traverseKey = TRAVERSAL_NODE_KEY

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val interopView = interopView ?: return
        interopView.container.placeInteropView(interopView)
    }

    override fun onUnplaced() {
        val interopView = interopView ?: return
        interopView.container.unplaceInteropView(interopView)
    }

    override fun onDetach() {
        onUnplaced()
        super.onDetach()
    }
}

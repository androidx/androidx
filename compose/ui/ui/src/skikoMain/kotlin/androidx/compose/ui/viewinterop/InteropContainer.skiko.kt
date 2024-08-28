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
import androidx.compose.runtime.snapshots.SnapshotStateObserver
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
    val snapshotObserver: SnapshotStateObserver

    fun contains(holder: InteropViewHolder): Boolean

    fun holderOfView(view: InteropView): InteropViewHolder?

    /**
     * Calculates the proper index for the interop view in the container and issues a request to
     * update the view hierarchy.
     */
    fun place(holder: InteropViewHolder)

    /**
     * Issues a request to remove the interop view from the hierarchy.
     */
    fun unplace(holder: InteropViewHolder)

    /**
     * Schedule an update to be performed on interop view. Platforms have their different strategy
     * to align the updates with the rendering and threading requirements when modifying
     * interop views.
     *
     * @param action The action to be performed. Could be layout change, or other visual updates
     * to the view state, such as background, corner radius, etc.
     */
    fun scheduleUpdate(action: () -> Unit)

    // TODO: Should be the same as [Owner.onInteropViewLayoutChange]?
//    /**
//     * Callback to be invoked when the layout of the interop view changes to notify the system
//     * that something has changed.
//     */
//    fun onInteropViewLayoutChange(holder: InteropViewHolder)
}

/**
 * Counts the number of interop components before the given native view in the container.
 *
 * @param holder The holder for native view to count interop components before.
 * @return The number of interop components before the given native view.
 */
internal fun InteropContainer.countInteropComponentsBelow(holder: InteropViewHolder): Int {
    var componentsBefore = 0
    rootModifier?.traverseDescendantsInDrawOrder {
        val currentHolder = it.interopViewHolder
        if (currentHolder != null && currentHolder != holder) {
            // It might be inside a Compose tree before adding in InteropContainer in case
            // if it was initiated out of scroll visible bounds for example.

            if (contains(currentHolder)) {
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
 *
 * TODO: refactor to use a root node modifier instead of emitting an extra node
 *      https://youtrack.jetbrains.com/issue/CMP-5896
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
        interopViewHolder = null
    ).also {
        onModifierNodeCreated.invoke(it)
    }

    override fun update(node: TrackInteropPlacementModifierNode) {
    }
}

/**
 * Modifier to track interop view inside [LayoutNode] hierarchy.
 *
 * @param interopViewHolder The interop view holder that matches the current node.
 */
internal fun Modifier.trackInteropPlacement(interopViewHolder: InteropViewHolder): Modifier =
    this then TrackInteropPlacementModifierElement(interopViewHolder)

/**
 * A helper modifier element that tracks an interop view inside a [LayoutNode] hierarchy.
 *
 * @property interopViewHolder The native view associated with this modifier element.
 *
 * @see TrackInteropPlacementModifierNode
 * @see ModifierNodeElement
 */
private data class TrackInteropPlacementModifierElement(
    val interopViewHolder: InteropViewHolder,
) : ModifierNodeElement<TrackInteropPlacementModifierNode>() {
    override fun create() = TrackInteropPlacementModifierNode(
        interopViewHolder = interopViewHolder
    )

    override fun update(node: TrackInteropPlacementModifierNode) {
        node.interopViewHolder = interopViewHolder
    }
}

private const val TRAVERSAL_NODE_KEY =
    "androidx.compose.ui.node.TRACK_INTEROP_TRAVERSAL_NODE_KEY"

/**
 * A modifier node for tracking and traversing interop purposes.
 *
 * @property interopViewHolder the native view that matches the current node.
 *
 * @see TraversableNode
 */
internal class TrackInteropPlacementModifierNode(
    var interopViewHolder: InteropViewHolder?,
) : Modifier.Node(), TraversableNode, LayoutAwareModifierNode, OnUnplacedModifierNode {
    override val traverseKey = TRAVERSAL_NODE_KEY

    override fun onPlaced(coordinates: LayoutCoordinates) {
        interopViewHolder?.place()
    }

    override fun onUnplaced() {
        interopViewHolder?.unplace()
    }

    override fun onDetach() {
        onUnplaced()
        super.onDetach()
    }
}

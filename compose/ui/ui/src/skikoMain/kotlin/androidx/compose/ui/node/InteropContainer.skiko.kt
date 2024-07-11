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

package androidx.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OverlayLayout

/**
 * An interface for container that controls interop views/components.
 *
 * @param T The type of the native view.
 */
internal interface InteropContainer<T> {
    var rootModifier: TrackInteropModifierNode<T>?
    val interopViews: Set<T>

    fun placeInteropView(nativeView: T)
    fun unplaceInteropView(nativeView: T)
}

/**
 * Counts the number of interop components before the given native view in the container.
 *
 * @param nativeView The native view to count interop components before.
 * @return The number of interop components before the given native view.
 */
internal fun <T> InteropContainer<T>.countInteropComponentsBelow(nativeView: T): Int {
    var componentsBefore = 0
    rootModifier?.traverseDescendantsInDrawOrder {
        if (it.nativeView != nativeView) {
            // It might be inside a Compose tree before adding in InteropContainer in case
            // if it was initiated out of scroll visible bounds for example.
            if (it.nativeView in interopViews) {
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
internal fun <T> InteropContainer<T>.TrackInteropContainer(content: @Composable () -> Unit) {
    OverlayLayout(
        modifier = RootTrackInteropModifierElement { rootModifier = it },
        content = content
    )
}

/**
 * A helper modifier element to track interop views inside a [LayoutNode] hierarchy.
 *
 * @property onModifierNodeCreated An optional block of code that to receive the reference to
 * [TrackInteropModifierNode].
 */
private data class RootTrackInteropModifierElement<T>(
    val onModifierNodeCreated: (TrackInteropModifierNode<T>) -> Unit
) : ModifierNodeElement<TrackInteropModifierNode<T>>() {
    override fun create() = TrackInteropModifierNode<T>(
        container = null,
        nativeView = null
    ).also {
        onModifierNodeCreated.invoke(it)
    }

    override fun update(node: TrackInteropModifierNode<T>) {
    }
}

/**
 * A helper modifier element that tracks an interop view inside a [LayoutNode] hierarchy.
 *
 * @property nativeView The native view associated with this modifier element.
 * @param T The type of the native view.
 *
 * @see TrackInteropModifierNode
 * @see ModifierNodeElement
 */
internal data class TrackInteropModifierElement<T>(
    val container: InteropContainer<T>,
    val nativeView: T,
) : ModifierNodeElement<TrackInteropModifierNode<T>>() {
    override fun create() = TrackInteropModifierNode(
        container = container,
        nativeView = nativeView
    )

    override fun update(node: TrackInteropModifierNode<T>) {
        node.container = container
        node.nativeView = nativeView
    }
}

private const val TRAVERSAL_NODE_KEY =
    "androidx.compose.ui.node.TRACK_INTEROP_TRAVERSAL_NODE_KEY"

/**
 * A modifier node for tracking and traversing interop purposes.
 *
 * @param T the type of the native view
 * @property nativeView the native view that matches the current node.
 *
 * @see TraversableNode
 */
internal class TrackInteropModifierNode<T>(
    var container: InteropContainer<T>?,
    var nativeView: T?,
) : Modifier.Node(), TraversableNode, LayoutAwareModifierNode, OnUnplacedModifierNode {
    override val traverseKey = TRAVERSAL_NODE_KEY

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val nativeView = nativeView ?: return
        container?.placeInteropView(nativeView)
    }

    override fun onUnplaced() {
        val nativeView = nativeView ?: return
        container?.unplaceInteropView(nativeView)
    }

    override fun onDetach() {
        onUnplaced()
        super.onDetach()
    }
}

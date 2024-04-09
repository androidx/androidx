/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmMultifileClass
@file:JvmName("BringIntoViewRequesterKt")

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * A parent that can respond to [bringChildIntoView] requests from its children, and scroll so that
 * the item is visible on screen. To apply a responder to an element, pass it to the
 * [bringIntoViewResponder] modifier.
 *
 * When a component calls [BringIntoViewRequester.bringIntoView], the nearest
 * [BringIntoViewResponder] is found, which is responsible for, in order:
 *
 * 1. Calculating a rectangle that its parent responder should bring into view by returning it from
 *    [calculateRectForParent].
 * 2. Performing any scroll or other layout adjustments needed to ensure the requested rectangle is
 *    brought into view in [bringChildIntoView].
 *
 * Here is a sample where a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * Here is a sample where a part of a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 *
 * @see BringIntoViewRequester
 *
 * Note: this API is experimental while we optimise the performance and find the right API shape
 * for it
 */
@ExperimentalFoundationApi
interface BringIntoViewResponder {

    /**
     * Return the rectangle in this node that should be brought into view by this node's parent,
     * in coordinates relative to this node. If this node needs to adjust itself to bring
     * [localRect] into view, the returned rectangle should be the destination rectangle that
     * [localRect] will eventually occupy once this node's content is adjusted.
     *
     * @param localRect The rectangle that should be brought into view, relative to this node. This
     * will be the same rectangle passed to [bringChildIntoView].
     * @return The rectangle in this node that should be brought into view itself, relative to this
     * node. If this node needs to scroll to bring [localRect] into view, the returned rectangle
     * should be the destination rectangle that [localRect] will eventually occupy, once the
     * scrolling animation is finished.
     */
    @ExperimentalFoundationApi
    fun calculateRectForParent(localRect: Rect): Rect

    /**
     * Bring this specified rectangle into bounds by making this scrollable parent scroll
     * appropriately.
     *
     * This method should ensure that only one call is being handled at a time. If you use Compose's
     * `Animatable` you get this for free, since it will cancel the previous animation when a new
     * one is started while preserving velocity.
     *
     * @param localRect A function returning the rectangle that should be brought into view,
     * relative to this node. This is the same rectangle that will have been passed to
     * [calculateRectForParent]. The function may return a different value over time, if the bounds
     * of the request change while the request is being processed. If the rectangle cannot be
     * calculated, e.g. because the [LayoutCoordinates] are not attached, return null.
     */
    @ExperimentalFoundationApi
    suspend fun bringChildIntoView(localRect: () -> Rect?)
}

/**
 * A parent that can respond to [BringIntoViewRequester] requests from its children, and scroll so
 * that the item is visible on screen. See [BringIntoViewResponder] for more details about how
 * this mechanism works.
 *
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * @see BringIntoViewRequester
 *
 * Note: this API is experimental while we optimise the performance and find the right API shape
 * for it
 */
@Suppress("ModifierInspectorInfo")
@ExperimentalFoundationApi
fun Modifier.bringIntoViewResponder(
    responder: BringIntoViewResponder
): Modifier = this.then(BringIntoViewResponderElement(responder))

@ExperimentalFoundationApi
private class BringIntoViewResponderElement(
    private val responder: BringIntoViewResponder
) : ModifierNodeElement<BringIntoViewResponderNode>() {
    override fun create(): BringIntoViewResponderNode = BringIntoViewResponderNode(responder)

    override fun update(node: BringIntoViewResponderNode) {
        node.responder = responder
    }
    override fun equals(other: Any?): Boolean {
        return (this === other) ||
            (other is BringIntoViewResponderElement) && (responder == other.responder)
    }

    override fun hashCode(): Int {
        return responder.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bringIntoViewResponder"
        properties["responder"] = responder
    }
}

/**
 * A modifier that holds state and modifier implementations for [bringIntoViewResponder]. It has
 * access to the next [BringIntoViewParent] via [findBringIntoViewParent] and additionally
 * provides itself as the [BringIntoViewParent] for subsequent modifiers. This class is responsible
 * for recursively propagating requests up the responder chain.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class BringIntoViewResponderNode(
    var responder: BringIntoViewResponder
) : Modifier.Node(), BringIntoViewParent, LayoutAwareModifierNode, TraversableNode {

    override val traverseKey: Any
        get() = TraverseKey

    override val shouldAutoInvalidate: Boolean = false

    // TODO(b/324613946) Get rid of this check.
    private var hasBeenPlaced = false
    override fun onPlaced(coordinates: LayoutCoordinates) {
        hasBeenPlaced = true
    }

    /**
     * Responds to a child's request by first converting [boundsProvider] into this node's
     * [LayoutCoordinates] and then, concurrently, calling the [responder] and the [parent] to
     * handle the request.
     */
    override suspend fun bringChildIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?
    ) {
        @Suppress("NAME_SHADOWING")
        fun localRect(): Rect? {
            if (!isAttached) return null
            // Can't do any calculations before the node is initially placed.
            if (!hasBeenPlaced) return null

            // Either coordinates can become detached at any time, so we have to check before every
            // calculation.
            val layoutCoordinates = requireLayoutCoordinates()
            val childCoordinates = childCoordinates.takeIf { it.isAttached } ?: return null
            val rect = boundsProvider() ?: return null
            return layoutCoordinates.localRectOf(childCoordinates, rect)
        }

        val parentRect = { localRect()?.let(responder::calculateRectForParent) }

        coroutineScope {
            // For the item to be visible, if needs to be in the viewport of all its
            // ancestors.
            // Note: For now we run both of these concurrently, but in the future we could
            // make this configurable. (The child relocation could be executed before the
            // parent, or parent before the child).
            launch {
                // Bring the requested Child into this parent's view.
                responder.bringChildIntoView(::localRect)
            }

            // Launch this as well so that if the parent is cancelled (this throws a CE) due to
            // animation interruption, the child continues animating. If we just call
            // bringChildIntoView directly without launching, if that function throws a
            // CancellationException, it will cancel this coroutineScope, which will also cancel the
            // responder's coroutine.
            launch {
                if (isAttached) {
                    val parent = findBringIntoViewParent()
                    parent?.bringChildIntoView(
                        childCoordinates = requireLayoutCoordinates(),
                        boundsProvider = parentRect
                    )
                }
            }
        }
    }

    companion object TraverseKey
}

/**
 * Finds the nearest ancestor [BringIntoViewResponderNode], or returns [defaultBringIntoViewParent]
 * if none can be found. Returns null if the node is not attached.
 */
internal fun DelegatableNode.findBringIntoViewParent(): BringIntoViewParent? {
    if (!node.isAttached) return null
    return (findNearestAncestor(BringIntoViewResponderNode) as BringIntoViewParent?)
        ?: defaultBringIntoViewParent()
}

/**
 * Translates [rect], specified in [sourceCoordinates], into this [LayoutCoordinates].
 */
private fun LayoutCoordinates.localRectOf(
    sourceCoordinates: LayoutCoordinates,
    rect: Rect
): Rect {
    // Translate the supplied layout coordinates into the coordinate system of this parent.
    val localRect = localBoundingBoxOf(sourceCoordinates, clipBounds = false)

    // Translate the rect to this parent's local coordinates.
    return rect.translate(localRect.topLeft)
}

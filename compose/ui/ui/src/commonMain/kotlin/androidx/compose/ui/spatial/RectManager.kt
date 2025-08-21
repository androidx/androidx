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

package androidx.compose.ui.spatial

import androidx.collection.IntObjectMap
import androidx.collection.intObjectMapOf
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.currentTimeMillis
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.requireSemanticsInfo
import androidx.compose.ui.postDelayed
import androidx.compose.ui.removePost
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import kotlin.math.max

internal class RectManager(
    /** [LayoutNode.semanticsId] to [LayoutNode] mapping, maintained by Owner. */
    private val layoutNodes: IntObjectMap<LayoutNode> = intObjectMapOf()
) {
    val rects: RectList = RectList()

    private val throttledCallbacks = ThrottledCallbacks()
    private val callbacks = mutableObjectListOf<() -> Unit>()
    private var isDirty = false
    private var isScreenOrWindowDirty = false
    private var isFragmented = false
    private var dispatchToken: Any? = null
    private var scheduledDispatchDeadline: Long = -1
    private val dispatchLambda = {
        dispatchToken = null
        trace("OnPositionedDispatch") { dispatchCallbacks() }
    }

    fun invalidate() {
        isDirty = true
    }

    fun updateOffsets(
        screenOffset: IntOffset,
        windowOffset: IntOffset,
        viewToWindowMatrix: Matrix,
        windowWidth: Int,
        windowHeight: Int,
    ) {
        val analysis = viewToWindowMatrix.analyzeComponents()
        isScreenOrWindowDirty =
            throttledCallbacks.updateOffsets(
                screenOffset,
                windowOffset,
                if (analysis.hasNonTranslationComponents) viewToWindowMatrix else null,
                windowWidth,
                windowHeight,
            ) || isScreenOrWindowDirty
    }

    // TODO: we need to make sure these are dispatched after draw if needed
    fun dispatchCallbacks() {
        val currentTime = currentTimeMillis()

        // For ThrottledCallbacks on global changes we need to make sure they are all called for any
        // change
        val isDispatchGlobalCallbacks = isDirty || isScreenOrWindowDirty

        // TODO: we need to move this to avoid double-firing
        if (isDirty) {
            isDirty = false
            callbacks.forEach { it() }
            rects.forEachUpdatedRect { id, topLeft, bottomRight ->
                throttledCallbacks.fireOnUpdatedRect(id, topLeft, bottomRight, currentTime)
            }
            throttledCallbacks.forEachNewCallbackNeverInvoked { entry ->
                rects.withTopLeftBottomRight(entry.id) { topLeft, bottomRight ->
                    throttledCallbacks.fireWithUpdatedRect(entry, topLeft, bottomRight, currentTime)
                }
            }
            rects.clearUpdated()
        }
        if (isScreenOrWindowDirty) {
            isScreenOrWindowDirty = false
            throttledCallbacks.fireOnRectChangedEntries(currentTime)
        }
        if (isDispatchGlobalCallbacks) {
            throttledCallbacks.fireGlobalChangeEntries(currentTime)
        }
        // The hierarchy is "settled" in terms of nodes being added/removed for this frame
        // This makes it a reasonable time to "defragment" the RectList data structure. This
        // will keep operations on this data structure efficient over time. This is a fairly
        // cheap operation to run, so we just do it every time
        if (isFragmented) {
            isFragmented = false
            // TODO: if we want to take advantage of the "generation", we will be motivated to
            //  call this less often. Alternatively we could track the number of remove() calls
            //  we have made and only call defragment once it exceeds a certain percentage of
            //  the overall list.
            rects.defragment()
        }
        // this gets called frequently, but we might need to schedule it more often to ensure that
        // debounced callbacks get fired
        throttledCallbacks.triggerDebounced(currentTime)
    }

    fun scheduleDebounceCallback(ensureSomethingScheduled: Boolean) {
        val canExitEarly = !ensureSomethingScheduled || dispatchToken != null
        val nextDeadline = throttledCallbacks.minDebounceDeadline
        if (nextDeadline < 0 && canExitEarly) {
            return
        }
        val currentScheduledDeadline = scheduledDispatchDeadline
        if (currentScheduledDeadline == nextDeadline && canExitEarly) {
            return
        }
        if (dispatchToken != null) {
            removePost(dispatchToken)
        }
        val currentTime = currentTimeMillis()
        val nextFrameIsh = currentTime + 16
        val deadline = max(nextDeadline, nextFrameIsh)
        scheduledDispatchDeadline = deadline
        val delay = deadline - currentTime
        dispatchToken = postDelayed(delay, dispatchLambda)
    }

    fun registerOnChangedCallback(callback: () -> Unit): Any? {
        callbacks.add(callback)
        return callback
    }

    fun registerOnRectChangedCallback(
        id: Int,
        throttleMillis: Long,
        debounceMillis: Long,
        node: DelegatableNode,
        callback: (RelativeLayoutBounds) -> Unit,
    ): RegistrationHandle {
        return throttledCallbacks
            .registerOnRectChanged(id, throttleMillis, debounceMillis, node, callback)
            .also {
                invalidate()
                scheduleDebounceCallback(true)
            }
    }

    fun registerOnGlobalLayoutCallback(
        id: Int,
        throttleMillis: Long,
        debounceMillis: Long,
        node: DelegatableNode,
        callback: (RelativeLayoutBounds) -> Unit,
    ): RegistrationHandle {
        return throttledCallbacks.registerOnGlobalChange(
            id = id,
            throttleMillis = throttleMillis,
            debounceMillis = debounceMillis,
            node = node,
            callback = callback,
        )
    }

    fun unregisterOnChangedCallback(token: Any?) {
        @Suppress("UNCHECKED_CAST")
        token as? (() -> Unit) ?: return
        callbacks.remove(token)
    }

    fun invalidateCallbacksFor(layoutNode: LayoutNode) {
        isDirty = true
        rects.markUpdated(layoutNode.semanticsId)
        scheduleDebounceCallback(ensureSomethingScheduled = true)
    }

    fun updateFlagsFor(layoutNode: LayoutNode, focusable: Boolean, gesturable: Boolean) {
        if (layoutNode.isAttached) {
            rects.updateFlagsFor(
                value = layoutNode.semanticsId,
                focusable = focusable,
                gesturable = gesturable,
            )
        }
    }

    fun onLayoutLayerPositionalPropertiesChanged(layoutNode: LayoutNode) {
        @OptIn(ExperimentalComposeUiApi::class) if (!ComposeUiFlags.isRectTrackingEnabled) return
        val outerToInnerOffset = layoutNode.outerToInnerOffset()
        if (outerToInnerOffset.isSet) {
            // translational properties only. AABB still valid.
            layoutNode.outerToInnerOffset = outerToInnerOffset
            layoutNode.outerToInnerOffsetDirty = false
            layoutNode.forEachChild {
                // NOTE: this calls rectlist.move(...) so does not need to be recursive
                // TODO: we could potentially move to a single call of `updateSubhierarchy(...)`
                onLayoutPositionChanged(it, false)
            }
            invalidateCallbacksFor(layoutNode)
        } else {
            // there are rotations/skews/scales going on, so we need to do a more expensive update
            insertOrUpdateTransformedNodeSubhierarchy(layoutNode)
        }
    }

    fun onLayoutPositionChanged(layoutNode: LayoutNode, firstPlacement: Boolean) {
        @OptIn(ExperimentalComposeUiApi::class) if (!ComposeUiFlags.isRectTrackingEnabled) return
        // Our goal here is to get the right "root" coordinates for every layout. We can use
        // LayoutCoordinates.localToRoot to calculate this somewhat readily, however this function
        // is getting called with a very high frequency and so it is important that extracting these
        // coordinates remains relatively cheap to limit the overhead of this tracking. The
        // LayoutCoordinates will traverse up the entire "spine" of the hierarchy, so as we do this
        // calculation for many nodes, we would be making many redundant calculations. In order to
        // minimize this, we store the "offsetFromRoot" of each layout node as we calculate it, and
        // attempt to utilize this value when calculating it for a node that is below it.
        // Additionally, we calculate and cache the parent's "outer to inner offset" which may
        val delegate = layoutNode.measurePassDelegate
        val width = delegate.measuredWidth
        val height = delegate.measuredHeight

        val lastOffset = layoutNode.offsetFromRoot
        val lastSize = layoutNode.lastSize
        val lastWidth = lastSize.width
        val lastHeight = lastSize.height

        recalculateOffsetFromRoot(layoutNode)

        val offset = layoutNode.offsetFromRoot

        // If unset is returned then that means there is a rotation/skew/scale
        if (!offset.isSet) {
            insertOrUpdateTransformedNode(layoutNode, firstPlacement)
            return
        }

        layoutNode.lastSize = IntSize(width, height)

        val l = offset.x
        val t = offset.y
        val r = l + width
        val b = t + height

        if (!firstPlacement && offset == lastOffset && lastWidth == width && lastHeight == height) {
            return
        }

        insertOrUpdate(layoutNode, firstPlacement, l, t, r, b)
    }

    private fun recalculateOffsetFromRoot(layoutNode: LayoutNode) {
        val position = layoutNode.outerCoordinator.position
        val parent = layoutNode.parent
        layoutNode.offsetFromRoot =
            if (parent != null) {
                if (!parent.offsetFromRoot.isSet) {
                    recalculateOffsetFromRoot(parent)
                }

                val parentOffset = parent.offsetFromRoot
                if (!parentOffset.isSet) {
                    // if after recalculateOffsetFromRoot() on parent we still have unset return
                    // unset
                    IntOffset.Max
                } else {
                    val parentOuterInnerOffset =
                        if (parent.outerToInnerOffsetDirty) {
                            val it = parent.outerToInnerOffset()

                            parent.outerToInnerOffset = it
                            parent.outerToInnerOffsetDirty = false
                            it
                        } else {
                            parent.outerToInnerOffset
                        }
                    if (!parentOuterInnerOffset.isSet) {
                        IntOffset.Max
                    } else {
                        parentOffset + parentOuterInnerOffset + position
                    }
                }
            } else {
                // root
                position
            }
    }

    private fun insertOrUpdateTransformedNodeSubhierarchy(layoutNode: LayoutNode) {
        layoutNode.forEachChild {
            insertOrUpdateTransformedNode(it, false)
            insertOrUpdateTransformedNodeSubhierarchy(it)
        }
    }

    private val cachedRect = MutableRect(0f, 0f, 0f, 0f)

    private fun insertOrUpdateTransformedNode(layoutNode: LayoutNode, firstPlacement: Boolean) {
        val coord = layoutNode.outerCoordinator
        val delegate = layoutNode.measurePassDelegate
        val width = delegate.measuredWidth
        val height = delegate.measuredHeight
        val rect = cachedRect

        rect.set(0f, 0f, width.toFloat(), height.toFloat())

        coord.boundingRectInRoot(rect)

        val l = rect.left.toInt()
        val t = rect.top.toInt()
        val r = rect.right.toInt()
        val b = rect.bottom.toInt()
        val id = layoutNode.semanticsId
        // NOTE: we call update here instead of move since the subhierarchy will not be moved by a
        // simple delta since we are dealing with rotation/skew/scale/etc.
        if (firstPlacement || !rects.update(id, l, t, r, b)) {
            val parentId = layoutNode.parent?.semanticsId ?: -1
            rects.insert(
                id,
                l,
                t,
                r,
                b,
                parentId = parentId,
                focusable = layoutNode.nodes.has(Nodes.FocusTarget),
                gesturable = layoutNode.nodes.has(Nodes.PointerInput),
            )
        }
        invalidate()
    }

    private fun insertOrUpdate(
        layoutNode: LayoutNode,
        firstPlacement: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val id = layoutNode.semanticsId
        if (firstPlacement || !rects.move(id, l, t, r, b)) {
            val parentId = layoutNode.parent?.semanticsId ?: -1
            rects.insert(
                id,
                l,
                t,
                r,
                b,
                parentId = parentId,
                focusable = layoutNode.nodes.has(Nodes.FocusTarget),
                gesturable = layoutNode.nodes.has(Nodes.PointerInput),
            )
        }
        invalidate()
    }

    private fun NodeCoordinator.boundingRectInRoot(rect: MutableRect) {
        // TODO: can we use offsetFromRoot here to speed up calculation?
        var coordinator: NodeCoordinator? = this
        while (coordinator != null) {
            val layer = coordinator.layer
            rect.translate(coordinator.position.toOffset())
            coordinator = coordinator.wrappedBy
            if (layer != null) {
                val matrix = layer.underlyingMatrix
                if (!matrix.isIdentity()) {
                    matrix.map(rect)
                }
            }
        }
    }

    private fun LayoutNode.outerToInnerOffset(): IntOffset {
        val terminator = outerCoordinator
        var position = Offset.Zero
        var coordinator: NodeCoordinator? = innerCoordinator
        while (coordinator != null) {
            if (coordinator === terminator) break
            val layer = coordinator.layer
            position += coordinator.position
            coordinator = coordinator.wrappedBy
            if (layer != null) {
                val matrix = layer.underlyingMatrix
                val analysis = matrix.analyzeComponents()
                if (analysis.isIdentity) continue
                if (analysis.hasNonTranslationComponents) {
                    return IntOffset.Max
                }
                position = matrix.map(position)
            }
        }
        return position.round()
    }

    fun remove(layoutNode: LayoutNode) {
        rects.remove(layoutNode.semanticsId)
        invalidate()
        isFragmented = true
    }

    /**
     * For occlusion calculation, returns whether the [LayoutNode] corresponding to [targetId] is
     * drawn first compared to the [LayoutNode] corresponding to [otherId].
     *
     * @return true when the target will be drawn first, false for everything else, including when
     *   either is an ancestor of the other.
     */
    internal fun isTargetDrawnFirst(targetId: Int, otherId: Int): Boolean {
        var nodeA = layoutNodes[targetId] ?: return false
        var nodeB = layoutNodes[otherId] ?: return false

        if (nodeA.depth == 0 || nodeB.depth == 0) {
            // Fail early when either is Root
            return false
        }

        while (nodeA.depth > nodeB.depth) {
            nodeA = nodeA.parent ?: return false // Early check avoids these null parent cases
        }

        if (nodeA === nodeB) {
            // Node B was parent of Node A
            return false
        }

        while (nodeB.depth > nodeA.depth) {
            nodeB = nodeB.parent ?: return false
        }

        if (nodeA === nodeB) {
            // Node A was parent of Node B
            return false
        }

        // Keep track of the branching parent for both nodes, the draw order of these decide how
        // [targetId] and [otherId] compare.
        var lastParentA: LayoutNode = nodeA
        var lastParentB: LayoutNode = nodeB

        while (nodeA !== nodeB) {
            lastParentA = nodeA
            lastParentB = nodeB
            // Null parent means we went past the root without finding common ancestor, shouldn't
            // happen but we only return true when we know the target is drawn first
            nodeA = nodeA.parent ?: return false
            nodeB = nodeB.parent ?: return false
        }

        // Lower zIndex is drawn first, otherwise lower placeOrder decides draw order
        if (lastParentA.measurePassDelegate.zIndex == lastParentB.measurePassDelegate.zIndex) {
            return lastParentA.placeOrder < lastParentB.placeOrder
        } else {
            return lastParentA.measurePassDelegate.zIndex < lastParentB.measurePassDelegate.zIndex
        }
    }

    /**
     * Traverses the [RectList] to find the most suitable focusable node matching the given rect
     * ([left], [top], [right], [bottom]).
     *
     * Returns the best candidate node that:
     * - Is focusable
     * - Is not currently focused
     * - Is a descendant of the container ([containerId])
     * - Intersects with the given rect ([left], [top], [right], [bottom])
     * - Appears as high as possible in the node hierarchy
     *
     * Returns `null` if:
     * - No matching focusable node is found, or
     * - The given rect intersects a node that is already focused (regardless of its position in the
     *   hierarchy)
     *
     * **Note:** If there are multiple focusable modifier nodes inside the given container node,
     * only the first one in the chain will have an effect. Subsequent focusable modifiers will be
     * ignored.
     *
     * @param containerId the container layout node semantic id that we want to restrict our search
     *   to.
     * @return The most relevant focusable node, or `null` if none is applicable.
     */
    internal fun findFocusableNodeFromRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        containerId: Int,
    ): FocusTargetModifierNode? {
        val container = layoutNodes[containerId] ?: return null
        val currentlyFocusedId =
            container
                .requireOwner()
                .focusOwner
                .activeFocusTargetNode
                ?.requireSemanticsInfo()
                ?.semanticsId ?: -1

        var bestTarget: FocusTargetModifierNode? = null
        var bestDepth = Int.MAX_VALUE

        rects.forEachFocusableIntersection(left, top, right, bottom) { semanticsId ->
            val node = layoutNodes[semanticsId]
            if (node != null) {
                // If we are still intersecting the currently focused node, do not change focus.
                if (currentlyFocusedId == semanticsId && currentlyFocusedId != -1) return null
                // We want to find the "highest" focusable node that intersects, which means the
                // smallest depth. We also want to constrain the results to only those which are
                // inside of the container.
                if (node.depth < bestDepth && node.isDescendantOf(container)) {
                    val target = node.nodes.head(Nodes.FocusTarget)
                    if (target != null && target.intersects(left, top, right, bottom)) {
                        bestTarget = target
                        bestDepth = node.depth
                    }
                }
            }
        }

        return bestTarget
    }

    /**
     * The boundaries of a Modifier.Node may differ from those of its LayoutNode (e.g., due to
     * modifiers like `offset()` or `padding()`).
     *
     * This method checks whether the actual global coordinates of the Modifier.Node intersect with
     * the given rectangle.
     *
     * @return `true` if the Modifier.Node intersects the given coordinates, or if it shares
     *   coordinates with its LayoutNode.
     */
    internal fun DelegatableNode.intersects(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        val coordinator = requireCoordinator(Nodes.FocusTarget)
        val layout = coordinator.layoutNode

        // The LayoutNode's intersection has already been checked earlier.
        // If this ModifierNode uses the same coordinator, we can skip it.
        if (coordinator == layout.outerCoordinator) {
            return true
        }

        // Get the local position of the modifier node (relative to its LayoutNode).
        val localTopLeft = layout.outerCoordinator.localPositionOf(coordinator)
        // Convert the local position to global (relative to the root).
        val topLeft = layout.outerCoordinator.localToRoot(localTopLeft)
        val size = coordinator.size

        val containerLeft: Int = topLeft.x.fastRoundToInt()
        val containerRight: Int = containerLeft + size.width
        val containerTop: Int = topLeft.y.fastRoundToInt()
        val containerBottom: Int = containerTop + size.height

        // Intersection.
        return left < containerRight &&
            right > containerLeft &&
            top < containerBottom &&
            bottom > containerTop
    }

    internal fun LayoutNode.isDescendantOf(container: LayoutNode): Boolean {
        val ups = this.depth - container.depth
        if (ups <= 0) return false // node has higher or equal depth than container
        var node = this
        repeat(ups) { node = node.parent ?: return false }
        return node === container
    }
}

/**
 * Returns true if the offset is not IntOffset.Max. In this class we are using `IntOffset.Max` to be
 * a sentinel value for "unspecified" so that we can avoid boxing.
 */
private val IntOffset.isSet: Boolean
    get() = this != IntOffset.Max

/**
 * We have logic that looks at whether or not a Matrix is an identity matrix, in which case we avoid
 * doing expensive matrix calculations. Additionally, even if the matrix is non-identity, we can
 * avoid a lot of extra work if the matrix is only doing translations, and no rotations/skews/scale.
 *
 * Since checking for these conditions involves a lot of overlapping work, we have this bespoke
 * function which will return an Int that encodes the answer to both questions. If the 2nd bit of
 * the result is set, this means that there are no rotations/skews/scales. If the first bit of the
 * result is set, it means that there are no translations.
 *
 * This also means that the result of `0b11` indicates that it is the identity matrix.
 */
private fun Matrix.analyzeComponents(): Int {
    // See top-level comment
    val v = values
    if (v.size < 16) return 0
    val isIdentity3x3 =
        v[0] == 1f &&
            v[1] == 0f &&
            v[2] == 0f &&
            v[4] == 0f &&
            v[5] == 1f &&
            v[6] == 0f &&
            v[8] == 0f &&
            v[9] == 0f &&
            v[10] == 1f

    // translation components
    val hasNoTranslationComponents = v[12] == 0f && v[13] == 0f && v[14] == 0f && v[15] == 1f

    return isIdentity3x3.toInt() shl 1 or hasNoTranslationComponents.toInt()
}

@Suppress("NOTHING_TO_INLINE")
private inline val Int.isIdentity: Boolean
    get() = this == 0b11

@Suppress("NOTHING_TO_INLINE")
private inline val Int.hasNonTranslationComponents: Boolean
    get() = this and 0b10 == 0

@Suppress("NOTHING_TO_INLINE") internal inline fun Boolean.toInt(): Int = if (this) 1 else 0

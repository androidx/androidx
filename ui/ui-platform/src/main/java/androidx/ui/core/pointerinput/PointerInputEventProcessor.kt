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

package androidx.ui.core.pointerinput

import androidx.ui.core.ComponentNode
import androidx.ui.core.ConsumedData
import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputNode
import androidx.ui.core.PxPosition
import androidx.ui.core.Timestamp
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.toOffset
import androidx.ui.engine.geometry.Rect

/**
 * The core element that receives [PointerInputEvent]s and process them in Compose UI.
 */
internal class PointerInputEventProcessor(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker()
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()

    /**
     * Receives [PointerInputEvent]s and process them through the tree rooted on [root].
     */
    fun process(pointerEvent: PointerInputEvent) {

        // Gets a new PointerInputChangeEvent with the PointerInputEvent.
        val pointerInputChangeEvent = pointerInputChangeEventProducer.produce(pointerEvent)

        // Add new hit paths to the tracker due to down events.
        pointerInputChangeEvent.changes.filter { it.changedToDownIgnoreConsumed() }.forEach {
            val hitResult: MutableList<PointerInputNode> = mutableListOf()
            root.hitTest(
                it.current.position!!,
                Rect.largest,
                hitResult
            )
            hitPathTracker.addHitPath(it.id, hitResult.reversed())
        }

        // Remove PointerInputNodes that are no longer valid and refresh the offset information for
        // those that are.
        hitPathTracker.refreshPathInformation()

        // Dispatch the PointerInputChanges to the hit PointerInputNodes.
        var changes = pointerInputChangeEvent.changes
        hitPathTracker.apply {
            changes = dispatchChanges(changes, PointerEventPass.InitialDown, PointerEventPass.PreUp)
            changes = dispatchChanges(changes, PointerEventPass.PreDown, PointerEventPass.PostUp)
            dispatchChanges(changes, PointerEventPass.PostDown)
        }

        // Remove hit paths from the tracker due to up events.
        pointerInputChangeEvent.changes.filter { it.changedToUpIgnoreConsumed() }.forEach {
            hitPathTracker.removePointerId(it.id)
        }
    }

    /**
     * Searches for [PointerInputNode]s among the [ComponentNode]'s descendants, determines if the
     * [point] is within their virtual bounds, and adds them to [hitPointerInputNodes] if they are.
     *
     * This method actually just recursively searches for PointerInputNodes among the descendants
     * of the ComponentNode in a DFS in a reverse child order (so children that will be drawn on top
     * of their siblings will be checked first) and calls [hitTest] on them when found. If that
     * method returns true, it stops searching so that other PointerInputNodes that are drawn under
     * the hit PointerInputNode can't also be hit.
     */
    private fun ComponentNode.hitTest(
        point: PxPosition,
        maxBoundingBox: Rect,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ): HitTestBoundingBoxResult {

        // Step 1: If we are a layout node, decrease the hittable box to be what was passed to us
        // intersected with our bounds.  This prevents hits from occurring outside of LayoutNode
        // bounds among the descendants of LayoutNode.

        val newMaxBoundingBox =
            if (this is LayoutNode) {
                val layoutNodeRect = Rect(
                    0f,
                    0f,
                    this.width.value.toFloat(),
                    this.height.value.toFloat()
                )
                val intersectingBoundingBox = maxBoundingBox.intersect(layoutNodeRect)
                // If the point is not inside the new max bounding box, it won't hit any of our
                // children so there is no point in looking any further.  Return early with
                // the same intersecting bounding box (which is our size intersected with the max
                // bounding box we were given).
                if (!intersectingBoundingBox.contains(point.toOffset())) {
                    return HitTestBoundingBoxResult(layoutNodeRect, false)
                } else {
                    intersectingBoundingBox
                }
            } else {
                maxBoundingBox
            }

        // Step 2: Traverse down the hierarchy into our children and then back out with the
        // result of the traversal.  The outcome will either be that we hit a PointerInputNode, at
        // which point we can quickly backtrack out of the tree traversal, or we didn't hit a leaf
        // PointerInputNode yet, in which case we have a bounding box to use to do hit testing
        // against if we, or a ancestor is a PointerInputNode.

        var hitDescendantPointerInputNode = false
        var overarchingBoundingBox: Rect? = null
        // TODO(shepshapard): This visitChildren use is ugly since once we successfully hit a child
        // we have to continue to loop through the rest of the children event though we don't
        // actually need to.  Figure out a better call here.
        visitChildrenReverse { child ->
            if (!hitDescendantPointerInputNode) {

                val result =
                    if (child is LayoutNode) {
                        // If the child is a LayoutNode, offset the point and bounding box to be
                        // relative to the LayoutNode's (0,0), and then when we get a result, offset
                        // back to our (0,0).
                        val resultRelativeToChild = child.hitTest(
                            PxPosition(point.x - child.x, point.y - child.y),
                            newMaxBoundingBox.translate(
                                -child.x.value.toFloat(),
                                -child.y.value.toFloat()
                            ),
                            hitPointerInputNodes
                        )
                        HitTestBoundingBoxResult(
                            resultRelativeToChild.boundingBox?.translate(
                                child.x.value.toFloat(),
                                child.y.value.toFloat()
                            ),
                            resultRelativeToChild.hit
                        )
                    } else {
                        child.hitTest(point, newMaxBoundingBox, hitPointerInputNodes)
                    }

                hitDescendantPointerInputNode = result.hit

                // If this is not a LayoutNode and we haven't hit a leaf PointerInputNode, then we
                // should build up the layout node bounding box that may be used for an ancestor
                // PointerInputNodes to test for hit testing.
                //
                // If we are a layout node, we will just return our size (intersected with the max
                // bounding box provided to us so we don't care about some other bounding box).
                //
                // If we have hit a leaf PointerInputNode, we will quickly back track and also hit
                // all ancestor PointerInputNodes.
                if (this !is LayoutNode && !hitDescendantPointerInputNode) {
                    overarchingBoundingBox =
                        overarchingBoundingBox.expandToInclude(result.boundingBox)
                }
            }
        }

        // Step 3: Return up the traversal with a result. If we are a PointerInputNode, we either
        // have already hit a descendant PointerInputNode, or can test to see if we might hit
        // our self, and if either are true we add ourselves to the list of hit PointerInputNodes
        // and start/continue the fast backtrack. Otherwise, we may know that we have hit a
        // PointerInputNode and thus can continue the quick backtrack.  Or we may not have hit
        // anything yet, and thus need to pass back up a bounding box.

        if (this is PointerInputNode &&
            (hitDescendantPointerInputNode ||
                    overarchingBoundingBox?.contains(point.toOffset()) == true)
        ) {
            // If this is a PointerInputNode and we know we hit (either because we know we hit a
            // descendant PointerInputNode, or we just determined that we were hit, add us and
            // continue or start the fast backtrack.
            hitPointerInputNodes.add(this)
            return HitTestBoundingBoxResult(null, true)
        } else if (hitDescendantPointerInputNode) {
            // If we hit a descendant PointerInputNode, continue the fast back track.
            return HitTestBoundingBoxResult(null, true)
        } else {
            // We haven't hit anything yet so return a bounding box. If we are a LayoutNode,
            // return the newMaxBoundingBox (which is our box intersected with the passed in
            // bounding box). If we aren't a layout node, return the overarchingBoundingBox, which
            // is the box around any previously returned LayoutNode bounding boxes.
            return HitTestBoundingBoxResult(
                if (this is LayoutNode) newMaxBoundingBox else overarchingBoundingBox,
                false
            )
        }
    }
}

private data class HitTestBoundingBoxResult(val boundingBox: Rect?, val hit: Boolean)

/**
 * Produces [PointerInputChangeEvent]s by tracking changes between [PointerInputEvent]s
 */
private class PointerInputChangeEventProducer {
    private val previousPointerInputData: MutableMap<Int, PointerInputData> = mutableMapOf()

    internal fun produce(pointerEvent: PointerInputEvent):
            PointerInputChangeEvent {
        val changes: MutableList<PointerInputChange> = mutableListOf()
        pointerEvent.pointers.forEach {
            changes.add(
                PointerInputChange(
                    it.id,
                    it.pointerInputData,
                    previousPointerInputData[it.id] ?: PointerInputData(),
                    ConsumedData()
                )
            )
            if (it.pointerInputData.down) {
                previousPointerInputData[it.id] = it.pointerInputData
            } else {
                previousPointerInputData.remove(it.id)
            }
        }
        return PointerInputChangeEvent(pointerEvent.timestamp, changes)
    }
}

// TODO(shepshapard): The timestamp property probably doesn't need to exist (and therefore, nor does
// this class, but going to wait to refactor it out till after things like API review to avoid
// thrashing.
private data class PointerInputChangeEvent(
    val timestamp: Timestamp,
    val changes: List<PointerInputChange>
)

/**
 * Same as [Rect.expandToInclude] but allows either parameter to be null.
 *
 * If either parameter is null, the opposite parameter is returned such that a null Rect can be
 * expanded by a nonnull Rect such that the result is the nonnull Rect, and a nonnull Rect that
 * is expanded by a null Rect, will just return itself.
 */
private fun Rect?.expandToInclude(other: Rect?) =
    when {
        other == null -> this
        this == null -> other
        else -> this.expandToInclude(other)
    }
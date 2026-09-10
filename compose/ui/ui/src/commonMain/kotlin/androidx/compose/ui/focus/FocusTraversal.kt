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

package androidx.compose.ui.focus

import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusRequester.Companion.Redirect
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitChildren
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl

/**
 * Search up the component tree for any parent/parents that have specified a custom focus order.
 * Allowing parents higher up the hierarchy to overwrite the focus order specified by their
 * children.
 *
 * @param focusDirection the focus direction passed to [FocusManager.moveFocus] that triggered this
 *   focus search.
 * @param layoutDirection the current system [LayoutDirection].
 */
internal fun FocusTargetNode.customFocusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection,
): FocusRequester {
    val focusProperties = fetchFocusProperties()
    return when (focusDirection) {
        Next -> focusProperties.next
        Previous -> focusProperties.previous
        Up -> focusProperties.up
        Down -> focusProperties.down
        Left ->
            when (layoutDirection) {
                Ltr -> focusProperties.start
                Rtl -> focusProperties.end
            }.takeUnless { it === Default } ?: focusProperties.left
        Right ->
            when (layoutDirection) {
                Ltr -> focusProperties.end
                Rtl -> focusProperties.start
            }.takeUnless { it === Default } ?: focusProperties.right
        // TODO(b/183746982): add focus order API for "In" and "Out".
        //  Developers can to specify a custom "In" to specify which child should be visited when
        //  the user presses dPad center. (They can also redirect the "In" to some other item).
        //  Developers can specify a custom "Out" to specify which composable should take focus
        //  when the user presses the back button.
        Enter,
        Exit -> {
            val scope = CancelIndicatingFocusBoundaryScope(focusDirection)
            with(focusProperties) {
                val focusOwner = requireOwner().focusOwner
                val activeNodeBefore = focusOwner.activeFocusTargetNode
                if (focusDirection == Enter) {
                    scope.onEnter()
                } else {
                    scope.onExit()
                }
                if (scope.isCanceled) {
                    Cancel
                } else if (activeNodeBefore !== focusOwner.activeFocusTargetNode) {
                    Redirect
                } else {
                    Default
                }
            }
        }
        else -> error("invalid FocusDirection")
    }
}

/**
 * Moves focus based on the requested focus direction.
 *
 * @param focusDirection The requested direction to move focus.
 * @param layoutDirection Whether the layout is RTL or LTR.
 * @param previouslyFocusedRect The bounds of the previously focused item.
 * @param onFound This lambda is invoked if focus search finds the next focus node.
 * @return if no focus node is found, we return false. If we receive a cancel, we return null
 *   otherwise we return the result of [onFound].
 */
internal fun FocusTargetNode.focusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection,
    previouslyFocusedRect: Rect?,
    onFound: (FocusTargetNode) -> Boolean,
): Boolean? {
    return when (focusDirection) {
        Next,
        Previous -> oneDimensionalFocusSearch(focusDirection, onFound)
        Left,
        Right,
        Up,
        Down -> twoDimensionalFocusSearch(focusDirection, previouslyFocusedRect, onFound)
        Enter -> {
            // we search among the children of the active item.
            val direction =
                when (layoutDirection) {
                    Rtl -> Left
                    Ltr -> Right
                }
            findActiveFocusNode()
                ?.twoDimensionalFocusSearch(direction, previouslyFocusedRect, onFound)
        }
        Exit ->
            findActiveFocusNode()?.findNonDeactivatedParent().let {
                if (it == null || it == this) false else onFound.invoke(it)
            }
        else -> error("Focus search invoked with invalid FocusDirection $focusDirection")
    }
}

/**
 * Returns the focus area defined by the [FocusProperties] that are applied on this
 * [FocusTargetNode] in root coordinates. By default if there nothing applied by the
 * [FocusProperties], this function returns the bounding box of the node.
 *
 * If the node hasn't had a layout, it just returns [Rect.Zero].
 */
internal fun FocusTargetNode.focusRect(): Rect {
    if (!isAttached) return Rect.Zero
    val rootCoordinates =
        coordinator?.findRootCoordinates()?.takeIf { it.isAttached } ?: return Rect.Zero

    return fetchFocusRect(rootCoordinates)
}

/** Whether this node should be considered when searching for the next item during a traversal. */
internal val FocusTargetNode.isEligibleForFocusSearch: Boolean
    get() =
        coordinator?.layoutNode?.isPlaced == true &&
            coordinator?.layoutNode?.isAttached == true &&
            !isOccludedByInteractionBarrier()

internal val FocusTargetNode.activeChild: FocusTargetNode?
    get() {
        if (!node.isAttached) return null
        visitChildren(Nodes.FocusTarget) {
            if (!it.node.isAttached) return@visitChildren
            when (it.focusState) {
                Active,
                ActiveParent,
                Captured -> return it
                Inactive -> return@visitChildren
            }
        }
        return null
    }

internal fun FocusTargetNode.findActiveFocusNode(): FocusTargetNode? {
    val activeNode = requireOwner().focusOwner.activeFocusTargetNode
    return if (activeNode != null && activeNode.isAttached) activeNode else null
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun FocusTargetNode.findNonDeactivatedParent(): FocusTargetNode? {
    visitAncestors(Nodes.FocusTarget) { if (it.fetchFocusProperties().canFocus) return it }
    return null
}

internal fun FocusTargetNode.isOccludedByInteractionBarrier(): Boolean {
    val myLayoutNode = coordinator?.layoutNode ?: return false
    val owner = myLayoutNode.owner ?: return false
    val activeBarriers = owner.focusOwner.activeInteractionBarriers ?: return false
    if (activeBarriers.isEmpty()) return false

    val rootCoords = owner.root.coordinates
    val myBoundsInRoot =
        coordinator?.let {
            if (it.isAttached) rootCoords.localBoundingBoxOf(it, false) else return false
        } ?: return false

    for (i in 0 until activeBarriers.size) {
        val barrierNode = activeBarriers[i]
        if (!barrierNode.isAttached) continue
        val barrierLayoutNode = barrierNode.coordinator?.layoutNode ?: continue
        if (!barrierLayoutNode.isPlaced) continue
        if (barrierLayoutNode === myLayoutNode) continue

        // Fast-exit: if barrier doesn't contain focus target bounds, it cannot occlude it
        val barrierBoundsInRoot =
            barrierLayoutNode.coordinates.let {
                if (it.isAttached) rootCoords.localBoundingBoxOf(it, false) else return false
            }
        if (!barrierBoundsInRoot.contains(myBoundsInRoot)) {
            continue
        }

        // Check if barrier is ancestor
        val lca = findLca(myLayoutNode, barrierLayoutNode) ?: continue
        if (lca === barrierLayoutNode) {
            // Barrier is ancestor, does not occlude me
            continue
        }

        // Compare z-order at LCA level
        val childFocus = findChildOfLca(lca, myLayoutNode) ?: continue
        val childBarrier = findChildOfLca(lca, barrierLayoutNode) ?: continue

        val zSorted = lca.zSortedChildren
        val indexFocus = zSorted.indexOf(childFocus)
        val indexBarrier = zSorted.indexOf(childBarrier)

        if (indexBarrier > indexFocus) {
            return true
        }
    }

    return false
}

private fun findLca(node1: LayoutNode, node2: LayoutNode): LayoutNode? {
    var depth1 = 0
    var curr1: LayoutNode? = node1
    while (curr1 != null) {
        depth1++
        curr1 = curr1.parent
    }

    var depth2 = 0
    var curr2: LayoutNode? = node2
    while (curr2 != null) {
        depth2++
        curr2 = curr2.parent
    }

    curr1 = node1
    curr2 = node2

    while (depth1 > depth2) {
        curr1 = curr1?.parent
        depth1--
    }
    while (depth2 > depth1) {
        curr2 = curr2?.parent
        depth2--
    }

    while (curr1 != null && curr2 != null && curr1 !== curr2) {
        curr1 = curr1.parent
        curr2 = curr2.parent
    }

    return if (curr1 === curr2) curr1 else null
}

private fun findChildOfLca(lca: LayoutNode, descendant: LayoutNode): LayoutNode? {
    var curr = descendant
    while (curr.parent != null && curr.parent !== lca) {
        curr = curr.parent!!
    }
    return if (curr.parent === lca) curr else null
}

private operator fun Rect.contains(other: Rect): Boolean {
    return left <= other.left && right >= other.right && top <= other.top && bottom >= other.bottom
}

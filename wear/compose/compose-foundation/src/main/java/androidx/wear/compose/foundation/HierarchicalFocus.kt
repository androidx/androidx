/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager

/**
 * [hierarchicalFocusGroup] is used to annotate composables in an application, so we can keep track
 * of what is the active part of the composition. In turn, this is used to coordinate focus in a
 * declarative way, requesting focus when needed, as the user navigates through the app (such as
 * between screens or between pages within a screen). In most cases, this is automatically handled
 * by Wear Compose components and no action is necessary. In particular this is done by
 * [BasicSwipeToDismissBox], [HorizontalPager], [VerticalPager] and PickerGroup. This modifier is
 * useful if you implement a custom component that needs to direct focus to one of several children,
 * like a custom Pager, a Tabbed layout, etc.
 *
 * [hierarchicalFocusGroup]s can be nested to form a focus tree, with an implicit root. For sibling
 * [hierarchicalFocusGroup]s, only one should have active = true. Within the focus tree, components
 * that need to request focus can do so using [Modifier.requestFocusOnHierarchyActive]. Note that
 * ScalingLazyColumn and TransformingLazyColumn are using it already, so there is no need to add it
 * explicitly.
 *
 * When focus changes, the focus tree is examined and the topmost (closest to the root of the tree)
 * [requestFocusOnHierarchyActive] which has all its [hierarchicalFocusGroup] ancestors with active
 * = true will request focus. If no such [requestFocusOnHierarchyActive] exists, the focus will be
 * cleared.
 *
 * NOTE: This shouldn't be used together with [FocusRequester.requestFocus] calls in
 * [LaunchedEffect].
 *
 * Example usage:
 *
 * @sample androidx.wear.compose.foundation.samples.HierarchicalFocusSample
 *
 * Sample using nested [hierarchicalFocusGroup]:
 *
 * @sample androidx.wear.compose.foundation.samples.HierarchicalFocus2Levels
 * @param active Pass true when this sub tree of the focus tree is active and may require the
 *   focus - otherwise, pass false. For example, a pager can apply this modifier to each page's
 *   content with a call to [hierarchicalFocusGroup], marking only the current page as active.
 */
public fun Modifier.hierarchicalFocusGroup(active: Boolean): Modifier {
    return this.then(
        HierarchicalFocusCoordinatorModifierElement(
            active = active,
            activeFocus = false,
            onFocusChanged = null,
        )
    )
}

/**
 * This Modifier is used in conjunction with [hierarchicalFocusGroup] and will request focus on the
 * following focusable element when needed (i.e. this needs to be before that element in the
 * Modifier chain). The focusable element is usually a [Modifier.rotaryScrollable] (or, in some
 * rarer cases a [Modifier.focusable] or [Modifier.focusTarget])
 *
 * Multiple [requestFocusOnHierarchyActive] Modifiers shouldn't be siblings, in those cases they
 * need to surround each with a [hierarchicalFocusGroup], and at most one of them should have active
 * = true, to inform which [requestFocusOnHierarchyActive] should get the focus.
 *
 * NOTE: This shouldn't be used together with [FocusRequester.requestFocus] calls in
 * [LaunchedEffect].
 *
 * Example usage:
 *
 * @sample androidx.wear.compose.foundation.samples.HierarchicalFocusSample
 */
public fun Modifier.requestFocusOnHierarchyActive(): Modifier =
    this.then(
        HierarchicalFocusCoordinatorModifierElement(
            active = true,
            activeFocus = true,
            onFocusChanged = null,
        )
    )

private const val HFCTraversalKey = "HFCTraversalKey"

// Only used to support backwards compatibility, and to avoid marking more private APIs as internal
internal fun Modifier.hierarchicalOnFocusChanged(onFocusChanged: (Boolean) -> Unit): Modifier =
    this.then(
        HierarchicalFocusCoordinatorModifierElement(
            active = true,
            activeFocus = true,
            onFocusChanged = onFocusChanged,
        )
    )

private class HierarchicalFocusCoordinatorModifierElement(
    val active: Boolean,
    val activeFocus: Boolean,
    val onFocusChanged: ((Boolean) -> Unit)?,
) : ModifierNodeElement<HierarchicalFocusCoordinatorModifierNode>() {
    override fun create(): HierarchicalFocusCoordinatorModifierNode =
        HierarchicalFocusCoordinatorModifierNode(active, activeFocus, onFocusChanged)

    override fun update(node: HierarchicalFocusCoordinatorModifierNode) {
        node.onFocusChanged = onFocusChanged
        node.activeFocus = activeFocus
        node.active = active
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ModifierNodeElement"
    }

    override fun equals(other: Any?) =
        other is HierarchicalFocusCoordinatorModifierElement &&
            active == other.active &&
            activeFocus == other.activeFocus &&
            onFocusChanged === other.onFocusChanged

    override fun hashCode() =
        (active.hashCode() * 31 + onFocusChanged.hashCode()) * 31 + activeFocus.hashCode()
}

private class HierarchicalFocusCoordinatorModifierNode(
    active: Boolean,
    var activeFocus: Boolean,
    var onFocusChanged: ((Boolean) -> Unit)?,
) :
    Modifier.Node(),
    TraversableNode,
    CompositionLocalConsumerModifierNode,
    FocusRequesterModifierNode {
    override val traverseKey = HFCTraversalKey

    var active: Boolean = active
        set(value) {
            if (value != this.active) {
                field = value
                scheduleUpdateAfterTreeSettles()
            }
        }

    override fun onAttach() {
        super.onAttach()
        scheduleUpdateAfterTreeSettles()
    }

    override fun onDetach() {
        super.onDetach()
        if (lastActiveNodePath.remove(this)) {
            // This was the part of the active node chain, and it's now leaving the composition.
            onFocusChanged?.invoke(false)
            // No need to clear focus, if the focused node is gone.
        }
    }

    private fun scheduleUpdateAfterTreeSettles() {
        changedNodes.add(this)
        sideEffect {
            // Once all changes are applied, process the potential candidates and see if changes to
            // focus need to be made. We do this once, on the first sideEffect that triggers.
            if (changedNodes.isNotEmpty()) {
                // Ensure that the last active node is a candidate, since when we add/remove top
                // level HFCSelectors they will otherwise trigger the wrong callbacks (we will
                // reach the innermost block, with an empty nextActiveNodePath).
                lastActiveNodePath.lastOrNull()?.let { node -> changedNodes.add(node) }

                val parentActiveNodes =
                    changedNodes.fastFilter { it.isAttached && it.parentChainActive() }

                // We only care about changes in the active part of the tree.
                if (parentActiveNodes.isNotEmpty()) {
                    val nextActiveNodePath =
                        parentActiveNodes.fastFirstOrNull { it.active }?.findActive()?.parentChain()
                            ?: emptyList()

                    if (nextActiveNodePath != lastActiveNodePath) {
                        // Note that we assume the lists to be small (less than 5 elements, if this
                        // proves not the be the case, we can do something fancier (like assigning
                        // ids to each node, sorting and merging)
                        var focusSet = false
                        nextActiveNodePath.fastForEach { node ->
                            if (!lastActiveNodePath.contains(node)) {
                                // Gaining focus
                                if (node.activeFocus) {
                                    if (!focusSet) {
                                        node.onFocusChanged?.invoke(true) ?: node.requestFocus()
                                        focusSet = true
                                    }
                                } else {
                                    node.onFocusChanged?.invoke(true)
                                }
                            }
                        }

                        lastActiveNodePath.fastForEach { node ->
                            if (!nextActiveNodePath.contains(node)) {
                                // Losing focus
                                node.onFocusChanged?.invoke(false)
                            }
                        }

                        if (!focusSet) currentValueOf(LocalFocusManager).clearFocus()

                        lastActiveNodePath = nextActiveNodePath.toMutableList()
                    }
                }
                changedNodes.clear()
            }
        }
    }

    // Returns true iff all ancestors up to and including root are active, not including this node.
    private fun parentChainActive(): Boolean {
        var node: HierarchicalFocusCoordinatorModifierNode? = this
        while (node != null) {
            node = node.findNearestAncestor()
            if (node?.active == false) return false
        }
        return true
    }

    // Returns the path of nodes to the given one.
    private fun parentChain(): List<HierarchicalFocusCoordinatorModifierNode> =
        (findNearestAncestor()?.parentChain() ?: emptyList()) + this

    // Search on the subtree rooted at this node the first node that are active and has a
    // path of active nodes up to and including this node.
    // If this node has no children, it returns itself. If all children of this node have
    // active == false, return null
    private fun findActive(): HierarchicalFocusCoordinatorModifierNode? {
        var hasChildren = false
        var activeNode: HierarchicalFocusCoordinatorModifierNode? = null
        traverseDescendants {
            hasChildren = true
            if (it.active && it.isAttached) {
                activeNode = it.findActive()
                TraverseDescendantsAction.CancelTraversal
            } else {
                TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
            }
        }
        return if (!hasChildren) return this else activeNode
    }

    companion object {
        private var lastActiveNodePath: MutableList<HierarchicalFocusCoordinatorModifierNode> =
            mutableListOf()
        private val changedNodes: MutableList<HierarchicalFocusCoordinatorModifierNode> =
            mutableListOf()
    }
}

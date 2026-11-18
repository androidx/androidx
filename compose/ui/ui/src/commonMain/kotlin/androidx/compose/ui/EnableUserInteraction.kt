/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Configures whether user interaction is enabled for this component and its entire descendant
 * subtree.
 *
 * When [enabled] is `false`, user interaction with this component and its descendants is disabled:
 * - **Pointer input:** Touch, mouse, stylus, and hover events pass through this subtree during hit
 *   testing. Any active pointer gesture within this subtree when [enabled] becomes `false` is
 *   immediately cancelled.
 * - **Accessibility:** This component and its descendants are pruned from the accessibility tree
 *   and skipped during accessibility hit testing so screen readers cannot focus or announce them,
 *   while their [SemanticsNode][androidx.compose.ui.semantics.SemanticsNode]s remain intact in the
 *   Compose semantics hierarchy for UI testing and layout inspection.
 * - **Content capture and autofill:** Elements in this subtree that remain visible on screen
 *   continue to be reported to content capture, and autofill structures report fields in this
 *   subtree as disabled, while semantics-driven actions (such as live translation and
 *   `performAutofill`) are not triggered on them.
 *
 * Use `enableUserInteraction(false)` on background content or inactive scenes/layers when
 * presenting a custom modal overlay (such as a dialog, bottom sheet, navigation drawer, or active
 * window/scene in a multi-layer container), or to temporarily disable input across a UI section
 * while loading or animating without threading an `enabled` parameter into every child component.
 *
 * This modifier operates strictly on the composition hierarchy (this node and its descendants)
 * rather than geometric overlap:
 * - Sibling composables drawn behind or in front of this node are unaffected; both pointer hit
 *   testing and screen-reader discovery pass through a disabled subtree without occluding content
 *   behind it.
 * - Content hosted in a separate window (such as `Popup` or `Dialog`) has its own layout hierarchy
 *   and is not controlled by an ancestor's `enableUserInteraction`.
 * - Apply `enableUserInteraction` to whole interactive components or containers rather than
 *   individual children inside a `semantics(mergeDescendants = true)` parent, as merged semantics
 *   configurations continue to include non-clearing descendants.
 *
 * Setting [enabled] to `true` (the default behavior when this modifier is absent) has no effect on
 * its own and cannot re-enable interaction inside an ancestor where `enableUserInteraction(false)`
 * has been applied. When multiple `enableUserInteraction` modifiers are present on the same layout
 * node or along an ancestor chain, user interaction is enabled only when all of them set [enabled]
 * to `true` (for example, `Modifier.enableUserInteraction(false).enableUserInteraction(true)`
 * leaves user interaction disabled).
 *
 * @param enabled Whether user interaction is enabled for this component and its descendants.
 */
@Stable
internal fun Modifier.enableUserInteraction(enabled: Boolean): Modifier =
    this then UserInteractionElement(enabled)

private data class UserInteractionElement(val enabled: Boolean) :
    ModifierNodeElement<UserInteractionNode>() {
    override fun create(): UserInteractionNode = UserInteractionNode(enabled)

    override fun update(node: UserInteractionNode) {
        node.update(enabled)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "enableUserInteraction"
        properties["enabled"] = enabled
    }
}

internal class UserInteractionNode(var enabled: Boolean) : Modifier.Node(), SemanticsModifierNode {

    private var isCountedDisabled: Boolean = false

    override val shouldAutoInvalidate: Boolean
        get() = false

    override val isImportantForBounds: Boolean
        get() = false

    override fun onAttach() {
        super.onAttach()
        if (!enabled) {
            incrementDisabledCountIfNeeded()
            onUserInteractionDisabled()
        }
    }

    override fun onDetach() {
        decrementDisabledCountIfNeeded()
        super.onDetach()
    }

    fun update(enabled: Boolean) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (isAttached) {
                invalidateSemantics()
                if (!enabled) {
                    incrementDisabledCountIfNeeded()
                    onUserInteractionDisabled()
                } else {
                    decrementDisabledCountIfNeeded()
                }
            }
        }
    }

    private fun incrementDisabledCountIfNeeded() {
        if (!isCountedDisabled) {
            requireOwner().incrementDisabledUserInteractionCount()
            isCountedDisabled = true
        }
    }

    private fun decrementDisabledCountIfNeeded() {
        if (isCountedDisabled) {
            requireOwner().decrementDisabledUserInteractionCount()
            isCountedDisabled = false
        }
    }

    private fun onUserInteractionDisabled() {
        requireLayoutNode().cancelActivePointerInputInSubtree()
    }

    // Empty implementation forces this LayoutNode to generate a SemanticsNode,
    // ensuring accessibility traversal visits and prunes at this exact boundary.
    override fun SemanticsPropertyReceiver.applySemantics() {}
}

internal fun LayoutNode.isUserInteractionDisabled(): Boolean {
    nodes.headToTail(Nodes.UserInteraction.mask) {
        if (it is UserInteractionNode && !it.enabled) return true
    }
    return false
}

internal fun LayoutNode.isInUserInteractionDisabledSubtree(): Boolean {
    if (owner?.disabledUserInteractionCount == 0) return false
    var curr: LayoutNode? = this
    while (curr != null) {
        if (curr.isUserInteractionDisabled()) return true
        curr = curr.parent
    }
    return false
}

internal fun SemanticsNode.isInUserInteractionDisabledSubtree(): Boolean {
    val targetLayoutNode = (if (isFake) parent?.layoutNode else layoutNode) ?: return false
    return targetLayoutNode.isInUserInteractionDisabledSubtree()
}

private fun LayoutNode.cancelActivePointerInputInSubtree() {
    nodes.tailToHead(Nodes.PointerInput.mask) { node ->
        if (node.isAttached) {
            node.removeFromPointerInputTracking()
        }
    }
    forEachChild { child -> child.cancelActivePointerInputInSubtree() }
}

/**
 * Removes [this] pointer-input spine node from [androidx.compose.ui.input.pointer.HitPathTracker]
 * if it is currently part of an active hit path, dispatching pointer cancel children-first.
 *
 * `HitPathTracker.addHitPath` installs `removePointerInputModifierNode(node)` on
 * [Modifier.Node.detachedListener]. Invoking it removes the node from the tracker immediately and
 * remains an idempotent no-op if the node later detaches for real, while subsequent hits (after
 * re-enabling) re-install the listener on `addHitPath`.
 */
private fun Modifier.Node.removeFromPointerInputTracking() {
    detachedListener?.invoke()
}

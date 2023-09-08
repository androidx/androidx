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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass.Main
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.VisitSubtreeIfAction
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseSubtree
import androidx.compose.ui.node.traverseSubtreeIf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.unit.IntSize

/**
 * Represents a pointer icon to use in [Modifier.pointerHoverIcon]
 */
@Stable
interface PointerIcon {

    /**
     * A collection of common pointer icons used for the mouse cursor. These icons will be used to
     * assign default pointer icons for various widgets.
     */
    companion object {

        /** The default arrow icon that is commonly used for cursor icons. */
        val Default = pointerIconDefault

        /** Commonly used when selecting precise portions of the screen. */
        val Crosshair = pointerIconCrosshair

        /** Also called an I-beam cursor, this is commonly used on selectable or editable text. */
        val Text = pointerIconText

        /** Commonly used to indicate to a user that an element is clickable. */
        val Hand = pointerIconHand
    }
}

internal expect val pointerIconDefault: PointerIcon
internal expect val pointerIconCrosshair: PointerIcon
internal expect val pointerIconText: PointerIcon
internal expect val pointerIconHand: PointerIcon

internal interface PointerIconService {
    fun getIcon(): PointerIcon
    fun setIcon(value: PointerIcon?)
}

/**
 * Modifier that lets a developer define a pointer icon to display when the cursor is hovered over
 * the element. When [overrideDescendants] is set to true, descendants cannot override the
 * pointer icon using this modifier.
 *
 * @sample androidx.compose.ui.samples.PointerIconSample
 *
 * @param icon The icon to set
 * @param overrideDescendants when false (by default) descendants are able to set their own pointer
 * icon. If true, no descendants under this parent are eligible to change the icon (it will be set
 * to the this [the parent's] icon).
 */
@Stable
fun Modifier.pointerHoverIcon(icon: PointerIcon, overrideDescendants: Boolean = false) =
    this then PointerHoverIconModifierElement(
        icon = icon,
        overrideDescendants = overrideDescendants
    )

internal data class PointerHoverIconModifierElement(
    val icon: PointerIcon,
    val overrideDescendants: Boolean = false
) : ModifierNodeElement<PointerHoverIconModifierNode>() {
    override fun create() = PointerHoverIconModifierNode(icon, overrideDescendants)

    override fun update(node: PointerHoverIconModifierNode) {
        node.icon = icon
        node.overrideDescendants = overrideDescendants
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pointerHoverIcon"
        properties["icon"] = icon
        properties["overrideDescendants"] = overrideDescendants
    }
}

/*
 * Changes the pointer hover icon if the node is in bounds and if the node is not overridden
 * by a parent pointer hover icon node. This node implements [PointerInputModifierNode] so it can
 * listen to pointer input events and determine if the pointer has entered or exited the bounds of
 * the modifier itself.
 *
 * If the icon or overrideDescendants values are changed, this node will determine if it needs to
 * walk down and/or up the modifier chain to update those pointer hover icon modifier nodes as well.
 */
internal class PointerHoverIconModifierNode(
    icon: PointerIcon,
    overrideDescendants: Boolean = false
) : Modifier.Node(),
    TraversableNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {
    /* Traversal key used with the [TraversableNode] interface to enable all the traversing
     * functions (ancestor, child, subtree, and subtreeIf).
     */
    override val traverseKey = "androidx.compose.ui.input.pointer.PointerHoverIcon"

    var icon = icon
        set(value) {
            if (field != value) {
                field = value
                if (cursorInBoundsOfNode) {
                    displayIconIfDescendantsDoNotHavePriority()
                }
            }
        }

    var overrideDescendants = overrideDescendants
        set(value) {
            if (field != value) {
                field = value

                if (overrideDescendants) { // overrideDescendants changed from false -> true
                    // If this node or any descendants have the cursor in bounds, change the icon.
                    if (cursorInBoundsOfNode) {
                        displayIcon()
                    }
                } else { // overrideDescendants changed from true -> false
                    if (cursorInBoundsOfNode) {
                        displayIconFromCurrentNodeOrDescendantsWithCursorInBounds()
                    }
                }
            }
        }

    // Service used to actually update the icon with the system when needed.
    private val pointerIconService: PointerIconService?
        get() = currentValueOf(LocalPointerIconService)

    private var cursorInBoundsOfNode = false

    // Pointer Input callback for determining if a Pointer has Entered or Exited this node.
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == Main) {
            // Cursor within the surface area of this node's bounds
            if (pointerEvent.type == PointerEventType.Enter) {
                cursorInBoundsOfNode = true
                displayIconIfDescendantsDoNotHavePriority()
            } else if (pointerEvent.type == PointerEventType.Exit) {
                cursorInBoundsOfNode = false
                displayIconFromAncestorNodeWithCursorInBoundsOrDefaultIcon()
            }
        }
    }

    override fun onCancelPointerInput() {
        // We aren't processing the event (only listening for enter/exit), so we don't need to
        // do anything.
    }

    override fun onDetach() {
        cursorInBoundsOfNode = false
        displayIconFromAncestorNodeWithCursorInBoundsOrDefaultIcon()

        super.onDetach()
    }

    private fun displayIcon() {
        // If there are any ancestor that override this node, we must use that icon. Otherwise, we
        // use the current node's icon
        val iconToUse = findOverridingAncestorNode()?.icon ?: icon
        pointerIconService?.setIcon(iconToUse)
    }

    private fun displayDefaultIcon() {
        pointerIconService?.setIcon(null)
    }

    private fun displayIconIfDescendantsDoNotHavePriority() {
        var hasIconRightsOverDescendants = true

        if (!overrideDescendants) {
            traverseSubtree {
                // Descendant in bounds has rights to the icon (and has already set it),
                // so we ignore.
                val continueTraversal = if (it.cursorInBoundsOfNode) {
                    hasIconRightsOverDescendants = false
                    false
                } else {
                    true
                }
                continueTraversal
            }
        }

        if (hasIconRightsOverDescendants) {
            displayIcon()
        }
    }

    /*
     * Finds and returns the lowest descendant node with the cursor within its bounds (true node
     * that gets to decide the icon).
     *
     * Note: Multiple descendant nodes may have `cursorInBoundsOfNode` set to true (for when the
     * cursor enters their bounds). The lowest one is the one that is the correct node for the
     * mouse (see example for explanation).
     *
     * Example: Parent node contains a child node within its visual border (both are pointer icon
     * nodes).
     * - Mouse moves over the PARENT node triggers the pointer input handler ENTER event which sets
     * `cursorInBoundsOfNode` = `true`.
     * - Mouse moves over CHILD node triggers the pointer input handler ENTER event which sets
     * `cursorInBoundsOfNode` = `true`.
     *
     * They are both true now because the pointer input event's exit is not triggered (which would
     * set cursorInBoundsOfNode` = `false`) unless the mouse moves outside the parent node. Because
     * the child node is contained visually within the parent node, it is not triggered. That is why
     * we need to get the lowest node with `cursorInBoundsOfNode` set to true.
     */
    private fun findDescendantNodeWithCursorInBounds(): PointerHoverIconModifierNode? {
        var descendantNodeWithCursorInBounds: PointerHoverIconModifierNode? = null

        traverseSubtreeIf {
            var actionForSubtreeOfCurrentNode = VisitSubtreeIfAction.VisitSubtree

            if (it.cursorInBoundsOfNode) {
                descendantNodeWithCursorInBounds = it

                // No descendant nodes below this one are eligible to set the icon.
                if (it.overrideDescendants) {
                    actionForSubtreeOfCurrentNode = VisitSubtreeIfAction.SkipSubtree
                }
            }
            actionForSubtreeOfCurrentNode
        }

        return descendantNodeWithCursorInBounds
    }

    private fun displayIconFromCurrentNodeOrDescendantsWithCursorInBounds() {
        if (!cursorInBoundsOfNode) return

        var pointerHoverIconModifierNode: PointerHoverIconModifierNode = this

        if (!overrideDescendants) {
            findDescendantNodeWithCursorInBounds()?.let {
                pointerHoverIconModifierNode = it
            }
        }

        pointerHoverIconModifierNode.displayIcon()
    }

    private fun findOverridingAncestorNode(): PointerHoverIconModifierNode? {
        var pointerHoverIconModifierNode: PointerHoverIconModifierNode? = null

        traverseAncestors {
            if (it.overrideDescendants &&
                it.cursorInBoundsOfNode) {
                pointerHoverIconModifierNode = it
            }
            // continue traversal
            true
        }

        return pointerHoverIconModifierNode
    }

    /*
     * Sets the icon to either the ancestor where the mouse is in its bounds (or to its
     * ancestors if one overrides it) or to a default icon.
     */
    private fun displayIconFromAncestorNodeWithCursorInBoundsOrDefaultIcon() {
        var pointerHoverIconModifierNode: PointerHoverIconModifierNode? = null

        traverseAncestors {
            if (pointerHoverIconModifierNode == null && it.cursorInBoundsOfNode) {
                pointerHoverIconModifierNode = it

            // We should only assign a node that override its descendants if there was a node
            // below it where the mouse was in bounds meaning the pointerHoverIconModifierNode
            // will not be null.
            } else if (pointerHoverIconModifierNode != null &&
                it.overrideDescendants &&
                it.cursorInBoundsOfNode) {
                pointerHoverIconModifierNode = it
            }

            // continue traversal
            true
        }
        pointerHoverIconModifierNode?.displayIcon() ?: displayDefaultIcon()
    }
}

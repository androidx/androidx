/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.collection.MutableLongSet
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.CustomDestinationResult.Cancelled
import androidx.compose.ui.focus.CustomDestinationResult.None
import androidx.compose.ui.focus.CustomDestinationResult.RedirectCancelled
import androidx.compose.ui.focus.CustomDestinationResult.Redirected
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeKind
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ancestors
import androidx.compose.ui.node.dispatchForKind
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.visitLocalDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations
 * to control focus.
 */
internal class FocusOwnerImpl(
    onRequestApplyChangesListener: (() -> Unit) -> Unit,
    private val onRequestFocusForOwner:
        (focusDirection: FocusDirection?, previouslyFocusedRect: Rect?) -> Boolean,
    private val onClearFocusForOwner: () -> Unit,
    private val layoutDirection: (() -> LayoutDirection)
) : FocusOwner {

    internal var rootFocusNode = FocusTargetNode()

    private val focusInvalidationManager = FocusInvalidationManager(
        onRequestApplyChangesListener,
        ::invalidateOwnerFocusState
    )

    override val focusTransactionManager: FocusTransactionManager = FocusTransactionManager()

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    // TODO(b/168831247): return an empty Modifier when there are no focusable children.
    override val modifier: Modifier = Modifier
        // The root focus target is not focusable, and acts like a focus group.
        //  We could save an allocation here by making FocusTargetNode implement
        //  FocusPropertiesModifierNode but to do that we would have to allocate
        //  a focus properties object. This way only the root node has this extra allocation.
        .focusProperties { canFocus = false }
        .then(
            object : ModifierNodeElement<FocusTargetNode>() {
                override fun create() = rootFocusNode
                override fun update(node: FocusTargetNode) {}
                override fun InspectorInfo.inspectableProperties() { name = "RootFocusTarget" }
                override fun hashCode(): Int = rootFocusNode.hashCode()
                override fun equals(other: Any?) = other === this
            }
        )

    /**
     * This function is called to ask the owner to request focus from the framework.
     * eg. If a composable calls requestFocus and the root view does not have focus, this function
     * can be used to request focus for the view.
     *
     * @param focusDirection If this focus request was triggered by a call to moveFocus or using the
     * keyboard, provide the owner with the direction of focus change.
     *
     * @param previouslyFocusedRect The bounds of the currently focused item.
     *
     * @return true if the owner successfully requested focus from the framework. False otherwise.
     */
    override fun requestFocusForOwner(
        focusDirection: FocusDirection?,
        previouslyFocusedRect: Rect?
    ): Boolean = onRequestFocusForOwner(focusDirection, previouslyFocusedRect)

    /**
     * Keeps track of which keys have received DOWN events without UP events – i.e. which keys are
     * currently down. This is used to detect UP events for keys that aren't down and ignore them.
     *
     * This set is lazily initialized the first time a DOWN event is received for a key.
     */
    // TODO(b/307580000) Factor this state out into a class to manage key inputs.
    private var keysCurrentlyDown: MutableLongSet? = null

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] gained focus, and that it should propagate this
     * focus to one of the focus modifiers in the component hierarchy.
     *
     * @param focusDirection the direction to search for the focus target.
     *
     * @param previouslyFocusedRect the bounds of the currently focused item.
     *
     * @return true, if a suitable [FocusTargetNode] was found and it took focus, false if no
     * [FocusTargetNode] was found or if the focus search was cancelled.
     */
    override fun takeFocus(focusDirection: FocusDirection, previouslyFocusedRect: Rect?): Boolean {
        return focusTransactionManager.withExistingTransaction {
            focusSearch(focusDirection, previouslyFocusedRect) {
                it.requestFocus(focusDirection) ?: false
            } ?: false
        }
    }

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    override fun releaseFocus() {
        focusTransactionManager.withExistingTransaction {
            rootFocusNode.clearFocus(forced = true, refreshFocusEvents = true)
        }
    }

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have
     * any components that have captured focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    override fun clearFocus(force: Boolean) {
        clearFocus(force, refreshFocusEvents = true, clearOwnerFocus = true)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun clearFocus(force: Boolean, refreshFocusEvents: Boolean, clearOwnerFocus: Boolean) {
        val clearedFocusSuccessfully = focusTransactionManager.withNewTransaction(
            onCancelled = { return@withNewTransaction }
        ) {
            // Don't clear focus if an item on the focused path has a custom exit specified.
            if (!force) {
                when (rootFocusNode.performCustomClearFocus(Exit)) {
                    Redirected, Cancelled, RedirectCancelled -> return@withNewTransaction false
                    None -> { /* Do nothing. */ }
                }
            }
            return@withNewTransaction rootFocusNode.clearFocus(force, refreshFocusEvents)
        }

        if (clearedFocusSuccessfully && clearOwnerFocus) {
            onClearFocusForOwner.invoke()
        }
    }

    /**
     * Moves focus in the specified direction.
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    override fun moveFocus(focusDirection: FocusDirection): Boolean {
        // moveFocus is an API that was added to compose, but isn't available in the classic view
        // system, so for now we only search among compose items and don't support moveFocus for
        // interop scenarios.
        val movedFocus = focusSearch(focusDirection, null) {
            it.requestFocus(focusDirection) ?: false
        } ?: return false

        // To wrap focus around, we clear focus and request initial focus.
        if (!movedFocus && focusDirection.supportsWrapAroundFocus()) {
            clearFocus(force = false, refreshFocusEvents = true, clearOwnerFocus = false)
            return takeFocus(focusDirection, previouslyFocusedRect = null)
        }

        return movedFocus
    }

    override fun focusSearch(
        focusDirection: FocusDirection,
        focusedRect: Rect?,
        onFound: (FocusTargetNode) -> Boolean
    ): Boolean? {
        val source = rootFocusNode.findActiveFocusNode()?.also {
            // Check if a custom focus traversal order is specified.
            when (val customDestination = it.customFocusSearch(focusDirection, layoutDirection())) {
                @OptIn(ExperimentalComposeUiApi::class)
                Cancel -> return null
                Default -> { /* Do Nothing */ }
                else -> return customDestination.findFocusTargetNode(onFound)
            }
        }

        return rootFocusNode.focusSearch(focusDirection, layoutDirection(), focusedRect) {
            when (it) {
                source -> false
                rootFocusNode -> error("Focus search landed at the root.")
                else -> onFound(it)
            }
        }
    }

    /**
     * Dispatches a key event through the compose hierarchy.
     */
    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        check(!focusInvalidationManager.hasPendingInvalidation()) {
            "Dispatching key event while focus system is invalidated."
        }

        if (!validateKeyEvent(keyEvent)) return false

        val activeFocusTarget = rootFocusNode.findActiveFocusNode()
        val focusedKeyInputNode = activeFocusTarget?.lastLocalKeyInputNode()
            ?: activeFocusTarget?.nearestAncestor(Nodes.KeyInput)?.node
            ?: rootFocusNode.nearestAncestor(Nodes.KeyInput)?.node

        focusedKeyInputNode?.traverseAncestors(
            type = Nodes.KeyInput,
            onPreVisit = { if (it.onPreKeyEvent(keyEvent)) return true },
            onVisit = { if (it.onKeyEvent(keyEvent)) return true }
        )
        return false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun dispatchInterceptedSoftKeyboardEvent(keyEvent: KeyEvent): Boolean {
        check(!focusInvalidationManager.hasPendingInvalidation()) {
            "Dispatching intercepted soft keyboard event while focus system is invalidated."
        }

        val focusedSoftKeyboardInterceptionNode = rootFocusNode.findActiveFocusNode()
            ?.nearestAncestor(Nodes.SoftKeyboardKeyInput)

        focusedSoftKeyboardInterceptionNode?.traverseAncestors(
            type = Nodes.SoftKeyboardKeyInput,
            onPreVisit = { if (it.onPreInterceptKeyBeforeSoftKeyboard(keyEvent)) return true },
            onVisit = { if (it.onInterceptKeyBeforeSoftKeyboard(keyEvent)) return true }
        )
        return false
    }

    /**
     * Dispatches a rotary scroll event through the compose hierarchy.
     */
    override fun dispatchRotaryEvent(event: RotaryScrollEvent): Boolean {
        check(!focusInvalidationManager.hasPendingInvalidation()) {
            "Dispatching rotary event while focus system is invalidated."
        }

        val focusedRotaryInputNode = rootFocusNode.findActiveFocusNode()
            ?.nearestAncestor(Nodes.RotaryInput)

        focusedRotaryInputNode?.traverseAncestors(
            type = Nodes.RotaryInput,
            onPreVisit = { if (it.onPreRotaryScrollEvent(event)) return true },
            onVisit = { if (it.onRotaryScrollEvent(event)) return true }
        )

        return false
    }

    override fun scheduleInvalidation(node: FocusTargetNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    override fun scheduleInvalidation(node: FocusEventModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    override fun scheduleInvalidation(node: FocusPropertiesModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    /**
     * At the end of the invalidations, we need to ensure that the focus system is in a valid state.
     */
    private fun invalidateOwnerFocusState() {
        // If an active item is removed, we currently clear focus from the hierarchy. We don't
        // clear focus from the root because that could cause initial focus logic to be re-run.
        // Now that all the invalidations are complete, we run owner.clearFocus() if needed.
        if (rootFocusNode.focusState == FocusStateImpl.Inactive) {
            onClearFocusForOwner()
        }
    }

    private inline fun <reified T : DelegatableNode> DelegatableNode.traverseAncestors(
        type: NodeKind<T>,
        onPreVisit: (T) -> Unit,
        onVisit: (T) -> Unit
    ) {
        val ancestors = ancestors(type)
        ancestors?.fastForEachReversed(onPreVisit)
        node.dispatchForKind(type, onPreVisit)
        node.dispatchForKind(type, onVisit)
        ancestors?.fastForEach(onVisit)
    }

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    override fun getFocusRect(): Rect? {
        return rootFocusNode.findActiveFocusNode()?.focusRect()
    }

    override val rootState: FocusState
        get() = rootFocusNode.focusState

    private fun DelegatableNode.lastLocalKeyInputNode(): Modifier.Node? {
        var focusedKeyInputNode: Modifier.Node? = null
        visitLocalDescendants(Nodes.FocusTarget or Nodes.KeyInput) { modifierNode ->
            if (modifierNode.isKind(Nodes.FocusTarget)) return focusedKeyInputNode

            focusedKeyInputNode = modifierNode
        }
        return focusedKeyInputNode
    }

    /**
     * focus search in the Android framework wraps around for 1D focus search, but not for 2D focus
     * search. This is a helper function that can be used to determine whether we should wrap around.
     */
    private fun FocusDirection.supportsWrapAroundFocus(): Boolean = when (this) {
        Next, Previous -> true
        else -> false
    }

    // TODO(b/307580000) Factor this out into a class to manage key inputs.
    private fun validateKeyEvent(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.key.keyCode
        when (keyEvent.type) {
            KeyDown -> {
                // It's probably rare for more than 3 hardware keys to be pressed simultaneously.
                val keysCurrentlyDown = keysCurrentlyDown ?: MutableLongSet(initialCapacity = 3)
                    .also { keysCurrentlyDown = it }
                keysCurrentlyDown += keyCode
            }

            KeyUp -> {
                if (keysCurrentlyDown?.contains(keyCode) != true) {
                    // An UP event for a key that was never DOWN is invalid, ignore it.
                    return false
                }
                keysCurrentlyDown?.remove(keyCode)
            }
            // Always process Unknown event types.
        }
        return true
    }
}

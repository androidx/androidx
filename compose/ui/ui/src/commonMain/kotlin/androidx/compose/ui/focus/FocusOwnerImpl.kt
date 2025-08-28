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
import androidx.collection.MutableObjectList
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
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
import androidx.compose.ui.focus.FocusRequester.Companion.Redirect
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeKind
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.ancestors
import androidx.compose.ui.node.dispatchForKind
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitLocalDescendants
import androidx.compose.ui.node.visitSubtree
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.trace

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations to
 * control focus.
 */
internal class FocusOwnerImpl(
    private val platformFocusOwner: PlatformFocusOwner,
    private val owner: Owner,
) : FocusOwner {

    // The root focus target is not focusable, and acts like a focus group.
    internal var rootFocusNode = FocusTargetNode(focusability = Focusability.Never)

    private val focusInvalidationManager = FocusInvalidationManager(this, owner)

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier list
     * that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    // TODO(b/168831247): return an empty Modifier when there are no focusable children.
    override val modifier: Modifier =
        object : ModifierNodeElement<FocusTargetNode>() {
            override fun create() = rootFocusNode

            override fun update(node: FocusTargetNode) {}

            override fun InspectorInfo.inspectableProperties() {
                name = "RootFocusTarget"
            }

            override fun hashCode(): Int = rootFocusNode.hashCode()

            override fun equals(other: Any?) = other === this
        }

    /**
     * This function is called to ask the owner to request focus from the framework. eg. If a
     * composable calls requestFocus and the root view does not have focus, this function can be
     * used to request focus for the view.
     *
     * @param focusDirection If this focus request was triggered by a call to moveFocus or using the
     *   keyboard, provide the owner with the direction of focus change.
     * @param previouslyFocusedRect The bounds of the currently focused item.
     * @return true if the owner successfully requested focus from the framework. False otherwise.
     */
    override fun requestOwnerFocus(focusDirection: FocusDirection?, previouslyFocusedRect: Rect?) =
        platformFocusOwner.requestOwnerFocus(focusDirection, previouslyFocusedRect)

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
     * informs the [focus manager][FocusOwnerImpl] that the [Owner][androidx.compose.ui.node.Owner]
     * gained focus, and that it should propagate this focus to one of the focus modifiers in the
     * component hierarchy.
     *
     * @param focusDirection the direction to search for the focus target.
     * @param previouslyFocusedRect the bounds of the currently focused item.
     * @return true, if a suitable [FocusTargetNode] was found and it took focus, false if no
     *   [FocusTargetNode] was found or if the focus search was cancelled.
     */
    override fun takeFocus(focusDirection: FocusDirection, previouslyFocusedRect: Rect?): Boolean {
        return focusSearch(focusDirection, previouslyFocusedRect) {
            it.requestFocus(focusDirection)
        } ?: false
    }

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusOwnerImpl] that the [Owner][androidx.compose.ui.node.Owner]
     * lost focus, and that it should clear focus from all the focus modifiers in the component
     * hierarchy.
     */
    override fun releaseFocus() {
        rootFocusNode.clearFocus(forced = true, refreshFocusEvents = true)
    }

    override fun clearOwnerFocus() {
        platformFocusOwner.clearOwnerFocus()
    }

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have any
     *   components that have captured focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    override fun clearFocus(force: Boolean) {
        clearFocus(force, refreshFocusEvents = true, clearOwnerFocus = true, focusDirection = Exit)
    }

    override fun clearFocus(
        force: Boolean,
        refreshFocusEvents: Boolean,
        clearOwnerFocus: Boolean,
        focusDirection: FocusDirection,
    ): Boolean {
        val clearedFocusSuccessfully =
            if (!force) {
                // Don't clear focus if an item on the focused path has a custom exit specified.
                when (rootFocusNode.performCustomClearFocus(focusDirection)) {
                    Redirected,
                    Cancelled,
                    RedirectCancelled -> false
                    None -> clearFocus(force, refreshFocusEvents)
                }
            } else {
                clearFocus(force, refreshFocusEvents)
            }

        if (clearedFocusSuccessfully && clearOwnerFocus) {
            clearOwnerFocus()
        }
        return clearedFocusSuccessfully
    }

    // We clear focus within the compose hierarchy and request focus again to simulate
    // a default focus scenario so that focus goes to the first item.
    override fun resetFocus(focusDirection: FocusDirection): Boolean {
        val successfulClear =
            clearFocus(
                force = false,
                refreshFocusEvents = true,
                clearOwnerFocus = false,
                focusDirection = focusDirection,
            )

        if (!successfulClear) return false

        val successfulReset =
            focusSearch(focusDirection = focusDirection, focusedRect = null) {
                it.requestFocus(focusDirection)
            } ?: false

        // We called clearFocus with clearOwnerFocus = false but didn't find anything else
        // to focus on, so just clear focus from the owner.
        if (!successfulReset) clearOwnerFocus()

        return successfulReset
    }

    private fun clearFocus(forced: Boolean = false, refreshFocusEvents: Boolean): Boolean {
        if (activeFocusTargetNode == null) return true
        if (isFocusCaptured && !forced) {
            return false // Cannot clear focus if it's captured unless forced
        }
        val previousActiveFocusTargetNode = activeFocusTargetNode
        activeFocusTargetNode = null
        if (refreshFocusEvents && previousActiveFocusTargetNode != null) {
            previousActiveFocusTargetNode.dispatchFocusCallbacks(
                if (isFocusCaptured) Captured else Active,
                Inactive,
            )
            previousActiveFocusTargetNode.visitAncestors(Nodes.FocusTarget) {
                it.dispatchFocusCallbacks(ActiveParent, Inactive)
            }
        }
        return true
    }

    /**
     * Moves focus in the specified direction.
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    override fun moveFocus(focusDirection: FocusDirection): Boolean {
        // First check to see if the focus should move within child Views
        @OptIn(ExperimentalComposeUiApi::class)
        if (
            ComposeUiFlags.isViewFocusFixEnabled ||
                (ComposeUiFlags.isBypassUnfocusableComposeViewEnabled &&
                    activeFocusTargetNode?.isInteropViewHost == true)
        ) {
            if (platformFocusOwner.moveFocusInChildren(focusDirection)) {
                return true
            }
        }
        var requestFocusSuccess: Boolean? = false
        val activeNodeBefore = activeFocusTargetNode
        val focusSearchSuccess =
            focusSearch(focusDirection, platformFocusOwner.getEmbeddedViewFocusRect()) {
                requestFocusSuccess = it.requestFocus(focusDirection)
                requestFocusSuccess
            }
        if (focusSearchSuccess == true && activeNodeBefore !== activeFocusTargetNode) {
            // There was a successful requestFocus() during the focusSearch
            return true
        }

        // If focus search was cancelled, or if focus search succeeded but request focus was
        // cancelled, it implies that moveFocus() failed.
        if (focusSearchSuccess == null || requestFocusSuccess == null) return false

        // If focus search and request focus succeeded, move focus succeeded.
        if (focusSearchSuccess && requestFocusSuccess) return true

        // To wrap focus around, we clear focus and request initial focus.
        if (focusDirection.is1dFocusSearch()) {
            val clearFocus =
                clearFocus(
                    force = false,
                    refreshFocusEvents = true,
                    clearOwnerFocus = false,
                    focusDirection = focusDirection,
                )
            return clearFocus && takeFocus(focusDirection, previouslyFocusedRect = null)
        }

        @OptIn(ExperimentalComposeUiApi::class)
        return if (
            ComposeUiFlags.isViewFocusFixEnabled ||
                ComposeUiFlags.isBypassUnfocusableComposeViewEnabled
        ) {
            false
        } else {
            // If we couldn't move focus within compose, we attempt to move focus within embedded
            // views.
            // We don't need this for 1D focus search because the wrap-around logic triggers a
            // focus exit which will perform a focus search among the subviews.
            platformFocusOwner.moveFocusInChildren(focusDirection)
        }
    }

    override fun focusSearch(
        focusDirection: FocusDirection,
        focusedRect: Rect?,
        onFound: (FocusTargetNode) -> Boolean,
    ): Boolean? {
        val source =
            findFocusTargetNode()?.also {
                // Check if a custom focus traversal order is specified.
                when (
                    val customDest = it.customFocusSearch(focusDirection, owner.layoutDirection)
                ) {
                    Cancel -> return null
                    Redirect -> return findFocusTargetNode()?.let(onFound)
                    Default -> {
                        /* Do Nothing */
                    }
                    else -> return customDest.findFocusTargetNode(onFound)
                }
            }

        return rootFocusNode.focusSearch(focusDirection, owner.layoutDirection, focusedRect) {
            when (it) {
                source -> false
                rootFocusNode -> error("Focus search landed at the root.")
                else -> onFound(it)
            }
        }
    }

    /** Dispatches a key event through the compose hierarchy. */
    override fun dispatchKeyEvent(keyEvent: KeyEvent, onFocusedItem: () -> Boolean): Boolean {
        trace("FocusOwnerImpl:dispatchKeyEvent") {
            if (focusInvalidationManager.hasPendingInvalidation()) {
                // Ignoring this to unblock b/346370327.
                println("$FocusWarning: Dispatching key event while focus system is invalidated.")
                return false
            }
            if (!validateKeyEvent(keyEvent)) return false

            val activeFocusTarget = findFocusTargetNode()
            val focusedKeyInputNode =
                activeFocusTarget?.lastLocalKeyInputNode()
                    ?: activeFocusTarget?.nearestAncestorIncludingSelf(Nodes.KeyInput)?.node
                    ?: rootFocusNode.nearestAncestor(Nodes.KeyInput)?.node

            focusedKeyInputNode?.traverseAncestorsIncludingSelf(
                type = Nodes.KeyInput,
                onPreVisit = { if (it.onPreKeyEvent(keyEvent)) return true },
                onVisit = { if (onFocusedItem.invoke()) return true },
                onPostVisit = { if (it.onKeyEvent(keyEvent)) return true },
            )
            return false
        }
    }

    override fun dispatchInterceptedSoftKeyboardEvent(keyEvent: KeyEvent): Boolean {
        if (focusInvalidationManager.hasPendingInvalidation()) {
            // Ignoring this to unblock b/346370327.
            println(
                "$FocusWarning: Dispatching intercepted soft keyboard event while the focus system" +
                    " is invalidated."
            )
            return false
        }

        val focusedSoftKeyboardInterceptionNode =
            rootFocusNode
                .findActiveFocusNode()
                ?.nearestAncestorIncludingSelf(Nodes.SoftKeyboardKeyInput)

        focusedSoftKeyboardInterceptionNode?.traverseAncestorsIncludingSelf(
            type = Nodes.SoftKeyboardKeyInput,
            onPreVisit = { if (it.onPreInterceptKeyBeforeSoftKeyboard(keyEvent)) return true },
            onVisit = { /* TODO(b/320510084): dispatch soft keyboard events to embedded views. */ },
            onPostVisit = { if (it.onInterceptKeyBeforeSoftKeyboard(keyEvent)) return true },
        )
        return false
    }

    /** Dispatches a rotary scroll event through the compose hierarchy. */
    override fun dispatchRotaryEvent(
        event: RotaryScrollEvent,
        onFocusedItem: () -> Boolean,
    ): Boolean {
        if (focusInvalidationManager.hasPendingInvalidation()) {
            // Ignoring this to unblock b/379289347.
            println(
                "$FocusWarning: Dispatching rotary event while the focus system is invalidated."
            )
            return false
        }

        val focusedRotaryInputNode =
            findFocusTargetNode()?.nearestAncestorIncludingSelf(Nodes.RotaryInput)

        focusedRotaryInputNode?.traverseAncestorsIncludingSelf(
            type = Nodes.RotaryInput,
            onPreVisit = { if (it.onPreRotaryScrollEvent(event)) return true },
            onVisit = { if (onFocusedItem()) return true },
            onPostVisit = { if (it.onRotaryScrollEvent(event)) return true },
        )

        return false
    }

    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    override fun dispatchIndirectTouchEvent(
        event: IndirectTouchEvent,
        onFocusedItem: () -> Boolean,
    ): Boolean {
        if (focusInvalidationManager.hasPendingInvalidation()) {
            // Ignoring this to unblock b/379289347.
            println(
                "$FocusWarning: Dispatching indirect touch event while the focus system is invalidated."
            )
            return false
        }

        val focusedIndirectTouchInputNode =
            findFocusTargetNode()?.nearestAncestorIncludingSelf(Nodes.IndirectTouchInput)
        focusedIndirectTouchInputNode?.traverseAncestorsIncludingSelf(
            type = Nodes.IndirectTouchInput,
            onPreVisit = { if (it.onPreIndirectTouchEvent(event)) return true },
            onVisit = { if (onFocusedItem()) return true },
            onPostVisit = { if (it.onIndirectTouchEvent(event)) return true },
        )

        return false
    }

    override fun focusTargetAvailable() {
        platformFocusOwner.focusTargetAvailable()
    }

    override fun scheduleInvalidation(node: FocusTargetNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    override fun scheduleInvalidation(node: FocusEventModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    override fun scheduleInvalidationForOwner() {
        focusInvalidationManager.scheduleInvalidation()
    }

    private inline fun <reified T : DelegatableNode> DelegatableNode.traverseAncestorsIncludingSelf(
        type: NodeKind<T>,
        onPreVisit: (T) -> Unit,
        onVisit: () -> Unit,
        onPostVisit: (T) -> Unit,
    ) {
        val ancestors = ancestors(type)
        ancestors?.fastForEachReversed(onPreVisit)
        node.dispatchForKind(type, onPreVisit)
        onVisit.invoke()
        node.dispatchForKind(type, onPostVisit)
        ancestors?.fastForEach(onPostVisit)
    }

    private inline fun <reified T : Any> DelegatableNode.nearestAncestorIncludingSelf(
        type: NodeKind<T>
    ): T? {
        visitAncestors(type, includeSelf = true) {
            return it
        }
        return null
    }

    /** Searches for the currently focused item, and returns its coordinates as a rect. */
    override fun getFocusRect(): Rect? {
        return findFocusTargetNode()?.focusRect()
    }

    override fun hasFocusableContent(): Boolean {
        if (!rootFocusNode.isAttached) return false

        rootFocusNode.visitSubtree(Nodes.FocusTarget) {
            if (it.isAttached && it.fetchFocusProperties().canFocus) {
                return true
            }
        }
        return false
    }

    override fun hasNonInteropFocusableContent(): Boolean {
        if (!rootFocusNode.isAttached) return false

        rootFocusNode.visitSubtree(Nodes.FocusTarget) {
            if (!it.isAttached) {
                return@visitSubtree
            }
            val focusProperties = it.fetchFocusProperties()
            @OptIn(ExperimentalComposeUiApi::class)
            if (it.isAttached && !it.isInteropViewHost && focusProperties.canFocus) {
                return true
            }
        }
        return false
    }

    private fun findFocusTargetNode(): FocusTargetNode? {
        return rootFocusNode.findActiveFocusNode()
    }

    override val rootState: FocusState
        get() = rootFocusNode.focusState

    override val listeners: MutableObjectList<FocusListener> = MutableObjectList(1)

    override var activeFocusTargetNode: FocusTargetNode? = null
        get() = if (field?.isAttached == true) field else null
        set(value) {
            val previousValue = field
            field = value
            if (value == null || previousValue !== value) isFocusCaptured = false
            if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isSemanticAutofillEnabled) {
                listeners.forEach { it.onFocusChanged(previousValue, value) }
            }
        }

    override var isFocusCaptured: Boolean = false
        set(value) {
            requirePrecondition(!value || activeFocusTargetNode != null) {
                "Cannot capture focus when the active focus target node is unset"
            }
            field = value
        }

    private fun DelegatableNode.lastLocalKeyInputNode(): Modifier.Node? {
        var focusedKeyInputNode: Modifier.Node? = null
        visitLocalDescendants(Nodes.FocusTarget or Nodes.KeyInput) { modifierNode ->
            if (modifierNode.isKind(Nodes.FocusTarget)) return focusedKeyInputNode

            focusedKeyInputNode = modifierNode
        }
        return focusedKeyInputNode
    }

    // TODO(b/307580000) Factor this out into a class to manage key inputs.
    private fun validateKeyEvent(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.key.keyCode
        when (keyEvent.type) {
            KeyDown -> {
                // It's probably rare for more than 3 hardware keys to be pressed simultaneously.
                val keysCurrentlyDown =
                    keysCurrentlyDown
                        ?: MutableLongSet(initialCapacity = 3).also { keysCurrentlyDown = it }
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

/**
 * focus search in the Android framework wraps around for 1D focus search, but not for 2D focus
 * search. This is a helper function that can be used to determine whether we should wrap around or
 * not.
 */
internal fun FocusDirection.is1dFocusSearch(): Boolean =
    when (this) {
        Next,
        Previous -> true
        else -> false
    }

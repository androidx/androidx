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

package androidx.compose.ui.focus

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.rotary.RotaryScrollEvent

/**
 * The focus owner provides some internal APIs that are not exposed by focus manager.
 */
internal interface FocusOwner : FocusManager {

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    val modifier: Modifier

    /**
     * This manager provides a way to ensure that only one focus transaction is running at a time.
     * We use this to prevent re-entrant focus operations. Starting a new transaction automatically
     * cancels the previous transaction and reverts any focus state changes made during that
     * transaction.
     */
    val focusTransactionManager: FocusTransactionManager

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
    fun requestFocusForOwner(focusDirection: FocusDirection?, previouslyFocusedRect: Rect?): Boolean

    /**
     * This function searches the compose hierarchy for the next focus target based on the supplied
     * parameters.
     *
     * @param focusDirection the direction to search for the focus target.
     *
     * @param focusedRect the bounds of the currently focused item.
     *
     * @param onFound This lambda is called with the focus search result.
     *
     * @return true, if a suitable [FocusTargetNode] was found, false if no [FocusTargetNode] was
     * found, and null if the focus search was cancelled.
     */
    fun focusSearch(
        focusDirection: FocusDirection,
        focusedRect: Rect?,
        onFound: (FocusTargetNode) -> Boolean
    ): Boolean?

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
    fun takeFocus(focusDirection: FocusDirection, previouslyFocusedRect: Rect?): Boolean

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    fun releaseFocus()

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have
     * any components that have captured focus.
     *
     * @param refreshFocusEvents: Whether we should send an event up the hierarchy to update
     * the associated onFocusEvent nodes.
     *
     * @param clearOwnerFocus whether we should also clear focus from the owner. This is usually
     * true, unless focus is being temporarily cleared (eg. to implement focus wrapping).
     *
     * @param focusDirection The focus direction of the focus transaction that triggered this clear
     * focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    fun clearFocus(
        force: Boolean,
        refreshFocusEvents: Boolean,
        clearOwnerFocus: Boolean,
        focusDirection: FocusDirection
    ): Boolean

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    fun getFocusRect(): Rect?

    /**
     * Dispatches a key event through the compose hierarchy.
     *
     * When an embedded subview has focus, we call onPreviewKeyEvents for all the parents, and then
     * invoke onFocusedItem before we call onKeyEvent on all the parents.
     *
     * @param keyEvent the key event to be dispatched
     *
     * @param onFocusedItem the block that is run after calling onPreviewKeyEvents on all the
     * parents. Returning true will consume the event and prevent the event from propagating
     * to the onKeyEvent modifiers on parents. This is used to dispatch key events to embedded
     * sub-views.
     */
    fun dispatchKeyEvent(keyEvent: KeyEvent, onFocusedItem: () -> Boolean = { false }): Boolean

    /**
     * Dispatches an intercepted soft keyboard key event through the compose hierarchy.
     */
    fun dispatchInterceptedSoftKeyboardEvent(keyEvent: KeyEvent): Boolean

    /**
     * Dispatches a rotary scroll event through the compose hierarchy.
     */
    fun dispatchRotaryEvent(event: RotaryScrollEvent): Boolean

    /**
     * Schedule a FocusTarget node to be invalidated after onApplyChanges.
     */
    fun scheduleInvalidation(node: FocusTargetNode)

    /**
     * Schedule a FocusEvent node to be invalidated after onApplyChanges.
     */
    fun scheduleInvalidation(node: FocusEventModifierNode)

    /**
     * Schedule a FocusProperties node to be invalidated after onApplyChanges.
     */
    fun scheduleInvalidation(node: FocusPropertiesModifierNode)

    /**
     * The focus state of the root focus node.
     */
    val rootState: FocusState
}

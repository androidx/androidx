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

package androidx.ui.test

import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.semantics.findClosestParentNode
import androidx.ui.geometry.Offset
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsActions
import androidx.ui.semantics.SemanticsPropertyKey

/**
 * Performs a click action on the element represented by the given semantics node.
 */
fun SemanticsNodeInteraction.performClick(): SemanticsNodeInteraction {
    // TODO(jellefresen): Replace with semantics action when semantics merging is done
    // The problem we currently have is that the click action might be defined on a different
    // semantics node than we're interacting with now, even though it is "semantically" the same.
    // E.g., findByText(buttonText) finds the Text's semantics node, but the click action is
    // defined on the wrapping Button's semantics node.
    // Since in general the intended click action can be on a wrapping node or a child node, we
    // can't just forward to the correct node, as we don't know if we should search up or down the
    // tree.
    return performGesture {
        click()
    }
}

/**
 * Scrolls to a node using SemanticsActions. It first identifies a parent semantics node with a
 * Semantics ScrollBy action, then it retrieves the location of the current element and computes
 * the relative coordinates that will be used by the scroller.
 *
 * Throws [AssertionError] if there is no parent node with ScrollBy SemanticsAction, the
 * current semantics node doesn't have a bounding rectangle set or if a layout node used to
 * compute the relative coordinates to be fed to the ScrollBy action can't be found.
 */
fun SemanticsNodeInteraction.performScrollTo(): SemanticsNodeInteraction {
    // find containing node with scroll action
    val errorMessageOnFail = "Failed to perform doScrollTo."
    val node = fetchSemanticsNode(errorMessageOnFail)
    val scrollableSemanticsNode = node.findClosestParentNode {
        hasScrollAction().matches(it)
    }
        ?: throw AssertionError(
            "Semantic Node has no parent layout with a Scroll SemanticsAction"
        )

    val globalPosition = node.globalPosition

    val layoutNode = scrollableSemanticsNode.componentNode

    @OptIn(ExperimentalLayoutNodeApi::class)
    val position = layoutNode.coordinates.localToGlobal(Offset(0.0f, 0.0f))

    runOnUiThread {
        scrollableSemanticsNode.config[SemanticsActions.ScrollBy].action(
            (globalPosition.x - position.x),
            (globalPosition.y - position.y)
        )
    }

    return this
}

/**
 * Executes the gestures specified in the given block.
 *
 * Example usage:
 * ```
 * onNodeWithTag("myWidget")
 *    .doGesture {
 *        sendSwipeUp()
 *    }
 * ```
 */
fun SemanticsNodeInteraction.performGesture(
    block: GestureScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to perform a gesture.")
    with(GestureScope(node)) {
        try {
            block()
        } finally {
            dispose()
        }
    }
    return this
}

/**
 * Executes the (partial) gesture specified in the given block. The gesture doesn't need to be
 * complete and can be resumed later. It is the responsibility of the caller to make sure partial
 * gestures don't leave the test in an inconsistent state.
 *
 * When [sending the down event][down], a token is returned which needs to be used in all
 * subsequent events of this gesture.
 *
 * Example usage:
 * ```
 * val position = Offset(10f, 10f)
 * onNodeWithTag("myWidget")
 *    .performPartialGesture { sendDown(position) }
 *    .assertHasClickAction()
 *    .performPartialGesture { sendUp(position) }
 * ```
 */
fun SemanticsNodeInteraction.performPartialGesture(
    block: PartialGestureScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to perform a partial gesture.")
    with(PartialGestureScope(node)) {
        try {
            block()
        } finally {
            dispose()
        }
    }
    return this
}

/**
 * Provides support to call custom semantics actions on this node.
 *
 * This method is supposed to be used for actions with parameters.
 *
 * This will properly verify that the actions exists and provide clear error message in case it
 * does not. It also handle synchronization and performing the action on the UI thread. This call
 * is blocking until the action is performed
 *
 * @param key Key of the action to be performed.
 * @param invocation Place where you call your action. In the argument is provided the underlying
 * action from the given Semantics action.
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
fun <T : Function<Boolean>> SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
) {
    val node = fetchSemanticsNode("Failed to perform ${key.name} action.")
    if (key !in node.config) {
        throw AssertionError(
            buildGeneralErrorMessage(
                "Failed to perform ${key.name} action as it is not defined on the node.",
                selector, node)
        )
    }

    runOnUiThread {
        invocation(node.config[key].action)
    }
}

/**
 * Provides support to call custom semantics actions on this node.
 *
 * This method is for calling actions that have no parameters.
 *
 * This will properly verify that the actions exists and provide clear error message in case it
 * does not. It also handle synchronization and performing the action on the UI thread. This call
 * is blocking until the action is performed
 *
 * @param key Key of the action to be performed.
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
fun SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
) {
    performSemanticsAction(key) { it.invoke() }
}

// DEPRECATED APIs SECTION

/**
 * Performs a click action on the given component.
 */
@Deprecated("Renamed to performClick",
    replaceWith = ReplaceWith("performClick()"))
fun SemanticsNodeInteraction.doClick(): SemanticsNodeInteraction = performClick()

/**
 * Scrolls to a component using SemanticsActions. It first identifies a parent component with a
 * Semantics ScrollTo action, then it retrieves the location of the current element and computes
 * the relative coordinates that will be used by the scroller.
 *
 * Throws [AssertionError] if there is no parent component with ScrollTo SemanticsAction, the
 * current semantics component doesn't have a bounding rectangle set or if a layout node used to
 * compute the relative coordinates to be fed to the ScrollTo action can't be found.
 */
@Deprecated("Renamed to performScrollTo",
    replaceWith = ReplaceWith("performScrollTo()"))
fun SemanticsNodeInteraction.doScrollTo(): SemanticsNodeInteraction = performScrollTo()

/**
 * Executes the gestures specified in the given block.
 *
 * Example usage:
 * ```
 * onNodeWithTag("myWidget")
 *    .doGesture {
 *        sendSwipeUp()
 *    }
 * ```
 */
@Deprecated("Renamed to performGesture",
    replaceWith = ReplaceWith("performGesture(block)"))
fun SemanticsNodeInteraction.doGesture(
    block: GestureScope.() -> Unit
): SemanticsNodeInteraction = performGesture(block)

/**
 * Executes the (partial) gesture specified in the given block. The gesture doesn't need to be
 * complete and can be resumed later. It is the responsibility of the caller to make sure partial
 * gestures don't leave the test in an inconsistent state.
 *
 * When [sending the down event][down], a token is returned which needs to be used in all
 * subsequent events of this gesture.
 *
 * Example usage:
 * ```
 * val position = Offset(10f, 10f)
 * onNodeWithTag("myWidget")
 *    .doPartialGesture { sendDown(position) }
 *    .assertHasClickAction()
 *    .doPartialGesture { sendUp(position) }
 * ```
 */
@Deprecated("Renamed to performPartialGesture",
    replaceWith = ReplaceWith("performPartialGesture(block)"))
fun SemanticsNodeInteraction.doPartialGesture(
    block: PartialGestureScope.() -> Unit
): SemanticsNodeInteraction = performPartialGesture(block)

/**
 * Provides support to call custom semantics actions on this node.
 *
 * This method is supposed to be used for actions with parameters.
 *
 * This will properly verify that the actions exists and provide clear error message in case it
 * does not. It also handle synchronization and performing the action on the UI thread. This call
 * is blocking until the action is performed
 *
 * @param key Key of the action to be performed.
 * @param invocation Place where you call your action. In the argument is provided the underlying
 * action from the given Semantics action.
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
@Deprecated("Renamed to performSemanticsAction",
    replaceWith = ReplaceWith("performSemanticsAction(key, invocation)"))
fun <T : Function<Boolean>> SemanticsNodeInteraction.callSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
) = performSemanticsAction(key, invocation)

/**
 * Provides support to call custom semantics actions on this node.
 *
 * This method is for calling actions that have no parameters.
 *
 * This will properly verify that the actions exists and provide clear error message in case it
 * does not. It also handle synchronization and performing the action on the UI thread. This call
 * is blocking until the action is performed
 *
 * @param key Key of the action to be performed.
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
@Deprecated("Renamed to performSemanticsAction",
    replaceWith = ReplaceWith("performSemanticsAction(key)"))
fun SemanticsNodeInteraction.callSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
) = performSemanticsAction(key)

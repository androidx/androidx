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

import android.os.Handler
import android.os.Looper
import androidx.ui.core.findClosestParentNode
import androidx.ui.core.findLastLayoutChild
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsActions
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Performs a click action on the given component.
 */
fun SemanticsNodeInteraction.doClick(): SemanticsNodeInteraction {
    // TODO(jellefresen): Replace with semantics action when semantics merging is done
    // The problem we currently have is that the click action might be defined on a different
    // semantics node than we're interacting with now, even though it is "semantically" the same.
    // E.g., findByText(buttonText) finds the Text's semantics node, but the click action is
    // defined on the wrapping Button's semantics node.
    // Since in general the intended click action can be on a wrapping node or a child node, we
    // can't just forward to the correct node, as we don't know if we should search up or down the
    // tree.
    return doGesture {
        sendClick()
    }
}

/**
 * Scrolls to a component using SemanticsActions. It first identifies a parent component with a
 * Semantics ScrollTo action, then it retrieves the location of the current element and computes
 * the relative coordinates that will be used by the scroller.
 *
 * Throws [AssertionError] if there is no parent component with ScrollTo SemanticsAction, the
 * current semantics component doesn't have a bounding rectangle set or if a layout node used to
 * compute the relative coordinates to be fed to the ScrollTo action can't be found.
 */
fun SemanticsNodeInteraction.doScrollTo(): SemanticsNodeInteraction {
    // find containing component with scroll action
    val errorMessageOnFail = "Failed to perform doScrollTo."
    val node = fetchSemanticsNode(errorMessageOnFail)
    val scrollableSemanticsNode = node.findClosestParentNode {
        hasScrollAction().condition(it.config)
    }
        ?: throw AssertionError(
            "Semantic Node has no parent layout with a Scroll SemanticsAction"
        )

    val globalRect = node.globalBounds

    val layoutNode = scrollableSemanticsNode.componentNode.findLastLayoutChild { true }
        ?: throw AssertionError(
            "No Layout Node found!"
        )

    val position = layoutNode.coordinates.localToGlobal(PxPosition(0.px, 0.px))

    runOnUiThread {
        scrollableSemanticsNode.config[SemanticsActions.ScrollTo].action(
            globalRect.left - position.x,
            globalRect.top - position.y
        )
    }

    return this
}

/**
 * Executes the gestures specified in the given block.
 *
 * Example usage:
 * findByTag("myWidget")
 *    .doGesture {
 *        sendSwipeUp()
 *    }
 */
fun SemanticsNodeInteraction.doGesture(
    block: GestureScope.() -> Unit
): SemanticsNodeInteraction {
    val scope = GestureScope(this)
    scope.block()
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
fun <T : Function<Unit>> SemanticsNodeInteraction.callSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
) {
    val node = fetchSemanticsNode("Failed to call ${key.name} action.")
    if (key !in node.config) {
        throw AssertionError(
            buildGeneralErrorMessage(
                "Failed to call ${key.name} action as it is not defined on the node.",
                semanticsTreeInteraction.selector, node)
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
fun SemanticsNodeInteraction.callSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Unit>>
) {
    callSemanticsAction(key) { it.invoke() }
}

// TODO(pavlis): We could expose this to the developers to avoid their dependency on TestRules
// After that we can build runOnIdleCompose on top of this and move that out of ComposeTestRule
private fun runOnUiThread(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        action.invoke()
        return
    }

    val latch = CountDownLatch(1)
    val handler = Handler(Looper.getMainLooper())
    handler.post(object : Runnable {
        override fun run() {
            action.invoke()
            latch.countDown()
        }
    })

    val success = latch.await(1_000, TimeUnit.MILLISECONDS)
    if (!success) {
        throw TimeoutException("Timeout has occurred when waiting for an action to be performed.")
    }
}
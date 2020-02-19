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

import androidx.ui.core.findClosestParentNode
import androidx.ui.core.findLastLayoutChild
import androidx.ui.semantics.SemanticsActions
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px

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

    semanticsTreeInteraction.performAction {
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
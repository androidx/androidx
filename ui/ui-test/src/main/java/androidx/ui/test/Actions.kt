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

import androidx.ui.core.PxPosition
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.findLastLayoutChild
import androidx.ui.core.localToGlobal
import androidx.ui.core.px
import androidx.ui.semantics.SemanticsActions

/**
 * Performs a click action on the given component.
 */
// TODO(jellefresen): Move method to GestureScope.kt and add semantics doClick action here
fun SemanticsNodeInteraction.doClick(): SemanticsNodeInteraction {
    // TODO(b/129400818): uncomment this after Merge Semantics is merged
    // assertHasClickAction()

    // TODO(catalintudor): get real coordinates after Semantics API is ready (b/125702443)
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
    val x = globalRect.left + 1f
    val y = globalRect.top + 1f

    semanticsTreeInteraction.sendInput {
        it.sendClick(x, y)
    }

    return this
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
    val scrollableSemanticsNode = semanticsTreeNode.findClosestParentNode {
        it is SemanticsComponentNode && it.semanticsConfiguration.hasScrollAction
    } as SemanticsComponentNode?

    if (scrollableSemanticsNode == null) {
        throw AssertionError(
            "Semantic Node has no parent layout with a Scroll SemanticsAction"
        )
    }

    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError(
            "Semantic Node has no coordinates set!"
        )

    val layoutNode = scrollableSemanticsNode.findLastLayoutChild { true }
        ?: throw AssertionError(
            "No Layout Node found!"
        )

    val position = layoutNode.localToGlobal(PxPosition(0.px, 0.px))

    semanticsTreeInteraction.performAction {
        scrollableSemanticsNode.semanticsConfiguration[SemanticsActions.ScrollTo].action(
            globalRect.left.px - position.x,
            globalRect.top.px - position.y

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

fun waitForIdleCompose(): Boolean = semanticsTreeInteractionFactory({ true }).waitForIdleCompose()

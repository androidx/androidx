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

import androidx.test.espresso.matcher.ViewMatchers
import androidx.ui.core.AndroidOwner
import androidx.ui.core.LayoutNode
import androidx.ui.core.findClosestParentNode
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.semantics.AccessibilityRangeInfo
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.height
import androidx.ui.unit.px
import androidx.ui.unit.width

/**
 * Asserts that the current component has hidden property set to true.
 *
 * Note that this does not verify parents of the component. For stronger guarantees of visibility
 * see [assertIsNotDisplayed]. If you want to assert that the component is not even in the hierarchy
 * use [SemanticsNodeInteraction.assertDoesNotExist].
 *
 * Throws [AssertionError] if the component is not hidden.
 */
fun SemanticsNodeInteraction.assertIsHidden(): SemanticsNodeInteraction = assert(isHidden())

/**
 * Asserts that the current component has hidden property set to false.
 *
 * Note that this does not verify parents of the component. For stronger guarantees of visibility
 * see [assertIsDisplayed]. If you only want to assert that the component is in the hierarchy use
 * [SemanticsNodeInteraction.assertExists]
 *
 * Throws [AssertionError] if the component is hidden.
 */
fun SemanticsNodeInteraction.assertIsNotHidden(): SemanticsNodeInteraction = assert(isNotHidden())

/**
 * Asserts that the current component is displayed on screen.
 *
 * Throws [AssertionError] if the component is not displayed.
 */
fun SemanticsNodeInteraction.assertIsDisplayed(): SemanticsNodeInteraction {
    // TODO(b/143607231): check semantics hidden property
    // TODO(b/143608742): check the correct AndroidCraneView is visible

    if (!checkIsDisplayed()) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: The component is not displayed!")
    }
    return this
}

/**
 * Asserts that the current component is not displayed on screen.
 *
 * Throws [AssertionError] if the component is displayed.
 */
fun SemanticsNodeInteraction.assertIsNotDisplayed(): SemanticsNodeInteraction {
    // TODO(b/143607231): check semantics hidden property
    // TODO(b/143608742): check no AndroidCraneView contains the given component

    if (checkIsDisplayed()) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: The component is displayed!")
    }
    return this
}

/**
 * Asserts that the current component is enabled.
 *
 * Throws [AssertionError] if the component is not enabled or does not define the property at all.
 */
fun SemanticsNodeInteraction.assertIsEnabled(): SemanticsNodeInteraction = assert(isEnabled())

/**
 * Asserts that the current component is not enabled.
 *
 * Throws [AssertionError] if the component is enabled or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsNotEnabled(): SemanticsNodeInteraction = assert(isNotEnabled())

/**
 * Asserts that the current component is checked.
 *
 * Throws [AssertionError] if the component is not unchecked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOn(): SemanticsNodeInteraction = assert(isOn())

/**
 * Asserts that the current component is unchecked.
 *
 * Throws [AssertionError] if the component is checked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOff(): SemanticsNodeInteraction = assert(isOff())

/**
 * Asserts that the current component is selected.
 *
 * Throws [AssertionError] if the component is unselected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction = assert(isSelected())

/**
 * Asserts that the current component is unselected.
 *
 * Throws [AssertionError] if the component is selected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsUnselected(): SemanticsNodeInteraction =
    assert(isUnselected())

/**
 * Asserts that the current component is toggleable.
 *
 * Throws [AssertionError] if the component is not toggleable.
 */
fun SemanticsNodeInteraction.assertIsToggleable(): SemanticsNodeInteraction =
    assert(isToggleable())

/**
 * Asserts that the current component is selectable.
 *
 * Throws [AssertionError] if the component is not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelectable(): SemanticsNodeInteraction =
    assert(isSelectable())

/**
 * Asserts the component is in a mutually exclusive group. This is used by radio groups to assert
 * only one is selected at a given time.
 */
fun SemanticsNodeInteraction.assertIsInMutuallyExclusiveGroup(): SemanticsNodeInteraction =
    assert(isInMutuallyExclusiveGroup())

/**
 * Asserts the component's label equals the given String.
 * For further details please check [SemanticsProperties.AccessibilityLabel].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertLabelEquals(value: String): SemanticsNodeInteraction =
    assert(hasText(value))

/**
 * Asserts the component's value equals the given value.
 *
 * For further details please check [SemanticsProperties.AccessibilityValue].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction =
    assert(hasValue(value))

/**
 * Asserts the component's range info equals the given value.
 *
 * For further details please check [SemanticsProperties.AccessibilityRangeInfo].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertRangeInfoEquals(value: AccessibilityRangeInfo):
        SemanticsNodeInteraction =
    assert(hasRangeInfo(value))

/**
 * Asserts that the current component has a click action.
 *
 * Throws [AssertionError] if the component is doesn't have a click action.
 */
fun SemanticsNodeInteraction.assertHasClickAction(): SemanticsNodeInteraction =
    assert(hasClickAction())

/**
 * Asserts that the current component doesn't have a click action.
 *
 * Throws [AssertionError] if the component has a click action.
 */
fun SemanticsNodeInteraction.assertHasNoClickAction(): SemanticsNodeInteraction =
    assert(hasNoClickAction())

/**
 * Asserts that the provided [matcher] is satisfied for this node.
 *
 * @param matcher Matcher to verify.
 * @param messagePrefixOnError Prefix to be put in front of an error that gets thrown in case this
 * assert fails. This can be helpful in situations where this assert fails as part of a bigger
 * operation that used this assert as a precondition check.
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun SemanticsNodeInteraction.assert(
    matcher: SemanticsMatcher,
    messagePrefixOnError: (() -> String)? = null
): SemanticsNodeInteraction {
    var errorMessageOnFail = "Failed to assert the following: (${matcher.description})"
    if (messagePrefixOnError != null) {
        errorMessageOnFail = messagePrefixOnError() + "\n" + errorMessageOnFail
    }
    val node = fetchSemanticsNode(errorMessageOnFail)
    if (!matcher.matches(node)) {
        throw AssertionError(buildGeneralErrorMessage(errorMessageOnFail, selector, node))
    }
    return this
}

/**
 * Asserts that this collection of nodes is equal to the given [expectedSize].
 *
 * Provides a detailed error message on failure.
 *
 * @throws AssertionError if the size is not equal to [expectedSize]
 */
fun SemanticsNodeInteractionCollection.assertCountEquals(
    expectedSize: Int
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assert count of nodes."
    val matchedNodes = fetchSemanticsNodes(errorOnFail)
    if (matchedNodes.size != expectedSize) {
        throw AssertionError(buildErrorMessageForCountMismatch(
            errorMessage = errorOnFail,
            selector = selector,
            foundNodes = matchedNodes,
            expectedCount = expectedSize))
    }
    return this
}

/**
 * Asserts that this collection contains at least one element that satisfies the given [matcher].
 *
 * @param matcher Matcher that has to be satisfied by at least one of the nodes in the collection.
 *
 * @throws AssertionError if not at least one matching node was node.
 */
fun SemanticsNodeInteractionCollection.assertAny(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assertAny(${matcher.description})"
    val nodes = fetchSemanticsNodes(errorOnFail)
    if (nodes.isEmpty()) {
        throw AssertionError(buildErrorMessageForAtLeastOneNodeExpected(errorOnFail, selector))
    }
    if (!matcher.matchesAny(nodes)) {
        throw AssertionError(buildErrorMessageForAssertAnyFail(selector, nodes, matcher))
    }
    return this
}

/**
 * Asserts that all the nodes in this collection satisfy the given [matcher].
 *
 * This passes also for empty collections.
 *
 * @param matcher Matcher that has to be satisfied by all the nodes in the collection.
 *
 * @throws AssertionError if the collection contains at least one element that does not satisfy
 * the given matcher.
 */
fun SemanticsNodeInteractionCollection.assertAll(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assertAll(${matcher.description})"
    val nodes = fetchSemanticsNodes(errorOnFail)

    val violations = mutableListOf<SemanticsNode>()
    nodes.forEach {
        if (!matcher.matches(it)) {
            violations.add(it)
        }
    }
    if (violations.isNotEmpty()) {
        throw AssertionError(buildErrorMessageForAssertAllFail(selector, violations, matcher))
    }
    return this
}

private fun SemanticsNodeInteraction.checkIsDisplayed(): Boolean {
    // hierarchy check - check layout nodes are visible
    val errorMessageOnFail = "Failed to perform isDisplayed check."
    val node = fetchSemanticsNode(errorMessageOnFail)

    fun isNotPlaced(node: LayoutNode): Boolean {
        return !node.isPlaced
    }

    val layoutNode = node.componentNode
    if (isNotPlaced(layoutNode) || layoutNode.findClosestParentNode(::isNotPlaced) != null) {
        return false
    }

    (layoutNode.owner as? AndroidOwner)?.let {
        if (!ViewMatchers.isDisplayed().matches(it.view)) {
            return false
        }
    }

    // check node doesn't clip unintentionally (e.g. row too small for content)
    val globalRect = node.globalBounds
    if (!node.isInScreenBounds()) {
        return false
    }

    return (globalRect.width > 0f && globalRect.height > 0f)
}

private fun SemanticsNode.isInScreenBounds(): Boolean {
    val nodeBounds = globalBounds
    if (nodeBounds.width == 0f && nodeBounds.height == 0f) {
        return false
    }

    val displayMetrics = (componentNode.owner as AndroidOwner).view.resources.displayMetrics
    val screenBounds = PxBounds(
        PxPosition.Origin,
        PxSize(displayMetrics.widthPixels.px, displayMetrics.heightPixels.px)
    )

    return nodeBounds.top >= screenBounds.top &&
            nodeBounds.left >= screenBounds.left &&
            nodeBounds.right <= screenBounds.right &&
            nodeBounds.bottom <= screenBounds.bottom
}

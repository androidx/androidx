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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties

/**
 * Asserts that the current semantics node is displayed on screen.
 *
 * Specifically, the node must be composed, placed and at least a portion of its bounds must be
 * visible on screen after clipping is applied.
 *
 * Throws [AssertionError] if the node is not displayed.
 */
fun SemanticsNodeInteraction.assertIsDisplayed(): SemanticsNodeInteraction {
    if (!isDisplayed()) {
        throw AssertionError(
            "Assert failed: The component with ${selector.description} is not displayed!"
        )
    }
    return this
}

/**
 * Asserts that the current semantics node is not displayed on screen.
 *
 * Throws [AssertionError] if the node is displayed.
 */
fun SemanticsNodeInteraction.assertIsNotDisplayed(): SemanticsNodeInteraction {
    if (!isNotDisplayed()) {
        throw AssertionError(
            "Assert failed: The component with ${selector.description} is displayed!"
        )
    }
    return this
}

/**
 * Asserts that the current semantics node is enabled.
 *
 * Throws [AssertionError] if the node is not enabled or does not define the property at all.
 */
fun SemanticsNodeInteraction.assertIsEnabled(): SemanticsNodeInteraction = assert(isEnabled())

/**
 * Asserts that the current semantics node is not enabled.
 *
 * Throws [AssertionError] if the node is enabled or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsNotEnabled(): SemanticsNodeInteraction = assert(isNotEnabled())

/**
 * Asserts that the current semantics node is checked.
 *
 * Throws [AssertionError] if the node is not unchecked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOn(): SemanticsNodeInteraction = assert(isOn())

/**
 * Asserts that the current semantics node is unchecked.
 *
 * Throws [AssertionError] if the node is checked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOff(): SemanticsNodeInteraction = assert(isOff())

/**
 * Asserts that the current semantics node is selected.
 *
 * Throws [AssertionError] if the node is unselected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction = assert(isSelected())

/**
 * Asserts that the current semantics node is not selected.
 *
 * Throws [AssertionError] if the node is selected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsNotSelected(): SemanticsNodeInteraction =
    assert(isNotSelected())

/**
 * Asserts that the current semantics node is toggleable.
 *
 * Throws [AssertionError] if the node is not toggleable.
 */
fun SemanticsNodeInteraction.assertIsToggleable(): SemanticsNodeInteraction = assert(isToggleable())

/**
 * Asserts that the current semantics node is selectable.
 *
 * Throws [AssertionError] if the node is not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelectable(): SemanticsNodeInteraction = assert(isSelectable())

/**
 * Asserts that the current semantics node has a focus.
 *
 * Throws [AssertionError] if the node is not in the focus or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsFocused(): SemanticsNodeInteraction = assert(isFocused())

/**
 * Asserts that the current semantics node does not have a focus.
 *
 * Throws [AssertionError] if the node is in the focus or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsNotFocused(): SemanticsNodeInteraction = assert(isNotFocused())

/**
 * Asserts that the node's list of content descriptions contains exactly the given [values] and
 * nothing else.
 *
 * The `ContentDescription` property is represented as a list of strings. In the merged semantics
 * tree (the default in Compose testing), this list often contains multiple descriptions merged from
 * child nodes. This function evaluates the entire list.
 *
 * The assertion will only pass if the node's list contains all the provided [values], and contains
 * no additional items. Note that the order of the elements does not matter.
 *
 * Typically, accessibility tooling will decide based on its heuristics which descriptions to
 * announce.
 *
 * Throws [AssertionError] if the node's descriptions don't contain all items from [values], or if
 * the descriptions contain extra items that are not in [values].
 *
 * @sample androidx.compose.ui.test.samples.assertContentDescriptionEqualsSample
 * @param values List of values to match (the order does not matter).
 * @see SemanticsProperties.ContentDescription
 */
fun SemanticsNodeInteraction.assertContentDescriptionEquals(
    vararg values: String
): SemanticsNodeInteraction = assert(hasContentDescriptionExactly(*values))

/**
 * Asserts that the node's list of content descriptions contains the given [value].
 *
 * The `ContentDescription` property is represented as a list of strings. In the merged semantics
 * tree (the default in Compose testing), this list often contains multiple descriptions merged from
 * child nodes. This function evaluates whether any individual item in that list matches the
 * provided [value].
 *
 * By default, this requires an exact string match with at least one complete item in the list.
 *
 * Typically, accessibility tooling will decide based on its heuristics which descriptions to
 * announce.
 *
 * Throws [AssertionError] if the node's value list does not contain `value`, or if the node has no
 * value.
 *
 * @sample androidx.compose.ui.test.samples.assertContentDescriptionContainsSample
 * @param value Value to match against the items in the list of content descriptions.
 * @param substring Whether this can be satisfied as a substring match of an item in the list of
 *   descriptions. Defaults to false.
 * @param ignoreCase Whether case should be ignored. Defaults to false.
 * @see SemanticsProperties.ContentDescription
 */
fun SemanticsNodeInteraction.assertContentDescriptionContains(
    value: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
): SemanticsNodeInteraction =
    assert(hasContentDescription(value, substring = substring, ignoreCase = ignoreCase))

/**
 * Asserts that the node's list of text values contains exactly the given [values] and nothing else.
 *
 * This will also search in [SemanticsProperties.EditableText] by default.
 *
 * The `Text` property is represented as a list of strings. In the merged semantics tree (the
 * default in Compose testing), this list often contains multiple text items merged from child
 * nodes. This function evaluates the entire list.
 *
 * The assertion will only pass if the node's list contains all the provided [values], and contains
 * no additional items. Note that the order of the elements does not matter.
 *
 * Typically, accessibility tooling will decide based on its heuristics which ones to use.
 *
 * Throws [AssertionError] if the node's text values don't contain all items from [values], or if
 * the text values contain extra items that are not in [values].
 *
 * @sample androidx.compose.ui.test.samples.assertTextEqualsSample
 * @param values List of values to match (the order does not matter).
 * @param includeEditableText Whether to also assert against the editable text. Defaults to true.
 * @see SemanticsProperties.Text
 */
fun SemanticsNodeInteraction.assertTextEquals(
    vararg values: String,
    includeEditableText: Boolean = true,
): SemanticsNodeInteraction =
    assert(hasTextExactly(*values, includeEditableText = includeEditableText))

/**
 * Asserts that the node's list of text values contains the given [value].
 *
 * This will also search in [SemanticsProperties.EditableText] and [SemanticsProperties.InputText].
 *
 * The `Text` property is represented as a list of strings. In the merged semantics tree (the
 * default in Compose testing), this list often contains multiple text items merged from child
 * nodes. This function evaluates whether any individual item in that list matches the provided
 * [value].
 *
 * By default, this requires an exact string match with at least one complete item in the list.
 *
 * Typically, accessibility tooling will decide based on its heuristics which ones to use.
 *
 * Throws [AssertionError] if the node's value list does not contain `value`, or if the node has no
 * value.
 *
 * @sample androidx.compose.ui.test.samples.assertTextContainsSample
 * @param value Value to match against the items in the list of text values.
 * @param substring Whether this can be satisfied as a substring match of an item in the list of
 *   text. Defaults to false.
 * @param ignoreCase Whether case should be ignored. Defaults to false.
 * @see SemanticsProperties.Text
 */
fun SemanticsNodeInteraction.assertTextContains(
    value: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
): SemanticsNodeInteraction = assert(hasText(value, substring = substring, ignoreCase = ignoreCase))

/**
 * Asserts the node's value equals the given value.
 *
 * For further details please check [SemanticsProperties.StateDescription]. Throws [AssertionError]
 * if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction =
    assert(hasStateDescription(value))

/**
 * Asserts the node's range info equals the given value.
 *
 * For further details please check [SemanticsProperties.ProgressBarRangeInfo]. Throws
 * [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertRangeInfoEquals(
    value: ProgressBarRangeInfo
): SemanticsNodeInteraction = assert(hasProgressBarRangeInfo(value))

/**
 * Asserts that the current semantics node has a click action.
 *
 * Throws [AssertionError] if the node is doesn't have a click action.
 */
fun SemanticsNodeInteraction.assertHasClickAction(): SemanticsNodeInteraction =
    assert(hasClickAction())

/**
 * Asserts that the current semantics node has doesn't have a click action.
 *
 * Throws [AssertionError] if the node has a click action.
 */
fun SemanticsNodeInteraction.assertHasNoClickAction(): SemanticsNodeInteraction =
    assert(hasNoClickAction())

/**
 * Asserts that the provided [matcher] is satisfied for this node.
 *
 * @param matcher Matcher to verify.
 * @param messagePrefixOnError Prefix to be put in front of an error that gets thrown in case this
 *   assert fails. This can be helpful in situations where this assert fails as part of a bigger
 *   operation that used this assert as a precondition check.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun SemanticsNodeInteraction.assert(
    matcher: SemanticsMatcher,
    messagePrefixOnError: (() -> String)? = null,
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
    val matchedNodes = fetchSemanticsNodes(atLeastOneRootRequired = expectedSize > 0, errorOnFail)
    if (matchedNodes.size != expectedSize) {
        throw AssertionError(
            buildErrorMessageForCountMismatch(
                errorMessage = errorOnFail,
                selector = selector,
                foundNodes = matchedNodes,
                expectedCount = expectedSize,
            )
        )
    }
    return this
}

/**
 * Asserts that this collection contains at least one element that satisfies the given [matcher].
 *
 * @param matcher Matcher that has to be satisfied by at least one of the nodes in the collection.
 * @throws AssertionError if not at least one matching node was node.
 */
fun SemanticsNodeInteractionCollection.assertAny(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assertAny(${matcher.description})"
    val nodes = fetchSemanticsNodes(errorMessageOnFail = errorOnFail)
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
 * @throws AssertionError if the collection contains at least one element that does not satisfy the
 *   given matcher.
 */
fun SemanticsNodeInteractionCollection.assertAll(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assertAll(${matcher.description})"
    val nodes = fetchSemanticsNodes(errorMessageOnFail = errorOnFail)

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

/**
 * Returns true if the matched node is displayed on screen.
 *
 * Specifically, the node must be composed, placed and at least a portion of its bounds must be
 * visible on screen after clipping is applied. If no matching node is found, returns false. If
 * multiple nodes match, throws an [AssertionError].
 *
 * @sample androidx.compose.ui.test.samples.waitForDisplayed
 * @throws AssertionError If multiple nodes match this [SemanticsNodeInteraction].
 */
fun SemanticsNodeInteraction.isDisplayed(): Boolean = checkIsDisplayed(assertIsFullyVisible = false)

/**
 * Asserts that the current semantics node is not displayed on screen.
 *
 * If no matching node is found, returns true. If multiple nodes match, throws an [AssertionError].
 *
 * @sample androidx.compose.ui.test.samples.waitForNotDisplayed
 * @throws AssertionError If multiple nodes match this [SemanticsNodeInteraction].
 */
fun SemanticsNodeInteraction.isNotDisplayed(): Boolean =
    !checkIsDisplayed(assertIsFullyVisible = true)

@Suppress("DocumentExceptions")
internal expect fun SemanticsNodeInteraction.checkIsDisplayed(
    assertIsFullyVisible: Boolean
): Boolean

internal expect fun SemanticsNode.clippedNodeBoundsInWindow(): Rect

internal expect fun SemanticsNode.isInScreenBounds(assertIsFullyVisible: Boolean): Boolean

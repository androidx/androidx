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

import androidx.ui.core.LayoutNode
import androidx.ui.core.RepaintBoundaryNode
import androidx.ui.core.findClosestParentNode
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import androidx.ui.unit.toPx

/**
 * Asserts that the current component has hidden property set to true.
 *
 * Note that this does not verify parents of the component. For stronger guarantees of visibility
 * see [assertIsNotDisplayed]. If you want to assert that the component is not even in the hierarchy
 * use [SemanticsNodeInteraction.assertDoesNotExist].
 *
 * Throws [AssertionError] if the component is not hidden.
 */
fun SemanticsNodeInteraction.assertIsHidden(): SemanticsNodeInteraction = verify(isHidden())

/**
 * Asserts that the current component has hidden property set to false.
 *
 * Note that this does not verify parents of the component. For stronger guarantees of visibility
 * see [assertIsDisplayed]. If you only want to assert that the component is in the hierarchy use
 * [SemanticsNodeInteraction.assertExists]
 *
 * Throws [AssertionError] if the component is hidden.
 */
fun SemanticsNodeInteraction.assertIsNotHidden(): SemanticsNodeInteraction = verify(isNotHidden())

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
 * Asserts that the current component is checked.
 *
 * Throws [AssertionError] if the component is not unchecked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOn(): SemanticsNodeInteraction = verify(isOn())

/**
 * Asserts that the current component is unchecked.
 *
 * Throws [AssertionError] if the component is checked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOff(): SemanticsNodeInteraction = verify(isOff())

/**
 * Asserts that the current component is selected.
 *
 * Throws [AssertionError] if the component is unselected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction = verify(isSelected())

/**
 * Asserts that the current component is unselected.
 *
 * Throws [AssertionError] if the component is selected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsUnselected(): SemanticsNodeInteraction =
    verify(isUnselected())

/**
 * Asserts that the current component is toggleable.
 *
 * Throws [AssertionError] if the component is not toggleable.
 */
fun SemanticsNodeInteraction.assertIsToggleable(): SemanticsNodeInteraction =
    verify(isToggleable())

/**
 * Asserts that the current component is selectable.
 *
 * Throws [AssertionError] if the component is not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelectable(): SemanticsNodeInteraction =
    verify(isSelectable())

/**
 * Asserts the component is in a mutually exclusive group. This is used by radio groups to assert
 * only one is selected at a given time.
 */
fun SemanticsNodeInteraction.assertIsInMutuallyExclusiveGroup(): SemanticsNodeInteraction =
    verify(isInMutuallyExclusiveGroup())

/**
 * Asserts the component's label equals the given String.
 * For further details please check [SemanticsProperties.AccessibilityLabel].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertLabelEquals(value: String): SemanticsNodeInteraction =
    verify(hasText(value))

/**
 * Asserts the component's value equals the given value.
 *
 * For further details please check [SemanticsProperties.AccessibilityValue].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction =
    verify(hasValue(value))

/**
 * Asserts that the semantics of the component are the same as the given semantics.
 * For further details please check [SemanticsConfiguration.assertEquals].
 */
fun SemanticsNodeInteraction.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SemanticsNodeInteraction {
    val errorMessageOnFail = "Failed to assert semantics is equal"
    fetchSemanticsNode(errorMessageOnFail).config.assertEquals(expectedProperties)
    return this
}
/**
 * Asserts that the current component has a click action.
 *
 * Throws [AssertionError] if the component is doesn't have a click action.
 */
fun SemanticsNodeInteraction.assertHasClickAction(): SemanticsNodeInteraction =
    verify(hasClickAction())

/**
 * Asserts that the current component doesn't have a click action.
 *
 * Throws [AssertionError] if the component has a click action.
 */
fun SemanticsNodeInteraction.assertHasNoClickAction(): SemanticsNodeInteraction =
    verify(hasNoClickAction())

/**
 * Asserts that this collection of nodes is equal to the given [expectedSize].
 *
 * Provides a detailed error message on failure.
 *
 * @throws AssertionError if the size is not equal to [expectedSize]
 */
// TODO: Rename to assertSizeEquals to be consistent with Collection.size
fun <T : Collection<SemanticsNodeInteraction>> T.assertCountEquals(expectedSize: Int): T {
    if (size != expectedSize) {
        // Quite often all the elements of a collection come from the same selector. So we try to
        // distinct them hoping we get just one selector to show it to the user on failure.
        // TODO: If there is more than one selector maybe show selector per node?
        val selectors = map { it.semanticsTreeInteraction.selector }
            .distinct()
        val selector = if (selectors.size == 1) {
            selectors.first()
        } else {
            null
        }
        throw AssertionError(buildErrorMessageForCountMismatch(
            errorMessage = "Failed to assert count of nodes.",
            selector = selector,
            foundNodes = map { it.fetchSemanticsNode("") },
            expectedCount = expectedSize))
    }
    return this
}

/**
 * Verifies that the provided condition is true.
 * Throws [AssertionError] if it is not.
 */
fun SemanticsNodeInteraction.verify(
    predicate: SemanticsPredicate
): SemanticsNodeInteraction {
    val errorMessageOnFail = "Failed to assert the following: (${predicate.description})"
    val node = fetchSemanticsNode(errorMessageOnFail)
    if (!predicate.condition(node.config)) {
        throw AssertionError(buildErrorMessageForPredicateFail(
            semanticsTreeInteraction.selector, node, predicate))
    }
    return this
}

/**
 * Verifies that the provided condition is true on all parent semantics nodes.
 * Throws [AssertionError] if it is not.
 */
fun SemanticsNodeInteraction.verifyHierarchy(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    // TODO(b/133217292)
    var node: SemanticsNode? = fetchSemanticsNode("Failed to verify hierarchy.")
    while (node != null) {
        if (!condition.invoke(node.config)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: ${assertionMessage(node.config)}")
        }
        node = node.parent
    }
}

private fun SemanticsNodeInteraction.checkIsDisplayed(): Boolean {
    // hierarchy check - check layout nodes are visible
    val errorMessageOnFail = "Failed to perform isDisplayed check."
    val node = fetchSemanticsNode(errorMessageOnFail)
    if (node.componentNode.findClosestParentNode {
            it is LayoutNode && !it.isPlaced
        } != null) {
        return false
    }

    // check node doesn't clip unintentionally (e.g. row too small for content)
    val globalRect = node.globalBounds
    if (!semanticsTreeInteraction.isInScreenBounds(globalRect)) {
        return false
    }

    // check if we have clipping via RepaintBoundaryNode
    val repaintBoundaryNode = node.componentNode.findClosestParentNode {
        it is RepaintBoundaryNode && it.clipToShape
    }
    if (repaintBoundaryNode == null) {
        // if we don't have a repaint boundary then the component is visible as we already checked
        // the layout nodes and screen bounds
        return true
    }

    // check boundary (e.g. essential for scrollable layouts)
    val layoutNode = repaintBoundaryNode.parentLayoutNode
        ?: throw AssertionError(
            "Semantic Node has no parent layout to check for visibility layout"
        )
    return layoutNode.contains(globalRect)
}

/**
 * Returns `true` if the given [rectangle] is completely contained within this
 * [LayoutNode].
 */
private fun LayoutNode.contains(rectangle: PxBounds): Boolean {
    val globalPositionTopLeft = coordinates.localToGlobal(PxPosition(0.px, 0.px))
    // TODO: This method generates a lot of objects when it could compare primitives

    val rect = Rect.fromLTWH(
        globalPositionTopLeft.x.value,
        globalPositionTopLeft.y.value,
        width.toPx().value + 1f,
        height.toPx().value + 1f)

    return rect.contains(Offset(rectangle.left.value, rectangle.top.value)) &&
            rect.contains(Offset(rectangle.right.value, rectangle.bottom.value))
}

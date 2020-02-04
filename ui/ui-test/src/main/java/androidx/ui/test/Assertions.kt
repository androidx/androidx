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
import androidx.ui.core.semantics.getOrNull
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.semantics.FoundationSemanticsProperties
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import androidx.ui.unit.toPx

/**
 * Asserts that current component is visible.
 */
// TODO(b/123702531): Provide guarantees of being visible VS being actually displayed
fun SemanticsNodeInteraction.assertIsVisible(): SemanticsNodeInteraction {
    verifyHierarchy({ "The component is not visible!" }) {
        it.getOrNull(SemanticsProperties.Hidden) != true
    }
    return this
}

/**
 * Asserts that current component is hidden. This requires that the component actually exists in
 * the hierarchy and is hidden. If you want to actually verify that the component does not  exist
 * at all, please use [SemanticsNodeInteraction.assertDoesNotExist]
 */
fun SemanticsNodeInteraction.assertIsHidden(): SemanticsNodeInteraction {
    verifyHierarchy({ "The component is visible!" }) {
        it.getOrNull(SemanticsProperties.Hidden) == true
    }

    return this
}

/**
 * Asserts that the current component is displayed.
 * This function also calls [assertIsVisible] to check if it is visible from a Semantics perspective
 * and afterwards checks if it's bounding rectangle is contained inside the closest layout node.
 */
fun SemanticsNodeInteraction.assertIsDisplayed(): SemanticsNodeInteraction {
    // TODO(b/143607231): check semantics hidden property
    // TODO(b/143608742): check the correct AndroidCraneView is visible

    verify({ "The component is not displayed!" }) {
        checkIsDisplayed()
    }
    return this
}

/**
 * Asserts that the current component is not displayed.
 * This function checks if it's bounding rectangle is not contained inside the closest layout node.
 */
fun SemanticsNodeInteraction.assertIsNotDisplayed(): SemanticsNodeInteraction {
    // TODO(b/143607231): check semantics hidden property
    // TODO(b/143608742): check no AndroidCraneView contains the given component

    verify({ "The component is displayed!" }) {
        !checkIsDisplayed()
    }

    return this
}

/**
 * Asserts that the current component is checked.
 *
 * Throws [AssertionError] if the component is not unchecked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOn(): SemanticsNodeInteraction {
    assertIsToggleable()
    verify({ "Component is toggled off, expected it to be toggled on" }) {
        it[FoundationSemanticsProperties.ToggleableState] == ToggleableState.On
    }
    return this
}

/**
 * Asserts that the current component is unchecked.
 *
 * Throws [AssertionError] if the component is checked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOff(): SemanticsNodeInteraction {
    assertIsToggleable()
    verify({ "Component is toggled on, expected it to be toggled off" }) {
        it[FoundationSemanticsProperties.ToggleableState] == ToggleableState.Off
    }

    return this
}

/**
 * Asserts that the current component is selected.
 *
 * Throws [AssertionError] if the component is unselected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction {
    assertIsSelectable()

    verify({ "Component is unselected, expected it to be selected" }) {
        it[FoundationSemanticsProperties.Selected]
    }
    return this
}

/**
 * Asserts that the current component is unselected.
 *
 * Throws [AssertionError] if the component is selected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsUnselected(): SemanticsNodeInteraction {
    assertIsSelectable()

    verify({ "Component is selected, expected it to be unselected" }) {
        !it[FoundationSemanticsProperties.Selected]
    }
    return this
}

/**
 * Asserts the component is in a mutually exclusive group. This is used by radio groups to assert
 * only one is selected at a given time.
 * For further details please check [SemanticsConfiguration.isInMutuallyExclusiveGroup].
 */
fun SemanticsNodeInteraction.assertIsInMutuallyExclusiveGroup(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be in a mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }
    return this
}

/**
 * Asserts the component's label equals the given String.
 * For further details please check [SemanticsConfiguration.accessibilityLabel].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertLabelEquals(value: String): SemanticsNodeInteraction {
    verify({ node -> "Expected label: $value, " +
            "Actual label: ${node.getOrNull(SemanticsProperties.AccessibilityLabel)}"
    }) {
        it.getOrElse(SemanticsProperties.AccessibilityLabel) {
            throw AssertionError("Expected label: $value, but had none")
        } == value
    }
    return this
}

/**
 * Asserts the component's value equals the given value.
 *
 * For further details please check [SemanticsConfiguration.accessibilityValue].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction {
    verify({ node -> "Expected value: $value, " +
                "Actual value: ${node.getOrNull(SemanticsProperties.AccessibilityValue)}"
    }) {
        it.getOrElse(SemanticsProperties.AccessibilityValue) {
            throw AssertionError("Expected value: $value, but had none")
        } == value
    }
    return this
}

/**
 * Asserts that the semantics of the component are the same as the given semantics.
 * For further details please check [SemanticsConfiguration.assertEquals].
 */
fun SemanticsNodeInteraction.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SemanticsNodeInteraction {
    semanticsNode.config.assertEquals(expectedProperties)

    return this
}

/**
 * Asserts that the current component has a click action.
 *
 * Throws [AssertionError] if the component is doesn't have a click action.
 */
fun SemanticsNodeInteraction.assertHasClickAction(): SemanticsNodeInteraction {
    verify({ "Component is not clickable, expected it to be clickable" }) {
        it.hasClickAction
    }

    return this
}

/**
 * Asserts that the current component doesn't have a click action.
 *
 * Throws [AssertionError] if the component has a click action.
 */
fun SemanticsNodeInteraction.assertHasNoClickAction(): SemanticsNodeInteraction {
    verify({ "Component is clickable, expected it to not be clickable" }) {
        !it.hasClickAction
    }

    return this
}

/**
 * Asserts that given a list of components, its size is equal to the passed in size.
 */
fun List<SemanticsNodeInteraction>.assertCountEquals(
    count: Int
): List<SemanticsNodeInteraction> {
    if (size != count) {
        // TODO(b/133217292)
        throw AssertionError("Found $size nodes but exactly $count was expected!")
    }

    return this
}

/**
 * Verifies that the provided condition is true.
 * Throws [AssertionError] if it is not.
 */
fun SemanticsNodeInteraction.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    if (!condition.invoke(semanticsNode.config)) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: ${assertionMessage(semanticsNode.config)}")
    }
}

/**
 * Verifies that the provided condition is true on all parent semantics nodes.
 * Throws [AssertionError] if it is not.
 */
fun SemanticsNodeInteraction.verifyHierarchy(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    var node: SemanticsNode? = semanticsNode
    while (node != null) {
        if (!condition.invoke(node.config)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: ${assertionMessage(semanticsNode.config)}")
        }
        node = node.parent
    }
}

/**
 * Asserts that the current component is toggleable.
 *
 * Throws [AssertionError] if the component is not toggleable.
 */
internal fun SemanticsNodeInteraction.assertIsToggleable(): SemanticsNodeInteraction {
    verify({ "Component is not toggleable, expected it to be toggleable" }) {
        it.isToggleable
    }

    return this
}

/**
 * Asserts that the current component is selectable.
 *
 * Throws [AssertionError] if the component is not selectable.
 */
internal fun SemanticsNodeInteraction.assertIsSelectable(): SemanticsNodeInteraction {
    verify({ "Component is not selectable, expected it to be selectable" }) {
        it.getOrNull(FoundationSemanticsProperties.Selected) != null
    }

    return this
}

private fun SemanticsNodeInteraction.checkIsDisplayed(): Boolean {
    // hierarchy check - check layout nodes are visible
    if (semanticsNode.componentNode.findClosestParentNode {
            it is LayoutNode && !it.isPlaced
        } != null) {
        return false
    }

    // check node doesn't clip unintentionally (e.g. row too small for content)
    val globalRect = semanticsNode.globalBounds
    if (!semanticsTreeInteraction.isInScreenBounds(globalRect)) {
        return false
    }

    // check if we have clipping via RepaintBoundaryNode
    val repaintBoundaryNode = semanticsNode.componentNode.findClosestParentNode {
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
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

import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.getOrNull
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.semantics.FoundationSemanticsProperties
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.semantics.accessibilityValue

/**
 * Asserts no items found given a criteria, throws [AssertionError] otherwise.
 */
fun assertDoesNotExist(
    selector: SemanticsConfiguration.() -> Boolean
) {
    val foundNodes = semanticsTreeInteractionFactory(selector)
        .findAllMatching()

    if (foundNodes.isNotEmpty()) {
        throw AssertionError("Found '${foundNodes.size}' components that match, " +
                "expected '0' components")
    }
}
/**
 * Asserts that current component is visible.
 */
// TODO(b/123702531): Provide guarantees of being visible VS being actually displayed
fun SemanticsNodeInteraction.assertIsVisible(): SemanticsNodeInteraction {
    verify({ "The component is not visible!" }) {
        it.getOrNull(SemanticsProperties.Hidden) != true
    }
    return this
}

/**
 * Asserts that current component is hidden. This requires that the component actually exists in
 * the hierarchy and is hidden. If you want to actually verify that the component does not  exist
 * at all, please use [assertNoLongerExists]
 */
fun SemanticsNodeInteraction.assertIsHidden(): SemanticsNodeInteraction {
    verify({ "The component is visible!" }) {
        it.getOrNull(SemanticsProperties.Hidden) == true
    }

    return this
}

/**
 * Asserts that the component isn't part of the component tree anymore. If the component exists but
 * is hidden use [assertIsHidden] instead.
 */
fun SemanticsNodeInteraction.assertNoLongerExists() {
    if (semanticsTreeInteraction.contains(semanticsTreeNode.data)) {
        throw AssertionError("Assert failed: The component does exist!")
    }
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
 * Asserts the component's value equals the given value. This is used by
 * [CircularProgressIndicator] to check progress.
 * For further details please check [SemanticsConfiguration.accessibilityValue].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction {
    verify({ node -> "Expected value: $value, Actual value: ${node.accessibilityValue}" }) {
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
    assertExists()
    semanticsTreeNode.data.assertEquals(expectedProperties)

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
        throw AssertionError("Found '$size' nodes but exactly '$count' was expected!")
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
    assertExists()

    if (!condition.invoke(semanticsTreeNode.data)) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: ${assertionMessage(semanticsTreeNode.data)}")
    }
}

/**
 * Asserts that the component is still part of the component tree.
 */
internal fun SemanticsNodeInteraction.assertExists() {
    if (!semanticsTreeInteraction.contains(semanticsTreeNode.data)) {
        throw AssertionError("The component does not exist!")
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
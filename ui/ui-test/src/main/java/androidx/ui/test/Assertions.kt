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
        !it.isHidden
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
        it.isHidden
    }

    return this
}

/**
 * Asserts that the component is still part of the component tree.
 */
fun SemanticsNodeInteraction.assertStillExists() {
    if (!semanticsTreeInteraction.contains(semanticsTreeNode.data)) {
        throw AssertionError("Assert failed: The component does not exist!")
    }
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
 */
fun SemanticsNodeInteraction.assertIsChecked(): SemanticsNodeInteraction {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is not checked!" }) {
        it.isChecked == true
    }
    return this
}

/**
 * Asserts that the current component is not checked.
 */
fun SemanticsNodeInteraction.assertIsNotChecked(): SemanticsNodeInteraction {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is checked!" }) {
        it.isChecked != true
    }
    return this
}

/**
 * Asserts that the current component is selected.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction {
        // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be selected" }) {
        it.isSelected == true
    }
    return this
}

/**
 * Asserts that the current component is not selected.
 */
fun SemanticsNodeInteraction.assertIsNotSelected(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to not be selected!" }) {
        it.isSelected == false
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
        { "The component is expected to be mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }
    return this
}

/**
 * Asserts the component's value equals the given value. This is used by
 * [CircularProgressIndicator] to check progress.
 * For further details please check [SemanticsConfiguration.value].
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction {
    verify({ node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
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
    assertStillExists()
    semanticsTreeNode.data.assertEquals(expectedProperties)

    return this
}

/**
 * Asserts that given a list of components, it's size is equal to the passed in size.
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

internal fun SemanticsNodeInteraction.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    assertStillExists()

    if (!condition.invoke(semanticsTreeNode.data)) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: ${assertionMessage(semanticsTreeNode.data)}")
    }
}
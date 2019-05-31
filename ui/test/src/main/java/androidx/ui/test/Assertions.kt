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
 * Asserts that current component is visible.
 */
// TODO(b/123702531): Provide guarantees of being visible VS being actually displayed
fun SemanticsTreeInteraction.assertIsVisible() =
    verifyAssertOnExactlyOne("The component is not visible!") {
        !it.isHidden
    }

/**
 * Asserts that current component is hidden. This requires that the component actually exists in
 * the hierarchy and is hidden. If you want to actually verify that the component does not  exist
 * at all, please use [assertDoesNotExist]
 */
fun SemanticsTreeInteraction.assertIsHidden() =
    verifyAssertOnExactlyOne("The component is visible!") {
        it.isHidden
    }

/**
 * Asserts that there is no component that was matched by the query. If the component exists but is
 * hidden use [assertIsHidden] instead.
 */
fun SemanticsTreeInteraction.assertDoesNotExist(): SemanticsTreeInteraction {
    val foundNodes = findAllMatching()
    if (foundNodes.isNotEmpty()) {
        throw AssertionError("Found '${foundNodes.size}' nodes but 0 was expected!")
    }
    return this
}

/**
 * Asserts that current component is visible.
 */
// TODO(pavlis): Provide guarantees of being visible VS being actually displayed
fun SemanticsTreeInteraction.assertIsChecked() =
    // TODO(pavlis): Throw exception if component is not checkable
    verifyAssertOnExactlyOne("The component is not checked!") {
        it.isChecked == true
    }

fun SemanticsTreeInteraction.assertIsNotChecked() =
    // TODO(pavlis): Throw exception if component is not checkable
    verifyAssertOnExactlyOne("The component is checked!") {
        it.isChecked != true
    }

fun SemanticsTreeInteraction.assertIsSelected(expected: Boolean) =
    // TODO(pavlis): Throw exception if component is not selectable
    verifyAssertOnExactlyOne(
        "The component is expected to be selected = '$expected', but it's not!"
    ) {
        it.isSelected == expected
    }

fun SemanticsTreeInteraction.assertIsInMutuallyExclusiveGroup() =
    // TODO(pavlis): Throw exception if component is not selectable
    verifyAssertOnExactlyOne(
        "The component is expected to be mutually exclusive group, but it's not!"
    ) {
        it.isInMutuallyExclusiveGroup
    }

fun SemanticsTreeInteraction.assertValueEquals(value: String) =
    verifyAssertOnExactlyOne({ node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
    }

fun SemanticsTreeInteraction.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SemanticsTreeInteraction {
    val foundNodes = findAllMatching()
    if (foundNodes.size != 1) {
        throw AssertionError("Found '${foundNodes.size}' nodes but 1 was expected!")
    }
    val nodeSemanticProperties = foundNodes.first().data
    nodeSemanticProperties.assertEquals(expectedProperties)
    return this
}

internal fun SemanticsTreeInteraction.verifyAssertOnExactlyOne(
    assertionMessage: String,
    condition: (SemanticsConfiguration) -> Boolean
): SemanticsTreeInteraction {
    return verifyAssertOnExactlyOne({ assertionMessage }, condition)
}

internal fun SemanticsTreeInteraction.verifyAssertOnExactlyOne(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
): SemanticsTreeInteraction {
    val foundNodes = findAllMatching()
    if (foundNodes.size != 1) {
        throw AssertionError("Found '${foundNodes.size}' nodes but 1 was expected!")
    }
    val node = foundNodes.first().data
    if (!condition.invoke(node)) {
        throw AssertionError("Assert failed: ${assertionMessage(node)}")
    }
    return this
}
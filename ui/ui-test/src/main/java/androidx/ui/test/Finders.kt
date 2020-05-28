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

import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.test.android.SynchronizedTreeCollector

/**
 * Finds a component identified by the given tag.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 *
 * @see find for general find method.
 */
fun findByTag(testTag: String): SemanticsNodeInteraction =
    find(hasTestTag(testTag))

/**
 * Finds all components identified by the given tag.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 */
fun findAllByTag(testTag: String): SemanticsNodeInteractionCollection =
    findAll(hasTestTag(testTag))

/**
 * Finds a component with the given text as its accessibilityLabel.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 * @see findBySubstring to search by substring instead of via exact match.
 * @see find for general find method.
 */
fun findByText(text: String, ignoreCase: Boolean = false): SemanticsNodeInteraction =
    find(hasText(text, ignoreCase))

/**
 *  Finds a component with accessibilityLabel that contains the given substring.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 * @see findByText to perform exact matches.
 * @see find for general find method.
 */
fun findBySubstring(text: String, ignoreCase: Boolean = false): SemanticsNodeInteraction =
    find(hasSubstring(text, ignoreCase))

/**
 * Finds all components with the given text as their accessibility label.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 */
fun findAllByText(text: String, ignoreCase: Boolean = false): SemanticsNodeInteractionCollection =
    findAll(hasText(text, ignoreCase))

/**
 * Finds the root semantics node of the Compose tree.  Useful for example for screenshot tests
 * of the entire scene.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 */
fun findRoot(): SemanticsNodeInteraction =
    find(isRoot())

/**
 * Finds a component that matches the given condition.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 * @see findAll to work with multiple elements
 */
fun find(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(SemanticsSelector(matcher))
}

/**
 * Finds all components that match the given condition.
 *
 * If you are working with elements that are not supposed to occur multiple times use [find]
 * instead.
 * @see find
 */
fun findAll(matcher: SemanticsMatcher): SemanticsNodeInteractionCollection {
    return SemanticsNodeInteractionCollection(SemanticsSelector(matcher))
}

internal fun getAllSemanticsNodes(): List<SemanticsNode> {
    return SynchronizedTreeCollector.collectAllSemanticsNodes()
}
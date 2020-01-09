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

import androidx.ui.core.SemanticsTreeNode

internal fun SemanticsNodeInteraction(
    semanticsTreeNode: SemanticsTreeNode,
    semanticsTreeInteraction: SemanticsTreeInteraction
): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(listOf(semanticsTreeNode), semanticsTreeInteraction)
}

/**
 * Represents a component with which one can interact with the hierarchy.
 * Examples of interactions include [findByTag], [isToggleable], [assertIsOn], [doClick]
 *
 * Example usage:
 * findByTag("myCheckbox")
 *    .doClick()
 *    .assertIsOn()
 */
class SemanticsNodeInteraction internal constructor(
    private val semanticsTreeNodes: List<SemanticsTreeNode>,
    internal val semanticsTreeInteraction: SemanticsTreeInteraction
) {

    internal val semanticsTreeNode: SemanticsTreeNode
    get() {
        if (semanticsTreeNodes.size != 1) {
            // TODO(b/133217292)
            throw AssertionError(
                "Found '${semanticsTreeNodes.size}' nodes but exactly '1' was expected!")
        }
        return semanticsTreeNodes.first()
    }

    /**
     * Asserts that no item was found or that the item is no longer in the hierarchy.
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertDoesNotExist() {
        if (semanticsTreeNodes.isEmpty()) {
            return
        }

        if (semanticsTreeNodes.size > 1) {
            // TODO(b/133217292)
            throw AssertionError(
                "Found '${semanticsTreeNodes.size}' components that match, expected '0' components")
        }

        // We have exactly one
        if (semanticsTreeInteraction.contains(semanticsTreeNodes[0].data)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: The component does exist!")
        }
    }

    /**
     * Asserts that the component was found and is part of the component tree.
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertExists() {
        if (semanticsTreeNodes.size != 1) {
            // TODO(b/133217292)
            throw AssertionError(
                "Found '${semanticsTreeNodes.size}' components that match, expected '1' components")
        }

        if (!semanticsTreeInteraction.contains(semanticsTreeNodes[0].data)) {
            // TODO(b/133217292)
            throw AssertionError("The component does not exist!")
        }
    }
}
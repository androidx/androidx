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

internal fun SemanticsNodeInteraction(
    nodeId: Int,
    semanticsTreeInteraction: SemanticsTreeInteraction
): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(listOf(nodeId), semanticsTreeInteraction)
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
    internal val nodeIds: List<Int>,
    internal val semanticsTreeInteraction: SemanticsTreeInteraction
) {
    internal val semanticsNodes: List<SemanticsNode>
        get() = semanticsTreeInteraction.getNodesByIds(nodeIds)

    /**
     * Returns the semantics node captured by this object.
     *
     * Note: Accessing this object involves synchronization with UI. This means that when using this
     * object the UI should be stable. If you are accessing this multiple times in one atomic
     * operation, it is better to cache the result instead of calling this API multiple times.
     *
     * @throws AssertionError if 0 or multiple nodes found.
     */
    val semanticsNode: SemanticsNode
        get() {
            if (semanticsNodes.size != 1) {
                // TODO(b/133217292)
                throw AssertionError(
                    "Found '${semanticsNodes.size}' nodes but exactly '1' was expected!"
                )
            }
            return semanticsNodes.first()
        }

    /**
     * Asserts that no item was found or that the item is no longer in the hierarchy.
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertDoesNotExist() {
        if (semanticsNodes.isNotEmpty()) {
            throw AssertionError(
                "Found '${semanticsNodes.size}' components that match, expected '0' components"
            )
        }
    }

    /**
     * Asserts that the component was found and is part of the component tree.
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertExists() {
        if (semanticsNodes.size != 1) {
            // TODO(b/133217292)
            throw AssertionError(
                "Found '${semanticsNodes.size}' components that match, expected '1' components"
            )
        }
    }
}
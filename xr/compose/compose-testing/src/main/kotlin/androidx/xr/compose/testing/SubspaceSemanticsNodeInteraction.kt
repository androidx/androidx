/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.testing

import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * Represents a semantics node and the path to fetch it from the semantics tree. One can perform
 * assertions or navigate to other nodes such as [onChildren].
 *
 * An instance of [SubspaceSemanticsNodeInteraction] can be obtained from [onSubspaceNode] and
 * convenience methods that use a specific filter.
 */
public class SubspaceSemanticsNodeInteraction
internal constructor(
    private val testContext: SubspaceTestContext,
    private val selector: SubspaceSemanticsSelector,
) {
    public constructor(
        testContext: SubspaceTestContext,
        matcher: SubspaceSemanticsMatcher,
    ) : this(testContext, SubspaceSemanticsSelector(matcher))

    private fun fetchSemanticsNodes(
        atLeastOneRootRequired: Boolean,
        errorMessageOnFail: String? = null,
    ): SubspaceSelectionResult {
        return selector.map(
            testContext.getAllSemanticsNodes(atLeastOneRootRequired = atLeastOneRootRequired),
            errorMessageOnFail.orEmpty(),
        )
    }

    /**
     * Returns the semantics node captured by this object.
     *
     * Note: Accessing this object involves synchronization with your UI. If you are accessing this
     * multiple times in one atomic operation, it is better to cache the result instead of calling
     * this API multiple times.
     *
     * This will fail if there is 0 or multiple nodes matching.
     *
     * @param errorMessageOnFail Error message prefix to be added to the message in case this fetch
     *   fails. This is typically used by operations that rely on this assert. Example prefix could
     *   be: "Failed to perform doOnClick.".
     * @throws [AssertionError] if 0 or multiple nodes found.
     */
    public fun fetchSemanticsNode(errorMessageOnFail: String? = null): SubspaceSemanticsInfo {
        return fetchOneOrThrow(errorMessageOnFail)
    }

    /**
     * Asserts that no item was found or that the item is no longer in the hierarchy.
     *
     * This will synchronize with the UI and fetch all the nodes again to ensure it has latest data.
     *
     * @throws [AssertionError] if the assert fails.
     */
    public fun assertDoesNotExist() {
        val result =
            fetchSemanticsNodes(
                atLeastOneRootRequired = false,
                errorMessageOnFail = "Failed: assertDoesNotExist.",
            )
        if (result.selectedNodes.isNotEmpty()) {
            throw AssertionError(
                "Failed: assertDoesNotExist. Expected 0 but found ${result.selectedNodes.size} nodes."
            )
        }
    }

    /**
     * Asserts that the component was found and is part of the component tree.
     *
     * This will synchronize with the UI and fetch all the nodes again to ensure it has latest data.
     * If you are using [fetchSemanticsNode] you don't need to call this. In fact you would just
     * introduce additional overhead.
     *
     * @param errorMessageOnFail Error message prefix to be added to the message in case this assert
     *   fails. This is typically used by operations that rely on this assert. Example prefix could
     *   be: "Failed to perform doOnClick.".
     * @throws [AssertionError] if the assert fails.
     */
    @CanIgnoreReturnValue
    public fun assertExists(errorMessageOnFail: String? = null): SubspaceSemanticsNodeInteraction {
        fetchOneOrThrow(errorMessageOnFail)
        return this
    }

    @CanIgnoreReturnValue
    private fun fetchOneOrThrow(errorMessageOnFail: String? = null): SubspaceSemanticsInfo {
        val finalErrorMessage = errorMessageOnFail ?: "Failed: assertExists."

        val result =
            fetchSemanticsNodes(
                atLeastOneRootRequired = true,
                errorMessageOnFail = finalErrorMessage
            )
        if (result.selectedNodes.count() != 1) {
            if (result.customErrorOnNoMatch != null) {
                throw AssertionError(finalErrorMessage + "\n" + result.customErrorOnNoMatch)
            }

            throw AssertionError(finalErrorMessage)
        }

        return result.selectedNodes.first()
    }
}

/**
 * Represents a collection of semantics nodes and the path to fetch them from the semantics tree.
 * One can interact with these nodes by performing assertions such as [assertCountEquals], or
 * navigate to other nodes such as [get].
 *
 * An instance of [SubspaceSemanticsNodeInteractionCollection] can be obtained from
 * [onAllSubspaceNodes] and convenience methods that use a specific filter, such as
 * [onAllNodesWithText].
 */
public class SubspaceSemanticsNodeInteractionCollection
private constructor(
    internal val testContext: SubspaceTestContext,
    internal val selector: SubspaceSemanticsSelector,
) {
    @Suppress("PrimitiveInCollection") private var nodeIds: List<Int>? = null

    public constructor(
        testContext: SubspaceTestContext,
        matcher: SubspaceSemanticsMatcher,
    ) : this(testContext, SubspaceSemanticsSelector(matcher))

    /**
     * Returns the semantics nodes captured by this object.
     *
     * Note: Accessing this object involves synchronization with your UI. If you are accessing this
     * multiple times in one atomic operation, it is better to cache the result instead of calling
     * this API multiple times.
     *
     * @param atLeastOneRootRequired Whether to throw an error in case there is no compose content
     *   in the current test app.
     * @param errorMessageOnFail Error message prefix to be added to the message in case this fetch
     *   fails. This is typically used by operations that rely on this assert. Example prefix could
     *   be: "Failed to perform doOnClick.".
     */
    private fun fetchSemanticsNodes(
        atLeastOneRootRequired: Boolean = true,
        errorMessageOnFail: String? = null,
    ): List<SubspaceSemanticsInfo> {
        if (nodeIds == null) {
            return selector
                .map(
                    testContext.getAllSemanticsNodes(atLeastOneRootRequired),
                    errorMessageOnFail.orEmpty()
                )
                .apply { nodeIds = selectedNodes.map { it.semanticsId }.toList() }
                .selectedNodes
        }

        return testContext.getAllSemanticsNodes(atLeastOneRootRequired).filter {
            it.semanticsId in nodeIds!!
        }
    }

    /**
     * Retrieve node at the given index of this collection.
     *
     * Any subsequent operation on its result will expect exactly one element found (unless
     * [SubspaceSemanticsNodeInteraction.assertDoesNotExist] is used) and will throw
     * [AssertionError] if none or more than one element is found.
     */
    private operator fun get(index: Int): SubspaceSemanticsNodeInteraction {
        return SubspaceSemanticsNodeInteraction(testContext, selector.addIndexSelector(index))
    }
}

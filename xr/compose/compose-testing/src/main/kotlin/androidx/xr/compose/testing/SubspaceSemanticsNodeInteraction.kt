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
 * Represents a semantics node and the path to fetch it from the subspace semantics tree.
 *
 * Allows performing assertions or retrieving the underlying semantics node information. An instance
 * of this class can be obtained from `onSubspaceNode` or convenience methods that use a specific
 * filter, such as `onSubspaceNodeWithTag`.
 *
 * For real usage patterns and explicit semantic properties validation, see the referenced samples.
 *
 * @sample androidx.xr.compose.testing.samples.subspacePanelRenderedAndInteractive
 * @sample androidx.xr.compose.testing.samples.subspaceNodeMatcherProperties
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
     * Returns the semantics node captured by this interaction.
     *
     * This operation synchronizes with the UI to ensure the latest semantics tree data is fetched.
     * Since synchronization introduces performance overhead, it is recommended to cache the result
     * if accessed multiple times within a single atomic operation.
     *
     * @param errorMessageOnFail the prefix to be added to the error message if the fetch operation
     *   fails. Typically used by higher-level operations that rely on this fetch.
     * @return the underlying `SubspaceSemanticsInfo` corresponding to the matched node.
     * @throws AssertionError if zero or multiple matching nodes are found.
     */
    public fun fetchSemanticsNode(errorMessageOnFail: String? = null): SubspaceSemanticsInfo {
        return fetchOneOrThrow(errorMessageOnFail)
    }

    /**
     * Asserts that no matching item was found or that the item is no longer in the hierarchy.
     *
     * This operation synchronizes with the UI and fetches all nodes to ensure it has the latest
     * data. It is useful for verifying that an element has been correctly removed or was never
     * present.
     *
     * @throws AssertionError if one or more matching nodes are found.
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
     * This operation synchronizes with the UI and fetches all nodes to ensure it has the latest
     * data. Note that if you are already calling `fetchSemanticsNode`, calling this method is
     * redundant and introduces unnecessary overhead.
     *
     * @param errorMessageOnFail the prefix to be added to the error message if the assertion fails.
     *   Typically used by operations that rely on this assertion.
     * @return this interaction object to allow chaining of further assertions.
     * @throws AssertionError if zero or multiple matching nodes are found.
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
                errorMessageOnFail = finalErrorMessage,
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
 * Represents a collection of semantics nodes and the path to fetch them from the subspace semantics
 * tree.
 *
 * One can interact with these nodes by fetching their semantics information or accessing a specific
 * node via its index. An instance of this collection can be obtained from `onAllSubspaceNodes` and
 * convenience methods that use a specific filter, such as `onAllSubspaceNodesWithTag`.
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
     * Returns the list of semantics nodes captured by this collection.
     *
     * This operation involves synchronization with the UI to fetch the latest node data. If
     * accessed multiple times in a single atomic operation, caching the result is recommended to
     * avoid performance overhead.
     *
     * @param atLeastOneRootRequired whether to throw an error if there is no compose content found
     *   in the current test application.
     * @param errorMessageOnFail the prefix to be added to the error message if the fetch operation
     *   fails.
     * @return the list of matched `SubspaceSemanticsInfo` instances.
     */
    public fun fetchSemanticsNodes(
        atLeastOneRootRequired: Boolean = true,
        errorMessageOnFail: String? = null,
    ): List<SubspaceSemanticsInfo> {
        if (nodeIds == null) {
            return selector
                .map(
                    testContext.getAllSemanticsNodes(atLeastOneRootRequired),
                    errorMessageOnFail.orEmpty(),
                )
                .apply { nodeIds = selectedNodes.map { it.semanticsId }.toList() }
                .selectedNodes
        }

        return testContext.getAllSemanticsNodes(atLeastOneRootRequired).filter {
            it.semanticsId in nodeIds!!
        }
    }

    /**
     * Retrieves the interaction for the node at the given index in this collection.
     *
     * Any subsequent operation on the returned interaction will expect exactly one element to be
     * found (unless `SubspaceSemanticsNodeInteraction.assertDoesNotExist` is used) and will fail if
     * zero or multiple elements are found.
     *
     * @param index the zero-based index of the node to retrieve from the matched collection.
     * @return the interaction corresponding to the node at the specified index.
     */
    public operator fun get(index: Int): SubspaceSemanticsNodeInteraction {
        return SubspaceSemanticsNodeInteraction(testContext, selector.addIndexSelector(index))
    }
}

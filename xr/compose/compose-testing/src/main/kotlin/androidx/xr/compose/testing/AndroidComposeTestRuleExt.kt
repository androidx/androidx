/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.ui.test.junit4.AndroidComposeTestRule

/**
 * Finds a semantics node in the Subspace hierarchy that matches the given condition.
 *
 * This function only locates nodes within the Subspace hierarchy and does not include nodes from
 * standard 2D compose contexts. For example, it targets `SpatialPanel`, `SpatialRow`, or
 * `SpatialColumn` nodes but not standard `Row`, `Column`, or `Text` nodes. For locating 2D nodes,
 * use `AndroidComposeTestRule.onNode`.
 *
 * Any subsequent operation on the returned interaction expects exactly one element to be found
 * (unless `SubspaceSemanticsNodeInteraction.assertDoesNotExist` is used) and throws an
 * `AssertionError` if zero or multiple elements are found.
 *
 * @sample androidx.xr.compose.testing.samples.subspacePanelRenderedAndInteractive
 * @sample androidx.xr.compose.testing.samples.subspaceNodeMatcherProperties
 * @param matcher the `SubspaceSemanticsMatcher` used to identify the matching semantics node.
 * @return the `SubspaceSemanticsNodeInteraction` for the matched node.
 * @see AndroidComposeTestRule.onAllSubspaceNodes
 */
public fun AndroidComposeTestRule<*, *>.onSubspaceNode(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsNodeInteraction =
    SubspaceSemanticsNodeInteraction(SubspaceTestContext(this), matcher)

/**
 * Finds all semantics nodes in the Subspace hierarchy that match the given condition.
 *
 * This function only locates nodes within the Subspace hierarchy and does not include nodes from
 * standard 2D compose contexts. For locating 2D nodes, use `AndroidComposeTestRule.onAllNodes`. If
 * you are dealing with elements guaranteed to occur exactly once, prefer using `onSubspaceNode` to
 * enforce uniqueness constraints and improve clarity.
 *
 * @sample androidx.xr.compose.testing.samples.subspacePanelRenderedAndInteractive
 * @sample androidx.xr.compose.testing.samples.subspaceNodeMatcherProperties
 * @param matcher the `SubspaceSemanticsMatcher` used to filter the semantics nodes.
 * @return the `SubspaceSemanticsNodeInteractionCollection` containing all matching nodes.
 * @see AndroidComposeTestRule.onSubspaceNode
 */
public fun AndroidComposeTestRule<*, *>.onAllSubspaceNodes(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsNodeInteractionCollection =
    SubspaceSemanticsNodeInteractionCollection(SubspaceTestContext(this), matcher)

/**
 * Finds a semantics node in the Subspace hierarchy identified by the provided test tag.
 *
 * This convenience function specifically searches for nodes within the Subspace hierarchy and does
 * not locate standard 2D compose elements. The search evaluates for an exact string match on the
 * test tag.
 *
 * @sample androidx.xr.compose.testing.samples.subspacePanelRenderedAndInteractive
 * @sample androidx.xr.compose.testing.samples.subspaceNodeMatcherProperties
 * @param testTag the specific tag string to search for within the hierarchy.
 * @return the `SubspaceSemanticsNodeInteraction` for the matched node.
 * @see AndroidComposeTestRule.onSubspaceNode
 */
public fun AndroidComposeTestRule<*, *>.onSubspaceNodeWithTag(
    testTag: String
): SubspaceSemanticsNodeInteraction = onSubspaceNode(hasTestTag(testTag))

/**
 * Finds all semantics nodes in the Subspace hierarchy identified by the provided test tag.
 *
 * This convenience function specifically searches for nodes within the Subspace hierarchy and does
 * not locate standard 2D compose elements. The search evaluates for an exact string match on the
 * test tag.
 *
 * @sample androidx.xr.compose.testing.samples.subspacePanelRenderedAndInteractive
 * @sample androidx.xr.compose.testing.samples.subspaceNodeMatcherProperties
 * @param testTag the specific tag string to search for within the hierarchy.
 * @return the `SubspaceSemanticsNodeInteractionCollection` containing all matching nodes.
 * @see AndroidComposeTestRule.onAllSubspaceNodes
 */
public fun AndroidComposeTestRule<*, *>.onAllSubspaceNodesWithTag(
    testTag: String
): SubspaceSemanticsNodeInteractionCollection = onAllSubspaceNodes(hasTestTag(testTag))

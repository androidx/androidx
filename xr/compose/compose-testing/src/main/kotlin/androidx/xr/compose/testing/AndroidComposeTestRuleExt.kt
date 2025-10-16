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

import androidx.annotation.RestrictTo
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.xr.runtime.Session

/**
 * The XR [Session] for the current [androidx.compose.ui.test.junit4.AndroidComposeTestRule].
 *
 * This will be null until the value is set or `LocalSession.current` is accessed in compose, after
 * which the value will be non-null and return the current [Session]. Setting this value after
 * calling `setContent` will not change the Session that is used for that content block. Setting the
 * value to null will indicate that the default test Session should be used.
 */
public var AndroidComposeTestRule<*, *>.session: Session?
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    get() =
        activity.window.decorView.getTag(androidx.xr.compose.R.id.compose_xr_session) as? Session
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    set(value) {
        activity.window.decorView.setTag(androidx.xr.compose.R.id.compose_xr_session, value)
    }

/**
 * Finds a semantics node (in the Subspace hierarchy) that matches the given condition.
 *
 * This only locates nodes in the Subspace hierarchy and will not include nodes from 2D compose
 * contexts. For example, it will return SpatialPanel, SpatialRow, or SpatialColumn nodes, but it
 * will not return Row, Column, or Text nodes. For 2D nodes, use [AndroidComposeTestRule.onNode].
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SubspaceSemanticsNodeInteraction.assertDoesNotExist] is used) and will throw an [AssertionError]
 * if none or more than one element is found.
 *
 * For usage patterns and semantics concepts see [SubspaceSemanticsNodeInteraction]
 *
 * @param matcher Matcher used for filtering
 * @see onAllSubspaceNodes to work with multiple elements
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun AndroidComposeTestRule<*, *>.onSubspaceNode(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsNodeInteraction =
    SubspaceSemanticsNodeInteraction(SubspaceTestContext(this), matcher)

/**
 * Finds all semantics nodes (in the Subspace hierarchy) that match the given condition.
 *
 * This only locates nodes in the Subspace hierarchy and will not include nodes from 2D compose
 * contexts. For example, it will return SpatialPanel, SpatialRow, or SpatialColumn nodes, but it
 * will not return Row, Column, or Text nodes. For 2D nodes, use [AndroidComposeTestRule.onNode].
 *
 * If you are working with elements that are not supposed to occur multiple times use
 * [onSubspaceNode] instead.
 *
 * For usage patterns and semantics concepts see [SubspaceSemanticsNodeInteraction]
 *
 * @param matcher Matcher used for filtering.
 * @see onSubspaceNode
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun AndroidComposeTestRule<*, *>.onAllSubspaceNodes(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsNodeInteractionCollection =
    SubspaceSemanticsNodeInteractionCollection(SubspaceTestContext(this), matcher)

/**
 * Finds a semantics node (in the Subspace hierarchy) identified by the given tag.
 *
 * This only locates nodes in the Subspace hierarchy and will not include nodes from 2D compose
 * contexts. For example, it will return SpatialPanel, SpatialRow, or SpatialColumn nodes, but it
 * will not return Row, Column, or Text nodes. For 2D nodes, use [AndroidComposeTestRule.onNode].
 *
 * For usage patterns and semantics concepts see [SubspaceSemanticsNodeInteraction]
 *
 * @param testTag The tag to search for. Looks for an exact match only.
 * @see onSubspaceNode for more information.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun AndroidComposeTestRule<*, *>.onSubspaceNodeWithTag(
    testTag: String
): SubspaceSemanticsNodeInteraction = onSubspaceNode(hasTestTag(testTag))

/**
 * Finds all semantics nodes (in the Subspace hierarchy) identified by the given tag.
 *
 * This only locates nodes in the Subspace hierarchy and will not include nodes from 2D compose
 * contexts. For example, it will return SpatialPanel, SpatialRow, or SpatialColumn nodes, but it
 * will not return Row, Column, or Text nodes. For 2D nodes, use [AndroidComposeTestRule.onNode].
 *
 * For usage patterns and semantics concepts see [SubspaceSemanticsNodeInteraction]
 *
 * @param testTag The tag to search for. Looks for an exact matches only.
 * @see onSubspaceNode for more information.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun AndroidComposeTestRule<*, *>.onAllSubspaceNodesWithTag(
    testTag: String
): SubspaceSemanticsNodeInteractionCollection = onAllSubspaceNodes(hasTestTag(testTag))

/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.testing.unit

import androidx.annotation.RestrictTo
import androidx.glance.Emittable
import androidx.glance.testing.GlanceNodeAssertion
import androidx.glance.testing.GlanceNodeAssertionCollection
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.TestContext
import androidx.glance.testing.matcherToSelector

// Equivalent to calling GlanceNodeAssertionsProvider.onNode
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun getGlanceNodeAssertionFor(
    emittable: Emittable,
    onNodeMatcher: GlanceNodeMatcher<MappedNode>
): GlanceNodeAssertion<MappedNode, GlanceMappedNode> {
    val testContext = TestContext<MappedNode, GlanceMappedNode>()
    testContext.rootGlanceNode = GlanceMappedNode(emittable)
    return GlanceNodeAssertion(
        testContext = testContext,
        selector = onNodeMatcher.matcherToSelector()
    )
}

// Equivalent to calling GlanceNodeAssertionsProvider.onAllNodes
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun getGlanceNodeAssertionCollectionFor(
    emittable: Emittable,
    onAllNodesMatcher: GlanceNodeMatcher<MappedNode>
): GlanceNodeAssertionCollection<MappedNode, GlanceMappedNode> {
    val testContext = TestContext<MappedNode, GlanceMappedNode>()
    testContext.rootGlanceNode = GlanceMappedNode(emittable)
    return GlanceNodeAssertionCollection(
        testContext = testContext,
        selector = onAllNodesMatcher.matcherToSelector()
    )
}

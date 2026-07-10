/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.tooling.data

import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionInstance
import kotlin.test.assertEquals
import org.junit.Test

class CompositionDataTreeTest {

    // A fake that implements both CompositionData and CompositionInstance
    private class FakeCompositionInstance(
        override val parent: CompositionInstance?,
        val groups: List<CompositionGroup>,
        private val contextGroup: CompositionGroup?,
    ) : CompositionInstance, CompositionData {

        override val data: CompositionData
            get() = this

        override val compositionGroups: Iterable<CompositionGroup>
            get() = groups

        override val isEmpty: Boolean
            get() = groups.isEmpty()

        override fun findContextGroup(): CompositionGroup? = contextGroup

        override fun find(identityToFind: Any): CompositionGroup? {
            return groups.firstOrNull { it.identity == identityToFind }
        }
    }

    private class FakeCompositionGroup(
        override val key: Any = 0,
        override val node: Any? = null,
        override val data: Iterable<Any?> = emptyList(),
        override val compositionGroups: Iterable<CompositionGroup> = emptyList(),
        override val identity: Any? = null,
    ) : CompositionGroup {
        override val sourceInfo: String? = null
        override val isEmpty: Boolean
            get() = compositionGroups.none()
    }

    private data class TestNode(val name: String, val children: List<TestNode>)

    @OptIn(UiToolingDataApi::class)
    @Test
    fun testUnanchoredChildFallbackStitching() {
        // Regression test for b/507836071
        // 1. Create Parent Composition
        val parentGroup = FakeCompositionGroup(key = "parent_root")
        val parentInstance =
            FakeCompositionInstance(
                parent = null,
                groups = listOf(parentGroup),
                contextGroup = null,
            )

        // 2. Create Child Composition (simulating transitive/unanchored state)
        val childGroup = FakeCompositionGroup(key = "child_root")
        val childInstance =
            FakeCompositionInstance(
                parent = parentInstance,
                groups = listOf(childGroup),
                contextGroup = null, // This is the bug condition: findContextGroup() returns null!
            )

        // 3. Run makeTree
        val compositions = setOf<CompositionData>(parentInstance, childInstance)

        val createNode =
            {
                group: CompositionGroup,
                _: SourceContext,
                children: List<TestNode>,
                stitched: List<TestNode> ->
                TestNode(name = "group_${group.key}", children = children + stitched)
            }

        val createResult =
            { _: CompositionInstance, node: TestNode?, _: List<CompositionInstance> ->
                node ?: TestNode("empty_instance", emptyList())
            }

        // Under the original code, this call would crash with NullPointerException.
        // Under the fixed code, it should complete successfully.
        val results =
            compositions.makeTree(
                prepareResult = {},
                createNode = createNode,
                createResult = createResult,
            )

        // 4. Assertions
        assertEquals(1, results.size, "Should return exactly one root result")
        val rootResult = results.first()

        assertEquals("group_parent_root", rootResult.name)
        assertEquals(1, rootResult.children.size, "Parent should have exactly one child stitched")

        val childResult = rootResult.children.first()
        assertEquals("group_child_root", childResult.name)
    }
}

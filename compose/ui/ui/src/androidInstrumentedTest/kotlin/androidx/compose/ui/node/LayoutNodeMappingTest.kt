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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.Row
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutNodeMappingTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var owner: Owner

    @Test
    fun basicTest() {
        rule.setTestContent { Column { repeat(5) { Box(Modifier.size(10.dp)) } } }
        rule.waitForIdle()

        // 6 Compsoables + setContent Root
        assertMappingConsistency(7)
    }

    @Test
    fun consistent_afterTogglingContent() {
        var showAll by mutableStateOf(true)

        rule.setTestContent {
            Column {
                repeat(5) { Box(Modifier.size(10.dp)) }
                if (showAll) {
                    repeat(5) { Box(Modifier.size(10.dp)) }
                }
            }
        }
        rule.waitForIdle()
        assertMappingConsistency(12)

        // Remove content, the mapping should reflect the change
        showAll = false
        rule.waitForIdle()
        assertMappingConsistency(7)

        // Add it back, we should get the same initial result
        showAll = true
        rule.waitForIdle()
        assertMappingConsistency(12)
    }

    /**
     * Test case for when several LayoutNodes are re-used.
     *
     * We test that the mapping size after removing the LazyList is consistent.
     */
    @Test
    fun cleanupCount_afterScrollingLazyList() =
        with(rule.density) {
            val rootSizePx = 600f
            val viewPortCount = 4
            val pages = 3

            val listItemHeight = rootSizePx / viewPortCount

            var showAll by mutableStateOf(true)

            @Composable
            fun LazyItemScope.MyItem() {
                Row(Modifier.fillParentMaxWidth().height(listItemHeight.toDp())) {
                    repeat(3) {
                        Box(Modifier.size((rootSizePx / 3f).toDp(), listItemHeight.toDp()))
                    }
                }
            }

            val lazyListState = LazyListState()

            rule.setTestContent {
                Box {
                    if (showAll) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.size(rootSizePx.toDp()),
                        ) {
                            items(viewPortCount * pages) { MyItem() }
                        }
                    }
                }
            }

            // Reproducible set of calls that re-use content on the screen
            var itemToScroll = pages * viewPortCount - viewPortCount
            // Scroll to last visible set of items
            rule.runOnIdle { lazyListState.requestScrollToItem(itemToScroll) }

            // Scroll two items back
            repeat(2) { rule.runOnIdle { lazyListState.requestScrollToItem(--itemToScroll) } }

            // Scroll two items forward
            repeat(2) { rule.runOnIdle { lazyListState.requestScrollToItem(++itemToScroll) } }
            rule.waitForIdle()

            // Note that not every registered LayoutNode is an 'active' LayoutNode
            assertMappingConsistency(43)

            // Remove LazyList, and assert that the mapping is still representative of what's on
            // screen
            showAll = false
            rule.waitForIdle()
            assertMappingConsistency(2)
        }

    /** Sets the test [owner] property. */
    private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
        setContent {
            owner = LocalView.current as Owner
            content()
        }
    }

    /**
     * Asserts the `id to LayoutNode` map has the [expectedSize] and that the keys match their
     * corresponding [LayoutNode.semanticsId] value.
     */
    private fun assertMappingConsistency(expectedSize: Int) {
        assertEquals(
            expected = expectedSize,
            actual = owner.layoutNodes.size,
            message = "Unexpected Map size",
        )
        owner.layoutNodes.forEach { key, value ->
            assertEquals(key, value.semanticsId, "Non-matching keys")
        }
    }
}

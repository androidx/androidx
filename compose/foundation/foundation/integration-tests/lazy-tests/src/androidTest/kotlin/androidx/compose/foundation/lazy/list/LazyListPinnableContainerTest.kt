/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.PinnableContainer.PinnedHandle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyListPinnableContainerTest(val useLookaheadScope: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useLookahead = {0}")
        fun params() = arrayOf(true, false)
    }

    @get:Rule
    val rule = createComposeRule()

    private var pinnableContainer: PinnableContainer? = null

    private val itemSizePx = 10
    private var itemSize = Dp.Unspecified

    private val composed = mutableSetOf<Int>()

    @Before
    fun setup() {
        itemSize = with(rule.density) { itemSizePx.toDp() }
    }

    private inline fun ComposeContentTestRule.setContentParameterized(
        crossinline content: @Composable () -> Unit
    ) {
        setContent {
            if (useLookaheadScope) {
                LookaheadScope {
                    content()
                }
            } else {
                content()
            }
        }
    }

    @Composable
    fun Item(index: Int) {
        Box(
            Modifier
                .size(itemSize)
                .testTag("$index")
        )
        DisposableEffect(index) {
            composed.add(index)
            onDispose {
                composed.remove(index)
            }
        }
    }

    @Test
    fun pinnedItemIsComposedAndPlacedWhenScrolledOut() {
        val state = LazyListState()
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            runBlocking {
                state.scrollToItem(3)
            }
        }

        rule.waitUntil {
            // not visible items were disposed
            !composed.contains(0)
        }

        rule.runOnIdle {
            // item 1 is still pinned
            assertThat(composed).contains(1)
        }

        rule.onNodeWithTag("1")
            .assertExists()
            .assertIsNotDisplayed()
            .assertIsPlaced()
    }

    @Test
    fun itemsBetweenPinnedAndCurrentVisibleAreNotComposed() {
        val state = LazyListState()
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(4)
            }
        }

        rule.waitUntil {
            // not visible items were disposed
            !composed.contains(0)
        }

        rule.runOnIdle {
            assertThat(composed).doesNotContain(0)
            assertThat(composed).contains(1)
            assertThat(composed).doesNotContain(2)
            assertThat(composed).doesNotContain(3)
            assertThat(composed).contains(4)
        }
    }

    @Test
    fun pinnedItemAfterVisibleOnesIsComposedAndPlacedWhenScrolledOut() {
        val state = LazyListState()
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 4) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(4)
            }
        }

        rule.waitUntil {
            // wait for not visible items to be disposed
            !composed.contains(1)
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
            assertThat(composed).contains(5)
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(0)
            }
            if (useLookaheadScope) {
                // Force another lookahead measure pass, because lookahead pass by design keeps
                // content from last measure pass until it's no longer needed in either pass.
                runBlocking {
                    state.scrollToItem(0)
                }
            }
        }

        rule.waitUntil {
            // wait for not visible items to be disposed
            !composed.contains(5)
        }

        rule.runOnIdle {
            assertThat(composed).contains(0)
            assertThat(composed).contains(1)
            assertThat(composed).doesNotContain(2)
            assertThat(composed).doesNotContain(3)
            assertThat(composed).contains(4)
            assertThat(composed).doesNotContain(5)
        }
    }

    @Test
    fun pinnedItemCanBeUnpinned() {
        val state = LazyListState()
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        val handle = rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3)
            }
        }

        rule.waitUntil {
            // wait for not visible items to be disposed
            !composed.contains(0)
        }

        rule.runOnIdle {
            handle.release()
        }

        rule.waitUntil {
            // wait for unpinned item to be disposed
            !composed.contains(1)
        }

        rule.onNodeWithTag("1")
            .assertIsNotPlaced()
    }

    @Test
    fun pinnedItemIsStillPinnedWhenReorderedAndNotVisibleAnymore() {
        val state = LazyListState()
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 3), state = state) {
                items(list, key = { it }) { index ->
                    if (index == 2) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            assertThat(composed).containsExactly(0, 1, 2)
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            list = listOf(0, 3, 4, 1, 2)
        }

        rule.waitUntil {
            // wait for not visible item to be disposed
            !composed.contains(1)
        }

        rule.runOnIdle {
            assertThat(composed).containsExactly(0, 3, 4, 2) // 2 is pinned
        }

        rule.onNodeWithTag("2")
            .assertIsPlaced()
    }

    @Test
    fun unpinnedWhenLazyListStateChanges() {
        var state by mutableStateOf(LazyListState(firstVisibleItemIndex = 2))
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 2) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            assertThat(composed).contains(3)
            runBlocking {
                state.scrollToItem(0)
            }
            if (useLookaheadScope) {
                // Force another lookahead measure pass, because lookahead pass by design keeps
                // content from last measure pass until it's no longer needed in either pass.
                runBlocking {
                    state.scrollToItem(0)
                }
            }
        }

        rule.waitUntil {
            // wait for not visible item to be disposed
            !composed.contains(3)
        }

        rule.runOnIdle {
            assertThat(composed).contains(2)
            state = LazyListState()
        }

        rule.waitUntil {
            // wait for pinned item to be disposed
            !composed.contains(2)
        }

        rule.onNodeWithTag("2")
            .assertIsNotPlaced()
    }

    @Test
    fun pinAfterLazyListStateChange() {
        var state by mutableStateOf(LazyListState())
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 0) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            state = LazyListState()
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            runBlocking {
                state.scrollToItem(2)
            }
        }

        rule.waitUntil {
            // wait for not visible item to be disposed
            !composed.contains(1)
        }

        rule.runOnIdle {
            assertThat(composed).contains(0)
        }
    }

    @Test
    fun itemsArePinnedBasedOnGlobalIndexes() {
        val state = LazyListState(firstVisibleItemIndex = 3)
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                repeat(100) { index ->
                    item {
                        if (index == 3) {
                            pinnableContainer = LocalPinnableContainer.current
                        }
                        Item(index)
                    }
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            assertThat(composed).contains(4)
            runBlocking {
                state.scrollToItem(6)
            }
        }

        rule.waitUntil {
            // wait for not visible item to be disposed
            !composed.contains(4)
        }

        rule.runOnIdle {
            assertThat(composed).contains(3)
        }

        rule.onNodeWithTag("3")
            .assertExists()
            .assertIsNotDisplayed()
            .assertIsPlaced()
    }

    @Test
    fun pinnedItemIsRemovedWhenNotVisible() {
        val state = LazyListState(3)
        var itemCount by mutableStateOf(10)
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(itemCount) { index ->
                    if (index == 3) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
            assertThat(composed).contains(4)
            runBlocking {
                state.scrollToItem(0)
            }
            if (useLookaheadScope) {
                // Force another lookahead measure pass, because lookahead pass by design keeps
                // content from last measure pass until it's no longer needed in either pass.
                runBlocking {
                    state.scrollToItem(0)
                }
            }
        }

        rule.waitUntil {
            // wait for not visible item to be disposed
            !composed.contains(4)
        }

        rule.runOnIdle {
            itemCount = 3
        }

        rule.waitUntil {
            // wait for pinned item to be disposed
            !composed.contains(3)
        }

        rule.onNodeWithTag("3")
            .assertIsNotPlaced()
    }

    @Test
    fun pinnedItemIsRemovedWhenVisible() {
        val state = LazyListState(0)
        var items by mutableStateOf(listOf(0, 1, 2))
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(items) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            items = listOf(0, 2)
        }

        rule.waitUntil {
            // wait for pinned item to be disposed
            !composed.contains(1)
        }

        rule.onNodeWithTag("1")
            .assertIsNotPlaced()
    }

    @Test
    fun pinnedMultipleTimes() {
        val state = LazyListState(0)
        // Arrange.
        rule.setContentParameterized {
            LazyColumn(Modifier.size(itemSize * 2), state = state) {
                items(100) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Item(index)
                }
            }
        }

        val handles = mutableListOf<PinnedHandle>()
        rule.runOnIdle {
            handles.add(requireNotNull(pinnableContainer).pin())
            handles.add(requireNotNull(pinnableContainer).pin())
        }

        rule.runOnIdle {
            // pinned 3 times in total
            handles.add(requireNotNull(pinnableContainer).pin())
            assertThat(composed).contains(0)
            runBlocking {
                state.scrollToItem(3)
            }
        }

        rule.waitUntil {
            // wait for not visible item to be disposed
            !composed.contains(0)
        }

        while (handles.isNotEmpty()) {
            rule.runOnIdle {
                assertThat(composed).contains(1)
                handles.removeFirst().release()
            }
        }

        rule.waitUntil {
            // wait for pinned item to be disposed
            !composed.contains(1)
        }
    }

    @Test
    fun pinningIsPropagatedToParentContainer() {
        var parentPinned = false
        val parentContainer = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                parentPinned = true
                return PinnedHandle { parentPinned = false }
            }
        }
        // Arrange.
        rule.setContentParameterized {
            CompositionLocalProvider(LocalPinnableContainer provides parentContainer) {
                LazyColumn {
                    item {
                        pinnableContainer = LocalPinnableContainer.current
                        Box(Modifier.size(itemSize))
                    }
                }
            }
        }

        val handle = rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            assertThat(parentPinned).isTrue()
            handle.release()
        }

        rule.runOnIdle {
            assertThat(parentPinned).isFalse()
        }
    }

    @Test
    fun parentContainerChange_pinningIsMaintained() {
        var parent1Pinned = false
        val parent1Container = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                parent1Pinned = true
                return PinnedHandle { parent1Pinned = false }
            }
        }
        var parent2Pinned = false
        val parent2Container = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                parent2Pinned = true
                return PinnedHandle { parent2Pinned = false }
            }
        }
        var parentContainer by mutableStateOf<PinnableContainer>(parent1Container)
        // Arrange.
        rule.setContentParameterized {
            CompositionLocalProvider(LocalPinnableContainer provides parentContainer) {
                LazyColumn {
                    item {
                        pinnableContainer = LocalPinnableContainer.current
                        Box(Modifier.size(itemSize))
                    }
                }
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            assertThat(parent1Pinned).isTrue()
            assertThat(parent2Pinned).isFalse()
            parentContainer = parent2Container
        }

        rule.runOnIdle {
            assertThat(parent1Pinned).isFalse()
            assertThat(parent2Pinned).isTrue()
        }
    }

    @Test
    fun pinnedItemIsRemovedAfterContainerExitsComposition() {
        var active by mutableStateOf(true)
        // Arrange.
        rule.setContentParameterized {
            if (active) {
                LazyColumn(Modifier.size(itemSize * 2)) {
                    items(3) { colIndex ->
                        LazyRow {
                            items(3) { rowIndex ->
                                if (colIndex == 1 && rowIndex == 1) {
                                    pinnableContainer = LocalPinnableContainer.current
                                }
                                Box(Modifier.size(itemSize).testTag("$colIndex:$rowIndex"))
                            }
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag("1:1")
            .assertIsPlaced()

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle {
            active = !active
        }

        rule.onNodeWithTag("1:1")
            .assertIsNotPlaced()
    }
}

/**
 * Asserts that the current semantics node is not placed.
 *
 * Throws [AssertionError] if the node is placed.
 */
internal fun SemanticsNodeInteraction.assertIsNotPlaced() {
    // TODO(b/187188981): We don't have a non-throwing API to check whether an item exists.
    //  So until this bug is fixed, we are going to catch the assertion error and then check
    //  whether the node is placed or not.
    try {
        // If the node does not exist, it implies that it is also not placed.
        assertDoesNotExist()
    } catch (e: AssertionError) {
        // If the node exists, we need to assert that it is not placed.
        val errorMessageOnFail = "Assert failed: The component is placed!"
        if (fetchSemanticsNode().layoutInfo.isPlaced) {
            throw AssertionError(errorMessageOnFail)
        }
    }
}

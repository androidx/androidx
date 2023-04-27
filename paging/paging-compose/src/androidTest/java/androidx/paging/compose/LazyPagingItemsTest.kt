/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.TestPagingSource
import androidx.paging.cachedIn
import androidx.paging.localLoadStatesOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyPagingItemsTest {
    @get:Rule
    val rule = createComposeRule()

    val items = (1..10).toList().map { it }
    private val itemsSizePx = 30f
    private val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    private fun createPager(
        config: PagingConfig = PagingConfig(
            pageSize = 1,
            enablePlaceholders = false,
            maxSize = 200,
            initialLoadSize = 3,
            prefetchDistance = 1,
        ),
        loadDelay: Long = 0,
        pagingSourceFactory: () -> PagingSource<Int, Int> = {
            TestPagingSource(items = items, loadDelay = loadDelay)
        }
    ): Pager<Int, Int> {
        return Pager(config = config, pagingSourceFactory = pagingSourceFactory)
    }

    private fun createPagerWithPlaceholders(
        config: PagingConfig = PagingConfig(
            pageSize = 1,
            enablePlaceholders = true,
            maxSize = 200,
            initialLoadSize = 3,
            prefetchDistance = 0,
        )
    ) = Pager(
        config = config,
        pagingSourceFactory = { TestPagingSource(items = items, loadDelay = 0) }
    )

    @Test
    fun lazyPagingInitialLoadState() {
        val pager = createPager()
        val loadStates: MutableList<CombinedLoadStates> = mutableListOf()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            loadStates.add(lazyPagingItems.loadState)
        }

        rule.waitForIdle()

        val expected = CombinedLoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(false),
            append = LoadState.NotLoading(false),
            source = LoadStates(
                LoadState.Loading,
                LoadState.NotLoading(false),
                LoadState.NotLoading(false)
            ),
            mediator = null
        )
        assertThat(loadStates).isNotEmpty()
        assertThat(loadStates.first()).isEqualTo(expected)
    }

    @Test
    fun lazyPagingLoadStateAfterRefresh() {
        val pager = createPager(loadDelay = 100)

        val loadStates: MutableList<CombinedLoadStates> = mutableListOf()

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            loadStates.add(lazyPagingItems.loadState)
        }

        // wait for both compose and paging to complete
        rule.waitUntil {
            rule.waitForIdle()
            lazyPagingItems.loadState.refresh == LoadState.NotLoading(false)
        }

        rule.runOnIdle {
            // we only want loadStates after manual refresh
            loadStates.clear()
            lazyPagingItems.refresh()
        }

        // wait for both compose and paging to complete
        rule.waitUntil {
            rule.waitForIdle()
            lazyPagingItems.loadState.refresh == LoadState.NotLoading(false)
        }

        assertThat(loadStates).isNotEmpty()
        val expected = CombinedLoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(false),
            append = LoadState.NotLoading(false),
            source = LoadStates(
                LoadState.Loading,
                LoadState.NotLoading(false),
                LoadState.NotLoading(false)
            ),
            mediator = null
        )
        assertThat(loadStates.first()).isEqualTo(expected)
    }

    @Test
    fun lazyPagingColumnShowsItems() {
        val pager = createPager()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(200.dp)) {
                items(count = lazyPagingItems.itemCount) { index ->
                    val item = lazyPagingItems[index]
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag("$item"))
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Suppress("DEPRECATION")
    @Test
    fun lazyPagingColumnShowsIndexedItems() {
        val pager = createPager()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(200.dp)) {
                itemsIndexed(lazyPagingItems) { index, item ->
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag("$index-$item")
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("0-1")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-2")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-3")
            .assertDoesNotExist()

        rule.onNodeWithTag("3-4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyPagingRowShowsItems() {
        val pager = createPager()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyRow(Modifier.width(200.dp)) {
                items(count = lazyPagingItems.itemCount) { index ->
                    val item = lazyPagingItems[index]
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag("$item"))
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Suppress("DEPRECATION")
    @Test
    fun lazyPagingRowShowsIndexedItems() {
        val pager = createPager()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyRow(Modifier.width(200.dp)) {
                itemsIndexed(lazyPagingItems) { index, item ->
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag("$index-$item")
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("0-1")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-2")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-3")
            .assertDoesNotExist()

        rule.onNodeWithTag("3-4")
            .assertDoesNotExist()
    }

    @Test
    fun differentContentTypes() {
        val pager = createPagerWithPlaceholders()
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

            for (i in 0 until lazyPagingItems.itemCount) {
                lazyPagingItems[i]
            }

            LazyColumn(Modifier.height(itemsSizeDp * 2.5f), state) {
                item(contentType = "not-to-reuse--1") {
                    Content("-1")
                }
                item(contentType = "reuse") {
                    Content("0")
                }
                items(
                    count = lazyPagingItems.itemCount,
                    contentType = lazyPagingItems.itemContentType(
                        contentType = { if (it == 8) "reuse" else "not-to-reuse-$it" }
                    )
                ) { index ->
                    val item = lazyPagingItems[index]
                    Content("$item")
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2)
                // now items -1 and 0 are put into reusables
            }
        }

        rule.onNodeWithTag("-1")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(8)
                // item 8 should reuse slot 0
            }
        }

        rule.onNodeWithTag("-1")
            .assertExists()
            .assertIsNotDisplayed()
        // node reused
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
        rule.onNodeWithTag("7")
            .assertIsDisplayed()
        rule.onNodeWithTag("8")
            .assertIsDisplayed()
        rule.onNodeWithTag("9")
            .assertIsDisplayed()
    }

    @Test
    fun nullItemContentType() {
        val pager = createPagerWithPlaceholders()
        lateinit var state: LazyListState

        var loadedItem6 = false

        rule.setContent {
            state = rememberLazyListState()

            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            // Trigger page fetch until all items 1-6 are loaded
            for (i in 0 until minOf(lazyPagingItems.itemCount, 6)) {
                lazyPagingItems[i]
                loadedItem6 = lazyPagingItems.peek(i) == 6
            }

            LazyColumn(Modifier.height(itemsSizeDp * 2.5f), state) {
                item(contentType = "not-to-reuse--1") {
                    Content("-1")
                }
                // to be reused later by placeholder item
                item(contentType = PagingPlaceholderContentType) {
                    Content("0")
                }
                items(
                    count = lazyPagingItems.itemCount,
                    // item 7 would be null, which should default to PagingPlaceholderContentType
                    contentType = lazyPagingItems.itemContentType(
                        contentType = { "not-to-reuse-$it" }
                    )
                ) { index ->
                    val item = lazyPagingItems[index]
                    Content("$item")
                }
            }
        }

        rule.waitUntil {
            loadedItem6
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2)
                // now items -1 and 0 are put into reusables
            }
        }

        rule.onNodeWithTag("-1")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(6)
                // item 7 which is null should reuse slot 0
            }
        }

        rule.onNodeWithTag("-1")
            .assertExists()
            .assertIsNotDisplayed()
        // node reused
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun nullContentType() {
        val pager = createPagerWithPlaceholders()
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            for (i in 0 until lazyPagingItems.itemCount) {
                lazyPagingItems[i]
            }

            LazyColumn(Modifier.height(itemsSizeDp * 2.5f), state) {
                item(contentType = "not-to-reuse--1") {
                    Content("-1")
                }
                // to be reused later by real items
                item(contentType = null) {
                    Content("0")
                }
                items(
                    count = lazyPagingItems.itemCount,
                    // should default to null
                    contentType = lazyPagingItems.itemContentType(null)
                ) { index ->
                    val item = lazyPagingItems[index]
                    Content("$item")
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2)
                // now items -1 and 0 are put into reusables
            }
        }

        rule.onNodeWithTag("-1")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            runBlocking {
                // item 4
                state.scrollToItem(3)
            }
        }

        rule.onNodeWithTag("-1")
            .assertExists()
            .assertIsNotDisplayed()
        // node reused
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
        rule.onNodeWithTag("4")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun itemSnapshotList() {
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        val pager = createPager()
        var composedCount = 0
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()

            for (i in 0 until lazyPagingItems.itemCount) {
                lazyPagingItems[i]
            }
            composedCount = lazyPagingItems.itemCount
        }

        rule.waitUntil {
            composedCount == items.size
        }

        assertThat(lazyPagingItems.itemSnapshotList).isEqualTo(items)
    }

    @Test
    fun peek() {
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        var composedCount = 0
        val pager = createPager()
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()

            // Trigger page fetch until all items 0-6 are loaded
            for (i in 0 until minOf(lazyPagingItems.itemCount, 5)) {
                lazyPagingItems[i]
            }
            composedCount = lazyPagingItems.itemCount
        }

        rule.waitUntil {
            composedCount == 6
        }

        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(6)
            for (i in 0..4) {
                assertThat(lazyPagingItems.peek(i)).isEqualTo(items[i])
            }
        }

        rule.runOnIdle {
            // Verify peek does not trigger page fetch.
            assertThat(lazyPagingItems.itemCount).isEqualTo(6)
        }
    }

    @Test
    fun retry() {
        var factoryCallCount = 0
        lateinit var pagingSource: TestPagingSource
        val pager = createPager {
            factoryCallCount++
            pagingSource = TestPagingSource(items = items, loadDelay = 0)
            if (factoryCallCount == 1) {
                pagingSource.errorNextLoad = true
            }
            pagingSource
        }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
        }

        assertThat(lazyPagingItems.itemSnapshotList).isEmpty()

        lazyPagingItems.retry()
        rule.waitForIdle()

        assertThat(lazyPagingItems.itemSnapshotList).isNotEmpty()
        // Verify retry does not invalidate.
        assertThat(factoryCallCount).isEqualTo(1)
    }

    @Test
    fun refresh() {
        var factoryCallCount = 0
        lateinit var pagingSource: TestPagingSource
        val pager = createPager {
            factoryCallCount++
            pagingSource = TestPagingSource(items = items, loadDelay = 0)
            if (factoryCallCount == 1) {
                pagingSource.errorNextLoad = true
            }
            pagingSource
        }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
        }

        assertThat(lazyPagingItems.itemSnapshotList).isEmpty()

        lazyPagingItems.refresh()
        rule.waitForIdle()

        assertThat(lazyPagingItems.itemSnapshotList).isNotEmpty()
        assertThat(factoryCallCount).isEqualTo(2)
    }

    @Test
    fun itemCountIsObservable() {
        val items = mutableListOf(0, 1)
        val pager = createPager {
            TestPagingSource(items = items, loadDelay = 0)
        }

        var composedCount = 0
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            composedCount = lazyPagingItems.itemCount
        }

        rule.waitUntil {
            composedCount == 2
        }

        rule.runOnIdle {
            items += 2
            lazyPagingItems.refresh()
        }

        rule.waitUntil {
            composedCount == 3
        }

        rule.runOnIdle {
            items.clear()
            items.add(0)
            lazyPagingItems.refresh()
        }

        rule.waitUntil {
            composedCount == 1
        }
    }

    @Test
    fun worksWhenUsedWithoutExtension() {
        val items = mutableListOf(10, 20)
        val pager = createPager {
            TestPagingSource(items = items, loadDelay = 0)
        }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(300.dp)) {
                items(lazyPagingItems.itemCount) {
                    val item = lazyPagingItems[it]
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag("$item"))
                }
            }
        }

        rule.onNodeWithTag("10")
            .assertIsDisplayed()

        rule.onNodeWithTag("20")
            .assertIsDisplayed()

        rule.runOnIdle {
            items.clear()
            items.addAll(listOf(30, 20, 40))
            lazyPagingItems.refresh()
        }

        rule.onNodeWithTag("30")
            .assertIsDisplayed()

        rule.onNodeWithTag("20")
            .assertIsDisplayed()

        rule.onNodeWithTag("40")
            .assertIsDisplayed()

        rule.onNodeWithTag("10")
            .assertDoesNotExist()
    }

    @Test
    fun updatingItem() {
        val items = mutableListOf(1, 2, 3)
        val pager = createPager(
            PagingConfig(
                pageSize = 3,
                enablePlaceholders = false,
                maxSize = 200,
                initialLoadSize = 3,
                prefetchDistance = 3,
            )
        ) {
            TestPagingSource(items = items, loadDelay = 0)
        }

        val itemSize = with(rule.density) { 100.dp.roundToPx().toDp() }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(itemSize * 3)) {
                items(count = lazyPagingItems.itemCount) { index ->
                    val item = lazyPagingItems[index]
                    Spacer(Modifier.height(itemSize).fillParentMaxWidth().testTag("$item"))
                }
            }
        }

        rule.runOnIdle {
            items.clear()
            items.addAll(listOf(1, 4, 3))
            lazyPagingItems.refresh()
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)

        rule.onNodeWithTag("2")
            .assertDoesNotExist()
    }

    @Test
    fun addingNewItem() {
        val items = mutableListOf(1, 2)
        val pager = createPager(
            PagingConfig(
                pageSize = 3,
                enablePlaceholders = false,
                maxSize = 200,
                initialLoadSize = 3,
                prefetchDistance = 3,
            )
        ) {
            TestPagingSource(items = items, loadDelay = 0)
        }

        val itemSize = with(rule.density) { 100.dp.roundToPx().toDp() }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(itemSize * 3)) {
                items(count = lazyPagingItems.itemCount) { index ->
                    val item = lazyPagingItems[index]
                    Spacer(Modifier.height(itemSize).fillParentMaxWidth().testTag("$item"))
                }
            }
        }

        rule.runOnIdle {
            items.clear()
            items.addAll(listOf(1, 2, 3))
            lazyPagingItems.refresh()
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
    }

    @Ignore // b/229089541
    @Test
    fun removingItem() {
        val items = mutableListOf(1, 2, 3)
        val pager = createPager(
            PagingConfig(
                pageSize = 3,
                enablePlaceholders = false,
                maxSize = 200,
                initialLoadSize = 3,
                prefetchDistance = 3,
            )
        ) {
            TestPagingSource(items = items, loadDelay = 0)
        }

        val itemSize = with(rule.density) { 100.dp.roundToPx().toDp() }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(itemSize * 3)) {
                items(count = lazyPagingItems.itemCount) { index ->
                    val item = lazyPagingItems[index]
                    Spacer(Modifier.height(itemSize).fillParentMaxWidth().testTag("$item"))
                }
            }
        }

        rule.runOnIdle {
            items.clear()
            items.addAll(listOf(2, 3))
            lazyPagingItems.refresh()
        }

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("1")
            .assertDoesNotExist()
    }

    @Test
    fun stateIsMovedWithItemWithCustomKey_items() {
        val items = mutableListOf(1)
        val pager = createPager {
            TestPagingSource(items = items, loadDelay = 0)
        }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        var counter = 0
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it },
                ) { index ->
                    val item = lazyPagingItems[index]
                    BasicText(
                        "Item=$item. counter=${remember { counter++ }}"
                    )
                }
            }
        }

        rule.runOnIdle {
            items.clear()
            items.addAll(listOf(0, 1))
            lazyPagingItems.refresh()
        }

        rule.onNodeWithText("Item=0. counter=1")
            .assertExists()

        rule.onNodeWithText("Item=1. counter=0")
            .assertExists()
    }

    @Suppress("DEPRECATION")
    @Test
    fun stateIsMovedWithItemWithCustomKey_itemsIndexed() {
        val items = mutableListOf(1)
        val pager = createPager {
            TestPagingSource(items = items, loadDelay = 0)
        }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn {
                itemsIndexed(lazyPagingItems, key = { _, item -> item }) { index, item ->
                    BasicText(
                        "Item=$item. index=$index. remembered index=${remember { index }}"
                    )
                }
            }
        }

        rule.runOnIdle {
            items.clear()
            items.addAll(listOf(0, 1))
            lazyPagingItems.refresh()
        }

        rule.onNodeWithText("Item=0. index=0. remembered index=0")
            .assertExists()

        rule.onNodeWithText("Item=1. index=1. remembered index=0")
            .assertExists()
    }

    @Test
    fun collectOnDefaultThread() {
        val items = mutableListOf(1, 2, 3)
        val pager = createPager {
            TestPagingSource(items = items, loadDelay = 0)
        }

        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()
        }

        rule.waitUntil {
            lazyPagingItems.itemCount == 3
        }
        assertThat(lazyPagingItems.itemSnapshotList).containsExactlyElementsIn(
            items
        )
    }

    @Test
    fun collectOnWorkerThread() {
        val items = mutableListOf(1, 2, 3)
        val pager = createPager {
            TestPagingSource(items = items, loadDelay = 0)
        }

        val context = StandardTestDispatcher()
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems(context)
        }

        rule.runOnIdle {
            assertFalse(context.isActive)
            // collection should not have started yet
            assertThat(lazyPagingItems.itemSnapshotList).isEmpty()
        }

        // start LaunchedEffects
        context.scheduler.advanceUntilIdle()

        rule.runOnIdle {
            // continue with pagingDataDiffer collections
            context.scheduler.advanceUntilIdle()
        }
        rule.waitUntil {
            lazyPagingItems.itemCount == items.size
        }
        assertThat(lazyPagingItems.itemSnapshotList).containsExactlyElementsIn(
            items
        )
    }

    @Test
    fun cachedData() {
        val flow = createPager().flow.cachedIn(TestScope(UnconfinedTestDispatcher()))
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        val restorationTester = StateRestorationTester(rule)
        val dispatcher = StandardTestDispatcher()
        var maxItem by mutableStateOf(6)
        restorationTester.setContent {
            lazyPagingItems = flow.collectAsLazyPagingItems(dispatcher)
            // load until we get 6 items
            for (i in 0 until minOf(lazyPagingItems.itemCount, maxItem - 1)) {
                lazyPagingItems[i]
            }
        }

        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            lazyPagingItems.itemCount == maxItem
        }

        // we don't advance load dispatchers after restoration to prevent loads
        restorationTester.emulateSavedInstanceStateRestore()

        // ensure we received the cached data
        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(6)
            assertThat(lazyPagingItems.itemSnapshotList).isEqualTo(listOf(1, 2, 3, 4, 5, 6))
        }

        // try to load more data
        maxItem = 7
        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            lazyPagingItems.itemCount == maxItem
        }

        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(7)
            assertThat(lazyPagingItems.itemSnapshotList).isEqualTo(listOf(1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test
    fun cachedEmptyData() {
        val flow = createPager().flow.cachedIn(TestScope(UnconfinedTestDispatcher()))
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        val restorationTester = StateRestorationTester(rule)
        val dispatcher = StandardTestDispatcher()
        restorationTester.setContent {
            lazyPagingItems = flow.collectAsLazyPagingItems(dispatcher)
            // load until we get 6 items
            for (i in 0 until minOf(lazyPagingItems.itemCount, 5)) {
                lazyPagingItems[i]
            }
        }

        rule.runOnIdle {
            assertThat(lazyPagingItems.itemSnapshotList).isEmpty()
        }

        // we don't let dispatchers load and directly restore state
        restorationTester.emulateSavedInstanceStateRestore()

        // empty cache
        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(0)
            assertThat(lazyPagingItems.itemSnapshotList).isEmpty()
        }

        // check that new data can be loaded in properly
        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            lazyPagingItems.itemCount == 6
        }
        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(6)
            assertThat(lazyPagingItems.itemSnapshotList).isEqualTo(listOf(1, 2, 3, 4, 5, 6))
        }
    }

    @Test
    fun cachedData_withPlaceholders() {
        val flow = createPagerWithPlaceholders().flow
            .cachedIn(TestScope(UnconfinedTestDispatcher()))
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        val restorationTester = StateRestorationTester(rule)
        val dispatcher = StandardTestDispatcher()
        var maxItem by mutableStateOf(6)
        var loadedMaxItem = false
        restorationTester.setContent {
            lazyPagingItems = flow.collectAsLazyPagingItems(dispatcher)
            // load until we get 6 items
            for (i in 0 until minOf(lazyPagingItems.itemCount, maxItem)) {
                lazyPagingItems[i]
                loadedMaxItem = lazyPagingItems.peek(i) == maxItem
            }
        }

        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            loadedMaxItem
        }

        // we don't advance load dispatchers after restoration to prevent loads
        restorationTester.emulateSavedInstanceStateRestore()

        // ensure we received the cached data + placeholders
        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(10)
            assertThat(lazyPagingItems.itemSnapshotList).isEqualTo(
                listOf(1, 2, 3, 4, 5, 6, null, null, null, null)
            )
        }

        // try to load more data
        maxItem = 7
        loadedMaxItem = false
        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            loadedMaxItem
        }
        rule.runOnIdle {
            assertThat(lazyPagingItems.itemSnapshotList).isEqualTo(
                listOf(1, 2, 3, 4, 5, 6, 7, null, null, null)
            )
        }
    }

    @Test
    fun cachedData_loadStates() {
        val flow = createPager().flow.cachedIn(TestScope(UnconfinedTestDispatcher()))
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        val restorationTester = StateRestorationTester(rule)
        val dispatcher = StandardTestDispatcher()
        var maxItem by mutableStateOf(4)
        restorationTester.setContent {
            lazyPagingItems = flow.collectAsLazyPagingItems(dispatcher)
            // load until we get 6 items
            for (i in 0 until minOf(lazyPagingItems.itemCount, maxItem - 1)) {
                lazyPagingItems[i]
            }
        }

        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            lazyPagingItems.itemCount == maxItem
        }

        assertThat(lazyPagingItems.loadState).isEqualTo(
            localLoadStatesOf(
                refreshLocal = LoadState.NotLoading(false),
                prependLocal = LoadState.NotLoading(true)
            )
        )

        // we don't advance load dispatchers after restoration to prevent loads
        restorationTester.emulateSavedInstanceStateRestore()

        // ensure we received the cached loadstates
        rule.runOnIdle {
            assertThat(lazyPagingItems.loadState).isEqualTo(
                localLoadStatesOf(
                    refreshLocal = LoadState.NotLoading(false),
                    prependLocal = LoadState.NotLoading(true)
                )
            )
        }
    }

    @Test
    fun cachedData_restoresListState() {
        val flow = createPager().flow.cachedIn(TestScope(UnconfinedTestDispatcher()))
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        lateinit var state: LazyListState
        val restorationTester = StateRestorationTester(rule)
        val dispatcher = StandardTestDispatcher()
        restorationTester.setContent {
            state = rememberLazyListState()
            lazyPagingItems = flow.collectAsLazyPagingItems(dispatcher)
            // load until we get 6 items
            for (i in 0 until minOf(lazyPagingItems.itemCount, 5)) {
                lazyPagingItems[i]
            }
            LazyColumn(Modifier.height(itemsSizeDp * 2.5f), state) {
                // Static items are what triggers scroll state to erroneously reset to 0
                item {
                    Content("header")
                }
                items(
                    lazyPagingItems.itemCount, lazyPagingItems.itemKey()
                ) { index ->
                    val item = lazyPagingItems[index]
                    Content("$item")
                }
            }
        }

        rule.waitUntil {
            dispatcher.scheduler.advanceUntilIdle() // let items load
            lazyPagingItems.itemCount == 6
        }

        rule.runOnIdle {
            runBlocking { state.scrollToItem(3) }
            assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        }

        // we don't advance load dispatchers after restoration to prevent loads
        restorationTester.emulateSavedInstanceStateRestore()

        // ensure we received the cached data and preserved scroll state
        rule.runOnIdle {
            assertThat(lazyPagingItems.itemCount).isEqualTo(6)
            assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        }
    }

    @Composable
    private fun Content(tag: String) {
        Spacer(Modifier.height(itemsSizeDp).width(10.dp).testTag(tag))
    }
}

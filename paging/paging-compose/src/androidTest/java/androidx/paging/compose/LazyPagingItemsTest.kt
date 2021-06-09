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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.TestPagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyPagingItemsTest {
    @get:Rule
    val rule = createComposeRule()

    val items = (1..10).toList().map { it }

    private fun createPager(
        config: PagingConfig = PagingConfig(
            pageSize = 1,
            enablePlaceholders = false,
            maxSize = 200,
            initialLoadSize = 3,
            prefetchDistance = 1,
        ),
        pagingSourceFactory: () -> PagingSource<Int, Int> = {
            TestPagingSource(items = items, loadDelay = 0)
        }
    ): Pager<Int, Int> {
        return Pager(config = config, pagingSourceFactory = pagingSourceFactory)
    }

    @FlakyTest(bugId = 190609811)
    @Test
    fun lazyPagingColumnShowsItems() {
        val pager = createPager()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.height(200.dp)) {
                items(lazyPagingItems) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag("$it"))
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

    @FlakyTest(bugId = 190609811)
    @Test
    fun lazyPagingRowShowsItems() {
        val pager = createPager()
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyRow(Modifier.width(200.dp)) {
                items(lazyPagingItems) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag("$it"))
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
    fun snapshot() {
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        val pager = createPager()
        var composedCount = 0
        rule.setContent {
            lazyPagingItems = pager.flow.collectAsLazyPagingItems()

            for (i in 0 until lazyPagingItems.itemCount) {
                lazyPagingItems.getAsState(i).value
            }
            composedCount = lazyPagingItems.itemCount
        }

        rule.waitUntil {
            composedCount == items.size
        }

        assertThat(lazyPagingItems.snapshot()).isEqualTo(items)
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
                lazyPagingItems.getAsState(i).value
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

        assertThat(lazyPagingItems.snapshot()).isEmpty()

        lazyPagingItems.retry()
        rule.waitForIdle()

        assertThat(lazyPagingItems.snapshot()).isNotEmpty()
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

        assertThat(lazyPagingItems.snapshot()).isEmpty()

        lazyPagingItems.refresh()
        rule.waitForIdle()

        assertThat(lazyPagingItems.snapshot()).isNotEmpty()
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
                    val item by lazyPagingItems.getAsState(it)
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

    @FlakyTest(bugId = 190609811)
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
                items(lazyPagingItems) {
                    Spacer(Modifier.height(itemSize).fillParentMaxWidth().testTag("$it"))
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
                items(lazyPagingItems) {
                    Spacer(Modifier.height(itemSize).fillParentMaxWidth().testTag("$it"))
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
                items(lazyPagingItems) {
                    Spacer(Modifier.height(itemSize).fillParentMaxWidth().testTag("$it"))
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
}

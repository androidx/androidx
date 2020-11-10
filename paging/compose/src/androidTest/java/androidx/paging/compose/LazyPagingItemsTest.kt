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
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.TestPagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Test
    fun lazyPagingColumnShowsItems() {
        rule.setContent {
            val lazyPagingItems = createPager().flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                items(lazyPagingItems) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag("$it"))
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
        rule.setContent {
            val lazyPagingItems = createPager().flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                itemsIndexed(lazyPagingItems) { index, item ->
                    Spacer(
                        Modifier.preferredHeight(101.dp).fillParentMaxWidth()
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
        rule.setContent {
            val lazyPagingItems = createPager().flow.collectAsLazyPagingItems()
            LazyRow(Modifier.preferredWidth(200.dp)) {
                items(lazyPagingItems) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag("$it"))
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
        rule.setContent {
            val lazyPagingItems = createPager().flow.collectAsLazyPagingItems()
            LazyRow(Modifier.preferredWidth(200.dp)) {
                itemsIndexed(lazyPagingItems) { index, item ->
                    Spacer(
                        Modifier.preferredWidth(101.dp).fillParentMaxHeight()
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
        rule.setContent {
            lazyPagingItems = createPager().flow.collectAsLazyPagingItems()
        }

        // Trigger page fetch until all items are loaded
        for (i in items.indices) {
            lazyPagingItems.get(i)
            rule.waitForIdle()
        }

        assertThat(lazyPagingItems.snapshot()).isEqualTo(items)
    }

    @Test
    fun peek() {
        lateinit var lazyPagingItems: LazyPagingItems<Int>
        rule.setContent {
            lazyPagingItems = createPager().flow.collectAsLazyPagingItems()
        }

        // Trigger page fetch until all items 0-6 are loaded
        for (i in 0..4) {
            lazyPagingItems.get(i)
            rule.waitForIdle()
        }

        assertThat(lazyPagingItems.itemCount).isEqualTo(6)
        for (i in 0..4) {
            assertThat(lazyPagingItems.peek(i)).isEqualTo(items[i])
        }
        // Verify peek does not trigger page fetch.
        assertThat(lazyPagingItems.itemCount).isEqualTo(6)
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
}
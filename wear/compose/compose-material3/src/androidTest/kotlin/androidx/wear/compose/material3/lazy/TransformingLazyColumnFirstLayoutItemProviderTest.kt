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

package androidx.wear.compose.material3.lazy

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnFirstLayoutItemProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnVisibleItemInfo
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class TransformingLazyColumnFirstLayoutItemProviderTest {

    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    private val itemsCount = 10
    private lateinit var state: TransformingLazyColumnState
    private lateinit var provider: TransformingLazyColumnFirstLayoutItemProvider

    private val current =
        TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
            key = "key5",
            index = 5,
            itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start,
            offset = 100,
        )

    @Test
    fun rememberTransformingLazyColumnFirstVisibleItemProvider_returnsFirstVisibleItem() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = rememberTransformingLazyColumnFirstVisibleItemProvider(state)
            TransformingLazyColumn(
                modifier = Modifier.height(300.dp),
                state = state,
                firstLayoutItemProvider = provider,
            ) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                state.scrollToItem(current.index)
            }
        }

        rule.runOnIdle {
            val firstVisibleItem = state.layoutInfo.visibleItems.first()
            val firstLayoutItem = provider.getFirstLayoutItem(current)
            assertThat(firstLayoutItem.index).isEqualTo(firstVisibleItem.index)
            assertThat(firstLayoutItem.key).isEqualTo(firstVisibleItem.key)
            assertThat(firstLayoutItem.offset).isEqualTo(firstVisibleItem.offset)
            assertThat(firstLayoutItem.itemEdge)
                .isEqualTo(TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstVisibleItemProvider_emptyVisibleItems_returnsCurrentItem() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = rememberTransformingLazyColumnFirstVisibleItemProvider(state)
            TransformingLazyColumn(
                modifier = Modifier.height(300.dp),
                state = state,
                firstLayoutItemProvider = provider,
            ) {
                items(0) {}
            }
        }

        rule.runOnIdle {
            val firstLayoutItem = provider.getFirstLayoutItem(current)
            assertThat(firstLayoutItem).isEqualTo(current)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstVisibleItemProvider_scrollInProgress_returnsCurrentItem() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = rememberTransformingLazyColumnFirstVisibleItemProvider(state)
            TransformingLazyColumn(
                modifier = Modifier.height(300.dp),
                state = state,
                firstLayoutItemProvider = provider,
            ) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        lateinit var firstLayoutItem: TransformingLazyColumnFirstLayoutItemProvider.ItemInfo
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                state.scrollToItem(current.index)
                state.scroll(MutatePriority.UserInput) {
                    Snapshot.sendApplyNotifications()
                    firstLayoutItem = provider.getFirstLayoutItem(current)
                }
            }
        }

        rule.runOnIdle { assertThat(firstLayoutItem).isEqualTo(current) }
    }

    @Test
    fun rememberTransformingLazyColumnFirstVisibleItemProvider_itemBelowFirstVisibleRemoved_keepsFirstVisibleItem() {
        val itemsList =
            mutableStateListOf<String>().apply { addAll((0 until itemsCount).map { "key$it" }) }
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = rememberTransformingLazyColumnFirstVisibleItemProvider(state)
            TransformingLazyColumn(
                modifier = Modifier.height(300.dp),
                state = state,
                firstLayoutItemProvider = provider,
            ) {
                items(itemsList, key = { it }) { key -> Spacer(Modifier.height(100.dp)) }
            }
        }

        // Scroll to item 5, so visible items should be [4, 5, 6] (first visible is 4)
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) { state.scrollToItem(5) }
        }

        lateinit var firstVisibleBefore: TransformingLazyColumnVisibleItemInfo
        lateinit var firstVisibleAfter: TransformingLazyColumnVisibleItemInfo

        rule.runOnIdle {
            firstVisibleBefore = state.layoutInfo.visibleItems.first()
            assertThat(firstVisibleBefore.index).isEqualTo(4)
            assertThat(firstVisibleBefore.key).isEqualTo("key4")

            // Remove item 5 (which is below first visible item 4)
            itemsList.removeAt(5)
        }

        rule.runOnIdle {
            firstVisibleAfter = state.layoutInfo.visibleItems.first()
            // The first visible item should still be "key4", but now item 5 (index 5) is removed,
            // so the next item in the list is "key6", which shifts to index 5.
            // But "key4" itself should not have shifted index! It should still be index 4.
            // And it should have the same offset.
            assertThat(firstVisibleAfter.key).isEqualTo("key4")
            assertThat(firstVisibleAfter.index).isEqualTo(4)
            assertThat(firstVisibleAfter.offset).isEqualTo(firstVisibleBefore.offset)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstVisibleItemProvider_firstVisibleItemRemoved_keepsSameOffsetForNextItem() {
        val itemsList =
            mutableStateListOf<String>().apply { addAll((0 until itemsCount).map { "key$it" }) }
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = rememberTransformingLazyColumnFirstVisibleItemProvider(state)
            TransformingLazyColumn(
                modifier = Modifier.height(300.dp),
                state = state,
                firstLayoutItemProvider = provider,
            ) {
                items(itemsList, key = { it }) { key -> Spacer(Modifier.height(100.dp)) }
            }
        }

        // Scroll to item 5, so visible items should be [4, 5, 6] (first visible is 4)
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) { state.scrollToItem(5) }
        }

        lateinit var firstVisibleBefore: TransformingLazyColumnVisibleItemInfo
        lateinit var firstVisibleAfter: TransformingLazyColumnVisibleItemInfo

        rule.runOnIdle {
            firstVisibleBefore = state.layoutInfo.visibleItems.first()
            assertThat(firstVisibleBefore.index).isEqualTo(4)
            assertThat(firstVisibleBefore.key).isEqualTo("key4")

            // Remove the first visible item ("key4", which is at index 4)
            itemsList.removeAt(4)
        }

        rule.runOnIdle {
            firstVisibleAfter = state.layoutInfo.visibleItems.first()
            // The old "key4" was removed.
            // The item that was at index 5 ("key5") should now be at index 4.
            // It should become the new first visible item.
            // And it should have the same top offset as the old first visible item ("key4") had.
            assertThat(firstVisibleAfter.key).isEqualTo("key5")
            assertThat(firstVisibleAfter.index).isEqualTo(4)
            assertThat(firstVisibleAfter.offset).isEqualTo(firstVisibleBefore.offset)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstLayoutItemProvider_alignsToItemTopEdge() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider =
                rememberTransformingLazyColumnFirstLayoutItemProvider(
                    state = state,
                    itemKey = { "key2" },
                    itemEdge = { TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start },
                )
            TransformingLazyColumn(modifier = Modifier.height(300.dp), state = state) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        rule.runOnIdle {
            val resolved = provider.getFirstLayoutItem(current)
            assertThat(resolved.index).isEqualTo(2)
            assertThat(resolved.key).isEqualTo("key2")
            assertThat(resolved.itemEdge)
                .isEqualTo(TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstLayoutItemProvider_alignsToItemBottomEdge() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider =
                rememberTransformingLazyColumnFirstLayoutItemProvider(
                    state = state,
                    itemKey = { "key2" },
                    itemEdge = { TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.End },
                )
            TransformingLazyColumn(modifier = Modifier.height(300.dp), state = state) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        rule.runOnIdle {
            val resolved = provider.getFirstLayoutItem(current)
            assertThat(resolved.index).isEqualTo(2)
            assertThat(resolved.key).isEqualTo("key2")
            assertThat(resolved.itemEdge)
                .isEqualTo(TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.End)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstLayoutItemProvider_keyNotFound_returnsCurrentItem() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider =
                rememberTransformingLazyColumnFirstLayoutItemProvider(
                    state = state,
                    itemKey = { "nonexistent" },
                )
            TransformingLazyColumn(modifier = Modifier.height(300.dp), state = state) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        rule.runOnIdle {
            val resolved = provider.getFirstLayoutItem(current)
            assertThat(resolved).isEqualTo(current)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstLayoutItemProvider_keyNull_returnsCurrentItem() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider =
                rememberTransformingLazyColumnFirstLayoutItemProvider(
                    state = state,
                    itemKey = { null },
                )
            TransformingLazyColumn(modifier = Modifier.height(300.dp), state = state) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        rule.runOnIdle {
            val resolved = provider.getFirstLayoutItem(current)
            assertThat(resolved).isEqualTo(current)
        }
    }

    @Test
    fun rememberTransformingLazyColumnFirstLayoutItemProvider_scrollInProgress_resolvesItem() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider =
                rememberTransformingLazyColumnFirstLayoutItemProvider(
                    state = state,
                    itemKey = { "key2" },
                )
            TransformingLazyColumn(modifier = Modifier.height(300.dp), state = state) {
                items(itemsCount, key = { "key$it" }) { Spacer(Modifier.height(100.dp)) }
            }
        }

        var resolved: TransformingLazyColumnFirstLayoutItemProvider.ItemInfo? = null
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                state.scroll(MutatePriority.UserInput) {
                    Snapshot.sendApplyNotifications()
                    resolved = provider.getFirstLayoutItem(current)
                }
            }
        }

        assertThat(resolved?.index).isEqualTo(2)
        assertThat(resolved?.key).isEqualTo("key2")
    }

    private class AutoTestFrameClock : MonotonicFrameClock {
        private val time = AtomicLong(0)

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            return onFrame(time.getAndAdd(16_000_000))
        }
    }
}

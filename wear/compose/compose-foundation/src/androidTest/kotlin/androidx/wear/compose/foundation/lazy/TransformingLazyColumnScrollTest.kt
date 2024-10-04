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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnScrollTest {
    @get:Rule val rule = createComposeRule()

    private val lazyListTag = "LazyList"

    private val itemsCount = 20
    private lateinit var state: TransformingLazyColumnState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    private fun testScroll(
        spacingPx: Int = 0,
        containerSizePx: Int = itemSizePx * 3,
        scrollBlock: suspend () -> Unit,
        assertBlock: () -> Unit
    ) {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            scope = rememberCoroutineScope()
            with(rule.density) {
                TestContent(
                    spacingPx.toDp(),
                    containerSizePx.toDp(),
                )
            }
        }
        runBlocking { withContext(Dispatchers.Main + AutoTestFrameClock()) { scrollBlock() } }
        rule.runOnIdle { assertBlock() }
    }

    @Test
    fun setupWorks() =
        testScroll(scrollBlock = {}) {
            assertThat(state.anchorItemIndex).isEqualTo(0)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
        }

    @Test
    fun scrollToItem() =
        testScroll(scrollBlock = { state.scrollToItem(3) }) {
            assertThat(state.anchorItemIndex).isEqualTo(3)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
        }

    @Test
    fun scrollToItemWithOffset() =
        testScroll(scrollBlock = { state.scrollToItem(3, 10) }) {
            assertThat(state.anchorItemIndex).isEqualTo(3)
            assertThat(state.anchorItemScrollOffset).isEqualTo(10)
        }

    @Test
    fun scrollToItemWithNegativeOffset() =
        testScroll(scrollBlock = { state.scrollToItem(3, -10) }) {
            assertThat(state.layoutInfo.visibleItems.firstOrNull()?.index).isEqualTo(2)
            val item3Offset = state.layoutInfo.visibleItems.first { it.index == 3 }.offset
            assertThat(item3Offset).isEqualTo(itemSizePx - 10)
        }

    @Test
    fun scrollToItemWithOffsetLargerThanAvailableSize() =
        testScroll(scrollBlock = { state.scrollToItem(itemsCount - 1, -10) }) {
            assertThat(state.anchorItemIndex).isEqualTo(itemsCount - 1)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0) // not 10
        }

    @Test
    fun scrollToItemWithIndexLargerThanItemsCount() =
        testScroll(scrollBlock = { state.scrollToItem(itemsCount + 2) }) {
            assertThat(state.anchorItemIndex).isEqualTo(itemsCount - 1)
        }

    @Composable
    private fun TestContent(
        spacingDp: Dp,
        containerSizeDp: Dp,
    ) =
        TransformingLazyColumn(
            Modifier.height(containerSizeDp).testTag(lazyListTag),
            state,
            verticalArrangement = Arrangement.spacedBy(spacingDp)
        ) {
            items(itemsCount) { Spacer(modifier = Modifier.height(itemSizeDp)) }
        }
}

private class AutoTestFrameClock : MonotonicFrameClock {
    private val time = AtomicLong(0)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return onFrame(time.getAndAdd(16_000_000))
    }
}

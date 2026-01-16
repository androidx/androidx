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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnPrefetcherTest {

    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    private val listTag = "TransformingLazyColumn"
    private val itemsCount = 20
    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    lateinit var state: TransformingLazyColumnState
    lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    fun notPrefetchingForwardInitially() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            scope = rememberCoroutineScope()
            TestContent()
        }

        // we can fit 1.5 items in the viewport, item 1 should not exist
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("2").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 2)
            scope = rememberCoroutineScope()
            TestContent()
        }

        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll() {
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            scope = rememberCoroutineScope()
            TestContent()
        }
        val preFetchIndex = 2
        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        rule.waitForIdle()
        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 1}").assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll() {
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = 5,
                    initialAnchorItemScrollOffset = -10,
                )
            scope = rememberCoroutineScope()
            TestContent()
        }

        val preFetchIndex = 3
        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        rule.waitForIdle()

        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule.onNodeWithTag("${preFetchIndex - 1}").assertDoesNotExist()
    }

    @Composable
    private fun TestContent(containerSizeDp: Dp = 1.5f * itemSizeDp) =
        TransformingLazyColumn(Modifier.height(containerSizeDp).testTag(listTag), state) {
            items(itemsCount) {
                Spacer(modifier = Modifier.height(itemSizeDp).testTag(it.toString()))
            }
        }
}

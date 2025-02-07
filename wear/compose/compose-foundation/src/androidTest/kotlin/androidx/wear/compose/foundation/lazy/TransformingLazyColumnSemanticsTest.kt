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

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.TEST_TAG
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnSemanticsTest {
    @get:Rule val rule = createComposeRule()

    private val lazyListTag = "LazyList"

    @Test
    fun testScrollingToEndOfList_displaysLastItem() {
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.height(100.dp).semantics { testTag = lazyListTag }
            ) {
                items(10) { index ->
                    Box(modifier = Modifier.height(100.dp).semantics { testTag = "Item $index" })
                }
            }
        }
        rule.onNodeWithTag(lazyListTag).performScrollToNode(hasTestTag("Item 9"))
        rule.onNodeWithTag("Item 9").assertIsPlaced()
        rule.onNodeWithTag("Item 0").assertIsNotPlaced()
    }

    @Test
    fun testScrollAxisRange_updatesOnScroll() {
        val state = TransformingLazyColumnState()

        rule.setContent {
            TransformingLazyColumn(Modifier.testTag(TEST_TAG), state = state) {
                items(100) { Box(Modifier.requiredSize(50.dp).testTag("item#$it")) }
            }
        }

        rule.runOnIdle { state.requestScrollToItem(50) }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertVerticalScrollAxisRange { scrollAxisRange ->
            val scrollRatio = scrollAxisRange.value() / scrollAxisRange.maxValue()
            abs(scrollRatio - 0.5f) < 0.001f
        }

        rule.runOnIdle { runBlocking { state.scrollBy(10_000f) } }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertVerticalScrollAxisRange { scrollAxisRange ->
            val scrollRatio = scrollAxisRange.value() / scrollAxisRange.maxValue()
            scrollRatio in 0.999f..1f
        }
    }
}

private fun SemanticsNodeInteraction.assertVerticalScrollAxisRange(
    check: (ScrollAxisRange) -> Boolean
) {
    assert(
        SemanticsMatcher("VerticalScrollAxisRange") { node ->
            node.config
                .find { it.key == SemanticsProperties.VerticalScrollAxisRange }
                ?.let { check(it.value as ScrollAxisRange) } == true
        }
    )
}

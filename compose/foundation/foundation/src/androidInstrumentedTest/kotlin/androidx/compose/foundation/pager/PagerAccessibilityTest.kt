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

package androidx.compose.foundation.pager

import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@RunWith(Parameterized::class)
class PagerAccessibilityTest(config: ParamConfig) : BasePagerTest(config = config) {

    private val accessibilityNodeProvider: AccessibilityNodeProvider
        get() = checkNotNull(composeView) {
            "composeView not initialized."
        }.let { composeView ->
            ViewCompat
                .getAccessibilityDelegate(composeView)!!
                .getAccessibilityNodeProvider(composeView)!!
                .provider as AccessibilityNodeProvider
        }

    @Test
    fun accessibilityScroll_scrollToPage() {
        createPager(outOfBoundsPageCount = 1)

        assertThat(pagerState.currentPage).isEqualTo(0)

        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("1").performScrollTo()

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(1)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Ignore
    @Test
    fun accessibilityPaging_animateScrollToPage() {
        createPager(initialPage = 5, pageCount = { DefaultPageCount })

        assertThat(pagerState.currentPage).isEqualTo(5)

        val actionBackward = if (vertical) {
            android.R.id.accessibilityActionPageUp
        } else {
            android.R.id.accessibilityActionPageLeft
        }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(
                id,
                actionBackward,
                null
            )
        }

        // Go to the previous page
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(4)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        val actionForward = if (vertical) {
            android.R.id.accessibilityActionPageDown
        } else {
            android.R.id.accessibilityActionPageRight
        }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(
                id,
                actionForward,
                null
            )
        }

        // Go to the next page
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Ignore
    @Test
    fun userScrollEnabledIsOff_shouldNotAllowPageAccessibilityActions() {
        // Arrange
        createPager(
            pageCount = { DefaultPageCount },
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        // Act
        onPager()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageUp))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageDown))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageRight))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageLeft))
    }

    @Test
    fun focusScroll_forwardAndBackward_shouldGoToPage_pageShouldBeCorrectlyPlaced() {
        // Arrange
        createPager(pageCount = { DefaultPageCount })
        rule.runOnUiThread { initialFocusedItem.requestFocus() }
        rule.waitForIdle()

        // Act: move forward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Next) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(1)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        // Act: move backward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Previous) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(0)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Test
    fun userScrollEnabledIsOff_fillPages_focusScroll_shouldNotMovePages() {
        // Arrange
        val initialPage = 5
        createPager(
            initialPage = initialPage,
            pageCount = { DefaultPageCount },
            userScrollEnabled = false
        ) {
            Page(index = it, initialFocusedItemIndex = initialPage)
        }
        rule.runOnUiThread { initialFocusedItem.requestFocus() }
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(initialPage) }

        // Act: move forward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Next) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        // Act: move backward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Previous) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Test
    fun userScrollEnabledIsOff_fixedPages_focusScroll_shouldNotMovePages() {
        // Arrange
        val initialPage = 5
        createPager(
            modifier = Modifier.size(100.dp),
            initialPage = initialPage,
            pageCount = { DefaultPageCount },
            userScrollEnabled = false,
            pageSize = { PageSize.Fixed(10.dp) }
        ) {
            Page(index = it, initialFocusedItemIndex = initialPage)
        }
        rule.runOnUiThread { initialFocusedItem.requestFocus() }
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(initialPage) }

        // Act: move forward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Next) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
            assertThat(focused).containsExactly(6)
        }

        // Act: move backward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Previous) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
            assertThat(focused).containsExactly(5)
        }
    }

    private fun <T> SemanticsNodeInteraction.withSemanticsNode(block: SemanticsNode.() -> T): T {
        return block.invoke(fetchSemanticsNode())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = AllOrientationsParams
    }
}

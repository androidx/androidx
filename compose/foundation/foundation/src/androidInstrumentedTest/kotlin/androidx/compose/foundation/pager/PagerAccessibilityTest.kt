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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@RunWith(Parameterized::class)
class PagerAccessibilityTest(config: ParamConfig) : BasePagerTest(config = config) {

    private val accessibilityNodeProvider: AccessibilityNodeProvider
        get() =
            checkPreconditionNotNull(composeView) { "composeView not initialized." }
                .let { composeView ->
                    ViewCompat.getAccessibilityDelegate(composeView)!!.getAccessibilityNodeProvider(
                            composeView
                        )!!
                        .provider as AccessibilityNodeProvider
                }

    @Test
    fun scrollBySemantics_shouldScrollCorrectly() {
        createPager(initialPage = 5)

        assertThat(pagerState.currentPage).isEqualTo(5)

        rule.onNodeWithTag(PagerTestTag).performSemanticsAction(ScrollBy) { it.invoke(100f, 100f) }

        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isWithin(0.001f).of(100f / pageSize)
        }
    }

    @Test
    fun accessibilityScroll_scrollToPage() {
        createPager(beyondViewportPageCount = 1)

        assertThat(pagerState.currentPage).isEqualTo(0)

        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("1").performScrollTo()

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(1)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Test
    fun accessibilityPaging_animateScrollToPage() {
        createPager(initialPage = 5, pageCount = { DefaultPageCount })

        assertThat(pagerState.currentPage).isEqualTo(5)

        val actionBackward =
            if (vertical) {
                android.R.id.accessibilityActionPageUp
            } else {
                android.R.id.accessibilityActionPageLeft
            }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(id, actionBackward, null)
        }

        // Go to the previous page
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(4)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        val actionForward =
            if (vertical) {
                android.R.id.accessibilityActionPageDown
            } else {
                android.R.id.accessibilityActionPageRight
            }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(id, actionForward, null)
        }

        // Go to the next page
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

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
    fun focusScroll_forwardAndBackward_pageIsFocusable_fullPage_shouldScrollFullPage() {
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
    fun focusScroll_forwardAndBackward_pageIsFocusable_fixedSizedPage_shouldScrollFullPage() {
        // Arrange
        createPager(
            modifier = Modifier.size(210.dp), // make sure one page is halfway shown
            pageCount = { DefaultPageCount },
            pageSize = { PageSize.Fixed(50.dp) }
        )
        val lastVisibleItem = pagerState.layoutInfo.visiblePagesInfo.last().index
        rule.runOnUiThread { focusRequesters[lastVisibleItem - 1]?.requestFocus() }
        rule.waitForIdle()

        // Act: move forward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Next) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(1) // current page moved by 1
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        // move focus back to the first visible item
        rule.runOnUiThread { focusRequesters[pagerState.firstVisiblePage]?.requestFocus() }
        rule.waitForIdle()

        // Act: move backward
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Previous) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(0)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Test
    fun focusScroll_forwardAndBackward_pageContentIsFocusable_fullPage_shouldScrollFullPage() {
        // Arrange
        createPager(
            pageCount = { DefaultPageCount },
            pageContent = { page ->
                val focusRequester =
                    FocusRequester().also { if (page == 0) initialFocusedItem = it }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(30.dp).focusRequester(focusRequester).focusable())
                }
            }
        )
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
    fun focusScroll_forwardAndBackward_pageContentIsFocusable_fixedSizePage_shouldScrollFullPage() {
        // Arrange
        createPager(
            modifier = Modifier.size(210.dp), // make sure one page is halfway shown
            pageCount = { DefaultPageCount },
            pageSize = { PageSize.Fixed(50.dp) },
            pageContent = { page ->
                val focusRequester = FocusRequester().also { focusRequesters[page] = it }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // focus bounds is smaller than page itself
                    Box(modifier = Modifier.size(30.dp).focusRequester(focusRequester).focusable())
                }
            }
        )
        val lastVisibleItem = pagerState.layoutInfo.visiblePagesInfo.last().index
        rule.runOnUiThread { focusRequesters[lastVisibleItem - 1]?.requestFocus() }

        // Act: move forward
        val resultForward = rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Next) }
        assertThat(resultForward).isTrue() // focus moved

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(1) // current page moved by 1
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        rule.runOnUiThread { focusManager.clearFocus(true) } // reset focus

        // move focus back to the first visible item
        rule.runOnUiThread { focusRequesters[pagerState.firstVisiblePage]?.requestFocus() }

        // Act: move backward
        val resultBackward = rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Previous) }
        assertThat(resultBackward).isTrue() // focus moved

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

    @Test
    fun accessibilityScroll_alwaysScrollsFullPage_forward() {
        createPager()

        assertThat(pagerState.currentPage).isEqualTo(0)

        val actionBackward =
            if (vertical) {
                android.R.id.accessibilityActionScrollDown
            } else {
                android.R.id.accessibilityActionScrollRight
            }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(id, actionBackward, null)
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
            assertThat(pagerState.currentPage).isEqualTo(1)
        }
    }

    @Test
    fun accessibilityScroll_alwaysScrollsFullPage_backward() {
        createPager(initialPage = 1)

        assertThat(pagerState.currentPage).isEqualTo(1)

        val actionBackward =
            if (vertical) {
                android.R.id.accessibilityActionScrollUp
            } else {
                android.R.id.accessibilityActionScrollLeft
            }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(id, actionBackward, null)
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
            assertThat(pagerState.currentPage).isEqualTo(0)
        }
    }

    private fun <T> SemanticsNodeInteraction.withSemanticsNode(block: SemanticsNode.() -> T): T {
        return block.invoke(fetchSemanticsNode())
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllOrientationsParams
    }
}

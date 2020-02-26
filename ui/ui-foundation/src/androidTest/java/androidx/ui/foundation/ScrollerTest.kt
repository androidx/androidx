/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.foundation

import android.os.Handler
import android.os.Looper
import androidx.animation.ExponentialDecay
import androidx.animation.ManualAnimationClock
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.graphics.Color
import androidx.ui.layout.Align
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Row
import androidx.ui.semantics.Semantics
import androidx.ui.test.GestureScope
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.doScrollTo
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.sendSwipeDown
import androidx.ui.test.sendSwipeLeft
import androidx.ui.test.sendSwipeRight
import androidx.ui.test.sendSwipeUp
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch

@SmallTest
@RunWith(JUnit4::class)
class ScrollerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scrollerTag = "ScrollerTest"

    private val defaultCrossAxisSize = 45
    private val defaultMainAxisSize = 40
    private val defaultCellSize = 5

    private val colors = listOf(
        Color(red = 0xFF, green = 0, blue = 0, alpha = 0xFF),
        Color(red = 0xFF, green = 0xA5, blue = 0, alpha = 0xFF),
        Color(red = 0xFF, green = 0xFF, blue = 0, alpha = 0xFF),
        Color(red = 0xA5, green = 0xFF, blue = 0, alpha = 0xFF),
        Color(red = 0, green = 0xFF, blue = 0, alpha = 0xFF),
        Color(red = 0, green = 0xFF, blue = 0xA5, alpha = 0xFF),
        Color(red = 0, green = 0, blue = 0xFF, alpha = 0xFF),
        Color(red = 0xA5, green = 0, blue = 0xFF, alpha = 0xFF)
    )

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_SmallContent() {
        val height = 40

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @Test
    fun verticalScroller_SmallContent_Unscrollable() {
        val scrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        composeVerticalScroller(scrollerPosition)

        composeTestRule.runOnIdleCompose {
            assertTrue(scrollerPosition.maxPosition == 0f)
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_NoScroll() {
        val height = 30

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_ScrollToEnd() {
        val scrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
        val height = 30
        val scrollDistance = 10

        composeVerticalScroller(scrollerPosition, height = height)

        validateVerticalScroller(height = height)

        composeTestRule.runOnIdleCompose {
            assertEquals(scrollDistance.toFloat(), scrollerPosition.maxPosition)
            scrollerPosition.scrollTo(scrollDistance.toFloat())
        }

        composeTestRule.runOnIdleCompose {} // Just so the block below is correct
        validateVerticalScroller(offset = scrollDistance, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_SmallContent() {
        val width = 40

        composeHorizontalScroller(width = width)

        validateHorizontalScroller(width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_NoScroll() {
        val width = 30

        composeHorizontalScroller(width = width)

        validateHorizontalScroller(width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        composeHorizontalScroller(scrollerPosition, width = width)

        validateHorizontalScroller(width = width)

        composeTestRule.runOnIdleCompose {
            assertEquals(scrollDistance.toFloat(), scrollerPosition.maxPosition)
            scrollerPosition.scrollTo(scrollDistance.toFloat())
        }

        composeTestRule.runOnIdleCompose {} // Just so the block below is correct
        validateHorizontalScroller(offset = scrollDistance, width = width)
    }

    @Test
    fun verticalScroller_scrollTo_scrollForward() {
        createScrollableContent(isVertical = true)

        findByText("50")
            .assertIsNotDisplayed()
            .doScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_scrollTo_scrollForward() {
        createScrollableContent(isVertical = false)

        findByText("50")
            .assertIsNotDisplayed()
            .doScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun verticalScroller_scrollTo_scrollBack() {
        createScrollableContent(isVertical = true)

        findByText("50")
            .assertIsNotDisplayed()
            .doScrollTo()
            .assertIsDisplayed()

        findByText("20")
            .assertIsNotDisplayed()
            .doScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_scrollTo_scrollBack() {
        createScrollableContent(isVertical = false)

        findByText("50")
            .assertIsNotDisplayed()
            .doScrollTo()
            .assertIsDisplayed()

        findByText("20")
            .assertIsNotDisplayed()
            .doScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun verticalScroller_swipeUp_swipeDown() {
        swipeScrollerAndBack(true, GestureScope::sendSwipeUp, GestureScope::sendSwipeDown)
    }

    @Test
    fun horizontalScroller_swipeLeft_swipeRight() {
        swipeScrollerAndBack(false, GestureScope::sendSwipeLeft, GestureScope::sendSwipeRight)
    }

    private fun swipeScrollerAndBack(
        isVertical: Boolean,
        firstSwipe: GestureScope.() -> Unit,
        secondSwipe: GestureScope.() -> Unit
    ) {
        val clock = ManualAnimationClock(0)
        val scrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = clock
        )

        createScrollableContent(isVertical, scrollerPosition = scrollerPosition)

        composeTestRule.runOnIdleCompose {
            assertThat(scrollerPosition.value).isEqualTo(0f)
        }

        findByTag(scrollerTag)
            .doGesture { firstSwipe() }

        composeTestRule.runOnIdleCompose {
            clock.clockTimeMillis += 5000
        }

        findByTag(scrollerTag)
            .awaitScrollAnimation(scrollerPosition)

        val scrolledValue = composeTestRule.runOnIdleCompose {
            scrollerPosition.value
        }
        assertThat(scrolledValue).isGreaterThan(0f)

        findByTag(scrollerTag)
            .doGesture { secondSwipe() }

        composeTestRule.runOnIdleCompose {
            clock.clockTimeMillis += 5000
        }

        findByTag(scrollerTag)
            .awaitScrollAnimation(scrollerPosition)

        composeTestRule.runOnIdleCompose {
            assertThat(scrollerPosition.value).isLessThan(scrolledValue)
        }
    }

    private fun composeVerticalScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        ),
        width: Int = defaultCrossAxisSize,
        height: Int = defaultMainAxisSize,
        rowHeight: Int = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Align(alignment = Alignment.TopStart) {
                    TestTag(scrollerTag) {
                        VerticalScroller(
                            scrollerPosition = scrollerPosition,
                            modifier = LayoutSize(width.toDp(), height.toDp())
                        ) {
                            Column {
                                colors.forEach { color ->
                                    Box(
                                        LayoutSize(width.toDp(), rowHeight.toDp()),
                                        backgroundColor = color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeHorizontalScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        ),
        width: Int = defaultMainAxisSize,
        height: Int = defaultCrossAxisSize,
        columnWidth: Int = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Align(alignment = Alignment.TopStart) {
                    TestTag(scrollerTag) {
                        HorizontalScroller(
                            scrollerPosition = scrollerPosition,
                            modifier = LayoutSize(width.toDp(), height.toDp())
                        ) {
                            Row {
                                colors.forEach { color ->
                                    Box(
                                        LayoutSize(columnWidth.toDp(), height.toDp()),
                                        backgroundColor = color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = 26)
    private fun validateVerticalScroller(
        offset: Int = 0,
        width: Int = 45,
        height: Int = 40,
        rowHeight: Int = 5
    ) {
        findByTag(scrollerTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(width.ipx, height.ipx)) { pos ->
                val colorIndex = (offset + pos.y.value) / rowHeight
                colors[colorIndex]
            }
    }

    @RequiresApi(api = 26)
    private fun validateHorizontalScroller(
        offset: Int = 0,
        width: Int = 40,
        height: Int = 45,
        columnWidth: Int = 5
    ) {
        findByTag(scrollerTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(width.ipx, height.ipx)) { pos ->
                val colorIndex = (offset + pos.x.value) / columnWidth
                colors[colorIndex]
            }
    }

    private fun createScrollableContent(
        isVertical: Boolean,
        itemCount: Int = 100,
        width: Dp = 100.dp,
        height: Dp = 100.dp,
        scrollerPosition: ScrollerPosition = ScrollerPosition(
            FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
    ) {
        composeTestRule.setContent {
            val content = @Composable {
                repeat(itemCount) {
                    Semantics(container = true) {
                        Text(text = "$it")
                    }
                }
            }
            Align(alignment = Alignment.TopStart) {
                Box(LayoutSize(width, height), backgroundColor = Color.White) {
                    TestTag(scrollerTag) {
                        Semantics(container = true) {
                            if (isVertical) {
                                VerticalScroller(scrollerPosition) {
                                    Column {
                                        content()
                                    }
                                }
                            } else {
                                HorizontalScroller(scrollerPosition) {
                                    Row {
                                        content()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO(b/147291885): This should not be needed in the future.
    private fun SemanticsNodeInteraction.awaitScrollAnimation(
        scroller: ScrollerPosition
    ): SemanticsNodeInteraction {
        if (!scroller.isAnimating) {
            return this
        }
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (scroller.isAnimating) {
                    handler.post(this)
                } else {
                    latch.countDown()
                }
            }
        })
        latch.await()
        return this
    }
}

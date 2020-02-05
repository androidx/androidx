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
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Draw
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.layout.Align
import androidx.ui.layout.Column
import androidx.ui.layout.Container
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
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.toPx
import androidx.ui.unit.toRect
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

    private val defaultCrossAxisSize = 45.ipx
    private val defaultMainAxisSize = 40.ipx
    private val defaultCellSize = 5.ipx

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
        val height = 40.ipx

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @Test
    fun verticalScroller_SmallContent_Unscrollable() {
        val scrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay()))

        composeVerticalScroller(scrollerPosition)

        composeTestRule.runOnIdleCompose {
            assertTrue(scrollerPosition.maxPosition == 0.px)
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_NoScroll() {
        val height = 30.ipx

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_ScrollToEnd() {
        val scrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay()))
        val height = 30.ipx
        val scrollDistance = 10.ipx

        composeVerticalScroller(scrollerPosition, height = height)

        validateVerticalScroller(height = height)

        composeTestRule.runOnIdleCompose {
            assertEquals(scrollDistance.toPx(), scrollerPosition.maxPosition)
            scrollerPosition.scrollTo(scrollDistance.toPx())
        }

        composeTestRule.runOnIdleCompose {} // Just so the block below is correct
        validateVerticalScroller(offset = scrollDistance, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_SmallContent() {
        val width = 40.ipx

        composeHorizontalScroller(width = width)

        validateHorizontalScroller(width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_NoScroll() {
        val width = 30.ipx

        composeHorizontalScroller(width = width)

        validateHorizontalScroller(width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_ScrollToEnd() {
        val width = 30.ipx
        val scrollDistance = 10.ipx

        val scrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay()))

        composeHorizontalScroller(scrollerPosition, width = width)

        validateHorizontalScroller(width = width)

        composeTestRule.runOnIdleCompose {
            assertEquals(scrollDistance.toPx(), scrollerPosition.maxPosition)
            scrollerPosition.scrollTo(scrollDistance.toPx())
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
        val scrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay()))

        createScrollableContent(isVertical, scrollerPosition = scrollerPosition)

        composeTestRule.runOnIdleCompose {
            assertThat(scrollerPosition.value).isEqualTo(0.px)
        }

        findByTag(scrollerTag)
            .doGesture { firstSwipe() }
            .awaitScrollAnimation(scrollerPosition)

        val scrolledValue = composeTestRule.runOnIdleCompose {
            scrollerPosition.value
        }
        assertThat(scrolledValue).isGreaterThan(0.px)

        findByTag(scrollerTag)
            .doGesture { secondSwipe() }
            .awaitScrollAnimation(scrollerPosition)

        composeTestRule.runOnIdleCompose {
            assertThat(scrollerPosition.value).isLessThan(scrolledValue)
        }
    }

    private fun composeVerticalScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay())),
        width: IntPx = defaultCrossAxisSize,
        height: IntPx = defaultMainAxisSize,
        rowHeight: IntPx = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Align(alignment = Alignment.TopLeft) {
                    TestTag(scrollerTag) {
                        VerticalScroller(
                            scrollerPosition = scrollerPosition,
                            modifier = LayoutSize(width.toDp(), height.toDp())
                        ) {
                            Column {
                                colors.forEach { color ->
                                    Container(
                                        height = rowHeight.toDp(),
                                        width = width.toDp()
                                    ) {
                                        Draw { canvas, parentSize ->
                                            val paint = Paint()
                                            paint.color = color
                                            paint.style = PaintingStyle.fill
                                            canvas.drawRect(parentSize.toRect(), paint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeHorizontalScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay())),
        width: IntPx = defaultMainAxisSize,
        height: IntPx = defaultCrossAxisSize,
        columnWidth: IntPx = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Align(alignment = Alignment.TopLeft) {
                    TestTag(scrollerTag) {
                        HorizontalScroller(
                            scrollerPosition = scrollerPosition,
                            modifier = LayoutSize(width.toDp(), height.toDp())
                        ) {
                            Row {
                                colors.forEach { color ->
                                    Container(
                                        width = columnWidth.toDp(),
                                        height = height.toDp()
                                    ) {
                                        Draw { canvas, parentSize ->
                                            val paint = Paint()
                                            paint.color = color
                                            paint.style = PaintingStyle.fill
                                            canvas.drawRect(parentSize.toRect(), paint)
                                        }
                                    }
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
        offset: IntPx = 0.ipx,
        width: IntPx = 45.ipx,
        height: IntPx = 40.ipx,
        rowHeight: IntPx = 5.ipx
    ) {
        findByTag(scrollerTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(width, height)) { pos ->
                val colorIndex = (offset.value + pos.y.value) / rowHeight.value
                colors[colorIndex]
            }
    }

    @RequiresApi(api = 26)
    private fun validateHorizontalScroller(
        offset: IntPx = 0.ipx,
        width: IntPx = 40.ipx,
        height: IntPx = 45.ipx,
        columnWidth: IntPx = 5.ipx
    ) {
        findByTag(scrollerTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(width, height)) { pos ->
                val colorIndex = (offset.value + pos.x.value) / columnWidth.value
                colors[colorIndex]
            }
    }

    private fun createScrollableContent(
        isVertical: Boolean,
        itemCount: Int = 100,
        width: Dp = 100.dp,
        height: Dp = 100.dp,
        scrollerPosition: ScrollerPosition = ScrollerPosition(FlingConfig(ExponentialDecay()))
    ) {
        composeTestRule.setContent {
            val content = @Composable {
                repeat(itemCount) {
                    Semantics(container = true) {
                        Text(text = "$it")
                    }
                }
            }
            Align(alignment = Alignment.TopLeft) {
                Container(width = width, height = height) {
                    DrawShape(RectangleShape, Color.White)
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
        if (!scroller.holder.animatedFloat.isRunning) {
            return this
        }
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (scroller.holder.animatedFloat.isRunning) {
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

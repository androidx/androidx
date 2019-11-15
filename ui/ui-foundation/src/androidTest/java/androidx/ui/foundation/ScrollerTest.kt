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

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.AndroidComposeView
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Px
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.setContent
import androidx.ui.core.toPx
import androidx.ui.core.toRect
import androidx.ui.core.withDensity
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Align
import androidx.ui.layout.Column
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Row
import androidx.ui.semantics.Semantics
import androidx.ui.test.GestureScope
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.doScrollTo
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.sendSwipeDown
import androidx.ui.test.sendSwipeLeft
import androidx.ui.test.sendSwipeRight
import androidx.ui.test.sendSwipeUp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ScrollerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // TODO(malkov/pavlis) : some tests here require activity access as we need
    // to take screen's bitmap, abstract it better
    private val activity
        get() = (composeTestRule as AndroidComposeTestRule).activityTestRule.activity

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

    private var drawLatch = CountDownLatch(1)
    private lateinit var handler: Handler

    @Before
    fun setupDrawLatch() {
        drawLatch = CountDownLatch(1)
        composeTestRule.runOnUiThread {
            handler = Handler()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_SmallContent() {
        val height = 40.ipx

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @Test
    fun verticalScroller_SmallContent_Unscrollable() {
        val scrollerPosition = ScrollerPosition()

        // latch to wait for a new max to come on layout
        val newMaxLatch = CountDownLatch(1)

        composeVerticalScroller(scrollerPosition)

        val onGlobalLayout = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                newMaxLatch.countDown()
            }
        }
        composeTestRule.runOnUiThread {
            activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayout)
        }
        assertTrue(newMaxLatch.await(1, TimeUnit.SECONDS))
        composeTestRule.runOnUiThread {
            activity.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayout)
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
        val scrollerPosition = ScrollerPosition()
        val height = 30.ipx
        val scrollDistance = 10.ipx

        composeVerticalScroller(scrollerPosition, height = height)

        validateVerticalScroller(height = height)

        // The 'draw' method will no longer be called because only the position
        // changes during scrolling. Therefore, we should just wait until the draw stage
        // completes and the scrolling will be finished by then.
        val latch = CountDownLatch(1)
        val onDrawListener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                latch.countDown()
            }
        }
        composeTestRule.runOnUiThread {
            activity.window.decorView.viewTreeObserver.addOnDrawListener(onDrawListener)
            assertEquals(scrollDistance.toPx(), scrollerPosition.maxPosition)
            scrollerPosition.scrollTo(scrollDistance.toPx())
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        composeTestRule.runOnUiThread {
            activity.window.decorView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }
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

        val scrollerPosition = ScrollerPosition()

        composeHorizontalScroller(scrollerPosition, width = width)

        validateHorizontalScroller(width = width)

        // The 'draw' method will no longer be called because only the position
        // changes during scrolling. Therefore, we should just wait until the draw stage
        // completes and the scrolling will be finished by then.
        val latch = CountDownLatch(1)
        val onDrawListener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                latch.countDown()
            }
        }
        composeTestRule.runOnUiThread {
            activity.window.decorView.viewTreeObserver.addOnDrawListener(onDrawListener)
            assertEquals(scrollDistance.toPx(), scrollerPosition.maxPosition)
            scrollerPosition.scrollTo(scrollDistance.toPx())
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        composeTestRule.runOnUiThread {
            activity.window.decorView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }
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
        val scrollerPosition = ScrollerPosition()

        createScrollableContent(isVertical, scrollerPosition = scrollerPosition)
        assertThat(scrollerPosition.getValueOnUiThread()).isEqualTo(0.px)

        findByTag("scroller")
            .doGesture { firstSwipe() }
            .awaitScrollAnimation(scrollerPosition)

        val scrolledValue = scrollerPosition.getValueOnUiThread()
        assertThat(scrolledValue).isGreaterThan(0.px)

        findByTag("scroller")
            .doGesture { secondSwipe() }
            .awaitScrollAnimation(scrollerPosition)

        assertThat(scrollerPosition.getValueOnUiThread()).isLessThan(scrolledValue)
    }

    private fun ScrollerPosition.getValueOnUiThread(): Px {
        var value = 0.px
        val latch = CountDownLatch(1)
        composeTestRule.runOnUiThread {
            value = this.value
            latch.countDown()
        }
        latch.await()
        return value
    }

    private fun composeVerticalScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition(),
        width: IntPx = defaultCrossAxisSize,
        height: IntPx = defaultMainAxisSize,
        rowHeight: IntPx = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        withDensity(composeTestRule.density) {
            val constraints = DpConstraints.tightConstraints(width.toDp(), height.toDp())
            composeTestRule.runOnUiThread {
                activity.setContent {
                    Align(alignment = Alignment.TopLeft) {
                        ConstrainedBox(constraints = constraints) {
                            VerticalScroller(scrollerPosition = scrollerPosition) {
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
                            Draw { _, _ ->
                                drawLatch.countDown()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeHorizontalScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition(),
        width: IntPx = defaultMainAxisSize,
        height: IntPx = defaultCrossAxisSize,
        columnWidth: IntPx = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        withDensity(composeTestRule.density) {
            val constraints = DpConstraints.tightConstraints(width.toDp(), height.toDp())
            composeTestRule.runOnUiThread {
                activity.setContent {
                    Align(alignment = Alignment.TopLeft) {
                        ConstrainedBox(constraints = constraints) {
                            HorizontalScroller(scrollerPosition = scrollerPosition) {
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
                            Draw { _, _ ->
                                drawLatch.countDown()
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
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val bitmap = waitAndScreenShot()
        assertTrue(bitmap.height >= height.value)
        assertTrue(bitmap.width >= width.value)
        for (y in 0 until height.value) {
            val colorIndex = (offset.value + y) / rowHeight.value
            val expectedColor = colors[colorIndex]

            for (x in 0 until width.value) {
                val pixel = bitmap.getPixel(x, y)
                assertEquals(
                    "Expected $expectedColor, but got ${Color(pixel)} at $x, $y",
                    expectedColor.toArgb(), pixel
                )
            }
        }
    }

    @RequiresApi(api = 26)
    private fun validateHorizontalScroller(
        offset: IntPx = 0.ipx,
        width: IntPx = 40.ipx,
        height: IntPx = 45.ipx,
        columnWidth: IntPx = 5.ipx
    ) {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val bitmap = waitAndScreenShot()
        assertTrue(bitmap.height >= height.value)
        assertTrue(bitmap.width >= width.value)
        for (x in 0 until width.value) {
            val colorIndex = (offset.value + x) / columnWidth.value
            val expectedColor = colors[colorIndex]

            for (y in 0 until height.value) {
                val pixel = bitmap.getPixel(x, y)
                assertEquals(
                    "Expected $expectedColor, but got ${Color(pixel)} at $x, $y",
                    expectedColor.toArgb(), pixel
                )
            }
        }
    }

    private fun createScrollableContent(
        isVertical: Boolean,
        itemCount: Int = 100,
        width: Dp = 100.dp,
        height: Dp = 100.dp,
        scrollerPosition: ScrollerPosition = ScrollerPosition()
    ) {
        composeTestRule.setContent {
            val content = @Composable {
                repeat(itemCount) {
                    Text(text = "$it")
                }
            }
            Align(alignment = Alignment.TopLeft) {
                Container(width = width, height = height) {
                    DrawShape(RectangleShape, Color.White)
                    TestTag("scroller") {
                        Semantics {
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

    @RequiresApi(api = 26) // For PixelCopy.request(Window, Rect, Bitmap, listener, Handler)
    private fun waitAndScreenShot(): Bitmap {
        val view = findAndroidComposeView()
        waitForDraw(view)

        val offset = intArrayOf(0, 0)
        val width = view.width
        val height = view.height

        val dest =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcRect = android.graphics.Rect(0, 0, width, height)
        srcRect.offset(offset[0], offset[1])
        val latch = CountDownLatch(1)
        var copyResult = 0
        val onCopyFinished = object : PixelCopy.OnPixelCopyFinishedListener {
            override fun onPixelCopyFinished(result: Int) {
                copyResult = result
                latch.countDown()
            }
        }
        PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler)
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, copyResult)
        return dest
    }

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

    // TODO(malkov): ALL below is copypaste from LayoutTest as this test in ui-foundation now

    private fun findAndroidComposeView(): AndroidComposeView {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findAndroidComposeView(contentViewGroup)!!
    }

    private fun findAndroidComposeView(parent: ViewGroup): AndroidComposeView? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is AndroidComposeView) {
                return child
            } else if (child is ViewGroup) {
                val composeView = findAndroidComposeView(child)
                if (composeView != null) {
                    return composeView
                }
            }
        }
        return null
    }

    private fun waitForDraw(view: View) {
        val viewDrawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                viewDrawLatch.countDown()
            }
        }
        view.post(object : Runnable {
            override fun run() {
                view.viewTreeObserver.addOnDrawListener(listener)
                view.invalidate()
            }
        })
        assertTrue(viewDrawLatch.await(1, TimeUnit.SECONDS))
    }
}

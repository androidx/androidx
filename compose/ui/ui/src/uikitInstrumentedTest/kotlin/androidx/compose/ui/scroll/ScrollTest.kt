/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.runUIKitInstrumentedTestWithInterop
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.CUPERTINO_TOUCH_SLOP
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.test.utils.dpRectInWindow
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIScrollView

internal class ScrollTest {

    /**
     * Tests that a drag of the same value as the touch slop threshold will not trigger overscroll behavior
     * in a vertically scrollable Column.
     **/
    @Test
    fun testExactTouchSlopDrag() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height)
                    .background(Color.Blue)
                )
            }
        }

        val initialBoxRect = boxRect.copy()
        val dyExact = CUPERTINO_TOUCH_SLOP.dp

        touchDown(screenSize.center)
            .dragBy(dy = dyExact)

        waitForIdle()

        assertEquals(initialBoxRect, boxRect)
    }

    @Test
    fun testExactTouchSlopDragWithCustomDensityInDialog() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(0.5f)) {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        usePlatformInsets = false,
                        useSoftwareKeyboardInset = false,
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.Red)
                                .onGloballyPositioned {
                                    boxRect = it.boundsInWindow().toDpRect(density)
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(screenSize.height)
                                .background(Color.Blue)
                        )
                    }
                }
            }
        }

        val initialBoxRect = boxRect.copy()
        val dyExact = CUPERTINO_TOUCH_SLOP.dp

        touchDown(screenSize.center)
            .dragBy(dy = dyExact)

        waitForIdle()

        assertEquals(initialBoxRect, boxRect)
    }

    /**
     * Tests that a drag just over the touch slop threshold will trigger overscroll behavior
     * in a vertically scrollable Column.
     **/
    @Test
    fun testJustOverTouchSlopDrag() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height)
                    .background(Color.White)
                )
            }
        }

        val dyJustOver = CUPERTINO_TOUCH_SLOP.dp + 1.dp

        touchDown(screenSize.center)
            .dragBy(dy = dyJustOver)

        waitForIdle()

        assertTrue(boxRect.top > 0.dp)
        // Scroll state remains at 0 despite visual overscroll
        assertEquals(0 * density.density, state.value.toFloat())
    }

    @Test
    fun testJustOverTouchSlopDragWithCustomDensityInDialog() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(0.5f)) {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        usePlatformInsets = false,
                        useSoftwareKeyboardInset = false,
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.Red)
                                .onGloballyPositioned {
                                    boxRect = it.boundsInWindow().toDpRect(density)
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(screenSize.height)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        val dyJustOver = CUPERTINO_TOUCH_SLOP.dp + 1.dp

        touchDown(screenSize.center)
            .dragBy(dy = dyJustOver)

        waitForIdle()

        assertTrue(boxRect.top > 0.dp)
        // Scroll state remains at 0 despite visual overscroll
        assertEquals(0 * density.density, state.value.toFloat())
    }

    @Test
    fun testTopOverscrollDragResistance() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Red)
                    .onGloballyPositioned {
                        boxRect = it.boundsInWindow().toDpRect(density)
                    }
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height.times(2))
                    .background(Color.White)
                )
            }
        }

        val touch = touchDown(screenSize.center)
        var previousBoxTop = boxRect.top
        var previousDiff = 0f
        val dragDelta = screenSize.center.y / 10
        repeat(10) { i ->
            touch.dragBy(dy = dragDelta, duration = 100.milliseconds)
            waitForIdle()

            val currentBoxTop = boxRect.top
            val currentDiff = (currentBoxTop - previousBoxTop).value

            // skip the first two iterations as we don't want to take into account the first drag position
            if (i > 1) {
                try {
                    assertEquals(currentDiff, previousDiff, 5e-5f)
                } catch (_: AssertionError) {
                    assertTrue(currentDiff < previousDiff, "$currentDiff < $previousDiff")
                }
            }

            previousBoxTop = currentBoxTop
            previousDiff = currentDiff
        }

        waitForIdle()
        touch.up()
        waitForIdle()
        // stabilizes back at the original position
        assertEquals(DpRect(DpOffset.Zero, DpSize(screenSize.width, 100.dp)), boxRect)
    }

    @Test
    fun testBottomOverscrollDragResistance() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        val boxHeight = 100.0
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height)
                    .background(Color.White)
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(boxHeight.dp)
                    .background(Color.Red)
                    .onGloballyPositioned {
                        boxRect = it.boundsInWindow().toDpRect(density)
                    }
                )
            }
        }

        val touch = touchDown(screenSize.center)
        var previousBoxTop = boxRect.top
        var previousDiff = 0f
        val dragDelta = screenSize.center.y / 10

        repeat(10) { i ->
            touch.dragBy(dy = -dragDelta, duration = 0.1.seconds)
            waitForIdle()

            val currentBoxTop = boxRect.top
            val currentDiff = (currentBoxTop - previousBoxTop).value

            // skip the first two iterations as we don't want to take into account the first drag position
            if (i > 1) {
                try {
                    assertEquals(currentDiff, previousDiff, 5e-5f)
                } catch (_: AssertionError) {
                    assertTrue(currentDiff > previousDiff)
                }
            }

            previousBoxTop = currentBoxTop
            previousDiff = currentDiff
        }

        waitForIdle()
        touch.up()
        waitForIdle()
        assertEquals(DpRect(DpOffset(x = 0.dp, y = screenSize.height - boxHeight.dp), DpSize(width = screenSize.width, height = boxHeight.dp)), boxRect)
    }

    @Test
    fun testOverscrollAndFling() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        val boxHeight = 100.0
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                repeat(20) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight.dp)
                            .background(if (index % 2 == 0) Color.Blue else Color.Red)
                            .then(
                                if (index == 0) {
                                    Modifier.onGloballyPositioned {
                                        boxRect = it.boundsInWindow().toDpRect(density)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }

        // overscroll
        val touch = touchDown(screenSize.center)
            .dragBy(dy = boxHeight.dp)

        // rubber band effect is applied
        assertTrue(0.dp < boxRect.top && boxRect.top < boxHeight.dp)
        // overscroll does not alter scroll state
        assertEquals(0 * density.density, state.value.toFloat())

        // fling up
        touch
            .dragBy(dy = -(boxHeight + 50).dp, duration = 100.milliseconds)
            .up()

        waitForIdle()

        // top box is out of visible bounds
        assertEquals(DpRectZero(), boxRect)
        // scroll state is updated
        assertTrue(state.value > boxHeight)
    }

    /**
     * Verifies that drag gestures smaller than the touch slop threshold
     * don't trigger scrolling behavior in a vertically scrollable Column.
     */
    @Test
    fun testNotScrollingForSmallDrag() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height)
                    .background(Color.White)
                )
            }
        }


        val dySmall = 5.dp
        assertTrue(dySmall.value < CUPERTINO_TOUCH_SLOP)

        // downward drag - overscroll
        touchDown(screenSize.center)
            .dragBy(dy = dySmall)
        waitForIdle()

        val initialBoxRect = DpRect(DpOffset.Zero, DpSize(screenSize.width, 100.dp))

        // expect no changes
        assertEquals(0 * density.density, state.value.toFloat())
        assertEquals(initialBoxRect, boxRect)

        // upward drag - scroll
        touchDown(screenSize.center)
            .dragBy(dy = -dySmall)
        waitForIdle()

        // expect no changes
        assertEquals(0 * density.density, state.value.toFloat())
        assertEquals(initialBoxRect, boxRect)
    }

    @Test
    fun testOverscrollForContentSmallerThanScreenSize() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Green)
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
            }
        }

        val initialBoxRect = DpRect(DpOffset(x = 0.dp, y = 100.dp), DpSize(screenSize.width, 100.dp))

        assertEquals(initialBoxRect, boxRect)

        touchDown(DpOffset(screenSize.center.x, 50.dp))
            .dragBy(dy = 50.dp)

        waitForIdle()
        assertEquals(initialBoxRect, boxRect)
    }

    @Test
    fun testOverscrollForContentSizeOfScreenSize() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height)
                    .background(Color.Green)
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
            }
        }

        val initialBoxRect = DpRect(DpOffset.Zero, screenSize)

        assertEquals(initialBoxRect, boxRect)

        touchDown(DpOffset(screenSize.center.x, 50.dp))
            .dragBy(dy = 50.dp)

        waitForIdle()
        assertEquals(initialBoxRect, boxRect)
    }

    @Test
    fun testHorizontalScrollWithRTL() = runUIKitInstrumentedTest {
        val itemSize = 150
        val lazyRowState = LazyListState()
        val totalScrollOffset = { lazyRowState.firstVisibleItemIndex * itemSize + lazyRowState.firstVisibleItemScrollOffset }

        setContent {
            HorizontalScrollContent(
                itemSize = itemSize.dp,
                lazyRowState = lazyRowState,
                layoutDirection = LayoutDirection.Rtl
            )
        }

        touchDown(DpOffset(screenSize.center.x, 50.dp))
            .dragBy(dx = (150 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(150, totalScrollOffset())
    }

    @Test
    fun testHorizontalScrollWithLTR() = runUIKitInstrumentedTest {
        val itemSize = 150
        val lazyRowState = LazyListState()
        val totalScrollOffset = { lazyRowState.firstVisibleItemIndex * itemSize + lazyRowState.firstVisibleItemScrollOffset }

        setContent {
            HorizontalScrollContent(
                itemSize = itemSize.dp,
                lazyRowState = lazyRowState,
                layoutDirection = LayoutDirection.Ltr
            )
        }

        touchDown(DpOffset(screenSize.center.x, 50.dp))
            .dragBy(dx = -(150 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(150, totalScrollOffset())
    }

    @Test
    fun testHorizontalOverscrollWithRTL() = runUIKitInstrumentedTest {
        val itemSize = 150
        val itemCount = 20
        val lazyRowState = LazyListState()
        val totalScrollOffset = { lazyRowState.firstVisibleItemIndex * itemSize + lazyRowState.firstVisibleItemScrollOffset }
        var firstBoxRect = DpRectZero()

        setContent {
            HorizontalScrollContent(
                itemSize = itemSize.dp,
                itemCount = itemCount,
                lazyRowState = lazyRowState,
                layoutDirection = LayoutDirection.Rtl,
                onFirstBoxGloballyPositioned = { firstBoxRect = it.boundsInWindow().toDpRect(density) }
            )
        }

        val touch = touchDown(DpOffset(screenSize.center.x, 50.dp))
            .dragBy(dx = -(50 + CUPERTINO_TOUCH_SLOP).dp)

        assertTrue(firstBoxRect.right < screenSize.width)
        assertTrue(firstBoxRect.right > screenSize.width - 50.dp)

        touch.up()

        waitForIdle()

        assertEquals(screenSize.width, firstBoxRect.right)
        assertEquals(0, totalScrollOffset())
    }

    @Test
    fun testHorizontalOverscrollWithLTR() = runUIKitInstrumentedTest {
        val itemSize = 150
        val lazyRowState = LazyListState()
        val totalScrollOffset = { lazyRowState.firstVisibleItemIndex * itemSize + lazyRowState.firstVisibleItemScrollOffset }
        var firstBoxRect = DpRectZero()

        setContent {
            HorizontalScrollContent(
                itemSize = itemSize.dp,
                lazyRowState = lazyRowState,
                layoutDirection = LayoutDirection.Ltr,
                onFirstBoxGloballyPositioned = { firstBoxRect = it.boundsInWindow().toDpRect(density) }
            )
        }

        val touch = touchDown(DpOffset(screenSize.center.x, 50.dp))
            .dragBy(dx = (50 + CUPERTINO_TOUCH_SLOP).dp)

        assertTrue(firstBoxRect.left > 0.dp)
        assertTrue(firstBoxRect.left < 50.dp)

        touch.up()

        waitForIdle()

        assertEquals(0.dp, firstBoxRect.left)
        assertEquals(0, totalScrollOffset())
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDragWithTouchStartInUIKitViewAndComposeView() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var boxRect = DpRectZero()
        var labelRect = DpRectZero()

        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Blue)
                )

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                    .testTag("Red Box")
                )

                UIKitView(
                    factory = {
                        val label = UILabel(frame = CGRectZero.readValue())
                        label.text = "UIKit.UILabel"
                        label.textColor = UIColor.blackColor
                        label.backgroundColor = UIColor.redColor
                        label
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .onGloballyPositioned { labelRect = it.boundsInWindow().toDpRect(density) }
                        .testTag("UIKit.UILabel"),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.height)
                    .background(Color.White)
                )
            }
        }

        val initialBoxRect = boxRect.copy()
        val initialLabelRect = labelRect.copy()

        findNodeWithTag("UIKit.UILabel")
            .touchDown()
            .dragBy(dy = -(100 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(DpRect(DpOffset(x = 0.dp, y = 0.dp), DpSize(screenSize.width, 100.dp)), boxRect)
        assertEquals(DpRect(DpOffset(x = 0.dp, y = 100.dp), DpSize(screenSize.width, 200.dp)), labelRect)

        findNodeWithTag("Red Box")
            .touchDown()
            .dragBy(dy = (100 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(DpRect(DpOffset(x = 0.dp, y = 100.dp), DpSize(screenSize.width, 100.dp)), initialBoxRect)
        assertEquals(DpRect(DpOffset(x = 0.dp, y = 200.dp), DpSize(screenSize.width, 200.dp)), initialLabelRect)
    }

    @Test
    fun testUIKitScrollViewInsideComposeScrollView_DragFromUIKitScrollView() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiScrollViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalUIKitScrollInsideVerticalScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                density = density,
                topContentHeight = 400.dp,
                uiKitScrollViewHeight = 400.dp,
                uiKitScrollViewRectInWindow = { uiScrollViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        val initialUIScrollViewRect = uiScrollViewRect()

        findNodeWithTag("UIKit.UIScrollView")
            .touchDown()
            .also {
                // There is an issue on iOS < 15 where the simulated drag is not applied immediately.
                if (!available(OS.Ios to OSVersion(16))) {
                    delay(100)
                }
            }
            .dragBy(dy = -(250 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(initialUIScrollViewRect, uiScrollViewRect())
        assertEquals(DpOffset(x = 0.dp, y = 250.dp), contentOffset())
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testUIKitScrollViewInsideComposeScrollView_DragFromCompose() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiScrollViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalUIKitScrollInsideVerticalScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                density = density,
                topContentHeight = 400.dp,
                uiKitScrollViewHeight = 400.dp,
                uiKitScrollViewRectInWindow = { uiScrollViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        findNodeWithTag("Top Box")
            .touchDown()
            .dragBy(dy = -(100 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(DpRect(DpOffset(x = 0.dp, y = 300.dp), DpSize(screenSize.width, 400.dp)), uiScrollViewRect())
        assertEquals(DpOffset.Zero, contentOffset())
    }

    @Test
    fun testOverscrollForUIKitHorizontalScrollViewAtTop() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiKitViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalScrollWithHorizontalUIKitScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                topContentHeight = 0.dp,
                uiKitScrollViewHeight = 200.dp,
                uiKitScrollViewRectInWindow = { uiKitViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        val initialUIKitViewRect = uiKitViewRect().copy()

        assertEquals(DpRect(DpOffset(x = 0.dp, y = 0.dp), DpSize(screenSize.width, 200.dp)), initialUIKitViewRect)

        val touch = touchDown(DpOffset(screenSize.center.x, 100.dp))
            .dragBy(dy = (50 + CUPERTINO_TOUCH_SLOP).dp)

        waitForIdle()

        assertTrue(uiKitViewRect().top > 0.dp)
        assertTrue(uiKitViewRect().top < (50 + CUPERTINO_TOUCH_SLOP).dp)

        assertEquals(DpOffset(x = 0.dp, y = 0.dp), contentOffset())

        touch.up()
        waitForIdle()

        assertEquals(initialUIKitViewRect, uiKitViewRect())
        assertEquals(0 * density.density, state.value.toFloat())
    }

    /**
     * Tests horizontal UIScrollView scrolling behavior when:
     * - Touch interaction starts inside the UIScrollView
     * - Drag gesture continues horizontally
     */
    @Test
    fun testHorizontalUIScrollViewInComposeScroll_HorizontalDrag() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiKitViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalScrollWithHorizontalUIKitScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                topContentHeight = 200.dp,
                uiKitScrollViewHeight = 200.dp,
                uiKitScrollViewRectInWindow = { uiKitViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        val initialUIKitViewRect = uiKitViewRect().copy()

        findNodeWithTag("UIKit.UIScrollView")
            .touchDown()
            .dragBy(dx = -(50 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(0 * density.density, state.value.toFloat())
        assertEquals(initialUIKitViewRect, uiKitViewRect())
        assertEquals(DpOffset(x = 50.dp, y = 0.dp), contentOffset())
    }

    /**
     * Tests horizontal UIScrollView scrolling behavior when:
     * - Touch interaction starts inside the UIScrollView
     * - Drag gesture continues vertically
     */
    @Test
    fun testHorizontalUIScrollViewInComposeVerticalScroll_VerticalDrag() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiKitViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalScrollWithHorizontalUIKitScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                topContentHeight = 200.dp,
                uiKitScrollViewHeight = 200.dp,
                uiKitScrollViewRectInWindow = { uiKitViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        findNodeWithTag("UIKit.UIScrollView")
            .touchDown()
            .dragBy(dy = -(50 + CUPERTINO_TOUCH_SLOP).dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(50 * density.density, state.value.toFloat())
        assertEquals(DpRect(DpOffset(x = 0.dp, y = 150.dp), DpSize(screenSize.width, 200.dp)), uiKitViewRect())
        assertEquals(DpOffset.Zero, contentOffset())
    }

    /**
     * Tests drag gestures that include both horizontal and vertical
     * components when interacting with a horizontal UIKit scroll view embedded in a vertical
     * Compose scroll container. Verifies proper gesture disambiguation and handling when:
     * 1. Drag gestures change direction mid-interaction
     * 2. Mixed horizontal and vertical movements occur simultaneously
     * 3. Drag gestures extend beyond the bounds of the UIKit view
     */
    @Test
    fun testHorizontalUIScrollViewInComposeVerticalScroll_MixedDrag() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiKitViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalScrollWithHorizontalUIKitScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                topContentHeight = 200.dp,
                uiKitScrollViewHeight = 200.dp,
                uiKitScrollViewRectInWindow = { uiKitViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        touchDown(DpOffset(screenSize.center.x, 250.dp))
            .dragBy(dx = -(50 + CUPERTINO_TOUCH_SLOP).dp, dy = -50.dp)
            .dragBy(dx = 20.dp, dy = -50.dp)
            .dragBy(dx = -70.dp, dy = 50.dp)
            .dragBy(dy = -150.dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        assertEquals(0 * density.density, state.value.toFloat())
        assertEquals(DpRect(DpOffset(x = 0.dp, y = 200.dp), DpSize(screenSize.width, 200.dp)), uiKitViewRect())
        assertEquals(DpOffset(x = 100.dp, y = 0.dp), contentOffset())
    }

    /**
     * Tests the resolution of ambiguous drag gestures between a horizontal UIKit scroll view
     * and a vertical Compose scroll container. Specifically:
     * 1. When drag starts with primarily vertical movement, Compose scroll takes precedence
     * 2. Small horizontal movements during a primarily vertical drag don't trigger UIKit scroll
     * 3. Direction disambiguation happens early in the gesture
     */
    @Test
    fun testHorizontalUIScrollViewInComposeVerticalScroll_VerticalAndSmallHorizontalDrag() = runUIKitInstrumentedTestWithInterop { overlay ->
        val state = ScrollState(0)
        var uiKitViewRect: () -> DpRect = { DpRectZero() }
        var contentOffset: () -> DpOffset = { DpOffset.Zero }

        setContent {
            VerticalScrollWithHorizontalUIKitScroll(
                placeUIKitViewAsOverlay = overlay,
                state = state,
                screenSize = screenSize,
                topContentHeight = 200.dp,
                uiKitScrollViewHeight = 200.dp,
                uiKitScrollViewRectInWindow = { uiKitViewRect = it },
                uiKitContentOffset = { contentOffset = it }
            )
        }

        findNodeWithTag("UIKit.UIScrollView")
            .touchDown()
            .dragBy(dx = -(5 + CUPERTINO_TOUCH_SLOP).dp, dy = -(50 + CUPERTINO_TOUCH_SLOP).dp)
            .dragBy(dx = -50.dp, dy = -50.dp)
            .also { delay(500) }
            .up()

        waitForIdle()

        // verify that only Compose scroll state changed but UIScrollView content offset stayed the same
        assertEquals(100 * density.density, state.value.toFloat())
        assertEquals(DpRect(DpOffset(x = 0.dp, y = 100.dp), DpSize(screenSize.width, 200.dp)), uiKitViewRect())
        assertEquals(DpOffset.Zero, contentOffset())
    }
}

@Composable
private fun VerticalUIKitScrollInsideVerticalScroll(
    placeUIKitViewAsOverlay: Boolean,
    state: ScrollState,
    screenSize: DpSize,
    density: Density,
    topContentHeight: Dp,
    uiKitScrollViewHeight: Dp,
    uiKitScrollViewRectInWindow: (() -> DpRect) -> Unit = {},
    uiKitContentOffset: (() -> DpOffset) -> Unit = { },
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topContentHeight)
                .background(Color.Red)
                .testTag("Top Box")
        )

        var scrollViewSize by mutableStateOf(DpSize.Zero)
        UIKitView(
            factory = {
                val scrollView = UIScrollView()
                scrollView.setContentSize(
                    CGSizeMake(scrollViewSize.width.value.toDouble(), 1000.0)
                )
                scrollView.backgroundColor = UIColor.lightGrayColor
                uiKitScrollViewRectInWindow({ scrollView.dpRectInWindow() })
                uiKitContentOffset({ scrollView.contentOffset.asDpOffset() })
                scrollView
            },
            update = { scrollView ->
                scrollView.setContentSize(
                    CGSizeMake(scrollViewSize.width.value.toDouble(), 1000.0)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("UIKit.UIScrollView")
                .height(uiKitScrollViewHeight)
                .onSizeChanged {
                scrollViewSize = with(density) {
                    DpSize(it.width.toDp(), it.height.toDp())
                }
            },
            properties = UIKitInteropProperties(placedAsOverlay = placeUIKitViewAsOverlay)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenSize.height)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun VerticalScrollWithHorizontalUIKitScroll(
    placeUIKitViewAsOverlay: Boolean,
    state: ScrollState,
    screenSize: DpSize,
    topContentHeight: Dp,
    uiKitScrollViewHeight: Dp,
    uiKitScrollViewContentWidth: Double = 5000.0,
    uiKitScrollViewRectInWindow: (() -> DpRect) -> Unit = {},
    uiKitContentOffset: (() -> DpOffset) -> Unit = { },
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
        if (topContentHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topContentHeight)
                    .background(Color.Red)
            )
        }

        UIKitView(
            factory = {
                val scrollView = UIScrollView()
                scrollView.setContentSize(CGSizeMake(uiKitScrollViewContentWidth, uiKitScrollViewHeight.value.toDouble()))
                scrollView.backgroundColor = UIColor.lightGrayColor
                uiKitScrollViewRectInWindow({ scrollView.dpRectInWindow() })
                uiKitContentOffset({ scrollView.contentOffset.asDpOffset() })
                scrollView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(uiKitScrollViewHeight)
                .testTag("UIKit.UIScrollView"),
            update = {},
            properties = UIKitInteropProperties(placedAsOverlay = placeUIKitViewAsOverlay)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenSize.height)
                .background(Color.White)
        )
    }
}

@Composable
private fun HorizontalScrollContent(
    itemSize: Dp,
    itemCount: Int = 20,
    lazyRowState: LazyListState,
    layoutDirection: LayoutDirection,
    onFirstBoxGloballyPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {}
) {
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyRow(modifier = Modifier.height(itemSize), state = lazyRowState) {
                items(itemCount) {
                    Box(
                        Modifier
                            .size(itemSize, itemSize)
                            .background(remember { Color(Random.nextInt()) })
                            .then(
                                if (it == 0) Modifier.onGloballyPositioned(onFirstBoxGloballyPositioned) else Modifier
                            )
                    ) {
                        Text("Text ${it}")
                    }
                }
            }
        }
    }
}

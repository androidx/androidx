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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.math.abs
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ScrollbarTest(
    private val orientation: Orientation,
    private val layoutDirection: LayoutDirection,
) {
    private val motionDurationScale =
        object : MotionDurationScale {
            override var scaleFactor: Float = 1f
        }

    @get:Rule
    val rule = createComposeRule(effectContext = motionDurationScale + StandardTestDispatcher())

    @After
    fun tearDown() {
        motionDurationScale.scaleFactor = 1f
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "orientation={0}, layoutDirection={1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(Orientation.Vertical, LayoutDirection.Ltr),
                arrayOf(Orientation.Vertical, LayoutDirection.Rtl),
                arrayOf(Orientation.Horizontal, LayoutDirection.Ltr),
                arrayOf(Orientation.Horizontal, LayoutDirection.Rtl),
            )
        }
    }

    @Test
    fun scrollbar_fadeAnimation() {
        val offsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = offsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                fadeDelayMillis = 100,
                fadeDurationMillis = 100,
                backgroundColor = TestContainerBackgroundColor,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        rule.mainClock.autoAdvance = false

        assertEquals(0, state.scrollOffset)

        // Trigger scroll
        rule.runOnIdle { offsetState.value = 10 }

        // Advance clock a bit to trigger the first draw
        rule.mainClock.advanceTimeBy(20)

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            val initialImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val initialColors =
                computeScrollbarTestColors(
                    initialImage.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )

            // Verify that the scrollbar is initially visible after scroll
            assertEquals(ScrollbarThumbColor, initialColors.thumbColor)
            assertEquals(ScrollbarTrackColor, initialColors.trackColor)

            // Advance clock past delay and animation duration
            rule.mainClock.advanceTimeBy(230)

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val actualColors =
                computeScrollbarTestColors(pixelMap, thicknessPx, viewportWidthPx, viewportHeightPx)

            // Verify that the scrollbar fades out
            assertEquals(TestContainerBackgroundColor, actualColors.thumbColor)
            assertEquals(TestContainerBackgroundColor, actualColors.trackColor)
        }
    }

    @Test
    fun scrollbar_fadeAnimation_whenSystemDisablesAnimations() {
        motionDurationScale.scaleFactor = 0f
        val offsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = offsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                fadeDelayMillis = 100,
                fadeDurationMillis = 100,
                backgroundColor = TestContainerBackgroundColor,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        rule.mainClock.autoAdvance = false

        assertEquals(0, state.scrollOffset)

        // Trigger scroll
        rule.runOnIdle { offsetState.value = 10 }

        // Advance clock a bit to trigger the first draw.
        // Since animations are disabled via motionDurationScale set to 0f, the fade out
        // animation itself would complete instantly, but the delay is handled via coroutine
        // delay() which is independent of animator duration scale.
        // So at 50ms (less than 100ms delay), the scrollbar should still be fully visible.
        rule.mainClock.advanceTimeBy(50)

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            val initialImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val initialColors =
                computeScrollbarTestColors(
                    initialImage.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )

            // Verify that the scrollbar is still visible during the delay
            assertEquals(ScrollbarThumbColor, initialColors.thumbColor)
            assertEquals(ScrollbarTrackColor, initialColors.trackColor)

            // Advance clock past the delay (100ms) by a safe margin (120ms, bringing total to
            // 170ms). This ensures that even if the coroutine launch was delayed by a few frames
            // due to layout passes, the delay has completed.
            rule.mainClock.advanceTimeBy(120)

            // Force the dispatcher to run the resumed coroutine so it calls animateTo()
            rule.mainClock.scheduler.runCurrent()

            // Since duration scale is 0f, the animateTo frame callback is scheduled and will
            // complete instantly in the next frame callback. We must advance the clock by one
            // frame to let the newly scheduled animateTo frame callback run.
            rule.mainClock.advanceTimeByFrame()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val actualColors =
                computeScrollbarTestColors(pixelMap, thicknessPx, viewportWidthPx, viewportHeightPx)

            // Verify that the scrollbar immediately disappears after the delay
            assertEquals(TestContainerBackgroundColor, actualColors.thumbColor)
            assertEquals(TestContainerBackgroundColor, actualColors.trackColor)
        }
    }

    @Test
    fun scrollbar_updatesOnScroll() {
        val offsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = offsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            ScrollbarTestContainer(
                state,
                orientation,
                layoutDirection,
                isFadeEnabled = false,
                thumbColor = ScrollbarThumbColor,
                trackColor = ScrollbarTrackColor,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            // Verify initial position (Start)
            val initialImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val initialColors =
                computeScrollbarTestColors(
                    initialImage.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                    thumbPosition = ThumbPosition.Start,
                )
            assertEquals(ScrollbarThumbColor, initialColors.thumbColor)

            // Scroll to the end
            rule.runOnIdle { offsetState.value = 100 }
            rule.waitForIdle()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val actualColors =
                computeScrollbarTestColors(
                    pixelMap,
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                    thumbPosition = ThumbPosition.End,
                )

            // Verify that the thumb position changes on scroll
            assertEquals(ScrollbarThumbColor, actualColors.thumbColor)
        }
    }

    @Test
    fun scrollbar_hidesWhenContentFitsViewport() {
        val viewportSizeState = mutableStateOf(100)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 100
                override val viewportSize: Int
                    get() = viewportSizeState.value
            }

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                backgroundColor = TestContainerBackgroundColor,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            // Initially viewportSize = 100, contentSize = 100 -> Hidden
            val imageBitmap1 = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val colors1 =
                computeScrollbarTestColors(
                    imageBitmap1.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(TestContainerBackgroundColor, colors1.thumbColor)
            assertEquals(TestContainerBackgroundColor, colors1.trackColor)

            // Change viewportSize = 50, contentSize = 100 -> Visible
            rule.runOnIdle { viewportSizeState.value = 50 }
            rule.waitForIdle()

            val imageBitmap2 = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val colors2 =
                computeScrollbarTestColors(
                    imageBitmap2.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(ScrollbarThumbColor, colors2.thumbColor)
            assertEquals(ScrollbarTrackColor, colors2.trackColor)

            // Change viewportSize = 150, contentSize = 100 -> Hidden
            rule.runOnIdle { viewportSizeState.value = 150 }
            rule.waitForIdle()

            val imageBitmap3 = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val colors3 =
                computeScrollbarTestColors(
                    imageBitmap3.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(TestContainerBackgroundColor, colors3.thumbColor)
            assertEquals(TestContainerBackgroundColor, colors3.trackColor)
        }
    }

    @Test
    fun scrollbar_coercesToMinThumbLength() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 1000
                override val viewportSize: Int = 100
            }

        val minLength = 30.dp

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                thumbMinLength = minLength,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val minLengthPx = minLength.roundToPx()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val thumbBounds = findColorBounds(pixelMap, ScrollbarThumbColor)

            val thumbLength =
                if (orientation == Orientation.Vertical) {
                    thumbBounds.height
                } else {
                    thumbBounds.width
                }

            assertEquals(minLengthPx.toFloat(), thumbLength, 1f)
        }
    }

    @Test
    fun scrollbar_coercesToMaxThumbLength() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 110
                override val viewportSize: Int = 100
            }

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                thumbMaxLengthFraction = 0.5f,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val thumbBounds = findColorBounds(pixelMap, ScrollbarThumbColor)

            val thumbLength =
                if (orientation == Orientation.Vertical) {
                    thumbBounds.height
                } else {
                    thumbBounds.width
                }

            val trackLengthPx =
                if (orientation == Orientation.Vertical) viewportHeightPx else viewportWidthPx
            val expectedMaxLengthPx = trackLengthPx * 0.5f

            assertEquals(expectedMaxLengthPx, thumbLength, 1f)
        }
    }

    @Test
    fun scrollbar_hidesWhenTrackTooSmallForMinThumbLength() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        // Set min length larger than viewport (container size is 100.dp)
        val minLength = 150.dp

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                thumbMinLength = minLength,
                backgroundColor = TestContainerBackgroundColor,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val colors =
                computeScrollbarTestColors(
                    imageBitmap.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )

            // Verify that the scrollbar is hidden
            assertEquals(TestContainerBackgroundColor, colors.thumbColor)
            assertEquals(TestContainerBackgroundColor, colors.trackColor)
        }
    }

    @Test
    fun scrollbar_respectsTrackInsets() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        val mainAxisInset = 10.dp
        val crossAxisInset = 5.dp
        val thickness = 10.dp

        rule.setContent {
            ScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                mainAxisTrackInset = mainAxisInset,
                crossAxisTrackInset = crossAxisInset,
                thickness = thickness,
                // Keep thumb transparent to measure full track bounds without overlap
                thumbColor = Color.Transparent,
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
        val pixelMap = imageBitmap.toPixelMap()

        // Measure only the track bounds. Since the thumb is transparent, the track is visible
        // end-to-end.
        val trackBounds = findColorBounds(pixelMap, ScrollbarTrackColor)
        assertNotEquals(Rect.Zero, trackBounds)

        with(rule.density) {
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()
            val mainAxisInsetPx = mainAxisInset.roundToPx()
            val crossAxisInsetPx = crossAxisInset.roundToPx()
            val thicknessPx = thickness.roundToPx()

            val expectedRect =
                if (orientation == Orientation.Vertical) {
                    val left =
                        if (layoutDirection == LayoutDirection.Ltr) {
                            viewportWidthPx - crossAxisInsetPx - thicknessPx
                        } else {
                            crossAxisInsetPx
                        }
                    Rect(
                        left.toFloat(),
                        mainAxisInsetPx.toFloat(),
                        (left + thicknessPx).toFloat(),
                        (viewportHeightPx - mainAxisInsetPx).toFloat(),
                    )
                } else {
                    Rect(
                        mainAxisInsetPx.toFloat(),
                        (viewportHeightPx - crossAxisInsetPx - thicknessPx).toFloat(),
                        (viewportWidthPx - mainAxisInsetPx).toFloat(),
                        (viewportHeightPx - crossAxisInsetPx).toFloat(),
                    )
                }

            assertEquals(expectedRect.left, trackBounds.left, 1f)
            assertEquals(expectedRect.top, trackBounds.top, 1f)
            assertEquals(expectedRect.right, trackBounds.right, 1f)
            assertEquals(expectedRect.bottom, trackBounds.bottom, 1f)
        }
    }

    @Test
    fun scrollbarModifier_inspectableProperties() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            val modifier =
                Modifier.scrollbar(
                    state = state,
                    orientation = orientation,
                    thumbColor = Color.Red,
                    trackColor = Color.Blue,
                    thickness = 5.dp,
                    thumbMinLength = 10.dp,
                    thumbMaxLengthFraction = 0.5f,
                    isFadeEnabled = false,
                    fadeDurationMillis = 100,
                    fadeDelayMillis = 200,
                    mainAxisTrackInset = 4.dp,
                    crossAxisTrackInset = 2.dp,
                ) as? InspectableValue

            assertNotNull(modifier)
            assertEquals("scrollbar", modifier?.nameFallback)

            val properties = modifier?.inspectableElements?.associate { it.name to it.value }
            assertNotNull(properties)
            assertEquals(state, properties?.get("state"))
            assertEquals(orientation, properties?.get("orientation"))
            assertEquals(false, properties?.get("isFadeEnabled"))
            assertEquals(Color.Red, properties?.get("thumbColor"))
            assertEquals(Color.Blue, properties?.get("trackColor"))
            assertEquals(5.dp, properties?.get("thickness"))
            assertEquals(10.dp, properties?.get("thumbMinLength"))
            assertEquals(0.5f, properties?.get("thumbMaxLengthFraction"))
            assertEquals(100, properties?.get("fadeDurationMillis"))
            assertEquals(200, properties?.get("fadeDelayMillis"))
            assertEquals(4.dp, properties?.get("mainAxisTrackInset"))
            assertEquals(2.dp, properties?.get("crossAxisTrackInset"))
        }
    }

    @Test
    fun scrollbarModifier_equals() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            val modifier1 =
                Modifier.scrollbar(state = state, orientation = orientation, thickness = 10.dp)
            val modifier2 =
                Modifier.scrollbar(state = state, orientation = orientation, thickness = 10.dp)
            val modifier3 =
                Modifier.scrollbar(state = state, orientation = orientation, thickness = 5.dp)

            assertEquals(modifier1, modifier2)
            assertEquals(modifier1.hashCode(), modifier2.hashCode())
            assertNotEquals(modifier1, modifier3)
        }
    }

    /**
     * Finds the bounding box of all pixels in a [PixelMap] that match the [targetColor] within the
     * given [tolerance].
     */
    private fun findColorBounds(
        pixelMap: PixelMap,
        targetColor: Color,
        tolerance: Float = 0.9f,
    ): Rect {
        var minX = pixelMap.width
        var minY = pixelMap.height
        var maxX = -1
        var maxY = -1

        for (x in 0 until pixelMap.width) {
            for (y in 0 until pixelMap.height) {
                val color = pixelMap[x, y]
                val matches =
                    abs(color.red - targetColor.red) <= tolerance &&
                        abs(color.green - targetColor.green) <= tolerance &&
                        abs(color.blue - targetColor.blue) <= tolerance

                if (matches) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX == -1 || maxY == -1) return Rect.Zero
        return Rect(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat())
    }

    /**
     * Extracts colors from the scrollbar track and thumb by mapping logical positions to physical
     * pixel coordinates based on orientation, layout direction, and insets.
     */
    private fun computeScrollbarTestColors(
        pixelMap: PixelMap,
        thicknessPx: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        mainAxisInsetPx: Int = 0,
        crossAxisInsetPx: Int = 0,
        thumbPosition: ThumbPosition = ThumbPosition.Start,
    ): ScrollbarTestColors {
        // Calculate cross-axis position (X for Vertical, Y for Horizontal)
        val crossAxisOffset = crossAxisInsetPx + thicknessPx / 2
        val crossPos =
            if (orientation == Orientation.Vertical) {
                if (layoutDirection == LayoutDirection.Ltr) viewportWidthPx - crossAxisOffset
                else crossAxisOffset
            } else {
                viewportHeightPx - crossAxisOffset
            }

        // Calculate main-axis bounds and sampling points
        val mainAxisLength =
            if (orientation == Orientation.Vertical) viewportHeightPx else viewportWidthPx
        val mainAxisAvailable = mainAxisLength - 2 * mainAxisInsetPx
        val firstQuarter = mainAxisInsetPx + mainAxisAvailable / 4
        val thirdQuarter = mainAxisInsetPx + mainAxisAvailable * 3 / 4

        // Determine if thumb is at the "start" or "end" of the physical axis.
        // For Horizontal RTL, the visual "Start" is physically at the "End" (right side).
        val isReverse =
            orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl
        val thumbIsFirstQuarter = (thumbPosition == ThumbPosition.Start) != isReverse

        val thumbMain = if (thumbIsFirstQuarter) firstQuarter else thirdQuarter
        val trackMain = if (thumbIsFirstQuarter) thirdQuarter else firstQuarter

        // Sample colors from the pixel map, swapping X/Y based on orientation
        val thumbColor =
            if (orientation == Orientation.Vertical) {
                pixelMap[crossPos, thumbMain]
            } else {
                pixelMap[thumbMain, crossPos]
            }
        val trackColor =
            if (orientation == Orientation.Vertical) {
                pixelMap[crossPos, trackMain]
            } else {
                pixelMap[trackMain, crossPos]
            }

        return ScrollbarTestColors(thumbColor, trackColor)
    }

    private enum class ThumbPosition {
        Start,
        End,
    }

    private data class ScrollbarTestColors(val thumbColor: Color, val trackColor: Color)
}

@Composable
private fun ScrollbarTestContainer(
    state: ScrollIndicatorState,
    orientation: Orientation,
    layoutDirection: LayoutDirection,
    modifier: Modifier = Modifier,
    thumbColor: Color = ScrollbarThumbColor,
    trackColor: Color = ScrollbarTrackColor,
    thickness: Dp = ScrollbarThickness,
    thumbMinLength: Dp = 24.dp,
    thumbMaxLengthFraction: Float = ScrollbarDefaults.ThumbMaxLengthFraction,
    isFadeEnabled: Boolean = true,
    mainAxisTrackInset: Dp = 0.dp,
    crossAxisTrackInset: Dp = 0.dp,
    fadeDurationMillis: Int = 250,
    fadeDelayMillis: Int = 400,
    backgroundColor: Color = Color.Transparent,
) {
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier
                .size(ScrollableContainerSize)
                .background(backgroundColor)
                .scrollbar(
                    state = state,
                    orientation = orientation,
                    thumbColor = thumbColor,
                    trackColor = trackColor,
                    thickness = thickness,
                    thumbMinLength = thumbMinLength,
                    thumbMaxLengthFraction = thumbMaxLengthFraction,
                    isFadeEnabled = isFadeEnabled,
                    mainAxisTrackInset = mainAxisTrackInset,
                    crossAxisTrackInset = crossAxisTrackInset,
                    fadeDurationMillis = fadeDurationMillis,
                    fadeDelayMillis = fadeDelayMillis,
                )
        )
    }
}

private const val ScrollableContainerTestTag = "scrollable_container"
private val ScrollableContainerSize = 100.dp
private val ScrollbarThickness = 10.dp
private val ScrollbarThumbColor = Color.Red
private val ScrollbarTrackColor = Color.Blue
private val TestContainerBackgroundColor = Color.Green

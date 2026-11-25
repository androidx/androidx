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
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Orientation- and layout-direction-independent tests for [Modifier.interactiveScrollbar]. */
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class InteractiveScrollbarTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun interactiveScrollbarModifier_inspectableProperties() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        val onThumbDrag: (Float) -> Unit = {}
        val onTrackPress: (Float) -> Unit = {}

        rule.setContent {
            val colors =
                InteractiveScrollbarDefaults.colors(thumbColor = Color.Red, trackColor = Color.Blue)
            val modifier =
                Modifier.interactiveScrollbar(
                    state = state,
                    orientation = Orientation.Vertical,
                    onThumbDrag = onThumbDrag,
                    onTrackPress = onTrackPress,
                    colors = colors,
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
            assertEquals("interactiveScrollbar", modifier?.nameFallback)

            val properties = modifier?.inspectableElements?.associate { it.name to it.value }
            assertNotNull(properties)
            assertEquals(state, properties?.get("state"))
            assertEquals(Orientation.Vertical, properties?.get("orientation"))
            assertEquals(onThumbDrag, properties?.get("onThumbDrag"))
            assertEquals(onTrackPress, properties?.get("onTrackPress"))
            assertEquals(null, properties?.get("thumbInteractionSource"))
            assertEquals(false, properties?.get("isFadeEnabled"))
            assertEquals(colors, properties?.get("colors"))
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
    fun interactiveScrollbarModifier_equals() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        val onThumbDrag: (Float) -> Unit = {}
        val onTrackPress: (Float) -> Unit = {}

        rule.setContent {
            val modifier1 =
                Modifier.interactiveScrollbar(
                    state = state,
                    orientation = Orientation.Vertical,
                    onThumbDrag = onThumbDrag,
                    onTrackPress = onTrackPress,
                    thickness = 10.dp,
                )
            val modifier2 =
                Modifier.interactiveScrollbar(
                    state = state,
                    orientation = Orientation.Vertical,
                    onThumbDrag = onThumbDrag,
                    onTrackPress = onTrackPress,
                    thickness = 10.dp,
                )
            val modifier3 =
                Modifier.interactiveScrollbar(
                    state = state,
                    orientation = Orientation.Vertical,
                    onThumbDrag = onThumbDrag,
                    onTrackPress = onTrackPress,
                    thickness = 5.dp,
                )

            assertEquals(modifier1, modifier2)
            assertEquals(modifier1.hashCode(), modifier2.hashCode())
            assertNotEquals(modifier1, modifier3)
        }
    }

    @Test
    fun interactiveScrollbar_isNotFocusableOrAccessible() {
        // Contract test: per M3 UX guidance, the scrollbar modifier itself must not add focus
        // targets or accessibility semantics/actions to the scrollable container.
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = Orientation.Vertical,
                layoutDirection = LayoutDirection.Ltr,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        val config = rule.onNodeWithTag(ScrollableContainerTestTag).fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsProperties.Focused))
        assertFalse(config.contains(SemanticsProperties.HorizontalScrollAxisRange))
        assertFalse(config.contains(SemanticsProperties.VerticalScrollAxisRange))
        assertFalse(config.contains(SemanticsActions.RequestFocus))
        assertFalse(config.contains(SemanticsActions.ScrollBy))
        assertFalse(config.contains(SemanticsActions.ScrollToIndex))
    }

    @Test
    fun interactiveScrollbar_maxThumbLengthSmallerThanMinThumbLength_usesMinThumbLength() {
        // Container of 200.dp with 0.dp insets gives a 200.dp track, so maxThumbLength
        // (200.dp * 0.5f = 100.dp) is smaller than thumbMinLength (120.dp); min should win.
        val containerSize = 200.dp
        val minLength = 120.dp
        val maxFractionState = mutableStateOf(0.5f)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 105
                override val viewportSize: Int = 100
            }

        rule.setContent {
            Box(
                Modifier.testTag(ScrollableContainerTestTag)
                    .size(containerSize)
                    .background(TestContainerBackgroundColor)
                    .interactiveScrollbar(
                        state = state,
                        orientation = Orientation.Vertical,
                        onThumbDrag = {},
                        onTrackPress = {},
                        colors =
                            InteractiveScrollbarDefaults.colors(
                                thumbColor = ScrollbarThumbColor,
                                trackColor = Color.Transparent,
                            ),
                        thumbMinLength = minLength,
                        thumbMaxLengthFraction = maxFractionState.value,
                        mainAxisTrackInset = 0.dp,
                        crossAxisTrackInset = 0.dp,
                        isFadeEnabled = false,
                    )
            )
        }

        val expectedLengthPx = with(rule.density) { minLength.toPx() }
        val firstBounds =
            findColorBounds(
                rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                ScrollbarThumbColor,
            )
        assertEquals(expectedLengthPx, firstBounds.height, 2f)

        // Also verify thumbMaxLengthFraction = 0f clamps to thumbMinLength without crashing.
        rule.runOnIdle { maxFractionState.value = 0f }
        val zeroFractionBounds =
            findColorBounds(
                rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                ScrollbarThumbColor,
            )
        assertEquals(expectedLengthPx, zeroFractionBounds.height, 2f)
    }

    @Test
    fun interactiveScrollbar_externalInteractionSource_drivesThumbColors() {
        val thumbInteractionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            Box(
                Modifier.size(ScrollableContainerSize)
                    .background(Color.White)
                    .testTag(ScrollableContainerTestTag)
                    .interactiveScrollbar(
                        state = state,
                        orientation = Orientation.Vertical,
                        onThumbDrag = {},
                        onTrackPress = {},
                        colors =
                            InteractiveScrollbarDefaults.colors(
                                thumbColor = Color.Red,
                                hoveredThumbColor = Color.Yellow,
                                pressedThumbColor = Color.Green,
                                draggedThumbColor = Color.Blue,
                                trackColor = Color.Transparent,
                            ),
                        thickness = ScrollbarThickness,
                        isFadeEnabled = false,
                        thumbInteractionSource = thumbInteractionSource,
                    )
            )
        }

        with(rule.density) {
            fun captureThumbColor(): Color =
                computeScrollbarTestColors(
                        pixelMap =
                            rule
                                .onNodeWithTag(ScrollableContainerTestTag)
                                .captureToImage()
                                .toPixelMap(),
                        orientation = Orientation.Vertical,
                        layoutDirection = LayoutDirection.Ltr,
                        thicknessPx = ScrollbarThickness.roundToPx(),
                        viewportWidthPx = ScrollableContainerSize.roundToPx(),
                        viewportHeightPx = ScrollableContainerSize.roundToPx(),
                        mainAxisInsetPx = TestMainAxisTrackInset.roundToPx(),
                        crossAxisInsetPx = TestCrossAxisTrackInset.roundToPx(),
                    )
                    .thumbColor

            assertEquals(Color.Red, captureThumbColor())

            val hover = HoverInteraction.Enter()
            rule.runOnIdle { thumbInteractionSource.tryEmit(hover) }
            assertEquals(Color.Yellow, captureThumbColor())

            val press = PressInteraction.Press(Offset.Zero)
            rule.runOnIdle { thumbInteractionSource.tryEmit(press) }
            assertEquals(Color.Green, captureThumbColor())

            val drag = DragInteraction.Start()
            rule.runOnIdle { thumbInteractionSource.tryEmit(drag) }
            assertEquals(Color.Blue, captureThumbColor())

            rule.runOnIdle { thumbInteractionSource.tryEmit(DragInteraction.Stop(drag)) }
            assertEquals(Color.Green, captureThumbColor())

            rule.runOnIdle { thumbInteractionSource.tryEmit(PressInteraction.Release(press)) }
            assertEquals(Color.Yellow, captureThumbColor())

            rule.runOnIdle { thumbInteractionSource.tryEmit(HoverInteraction.Exit(hover)) }
            assertEquals(Color.Red, captureThumbColor())
        }
    }

    @Test
    fun interactiveScrollbar_externalInteractionSource_doesNotWakeFadedScrollbar() {
        val thumbInteractionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = Orientation.Vertical,
                layoutDirection = LayoutDirection.Ltr,
                thumbInteractionSource = thumbInteractionSource,
                isFadeEnabled = true,
                backgroundColor = TestContainerBackgroundColor,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        rule.runOnIdle {
            thumbInteractionSource.tryEmit(HoverInteraction.Enter())
            thumbInteractionSource.tryEmit(PressInteraction.Press(Offset.Zero))
        }

        with(rule.density) {
            val colors =
                computeScrollbarTestColors(
                    pixelMap =
                        rule
                            .onNodeWithTag(ScrollableContainerTestTag)
                            .captureToImage()
                            .toPixelMap(),
                    orientation = Orientation.Vertical,
                    layoutDirection = LayoutDirection.Ltr,
                    thicknessPx = ScrollbarThickness.roundToPx(),
                    viewportWidthPx = ScrollableContainerSize.roundToPx(),
                    viewportHeightPx = ScrollableContainerSize.roundToPx(),
                    mainAxisInsetPx = TestMainAxisTrackInset.roundToPx(),
                    crossAxisInsetPx = TestCrossAxisTrackInset.roundToPx(),
                )
            assertEquals(TestContainerBackgroundColor, colors.thumbColor)
        }
    }

    @Test
    fun interactiveScrollbar_secondaryMouseButton_ignored() {
        var dragCalled = false
        var pressCalled = false
        val interactions = mutableListOf<Interaction>()
        val thumbInteractionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(thumbInteractionSource) {
                thumbInteractionSource.interactions.collect { interactions.add(it) }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = Orientation.Vertical,
                layoutDirection = LayoutDirection.Ltr,
                thumbInteractionSource = thumbInteractionSource,
                onThumbDrag = { dragCalled = true },
                onTrackPress = { pressCalled = true },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val containerPx = ScrollableContainerSize.toPx()
            val thicknessPx = ScrollbarThickness.toPx()
            val mainInsetPx = TestMainAxisTrackInset.toPx()
            val crossInsetPx = TestCrossAxisTrackInset.toPx()
            val trackLengthPx = containerPx - 2 * mainInsetPx
            val crossCenterPx = containerPx - crossInsetPx - thicknessPx / 2f
            val thumbCenter = Offset(crossCenterPx, mainInsetPx + trackLengthPx * 0.25f)
            val trackCenter = Offset(crossCenterPx, mainInsetPx + trackLengthPx * 0.75f)

            try {
                // Secondary and tertiary clicks on the track should not trigger onTrackPress.
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    moveTo(trackCenter)
                    press(MouseButton.Secondary)
                    release(MouseButton.Secondary)
                    press(MouseButton.Tertiary)
                    release(MouseButton.Tertiary)
                }
                // Secondary drag on the thumb should not trigger onThumbDrag or press/drag
                // interactions.
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    moveTo(thumbCenter)
                    press(MouseButton.Secondary)
                    moveBy(Offset(0f, 20f))
                    release(MouseButton.Secondary)
                }
                rule.waitForIdle()

                assertFalse(
                    "Expected secondary/tertiary clicks not to trigger track press",
                    pressCalled,
                )
                assertFalse("Expected secondary drag not to trigger thumb drag", dragCalled)
                assertTrue(
                    "Expected no press/drag interactions from secondary mouse button",
                    interactions.none { it is PressInteraction || it is DragInteraction },
                )
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on this device/runner
            }
        }
    }
}

internal fun findColorBounds(
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

internal fun computeScrollbarTestColors(
    pixelMap: PixelMap,
    orientation: Orientation,
    layoutDirection: LayoutDirection,
    thicknessPx: Int,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    mainAxisInsetPx: Int,
    crossAxisInsetPx: Int,
    thumbPosition: ThumbPosition = ThumbPosition.Start,
): ScrollbarTestColors {
    val crossAxisOffset = crossAxisInsetPx + thicknessPx / 2
    val crossPos =
        if (orientation == Orientation.Vertical) {
            if (layoutDirection == LayoutDirection.Ltr) viewportWidthPx - crossAxisOffset
            else crossAxisOffset
        } else {
            viewportHeightPx - crossAxisOffset
        }

    val mainAxisLength =
        if (orientation == Orientation.Vertical) viewportHeightPx else viewportWidthPx
    val mainAxisAvailable = mainAxisLength - 2 * mainAxisInsetPx
    val firstQuarter = mainAxisInsetPx + mainAxisAvailable / 4
    val thirdQuarter = mainAxisInsetPx + mainAxisAvailable * 3 / 4

    val isReverse = orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl
    val thumbIsFirstQuarter = (thumbPosition == ThumbPosition.Start) != isReverse

    val thumbMain = if (thumbIsFirstQuarter) firstQuarter else thirdQuarter
    val trackMain = if (thumbIsFirstQuarter) thirdQuarter else firstQuarter

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

internal enum class ThumbPosition {
    Start,
    End,
}

internal data class ScrollbarTestColors(val thumbColor: Color, val trackColor: Color)

@Composable
internal fun InteractiveScrollbarTestContainer(
    state: ScrollIndicatorState,
    orientation: Orientation,
    layoutDirection: LayoutDirection,
    modifier: Modifier = Modifier,
    containerSize: Dp = ScrollableContainerSize,
    thumbInteractionSource: MutableInteractionSource? = null,
    thickness: Dp = ScrollbarThickness,
    thumbMinLength: Dp = 24.dp,
    thumbMaxLengthFraction: Float = InteractiveScrollbarDefaults.ThumbMaxLengthFraction,
    thumbColor: Color = ScrollbarThumbColor,
    trackColor: Color = ScrollbarTrackColor,
    isFadeEnabled: Boolean = false,
    mainAxisTrackInset: Dp = TestMainAxisTrackInset,
    crossAxisTrackInset: Dp = TestCrossAxisTrackInset,
    fadeDurationMillis: Int = 250,
    fadeDelayMillis: Int = 400,
    backgroundColor: Color = Color.Transparent,
    onThumbDrag: (positionFraction: Float) -> Unit,
    onTrackPress: (positionFraction: Float) -> Unit,
) {
    val colors =
        InteractiveScrollbarDefaults.colors(
            thumbColor = thumbColor.copy(alpha = InteractiveScrollbarDefaults.ThumbOpacity),
            hoveredThumbColor =
                thumbColor.copy(alpha = InteractiveScrollbarDefaults.HoveredThumbOpacity),
            pressedThumbColor =
                thumbColor.copy(alpha = InteractiveScrollbarDefaults.PressedThumbOpacity),
            draggedThumbColor =
                thumbColor.copy(alpha = InteractiveScrollbarDefaults.PressedThumbOpacity),
            trackColor = trackColor,
        )
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier
                .size(containerSize)
                .background(backgroundColor)
                .interactiveScrollbar(
                    state = state,
                    orientation = orientation,
                    onThumbDrag = onThumbDrag,
                    onTrackPress = onTrackPress,
                    thumbInteractionSource = thumbInteractionSource,
                    colors = colors,
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
private val TestMainAxisTrackInset = InteractiveScrollbarDefaults.MainAxisTrackInset
private val TestCrossAxisTrackInset = InteractiveScrollbarDefaults.CrossAxisTrackInset
private val ScrollbarThumbColor = Color.Red
private val ScrollbarTrackColor = Color.Blue
private val TestContainerBackgroundColor = Color.Green

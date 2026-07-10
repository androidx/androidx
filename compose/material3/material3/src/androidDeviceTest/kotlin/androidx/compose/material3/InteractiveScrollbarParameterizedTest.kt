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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class InteractiveScrollbarParameterizedTest(
    private val orientation: Orientation,
    private val layoutDirection: LayoutDirection,
) {
    private val motionDurationScale =
        object : MotionDurationScale {
            override var scaleFactor: Float = 1f
        }

    @get:Rule val rule = createComposeRule(ComposeUiTestConfig(motionDurationScale))

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
    fun interactiveScrollbar_dragThumb_touchInput() {
        var latestFraction = 0f
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { positionFraction -> latestFraction = positionFraction },
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val dragAmount = 20.dp.toPx()
            val endOffset =
                if (orientation == Orientation.Vertical) {
                    Offset(thumbCenter.x, thumbCenter.y + dragAmount)
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(thumbCenter.x - dragAmount, thumbCenter.y)
                } else {
                    Offset(thumbCenter.x + dragAmount, thumbCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                swipe(start = thumbCenter, end = endOffset, durationMillis = 100)
            }

            val trackLength = (ScrollableContainerSize - TestMainAxisTrackInset * 2f).toPx()
            val thumbLength = (state.viewportSize.toFloat() / state.contentSize) * trackLength
            val thumbTravelRange = trackLength - thumbLength
            val expectedFraction = dragAmount / thumbTravelRange

            assertNotEquals(0f, latestFraction)
            assertEquals(expectedFraction, latestFraction, 0.05f)
        }
    }

    @Test
    fun interactiveScrollbar_halfSizedTrack_emitsCorrectFraction() {
        var latestFraction = 0f
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        val halfTrackSize = ScrollableContainerSize / 2
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { positionFraction -> latestFraction = positionFraction },
                onTrackPress = {},
                modifier = Modifier.size(halfTrackSize).testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackSizePx = (halfTrackSize - TestMainAxisTrackInset * 2f).toPx()
            val thumbLengthPx = trackSizePx * (state.viewportSize.toFloat() / state.contentSize)
            val thumbCenter = getThumbCenterOffset(state, containerSize = halfTrackSize)

            val thumbTravelRangePx = trackSizePx - thumbLengthPx
            val dragAmountPx = thumbTravelRangePx / 2f
            val endOffset =
                if (orientation == Orientation.Vertical) {
                    Offset(thumbCenter.x, thumbCenter.y + dragAmountPx)
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(thumbCenter.x - dragAmountPx, thumbCenter.y)
                } else {
                    Offset(thumbCenter.x + dragAmountPx, thumbCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                swipe(start = thumbCenter, end = endOffset, durationMillis = 100)
            }

            val expectedFraction = dragAmountPx / thumbTravelRangePx
            assertNotEquals(0f, latestFraction)
            assertEquals(expectedFraction, latestFraction, 0.05f)
        }
    }

    @Test
    fun interactiveScrollbar_dragThumb_mouseInput() {
        var latestFraction = 0f
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { positionFraction -> latestFraction = positionFraction },
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val dragAmount = 20.dp.toPx()
            val endOffset =
                if (orientation == Orientation.Vertical) {
                    Offset(thumbCenter.x, thumbCenter.y + dragAmount)
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(thumbCenter.x - dragAmount, thumbCenter.y)
                } else {
                    Offset(thumbCenter.x + dragAmount, thumbCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                moveTo(thumbCenter)
                press()
                moveTo(endOffset)
                release()
            }

            val trackLength = (ScrollableContainerSize - TestMainAxisTrackInset * 2f).toPx()
            val thumbLength = (state.viewportSize.toFloat() / state.contentSize) * trackLength
            val thumbTravelRange = trackLength - thumbLength
            val expectedFraction = dragAmount / thumbTravelRange

            assertNotEquals(0f, latestFraction)
            assertEquals(expectedFraction, latestFraction, 0.05f)
        }
    }

    @Test
    fun interactiveScrollbar_trackPressDelegatesToThumbDrag_touchInput() {
        val trackPressOffsets = mutableListOf<Float>()
        val dragDeltas = mutableListOf<Float>()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragDeltas.add(it) },
                onTrackPress = { offset ->
                    trackPressOffsets.add(offset)
                    scrollOffsetState.value =
                        (offset * (state.contentSize - state.viewportSize)).toInt()
                },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            val scrubEnd =
                if (orientation == Orientation.Vertical) {
                    Offset(trackCenter.x, trackCenter.y + 10.dp.toPx())
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(trackCenter.x - 10.dp.toPx(), trackCenter.y)
                } else {
                    Offset(trackCenter.x + 10.dp.toPx(), trackCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveTo(scrubEnd)
                up()
            }

            assertEquals(1, trackPressOffsets.size)
            assertTrue("Expected drag events after track press", dragDeltas.isNotEmpty())
        }
    }

    @Test
    fun interactiveScrollbar_trackPressDelegatesToThumbDrag_mouseInput() {
        val trackPressOffsets = mutableListOf<Float>()
        val dragDeltas = mutableListOf<Float>()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragDeltas.add(it) },
                onTrackPress = { offset ->
                    trackPressOffsets.add(offset)
                    scrollOffsetState.value =
                        (offset * (state.contentSize - state.viewportSize)).toInt()
                },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            val scrubEnd =
                if (orientation == Orientation.Vertical) {
                    Offset(trackCenter.x, trackCenter.y + 10.dp.toPx())
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(trackCenter.x - 10.dp.toPx(), trackCenter.y)
                } else {
                    Offset(trackCenter.x + 10.dp.toPx(), trackCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                moveTo(trackCenter)
                press()
                moveTo(scrubEnd)
                release()
            }

            assertEquals(1, trackPressOffsets.size)
            assertTrue("Expected drag events after track press", dragDeltas.isNotEmpty())
        }
    }

    // When onTrackPress scrolls by a fixed amount (e.g., page jump) rather than moving the thumb
    // under the pointer, subsequent pointer movement should not trigger onThumbDrag.
    @Test
    fun interactiveScrollbar_trackPressAndDragWhenThumbNotUnderPointer_doesNotTriggerThumbDrag_touchInput() {
        val trackPressOffsets = mutableListOf<Float>()
        val dragDeltas = mutableListOf<Float>()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 1000
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragDeltas.add(it) },
                onTrackPress = { offset ->
                    trackPressOffsets.add(offset)
                    // Simulate a 1-page jump (100px out of 900px maxScrollOffset), which does not
                    // reach trackCenter (~550px).
                    scrollOffsetState.value = state.viewportSize
                },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            val scrubEnd =
                if (orientation == Orientation.Vertical) {
                    Offset(trackCenter.x, trackCenter.y + 10.dp.toPx())
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(trackCenter.x - 10.dp.toPx(), trackCenter.y)
                } else {
                    Offset(trackCenter.x + 10.dp.toPx(), trackCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveTo(scrubEnd)
                up()
            }

            assertEquals(1, trackPressOffsets.size)
            assertTrue("Expected no thumb drag events after track press", dragDeltas.isEmpty())
        }
    }

    // When onTrackPress scrolls by a fixed amount (e.g., page jump) rather than moving the thumb
    // under the pointer, subsequent pointer movement should not trigger onThumbDrag.
    @Test
    fun interactiveScrollbar_trackPressAndDragWhenThumbNotUnderPointer_doesNotTriggerThumbDrag_mouseInput() {
        val trackPressOffsets = mutableListOf<Float>()
        val dragDeltas = mutableListOf<Float>()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 1000
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragDeltas.add(it) },
                onTrackPress = { offset ->
                    trackPressOffsets.add(offset)
                    // Simulate a 1-page jump (100px out of 900px maxScrollOffset), which does not
                    // reach trackCenter (~550px).
                    scrollOffsetState.value = state.viewportSize
                },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            val scrubEnd =
                if (orientation == Orientation.Vertical) {
                    Offset(trackCenter.x, trackCenter.y + 10.dp.toPx())
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(trackCenter.x - 10.dp.toPx(), trackCenter.y)
                } else {
                    Offset(trackCenter.x + 10.dp.toPx(), trackCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                moveTo(trackCenter)
                press()
                moveTo(scrubEnd)
                release()
            }

            assertEquals(1, trackPressOffsets.size)
            assertTrue("Expected no thumb drag events after track press", dragDeltas.isEmpty())
        }
    }

    @Test
    fun interactiveScrollbar_thumbPressDoesNotTriggerScroll() {
        val trackPressOffsets = mutableListOf<Float>()
        val dragDeltas = mutableListOf<Float>()
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
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = thumbInteractionSource,
                onThumbDrag = { dragDeltas.add(it) },
                onTrackPress = { offset -> trackPressOffsets.add(offset) },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                up()
            }
            rule.waitForIdle()

            assertEquals(0, trackPressOffsets.size)
            assertEquals(0, dragDeltas.size)
            assertEquals(2, interactions.size)
            assertTrue(interactions[0] is PressInteraction.Press)
            assertTrue(interactions[1] is PressInteraction.Release)
        }
    }

    @Test
    fun interactiveScrollbar_hidden_ignoresTouch() {
        var dragCalled = false
        var pressCalled = false
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 200 // Needs to not fit viewport to be visible
                override val viewportSize: Int = 100
            }

        rule.mainClock.autoAdvance = false

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = true,
                fadeDelayMillis = 100,
                fadeDurationMillis = 100,
                backgroundColor = TestContainerBackgroundColor,
                onThumbDrag = { dragCalled = true },
                onTrackPress = { pressCalled = true },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            // Trigger a scroll so the scrollbar becomes visible first.
            rule.runOnIdle { scrollOffsetState.value = 50 }
            rule.mainClock.advanceTimeBy(32L)
            rule.mainClock.scheduler.runCurrent()

            val visibleColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertNotEquals(TestContainerBackgroundColor, visibleColors.thumbColor)

            // Wait for the fade delay and fade duration to complete so alpha == 0f.
            rule.mainClock.advanceTimeBy(300L)
            rule.mainClock.scheduler.runCurrent()
            rule.mainClock.advanceTimeByFrame()

            val fadedColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(TestContainerBackgroundColor, fadedColors.thumbColor)

            val trackCenter = getTrackCenterOffset(state)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveBy(Offset(10f, 10f))
                up()
            }

            assertFalse(dragCalled)
            assertFalse(pressCalled)
        }
    }

    @Test
    fun interactiveScrollbar_contentFitsViewport_doesNotConsumeTouches() {
        var dragCalled = false
        var pressCalled = false
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 100 // Fits viewport
                override val viewportSize: Int = 100
            }
        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragCalled = true },
                onTrackPress = { pressCalled = true },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveBy(Offset(10f, 10f))
                up()
            }

            assertEquals(false, dragCalled)
            assertEquals(false, pressCalled)
        }
    }

    @Test
    fun interactiveScrollbar_contentShrinksToFitViewport_doesNotConsumeTouches() {
        var dragCalled = false
        var pressCalled = false
        var childReceivedUnconsumedDown = false
        val contentSizeState = mutableStateOf(200)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int
                    get() = contentSizeState.value

                override val viewportSize: Int = 100
            }

        rule.setContent {
            val colors = InteractiveScrollbarDefaults.colors()
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    Modifier.testTag(ScrollableContainerTestTag)
                        .size(ScrollableContainerSize)
                        .interactiveScrollbar(
                            state = state,
                            orientation = orientation,
                            onThumbDrag = { dragCalled = true },
                            onTrackPress = { pressCalled = true },
                            isFadeEnabled = false,
                            thickness = ScrollbarThickness,
                            mainAxisTrackInset = TestMainAxisTrackInset,
                            crossAxisTrackInset = TestCrossAxisTrackInset,
                            colors = colors,
                        )
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    if (event.changes.any { !it.isConsumed && it.pressed }) {
                                        childReceivedUnconsumedDown = true
                                    }
                                }
                            }
                        }
                )
            }
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)

            // Initially scrollable: track press is intercepted by the scrollbar.
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                up()
            }
            assertTrue(pressCalled)
            assertEquals(false, childReceivedUnconsumedDown)

            // Shrink content to fit viewport dynamically; cached bounds should be cleared.
            pressCalled = false
            rule.runOnIdle { contentSizeState.value = 100 }
            rule.waitForIdle()

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveBy(Offset(10f, 10f))
                up()
            }

            assertEquals(false, pressCalled)
            assertEquals(false, dragCalled)
            assertTrue(childReceivedUnconsumedDown)
        }
    }

    @Test
    fun interactiveScrollbar_doesNotExpandTouchTargetBeyondTrackBounds() {
        // Contract test: the scrollbar hit area must remain bounded to its visual track thickness
        // rather than expanding to the 48.dp minimum interactive touch target, so touches on
        // underlying scrollable content near the scrollbar edge are not intercepted.
        var dragCalled = false
        var pressCalled = false
        var contentReceivedTouch = false
        val thickness = InteractiveScrollbarDefaults.Thickness // 8.dp (< 48.dp)
        val crossInset = InteractiveScrollbarDefaults.CrossAxisTrackInset // 2.dp
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    Modifier.testTag(ScrollableContainerTestTag)
                        .size(ScrollableContainerSize)
                        .interactiveScrollbar(
                            state = state,
                            orientation = orientation,
                            onThumbDrag = { dragCalled = true },
                            onTrackPress = { pressCalled = true },
                            thickness = thickness,
                            crossAxisTrackInset = crossInset,
                            isFadeEnabled = false,
                        )
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    if (event.changes.any { !it.isConsumed && it.pressed }) {
                                        contentReceivedTouch = true
                                    }
                                }
                            }
                        }
                )
            }
        }

        with(rule.density) {
            val containerPx = ScrollableContainerSize.toPx()
            // Pick a cross-axis coordinate 20.dp from the scrollbar edge: outside the 10.dp
            // (2.dp inset + 8.dp thickness) track, but well within a 48.dp touch target.
            val outsideTrackInside48Dp = 20.dp.toPx()
            val crossPos =
                if (orientation == Orientation.Vertical && layoutDirection == LayoutDirection.Rtl) {
                    outsideTrackInside48Dp
                } else {
                    containerPx - outsideTrackInside48Dp
                }
            val mainPos = containerPx / 2f
            val touchPoint =
                if (orientation == Orientation.Vertical) {
                    Offset(crossPos, mainPos)
                } else {
                    Offset(mainPos, crossPos)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(touchPoint)
                moveBy(Offset(5f, 5f))
                up()
            }

            assertEquals(false, pressCalled)
            assertEquals(false, dragCalled)
            assertTrue(contentReceivedTouch)
        }
    }

    @Test
    fun interactiveScrollbar_emitsThumbInteractions() {
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
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = thumbInteractionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                up()
            }
            rule.waitForIdle()
            assertEquals(2, interactions.size)
            assertTrue(interactions[0] is PressInteraction.Press)
            assertTrue(interactions[1] is PressInteraction.Release)
            assertSame(interactions[0], (interactions[1] as PressInteraction.Release).press)
            interactions.clear()

            val dragEnd =
                if (orientation == Orientation.Vertical) {
                    Offset(thumbCenter.x, thumbCenter.y + 10.dp.toPx())
                } else {
                    Offset(thumbCenter.x + 10.dp.toPx(), thumbCenter.y)
                }
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                moveTo(dragEnd)
                up()
            }
            rule.waitForIdle()
            assertEquals(4, interactions.size)
            assertTrue(interactions[0] is PressInteraction.Press)
            assertTrue(interactions[1] is DragInteraction.Start)
            assertTrue(interactions[2] is DragInteraction.Stop)
            assertTrue(interactions[3] is PressInteraction.Release)
            assertSame(interactions[1], (interactions[2] as DragInteraction.Stop).start)
            assertSame(interactions[0], (interactions[3] as PressInteraction.Release).press)
            interactions.clear()

            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(thumbCenter)
                }
                rule.waitForIdle()
                assertEquals(1, interactions.size)
                assertTrue(interactions[0] is HoverInteraction.Enter)
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on the test runner
            }
        }
    }

    // Pressing on the track (outside the thumb) should not emit PressInteraction on the thumb or
    // display pressedThumbColor.
    @Test
    fun interactiveScrollbar_trackPress_doesNotTriggerThumbPressInteraction() {
        val interactions = mutableListOf<Interaction>()
        val thumbInteractionSource = MutableInteractionSource()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        rule.setContent {
            LaunchedEffect(thumbInteractionSource) {
                thumbInteractionSource.interactions.collect { interactions.add(it) }
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    Modifier.size(ScrollableContainerSize)
                        .background(Color.White)
                        .testTag(ScrollableContainerTestTag)
                        .interactiveScrollbar(
                            state = state,
                            orientation = orientation,
                            onThumbDrag = {},
                            onTrackPress = { scrollOffsetState.value += 1 },
                            colors =
                                InteractiveScrollbarDefaults.colors(
                                    thumbColor = Color.Red,
                                    pressedThumbColor = Color.Green,
                                    trackColor = Color.Transparent,
                                ),
                            thickness = ScrollbarThickness,
                            isFadeEnabled = false,
                            thumbInteractionSource = thumbInteractionSource,
                        )
                )
            }
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()
            val trackCenter = getTrackCenterOffset(state)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { down(trackCenter) }
            rule.waitForIdle()

            val colors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(Color.Red, colors.thumbColor)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { up() }
            rule.waitForIdle()

            assertTrue(
                "Expected no thumb press interactions when pressing on track",
                interactions.none { it is PressInteraction },
            )
        }
    }

    // When onTrackPress moves the thumb under the pointer, subsequent dragging should emit
    // DragInteraction on the thumb without emitting PressInteraction.
    @Test
    fun interactiveScrollbar_trackPressAndDragWhenThumbUnderPointer_emitsDragWithoutPressInteraction() {
        val interactions = mutableListOf<Interaction>()
        val thumbInteractionSource = MutableInteractionSource()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        rule.setContent {
            LaunchedEffect(thumbInteractionSource) {
                thumbInteractionSource.interactions.collect { interactions.add(it) }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = thumbInteractionSource,
                onThumbDrag = {},
                onTrackPress = { offset ->
                    scrollOffsetState.value =
                        (offset * (state.contentSize - state.viewportSize)).toInt()
                },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            val scrubEnd =
                if (orientation == Orientation.Vertical) {
                    Offset(trackCenter.x, trackCenter.y + 10.dp.toPx())
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    Offset(trackCenter.x - 10.dp.toPx(), trackCenter.y)
                } else {
                    Offset(trackCenter.x + 10.dp.toPx(), trackCenter.y)
                }

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveTo(scrubEnd)
                up()
            }
            rule.waitForIdle()

            assertEquals(2, interactions.size)
            assertTrue(interactions[0] is DragInteraction.Start)
            assertTrue(interactions[1] is DragInteraction.Stop)
        }
    }

    @Test
    fun interactiveScrollbar_remainsVisibleDuringInteraction() {
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
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = true,
                fadeDelayMillis = 100,
                fadeDurationMillis = 100,
                backgroundColor = TestContainerBackgroundColor,
                thumbColor = ScrollbarThumbColor,
                trackColor = ScrollbarTrackColor,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { offsetState.value = 10 }
        rule.mainClock.advanceTimeBy(20)

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { down(thumbCenter) }

            rule.mainClock.advanceTimeBy(300)
            rule.mainClock.scheduler.runCurrent()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val colors =
                computeScrollbarTestColors(
                    imageBitmap.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )

            val thumbColor = colors.thumbColor
            assertNotEquals(TestContainerBackgroundColor, thumbColor)
            assertTrue("Expected dominant red component, got $thumbColor", thumbColor.red > 0.8f)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { up() }

            rule.mainClock.scheduler.runCurrent()
            rule.mainClock.advanceTimeBy(150)
            rule.mainClock.advanceTimeByFrame()

            val fadedBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val fadedColors =
                computeScrollbarTestColors(
                    fadedBitmap.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )

            assertEquals(TestContainerBackgroundColor, fadedColors.thumbColor)

            // Wake scrollbar again via scroll, then press on the track: scrollbar should remain
            // visible while held down, fade out after release, and reappear on subsequent scroll.
            rule.runOnIdle { offsetState.value = 20 }
            rule.mainClock.advanceTimeBy(20)

            val trackCenter = getTrackCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { down(trackCenter) }

            rule.mainClock.advanceTimeBy(300)
            rule.mainClock.scheduler.runCurrent()

            val trackPressColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertNotEquals(TestContainerBackgroundColor, trackPressColors.thumbColor)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { up() }

            rule.mainClock.scheduler.runCurrent()
            rule.mainClock.advanceTimeBy(150)
            rule.mainClock.advanceTimeByFrame()

            val fadedAfterTrackPressColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(TestContainerBackgroundColor, fadedAfterTrackPressColors.thumbColor)

            // Verify subsequent scroll still brings the scrollbar back
            rule.runOnIdle { offsetState.value = 10 }
            rule.mainClock.advanceTimeBy(20)
            rule.mainClock.scheduler.runCurrent()

            val reappearedColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertNotEquals(TestContainerBackgroundColor, reappearedColors.thumbColor)
        }
    }

    @Test
    fun interactiveScrollbar_mouseMove_triggersHoverAndExit() {
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
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = thumbInteractionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val outsidePoint =
                if (orientation == Orientation.Vertical) {
                    if (layoutDirection == LayoutDirection.Ltr) Offset(0f, 10f)
                    else Offset(100.dp.toPx() - 10f, 10f)
                } else {
                    Offset(10f, 0f)
                }

            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(outsidePoint)
                    moveTo(thumbCenter)
                }
                rule.waitForIdle()
                assertEquals(1, interactions.size)
                assertTrue(interactions[0] is HoverInteraction.Enter)

                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    moveTo(outsidePoint)
                }
                rule.waitForIdle()
                assertEquals(2, interactions.size)
                assertTrue(interactions[1] is HoverInteraction.Exit)
                assertSame(interactions[0], (interactions[1] as HoverInteraction.Exit).enter)
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on this device/runner
            }
        }
    }

    @Test
    fun interactiveScrollbar_nodeDetached_emitsExitInteraction() {
        val interactions = mutableListOf<Interaction>()
        val thumbInteractionSource = MutableInteractionSource()
        val showScrollbar = mutableStateOf(true)
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
            if (showScrollbar.value) {
                InteractiveScrollbarTestContainer(
                    state = state,
                    orientation = orientation,
                    layoutDirection = layoutDirection,
                    thumbInteractionSource = thumbInteractionSource,
                    onThumbDrag = {},
                    onTrackPress = {},
                    modifier = Modifier.testTag(ScrollableContainerTestTag),
                )
            }
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(thumbCenter)
                }
                rule.waitForIdle()
                rule.mainClock.scheduler.runCurrent()
                assertEquals(1, interactions.size)
                assertTrue(interactions[0] is HoverInteraction.Enter)

                showScrollbar.value = false

                rule.waitForIdle()
                rule.mainClock.scheduler.runCurrent()
                assertEquals(2, interactions.size)
                assertTrue(interactions[1] is HoverInteraction.Exit)
                assertSame(interactions[0], (interactions[1] as HoverInteraction.Exit).enter)
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on the test runner
            }
        }
    }

    @Test
    fun interactiveScrollbar_stateAlphas() {
        // Disable motion duration scaling to keep transitions instant
        motionDurationScale.scaleFactor = 0f
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false, // Keep always visible
                backgroundColor = TestContainerBackgroundColor, // Green
                thumbColor = ScrollbarThumbColor, // Red
                trackColor = Color.Transparent, // No track to interfere
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()
            val thumbCenter = getThumbCenterOffset(state)

            // 1. Verify Default Alpha
            val defaultImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val defaultColors =
                computeScrollbarTestColors(
                    defaultImage.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(
                InteractiveScrollbarDefaults.ThumbOpacity,
                defaultColors.thumbColor.red,
                0.05f,
            )
            assertEquals(
                1f - InteractiveScrollbarDefaults.ThumbOpacity,
                defaultColors.thumbColor.green,
                0.05f,
            )

            // 2. Verify Hovered Alpha
            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(thumbCenter)
                }
                rule.waitForIdle()
                val hoverImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
                val hoverColors =
                    computeScrollbarTestColors(
                        hoverImage.toPixelMap(),
                        thicknessPx,
                        viewportWidthPx,
                        viewportHeightPx,
                    )
                assertEquals(
                    InteractiveScrollbarDefaults.HoveredThumbOpacity,
                    hoverColors.thumbColor.red,
                    0.05f,
                )
                assertEquals(
                    1f - InteractiveScrollbarDefaults.HoveredThumbOpacity,
                    hoverColors.thumbColor.green,
                    0.05f,
                )

                // Exit hover
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput { exit() }
                rule.waitForIdle()
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on this device/runner
            }

            // 3. Verify Pressed Alpha
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { down(thumbCenter) }
            rule.waitForIdle()
            val pressImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pressColors =
                computeScrollbarTestColors(
                    pressImage.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(
                InteractiveScrollbarDefaults.PressedThumbOpacity,
                pressColors.thumbColor.red,
                0.05f,
            )
            assertEquals(
                1f - InteractiveScrollbarDefaults.PressedThumbOpacity,
                pressColors.thumbColor.green,
                0.05f,
            )

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { up() }
            rule.waitForIdle()
        }
    }

    @Test
    fun interactiveScrollbar_pressedAndDraggedColors() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    Modifier.size(ScrollableContainerSize)
                        .background(Color.White)
                        .testTag(ScrollableContainerTestTag)
                        .interactiveScrollbar(
                            state = state,
                            orientation = orientation,
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
                        )
                )
            }
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()
            val thumbCenter = getThumbCenterOffset(state)

            // 1. On initial down on thumb (before movement), pressedThumbColor (Green) is used
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { down(thumbCenter) }
            rule.waitForIdle()
            val pressedColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(Color.Green, pressedColors.thumbColor)

            // 2. Once pointer moves, draggedThumbColor (Blue) is used
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                moveBy(if (orientation == Orientation.Vertical) Offset(0f, 5f) else Offset(5f, 0f))
            }
            rule.waitForIdle()
            val draggedColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(Color.Blue, draggedColors.thumbColor)

            // 3. On release, returns to default thumbColor (Red)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput { up() }
            rule.waitForIdle()
            val idleColors =
                computeScrollbarTestColors(
                    rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage().toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )
            assertEquals(Color.Red, idleColors.thumbColor)
        }
    }

    @Test
    fun interactiveScrollbar_trackHoverAwakensScrollbar() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = true,
                fadeDelayMillis = 100,
                fadeDurationMillis = 100,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(trackCenter)
                }
                rule.waitForIdle()
                rule.mainClock.scheduler.runCurrent()
                val hoverImage = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
                val colors =
                    computeScrollbarTestColors(
                        hoverImage.toPixelMap(),
                        ScrollbarThickness.roundToPx(),
                        ScrollableContainerSize.roundToPx(),
                        ScrollableContainerSize.roundToPx(),
                    )
                assertTrue(
                    "Expected thumb to remain visible during track hover",
                    colors.thumbColor.red > 0.3f,
                )

                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput { exit() }
                rule.waitForIdle()
                rule.mainClock.advanceTimeBy(300)
                rule.waitForIdle()
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on test runner
            }
        }
    }

    @Test
    fun interactiveScrollbar_pointerInputCancelled() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        val thumbInteractionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()
        var dragDelta = 0f

        rule.setContent {
            LaunchedEffect(thumbInteractionSource) {
                thumbInteractionSource.interactions.collect { interactions.add(it) }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = thumbInteractionSource,
                onThumbDrag = { dragDelta += it },
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                moveBy(
                    if (orientation == Orientation.Vertical) Offset(0f, 20f) else Offset(20f, 0f)
                )
                cancel()
            }
        }

        rule.runOnIdle {
            assertTrue(
                "Expected PressInteraction.Press to be emitted",
                interactions.any { it is PressInteraction.Press },
            )
            assertTrue(
                "Expected DragInteraction.Cancel or PressInteraction.Cancel to be emitted on cancel",
                interactions.any { it is DragInteraction.Cancel || it is PressInteraction.Cancel },
            )
        }
    }

    @Test
    fun interactiveScrollbar_multiTouchDisambiguation() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        var totalDragDelta = 0f

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { totalDragDelta += it },
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            // To guarantee outsidePoint is outside the scrollbar track padding, place it on the
            // opposite side.
            val outsidePoint =
                if (orientation == Orientation.Vertical) {
                    if (layoutDirection == LayoutDirection.Ltr) Offset(0f, 10f)
                    else Offset(100.dp.toPx() - 10f, 10f)
                } else {
                    Offset(10f, 0f)
                }
            val primaryDragOffset =
                if (orientation == Orientation.Vertical) Offset(0f, 10f)
                else if (layoutDirection == LayoutDirection.Rtl) Offset(-10f, 0f)
                else Offset(10f, 0f)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                // Down with pointer 1 (active pointer) on thumb
                down(pointerId = 0, thumbCenter)
                moveBy(pointerId = 0, primaryDragOffset)
            }
            // Wait for events to fire
            rule.waitForIdle()
            val deltaAfterP1Move = totalDragDelta

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                // Down with pointer 2 (secondary pointer) outside thumb
                down(pointerId = 1, outsidePoint)
                // Move pointer 2 (should be ignored for scrolling)
                moveBy(
                    pointerId = 1,
                    if (orientation == Orientation.Vertical) Offset(0f, 20f) else Offset(20f, 0f),
                )
            }
            rule.waitForIdle()
            assertEquals(
                "Movements from secondary pointer should be ignored",
                deltaAfterP1Move,
                totalDragDelta,
                0.01f,
            )

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                // Move pointer 1 (should update scrolling)
                moveBy(pointerId = 0, primaryDragOffset)
            }
            rule.waitForIdle()
            assertTrue(
                "Movements from primary active pointer should update scroll delta",
                abs(totalDragDelta - deltaAfterP1Move) > 0f,
            )

            // Release both pointers to complete touch injection gesture lifecycle cleanly
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                up(pointerId = 0)
                up(pointerId = 1)
            }
        }
    }

    @Test
    fun interactiveScrollbar_coercesToMinThumbLength() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 1000
                override val viewportSize: Int = 100
            }

        val minLength = 30.dp

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                thumbMinLength = minLength,
                onThumbDrag = {},
                onTrackPress = {},
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
    fun interactiveScrollbar_coercesToMaxThumbLength() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 110
                override val viewportSize: Int = 100
            }

        val maxFraction = 0.5f

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                thumbMaxLengthFraction = maxFraction,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackLengthPx = (ScrollableContainerSize - TestMainAxisTrackInset * 2f).roundToPx()
            val expectedMaxLengthPx = trackLengthPx * maxFraction

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val thumbBounds = findColorBounds(pixelMap, ScrollbarThumbColor)

            val thumbLength =
                if (orientation == Orientation.Vertical) {
                    thumbBounds.height
                } else {
                    thumbBounds.width
                }

            assertEquals(expectedMaxLengthPx, thumbLength, 1f)
        }
    }

    @Test
    fun interactiveScrollbar_hidesWhenTrackTooSmallForMinThumbLength() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        val minLength = 30.dp
        val tinyContainerSize = 20.dp

        rule.setContent {
            Box(
                Modifier.size(
                    width =
                        if (orientation == Orientation.Vertical) ScrollableContainerSize
                        else tinyContainerSize,
                    height =
                        if (orientation == Orientation.Vertical) tinyContainerSize
                        else ScrollableContainerSize,
                )
            ) {
                InteractiveScrollbarTestContainer(
                    state = state,
                    orientation = orientation,
                    layoutDirection = layoutDirection,
                    isFadeEnabled = false,
                    thumbMinLength = minLength,
                    onThumbDrag = {},
                    onTrackPress = {},
                    modifier = Modifier.testTag(ScrollableContainerTestTag),
                )
            }
        }

        with(rule.density) {
            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val thumbBounds = findColorBounds(pixelMap, ScrollbarThumbColor)
            val trackBounds = findColorBounds(pixelMap, ScrollbarTrackColor)

            assertEquals(Rect.Zero, thumbBounds)
            assertEquals(Rect.Zero, trackBounds)
        }
    }

    @Test
    fun interactiveScrollbar_respectsTrackInsets() {
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        val mainInset = 10.dp
        val crossInset = 5.dp

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = false,
                mainAxisTrackInset = mainInset,
                crossAxisTrackInset = crossInset,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val mainInsetPx = mainInset.roundToPx()
            val crossInsetPx = crossInset.roundToPx()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val pixelMap = imageBitmap.toPixelMap()

            val trackBounds = findColorBounds(pixelMap, ScrollbarTrackColor)

            val expectedMainStart = mainInsetPx.toFloat()
            val expectedCrossStart = crossInsetPx.toFloat()

            if (orientation == Orientation.Vertical) {
                assertEquals(expectedMainStart, trackBounds.top, 1f)
                if (layoutDirection == LayoutDirection.Ltr) {
                    assertEquals(
                        ScrollableContainerSize.roundToPx() -
                            crossInsetPx -
                            ScrollbarThickness.toPx(),
                        trackBounds.left,
                        1f,
                    )
                } else {
                    assertEquals(expectedCrossStart, trackBounds.left, 1f)
                }
            } else {
                if (layoutDirection == LayoutDirection.Ltr) {
                    assertEquals(expectedMainStart, trackBounds.left, 1f)
                } else {
                    assertEquals(
                        expectedMainStart,
                        ScrollableContainerSize.roundToPx() - trackBounds.right,
                        1f,
                    )
                }
                assertEquals(
                    ScrollableContainerSize.roundToPx() - crossInsetPx - ScrollbarThickness.toPx(),
                    trackBounds.top,
                    1f,
                )
            }
        }
    }

    @Test
    fun interactiveScrollbar_fadeAnimation_whenSystemDisablesAnimations() {
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
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                isFadeEnabled = true,
                fadeDelayMillis = 100,
                fadeDurationMillis = 100,
                backgroundColor = TestContainerBackgroundColor,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        rule.mainClock.autoAdvance = false

        assertEquals(0, state.scrollOffset)

        rule.runOnIdle { offsetState.value = 10 }

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

            assertNotEquals(TestContainerBackgroundColor, initialColors.thumbColor)
            assertTrue(
                "Expected thumb to be visible initially",
                initialColors.thumbColor.red > 0.5f,
            )

            rule.mainClock.scheduler.runCurrent()
            rule.mainClock.advanceTimeBy(150)
            rule.mainClock.advanceTimeByFrame()

            val imageBitmap = rule.onNodeWithTag(ScrollableContainerTestTag).captureToImage()
            val actualColors =
                computeScrollbarTestColors(
                    imageBitmap.toPixelMap(),
                    thicknessPx,
                    viewportWidthPx,
                    viewportHeightPx,
                )

            assertEquals(TestContainerBackgroundColor, actualColors.thumbColor)
            assertEquals(TestContainerBackgroundColor, actualColors.trackColor)
        }
    }

    @Test
    fun interactiveScrollbar_dragThumb_clampedToUnitRange() {
        val dragFractions = mutableListOf<Float>()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragFractions.add(it) },
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                moveBy(
                    if (orientation == Orientation.Vertical) Offset(0f, 1000f)
                    else Offset(1000f, 0f)
                )
                moveBy(
                    if (orientation == Orientation.Vertical) Offset(0f, -2000f)
                    else Offset(-2000f, 0f)
                )
                up()
            }

            assertTrue("Expected drag callbacks", dragFractions.isNotEmpty())
            for (fraction in dragFractions) {
                assertTrue("Fraction $fraction must be >= 0f", fraction >= 0f)
                assertTrue("Fraction $fraction must be <= 1f", fraction <= 1f)
            }
        }
    }

    @Test
    fun interactiveScrollbar_trackPress_emitsCorrectFraction() {
        val pressedFractions = mutableListOf<Float>()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 250
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = {},
                onTrackPress = { pressedFractions.add(it) },
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                up()
            }

            assertEquals(1, pressedFractions.size)
            val fraction = pressedFractions[0]

            val trackLengthPx = (ScrollableContainerSize - TestMainAxisTrackInset * 2f).toPx()
            val thumbLengthPx = trackLengthPx * (state.viewportSize.toFloat() / state.contentSize)
            val maxTrackOffsetPx = trackLengthPx - thumbLengthPx

            val trackAxisPos =
                if (orientation == Orientation.Vertical) {
                    trackCenter.y - TestMainAxisTrackInset.toPx()
                } else if (layoutDirection == LayoutDirection.Rtl) {
                    ScrollableContainerSize.toPx() - TestMainAxisTrackInset.toPx() - trackCenter.x
                } else {
                    trackCenter.x - TestMainAxisTrackInset.toPx()
                }
            val expectedThumbStart = trackAxisPos - thumbLengthPx / 2f
            val expectedFraction = expectedThumbStart / maxTrackOffsetPx

            assertEquals(
                "Expected mathematically derived fraction when pressing track",
                expectedFraction,
                fraction,
                0.05f,
            )
        }
    }

    @Test
    fun interactiveScrollbar_thumbFillsTrack_emitsZeroFraction() {
        // When content is scrollable but thumbMinLength fills the full track (maxTrackOffsetPx ==
        // 0f), dragging the thumb should safely emit 0f instead of NaN/Infinity.
        val dragFractions = mutableListOf<Float>()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }
        // Use an exact integer-pixel size so container rounding doesn't leave a sub-pixel gap
        // between trackLengthPx and minThumbLengthPx on fractional densities.
        val exactSize = with(rule.density) { 200.toDp() }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                containerSize = exactSize,
                thumbMinLength = exactSize,
                thumbMaxLengthFraction = 1f,
                mainAxisTrackInset = 0.dp,
                onThumbDrag = { dragFractions.add(it) },
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter =
                getThumbCenterOffset(state, containerSize = exactSize, mainAxisInset = 0.dp)
            val delta =
                if (orientation == Orientation.Vertical) Offset(0f, 10f) else Offset(10f, 0f)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                moveBy(delta)
                up()
            }

            assertTrue("Expected drag callback to be invoked", dragFractions.isNotEmpty())
            for (f in dragFractions) {
                assertEquals(0f, f, 0.001f)
            }
        }
    }

    @Test
    fun interactiveScrollbar_consumedPointerEvent_ignored() {
        val dragFractions = mutableListOf<Float>()
        val pressedFractions = mutableListOf<Float>()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                onThumbDrag = { dragFractions.add(it) },
                onTrackPress = { pressedFractions.add(it) },
                modifier =
                    Modifier.testTag(ScrollableContainerTestTag).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }

        with(rule.density) {
            val trackCenter = getTrackCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                moveBy(Offset(10f, 10f))
                up()
            }

            assertTrue(pressedFractions.isEmpty())
            assertTrue(dragFractions.isEmpty())
        }
    }

    @Test
    fun interactiveScrollbar_touchMove_doesNotTriggerHover() {
        val hoverInteractions = mutableListOf<HoverInteraction>()
        val interactionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is HoverInteraction) {
                        hoverInteractions.add(it)
                    }
                }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = interactionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                moveBy(Offset(10f, 10f))
                up()
            }

            assertTrue(
                "Touch movements should not emit HoverInteraction.Enter",
                hoverInteractions.none { it is HoverInteraction.Enter },
            )
        }
    }

    @Test
    fun interactiveScrollbar_pressedMouseMove_doesNotTriggerHover() {
        val hoverInteractions = mutableListOf<HoverInteraction>()
        val interactionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is HoverInteraction) {
                        hoverInteractions.add(it)
                    }
                }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = interactionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val containerCenter = Offset(50.dp.toPx(), 50.dp.toPx())
            rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                enter(containerCenter)
                press()
                moveTo(thumbCenter)
            }

            rule.waitForIdle()
            assertTrue(
                "Pressed mouse move should not emit HoverInteraction.Enter",
                hoverInteractions.none { it is HoverInteraction.Enter },
            )

            rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput { release() }
        }
    }

    @Test
    fun interactiveScrollbar_unpressedMouseMove_triggersHover() {
        val hoverInteractions = mutableListOf<HoverInteraction>()
        val interactionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is HoverInteraction) {
                        hoverInteractions.add(it)
                    }
                }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = interactionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput { enter(thumbCenter) }

            rule.waitForIdle()
            assertTrue(
                "Unpressed mouse move entering thumb should emit HoverInteraction.Enter",
                hoverInteractions.any { it is HoverInteraction.Enter },
            )
        }
    }

    @Test
    fun interactiveScrollbar_mouseDragReleaseOutside_exitsHover() {
        val hoverInteractions = mutableListOf<HoverInteraction>()
        val interactionSource = MutableInteractionSource()
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is HoverInteraction) {
                        hoverInteractions.add(it)
                    }
                }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = interactionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val outsidePoint = Offset(50.dp.toPx(), 50.dp.toPx())

            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(thumbCenter)
                    press()
                    moveTo(outsidePoint)
                    release()
                }
                rule.waitForIdle()
                assertEquals(2, hoverInteractions.size)
                assertTrue(hoverInteractions[0] is HoverInteraction.Enter)
                assertTrue(hoverInteractions[1] is HoverInteraction.Exit)
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on this device/runner
            }
        }
    }

    @Test
    fun interactiveScrollbar_scrollUnderStationaryMouse_updatesThumbHover() {
        // When the thumb scrolls out from under (or into) a stationary cursor hovering the track,
        // thumb hover state should update without requiring a new mouse Move event.
        val hoverInteractions = mutableListOf<HoverInteraction>()
        val interactionSource = MutableInteractionSource()
        val scrollOffsetState = mutableStateOf(0)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 300
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is HoverInteraction) {
                        hoverInteractions.add(it)
                    }
                }
            }
            InteractiveScrollbarTestContainer(
                state = state,
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = interactionSource,
                onThumbDrag = {},
                onTrackPress = {},
                modifier = Modifier.testTag(ScrollableContainerTestTag),
            )
        }

        with(rule.density) {
            val initialThumbCenter = getThumbCenterOffset(state)
            try {
                rule.onNodeWithTag(ScrollableContainerTestTag).performMouseInput {
                    enter(initialThumbCenter)
                }
                rule.waitForIdle()
                assertEquals(1, hoverInteractions.size)
                assertTrue(hoverInteractions[0] is HoverInteraction.Enter)

                // Scroll to the end so the thumb moves away from the stationary cursor.
                rule.runOnIdle { scrollOffsetState.value = 200 }
                rule.waitForIdle()
                assertEquals(2, hoverInteractions.size)
                assertTrue(hoverInteractions[1] is HoverInteraction.Exit)

                // Scroll back to the start so the thumb moves back under the stationary cursor.
                rule.runOnIdle { scrollOffsetState.value = 0 }
                rule.waitForIdle()
                assertEquals(3, hoverInteractions.size)
                assertTrue(hoverInteractions[2] is HoverInteraction.Enter)
            } catch (_: UnsupportedOperationException) {
                // Ignore if mouse input is not supported on this device/runner
            }
        }
    }

    @Test
    fun interactiveScrollbar_ancestorConsumesMidGesture_cancelsAndReleasesPointer() {
        // When an ancestor intercepts and consumes pointer changes in the Initial pass after the
        // scrollbar has captured a down event, the scrollbar must cancel active interactions and
        // release capturedPointerId so subsequent gestures continue to work.
        val interactions = mutableListOf<Interaction>()
        val interactionSource = MutableInteractionSource()
        var ancestorConsumeMoveAndUp = true
        var trackPressCount = 0
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 0
                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions.add(it) }
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    Modifier.testTag(ScrollableContainerTestTag)
                        .size(ScrollableContainerSize)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (ancestorConsumeMoveAndUp) {
                                        // Allow initial down through, then consume subsequent
                                        // move/up changes like a parent gesture interceptor.
                                        event.changes.forEach { change ->
                                            if (change.previousPressed) {
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .interactiveScrollbar(
                            state = state,
                            orientation = orientation,
                            onThumbDrag = {},
                            onTrackPress = { trackPressCount++ },
                            thumbInteractionSource = interactionSource,
                            thickness = ScrollbarThickness,
                            mainAxisTrackInset = TestMainAxisTrackInset,
                            crossAxisTrackInset = TestCrossAxisTrackInset,
                            isFadeEnabled = false,
                        )
                )
            }
        }

        with(rule.density) {
            val thumbCenter = getThumbCenterOffset(state)
            val trackCenter = getTrackCenterOffset(state)

            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(thumbCenter)
                moveBy(Offset(10f, 10f))
                up()
            }
            rule.waitForIdle()

            assertEquals(2, interactions.size)
            assertTrue(interactions[0] is PressInteraction.Press)
            assertTrue(interactions[1] is PressInteraction.Cancel)

            // Verify capturedPointerId was properly cleared by performing a fresh track press.
            ancestorConsumeMoveAndUp = false
            rule.onNodeWithTag(ScrollableContainerTestTag).performTouchInput {
                down(trackCenter)
                up()
            }
            assertEquals(1, trackPressCount)
        }
    }

    @Test
    fun interactiveScrollbar_detachedWhileVisible_resetsAlphaOnReattach() {
        // If a scrollbar node with fade enabled is detached while visible (cancelling fadeJob) and
        // later reattached, alpha should reset to 0f synchronously rather than staying visible.
        val scrollOffsetState = mutableStateOf(0)
        val isActiveState = mutableStateOf(true)
        val state =
            object : ScrollIndicatorState {
                override val scrollOffset: Int
                    get() = scrollOffsetState.value

                override val contentSize: Int = 200
                override val viewportSize: Int = 100
            }

        rule.mainClock.autoAdvance = false

        rule.setContent {
            ReusableContentHost(active = isActiveState.value) {
                InteractiveScrollbarTestContainer(
                    state = state,
                    orientation = orientation,
                    layoutDirection = layoutDirection,
                    isFadeEnabled = true,
                    fadeDelayMillis = 5000,
                    backgroundColor = TestContainerBackgroundColor,
                    onThumbDrag = {},
                    onTrackPress = {},
                    modifier = Modifier.testTag(ScrollableContainerTestTag),
                )
            }
        }

        with(rule.density) {
            val thicknessPx = ScrollbarThickness.roundToPx()
            val viewportWidthPx = ScrollableContainerSize.roundToPx()
            val viewportHeightPx = ScrollableContainerSize.roundToPx()

            // Scroll to make the scrollbar visible (alpha = 1f) while fadeDelayMillis (5000ms)
            // keeps it visible.
            rule.runOnIdle { scrollOffsetState.value = 50 }
            rule.mainClock.advanceTimeBy(32L)
            rule.mainClock.scheduler.runCurrent()

            val visibleColors =
                computeScrollbarTestColors(
                    pixelMap =
                        rule
                            .onNodeWithTag(ScrollableContainerTestTag)
                            .captureToImage()
                            .toPixelMap(),
                    thicknessPx = thicknessPx,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                )
            assertNotEquals(TestContainerBackgroundColor, visibleColors.thumbColor)

            // Deactivate (detaching the Modifier.Node without disposing it), then reactivate
            // (reattaching the same node instance) without scrolling.
            rule.runOnIdle { isActiveState.value = false }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.runOnIdle { isActiveState.value = true }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()

            val reattachedColors =
                computeScrollbarTestColors(
                    pixelMap =
                        rule
                            .onNodeWithTag(ScrollableContainerTestTag)
                            .captureToImage()
                            .toPixelMap(),
                    thicknessPx = thicknessPx,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                )
            assertEquals(TestContainerBackgroundColor, reattachedColors.thumbColor)
        }
    }

    private fun Density.getThumbCenterOffset(
        state: ScrollIndicatorState,
        containerSize: Dp = ScrollableContainerSize,
        thickness: Dp = ScrollbarThickness,
        mainAxisInset: Dp = TestMainAxisTrackInset,
        crossAxisInset: Dp = TestCrossAxisTrackInset,
    ): Offset {
        val containerPx = containerSize.toPx()
        val thicknessPx = thickness.toPx()
        val mainInsetPx = mainAxisInset.toPx()
        val crossInsetPx = crossAxisInset.toPx()

        val crossCenterPx =
            if (orientation == Orientation.Vertical && layoutDirection == LayoutDirection.Rtl) {
                crossInsetPx + thicknessPx / 2f
            } else {
                containerPx - crossInsetPx - thicknessPx / 2f
            }

        val trackLengthPx = containerPx - 2 * mainInsetPx
        val ratio = (state.viewportSize.toFloat() / state.contentSize.toFloat()).coerceIn(0f, 1f)
        val thumbLengthPx = trackLengthPx * ratio
        val maxOffset = state.contentSize - state.viewportSize
        val thumbOffsetValue =
            if (maxOffset > 0) {
                (state.scrollOffset.toFloat() / maxOffset) * (trackLengthPx - thumbLengthPx)
            } else {
                0f
            }
        val mainCenterPx =
            if (orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl) {
                containerPx - mainInsetPx - thumbLengthPx / 2f - thumbOffsetValue
            } else {
                mainInsetPx + thumbOffsetValue + thumbLengthPx / 2f
            }

        return if (orientation == Orientation.Vertical) {
            Offset(crossCenterPx, mainCenterPx)
        } else {
            Offset(mainCenterPx, crossCenterPx)
        }
    }

    private fun Density.getTrackCenterOffset(
        state: ScrollIndicatorState,
        containerSize: Dp = ScrollableContainerSize,
        thickness: Dp = ScrollbarThickness,
        mainAxisInset: Dp = TestMainAxisTrackInset,
        crossAxisInset: Dp = TestCrossAxisTrackInset,
    ): Offset {
        val containerPx = containerSize.toPx()
        val thicknessPx = thickness.toPx()
        val mainInsetPx = mainAxisInset.toPx()
        val crossInsetPx = crossAxisInset.toPx()

        val crossCenterPx =
            if (orientation == Orientation.Vertical && layoutDirection == LayoutDirection.Rtl) {
                crossInsetPx + thicknessPx / 2f
            } else {
                containerPx - crossInsetPx - thicknessPx / 2f
            }

        val trackLengthPx = containerPx - 2 * mainInsetPx
        val ratio = (state.viewportSize.toFloat() / state.contentSize.toFloat()).coerceIn(0f, 1f)
        val thumbLengthPx = trackLengthPx * ratio
        val maxOffset = state.contentSize - state.viewportSize
        val thumbOffsetValue =
            if (maxOffset > 0) {
                (state.scrollOffset.toFloat() / maxOffset) * (trackLengthPx - thumbLengthPx)
            } else {
                0f
            }
        val emptyTrackCenterPx =
            if (thumbOffsetValue > 0f) {
                // If scrolled down/right, pick the middle of the empty track before the thumb
                mainInsetPx + thumbOffsetValue / 2f
            } else {
                // If at start, pick the middle of the empty track after the thumb
                mainInsetPx + thumbLengthPx + (trackLengthPx - thumbLengthPx) / 2f
            }
        val mainCenterPx =
            if (orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl) {
                containerPx - emptyTrackCenterPx
            } else {
                emptyTrackCenterPx
            }

        return if (orientation == Orientation.Vertical) {
            Offset(crossCenterPx, mainCenterPx)
        } else {
            Offset(mainCenterPx, crossCenterPx)
        }
    }

    private fun Density.computeScrollbarTestColors(
        pixelMap: PixelMap,
        thicknessPx: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        mainAxisInsetPx: Int = TestMainAxisTrackInset.roundToPx(),
        crossAxisInsetPx: Int = TestCrossAxisTrackInset.roundToPx(),
        thumbPosition: ThumbPosition = ThumbPosition.Start,
    ): ScrollbarTestColors =
        computeScrollbarTestColors(
            pixelMap = pixelMap,
            orientation = orientation,
            layoutDirection = layoutDirection,
            thicknessPx = thicknessPx,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            mainAxisInsetPx = mainAxisInsetPx,
            crossAxisInsetPx = crossAxisInsetPx,
            thumbPosition = thumbPosition,
        )
}

private const val ScrollableContainerTestTag = "scrollable_container"
private val ScrollableContainerSize = 100.dp
private val ScrollbarThickness = 10.dp
private val TestMainAxisTrackInset = InteractiveScrollbarDefaults.MainAxisTrackInset
private val TestCrossAxisTrackInset = InteractiveScrollbarDefaults.CrossAxisTrackInset
private val ScrollbarThumbColor = Color.Red
private val ScrollbarTrackColor = Color.Blue
private val TestContainerBackgroundColor = Color.Green

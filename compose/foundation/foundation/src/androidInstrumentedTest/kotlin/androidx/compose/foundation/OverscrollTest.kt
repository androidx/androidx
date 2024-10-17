/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OverscrollTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule = AnimationDurationScaleRule.create()

    @Before
    fun before() {
        // if we don't do it the overscroll effect will not even start.
        animationScaleRule.setAnimationDurationScale(1f)
    }

    private val boxTag = "box"

    @Test
    fun rememberOverscrollEffect_defaultValue() {
        lateinit var effect: OverscrollEffect
        rule.setContent { effect = rememberOverscrollEffect()!! }
        rule.runOnIdle {
            assertThat(effect).isInstanceOf(AndroidEdgeEffectOverscrollEffect::class.java)
        }
    }

    @Test
    fun rememberOverscrollEffect_nullOverscrollFactory() {
        var effect: OverscrollEffect? = null
        rule.setContent {
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                effect = rememberOverscrollEffect()
            }
        }
        rule.runOnIdle { assertThat(effect).isNull() }
    }

    @Test
    fun rememberOverscrollEffect_ChangeOverscrollFactory() {
        lateinit var effect: OverscrollEffect
        val movableContent = movableContentOf { effect = rememberOverscrollEffect()!! }
        var setCustomFactory by mutableStateOf(false)
        class CustomEffect : OverscrollEffect {
            override val isInProgress = false
            override val effectModifier = Modifier

            override fun applyToScroll(
                delta: Offset,
                source: NestedScrollSource,
                performScroll: (Offset) -> Offset
            ) = performScroll(delta)

            override suspend fun applyToFling(
                velocity: Velocity,
                performFling: suspend (Velocity) -> Velocity
            ) {}
        }
        val customFactory =
            object : OverscrollFactory {
                override fun createOverscrollEffect(): OverscrollEffect = CustomEffect()

                override fun hashCode(): Int = -1

                override fun equals(other: Any?) = other === this
            }
        rule.setContent {
            if (setCustomFactory) {
                CompositionLocalProvider(
                    LocalOverscrollFactory provides customFactory,
                    content = movableContent
                )
            } else {
                movableContent()
            }
        }
        rule.runOnIdle {
            assertThat(effect).isInstanceOf(AndroidEdgeEffectOverscrollEffect::class.java)
            setCustomFactory = true
        }
        rule.runOnIdle { assertThat(effect).isInstanceOf(CustomEffect::class.java) }
    }

    @Test
    fun overscrollEffect_scrollable_drag() {
        testDrag(reverseDirection = false)
    }

    @Test
    fun overscrollEffect_scrollable_drag_reverseDirection() {
        // same asserts for `reverseDirection = true`, but that's the point
        // we don't want overscroll to depend on reverseLayout, it's coordinate-driven logic
        testDrag(reverseDirection = true)
    }

    @Test
    fun overscrollEffect_scrollable_fling() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { delta ->
            if (acummulatedScroll > 1000f) {
                0f
            } else {
                acummulatedScroll += delta
                delta
            }
        }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller
        )

        rule.waitUntil { controller.drawCallsCount == 1 }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(center, centerRight, endVelocity = 3000f)
        }

        rule.runOnIdle {
            assertThat(controller.lastVelocity.x).isGreaterThan(0f)
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.SideEffect)
        }
    }

    @Test
    fun overscrollEffect_scrollable_preDrag_respectsConsumption() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect(consumePreCycles = true)
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val viewConfig =
            rule.setOverscrollContentAndReturnViewConfig(
                scrollableState = scrollableState,
                overscrollEffect = controller
            )

        rule.waitUntil { controller.drawCallsCount == 1 }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            val slop = viewConfig.touchSlop
            // since we consume 1/10 of the delta in the pre scroll during overscroll, expect 9/10
            assertThat(abs(acummulatedScroll)).isWithin(0.1f).of((1000f - slop) * 9 / 10)

            assertThat(controller.lastPreScrollDelta).isEqualTo(Offset(1000f - slop, 0f))
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.UserInput)
        }

        rule.onNodeWithTag(boxTag).performTouchInput { up() }
    }

    @Test
    fun overscrollEffect_scrollable_skipsDeltasIfCannotScroll() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect(consumePreCycles = true)

        var canScroll = true

        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }

        val viewConfig =
            rule.setOverscrollContentAndReturnViewConfig(
                scrollableState =
                    object : ScrollableState by scrollableState {
                        override val canScrollForward: Boolean
                            get() = canScroll

                        override val canScrollBackward: Boolean
                            get() = canScroll
                    },
                overscrollEffect = controller
            )

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        rule.runOnIdle {
            val slop = viewConfig.touchSlop
            // since we consume 1/10 of the delta in the pre scroll during overscroll, expect 9/10
            assertThat(abs(acummulatedScroll)).isWithin(0.1f).of((1000f - slop) * 9 / 10)

            assertThat(controller.lastPreScrollDelta).isEqualTo(Offset(1000f - slop, 0f))
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.UserInput)
            controller.lastPreScrollDelta = Offset.Zero
        }

        // Inform scrollable that we cannot scroll anymore
        rule.runOnIdle { canScroll = false }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        rule.runOnIdle {
            // Scrollable should not have dispatched any new deltas
            assertThat(controller.lastPreScrollDelta).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun overscrollEffect_scrollable_preFling_respectsConsumption() {
        var acummulatedScroll = 0f
        var lastFlingReceived = 0f
        val controller = TestOverscrollEffect(consumePreCycles = true)
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val flingBehavior =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    lastFlingReceived = initialVelocity
                    return initialVelocity
                }
            }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller,
            flingBehavior = flingBehavior
        )

        rule.waitUntil { controller.drawCallsCount == 1 }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(center, centerRight, endVelocity = 3000f)
        }

        rule.runOnIdle {
            assertThat(abs(controller.preFlingVelocity.x)).isWithin(0.1f).of(3000f)
            assertThat(abs(lastFlingReceived)).isWithin(0.1f).of(3000f * 9 / 10)
        }
    }

    @Test
    fun overscrollEffect_scrollable_attemptsToStopAnimation() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val viewConfiguration =
            rule.setOverscrollContentAndReturnViewConfig(
                scrollableState = scrollableState,
                overscrollEffect = controller
            )

        rule.runOnIdle {
            // no down events, hence 0 animation stops
            assertThat(controller.isInProgressCallCount).isEqualTo(0)
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(500f, 0f))
            up()
        }

        val lastAccScroll =
            rule.runOnIdle {
                assertThat(controller.isInProgressCallCount).isEqualTo(1)
                // respect touch slop if overscroll animation is not running
                assertThat(acummulatedScroll).isEqualTo(500f - viewConfiguration.touchSlop)
                // pretend we're settling the overscroll animation
                controller.animationRunning = true
                acummulatedScroll
            }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(500f, 0f))
            up()
        }

        // ignores touch slop if overscroll animation is on progress while pointer goes down
        assertThat(acummulatedScroll - lastAccScroll).isEqualTo(500f)

        rule.runOnIdle { assertThat(controller.isInProgressCallCount).isEqualTo(2) }
    }

    @Test
    fun modifierIsProducingEqualsModifiersForTheSameInput() {
        var overscrollEffect: OverscrollEffect? = null
        rule.setContent {
            overscrollEffect =
                AndroidEdgeEffectOverscrollEffect(
                    LocalView.current.context,
                    LocalDensity.current,
                    OverscrollConfiguration(Color.Gray)
                )
        }

        val first = Modifier.overscroll(overscrollEffect!!)
        val second = Modifier.overscroll(overscrollEffect!!)
        assertThat(first).isEqualTo(second)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.R)
    fun glowOverscroll_doesNotClip() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.padding(10.dp).size(10.dp).overscroll(controller).drawBehind {
                            val extraOffset = 10.dp.roundToPx().toFloat()
                            // Draw a green box over the entire red parent container
                            drawRect(
                                Color.Green,
                                Offset(-extraOffset, -extraOffset),
                                size =
                                    Size(
                                        size.width + extraOffset * 2,
                                        size.height + extraOffset * 2
                                    )
                            )
                        }
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green (no clipping)
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Pull vertically down
        rule.runOnIdle {
            val offset = Offset(x = 0f, y = 50f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // We don't want to assert that the content is entirely green as the glow effect will
        // change this, so instead we assert that no red from the parent box is visible.
        rule.onNodeWithTag(tag).captureToImage().assertHasNoColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun stretchOverscroll_doesNotClipCrossAxis_verticalOverscroll() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.padding(10.dp)
                            .size(10.dp)
                            // Stretch overscroll will apply the stretch to the surrounding canvas,
                            // so add a graphics layer to get a canvas sized to the content. The
                            // expected usage is for this to be clipScrollableContainer() or
                            // similar, since a container that shows overscroll should clip the
                            // main axis, but we don't use that here as we want to test the
                            // implicit clipping behavior.
                            .graphicsLayer()
                            .overscroll(controller)
                            .drawBehind {
                                val extraOffset = 10.dp.roundToPx().toFloat()
                                // Draw a green box over the entire red parent container
                                drawRect(
                                    Color.Green,
                                    Offset(-extraOffset, -extraOffset),
                                    size =
                                        Size(
                                            size.width + extraOffset * 2,
                                            size.height + extraOffset * 2
                                        )
                                )
                            }
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green (no clipping)
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Stretch vertically down
        rule.runOnIdle {
            val offset = Offset(x = 0f, y = 50f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // Overscroll should be clipped vertically (to prevent stretching transparent pixels
        // outside the content), but not horizontally, so (roughly) the top and bottom third should
        // be red, and the center should be green. Because the stretch effect will move this a bit,
        // we instead roughly assert by splitting the bitmap into 9 sections and asserting each
        // center pixel.
        // +---+---+---+
        // | R | R | R |
        // +---+---+---+
        // | G | G | G |
        // +---+---+---+
        // | R | R | R |
        // +---+---+---+
        with(rule.onNodeWithTag(tag).captureToImage().toPixelMap()) {
            // Top left, top middle, top right should be red, as we clip vertically
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 1)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 3, y = (height / 6) * 1)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 1)
            // Middle left, middle, middle right should be green, as we don't clip horizontally
            assertPixelColor(expected = Color.Green, x = (width / 6) * 1, y = (height / 6) * 3)
            assertPixelColor(expected = Color.Green, x = (width / 6) * 3, y = (height / 6) * 3)
            assertPixelColor(expected = Color.Green, x = (width / 6) * 5, y = (height / 6) * 3)
            // Bottom left, bottom middle, bottom right should be red, as we clip vertically
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 5)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 3, y = (height / 6) * 5)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 5)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun stretchOverscroll_doesNotClipCrossAxis_horizontalOverscroll() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.padding(10.dp)
                            .size(10.dp)
                            // Stretch overscroll will apply the stretch to the surrounding canvas,
                            // so add a graphics layer to get a canvas sized to the content. The
                            // expected usage is for this to be clipScrollableContainer() or
                            // similar, since a container that shows overscroll should clip the
                            // main axis, but we don't use that here as we want to test the
                            // implicit clipping behavior.
                            .graphicsLayer()
                            .overscroll(controller)
                            .drawBehind {
                                val extraOffset = 10.dp.roundToPx().toFloat()
                                // Draw a green box over the entire red parent container
                                drawRect(
                                    Color.Green,
                                    Offset(-extraOffset, -extraOffset),
                                    size =
                                        Size(
                                            size.width + extraOffset * 2,
                                            size.height + extraOffset * 2
                                        )
                                )
                            }
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green (no clipping)
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Stretch horizontally right
        rule.runOnIdle {
            val offset = Offset(x = 50f, y = 0f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // Overscroll should be clipped horizontally (to prevent stretching transparent pixels
        // outside the content), but not vertically, so (roughly) the left and right third should
        // be red, and the center should be green. Because the stretch effect will move this a bit,
        // we instead roughly assert by splitting the bitmap into 9 sections and asserting each
        // center pixel.
        // +---+---+---+
        // | R | G | R |
        // +---+---+---+
        // | R | G | R |
        // +---+---+---+
        // | R | G | R |
        // +---+---+---+
        with(rule.onNodeWithTag(tag).captureToImage().toPixelMap()) {
            // Top left, top middle, top right should be red, green, red
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 1)
            assertPixelColor(expected = Color.Green, x = (width / 6) * 3, y = (height / 6) * 1)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 1)
            // Middle left, middle, middle right should be red, green, red
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 3)
            assertPixelColor(expected = Color.Green, x = (width / 6) * 3, y = (height / 6) * 3)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 3)
            // Bottom left, bottom middle, bottom right should be red, green, red
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 5)
            assertPixelColor(expected = Color.Green, x = (width / 6) * 3, y = (height / 6) * 5)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 5)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun stretchOverscroll_clipsBothAxes_overscrollInBothDirections() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.padding(10.dp)
                            .size(10.dp)
                            // Stretch overscroll will apply the stretch to the surrounding canvas,
                            // so add a graphics layer to get a canvas sized to the content. The
                            // expected usage is for this to be clipScrollableContainer() or
                            // similar, since a container that shows overscroll should clip the
                            // main axis, but we don't use that here as we want to test the
                            // implicit clipping behavior.
                            .graphicsLayer()
                            .overscroll(controller)
                            .drawBehind {
                                val extraOffset = 10.dp.roundToPx().toFloat()
                                // Draw a green box over the entire red parent container
                                drawRect(
                                    Color.Green,
                                    Offset(-extraOffset, -extraOffset),
                                    size =
                                        Size(
                                            size.width + extraOffset * 2,
                                            size.height + extraOffset * 2
                                        )
                                )
                            }
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green (no clipping)
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Stretch horizontally and vertically to the bottom right
        rule.runOnIdle {
            val offset = Offset(x = 50f, y = 50f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // Overscroll should be clipped horizontally and vertically to prevent stretching
        // transparent pixels outside the content, so only the center area should be green.
        // Because the stretch effect will move this a bit, we instead roughly assert by
        // splitting the bitmap into 9 sections and asserting each center pixel.
        // +---+---+---+
        // | R | R | R |
        // +---+---+---+
        // | R | G | R |
        // +---+---+---+
        // | R | R | R |
        // +---+---+---+
        with(rule.onNodeWithTag(tag).captureToImage().toPixelMap()) {
            // Top left, top middle, top right should be red
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 1)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 3, y = (height / 6) * 1)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 1)
            // Middle left, middle, middle right should be red, green, red
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 3)
            assertPixelColor(expected = Color.Green, x = (width / 6) * 3, y = (height / 6) * 3)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 3)
            // Bottom left, bottom middle, bottom right should be red
            assertPixelColor(expected = Color.Red, x = (width / 6) * 1, y = (height / 6) * 5)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 3, y = (height / 6) * 5)
            assertPixelColor(expected = Color.Red, x = (width / 6) * 5, y = (height / 6) * 5)
        }
    }

    /**
     * Similar to the tests above, but instead of asserting overall clipping behavior in all axes,
     * this is a specific regression test for b/313463733 to make sure that the stretch isn't
     * 'starting' from out of bound pixels, as this will either cause these pixels to become visible
     * when stretching down, or if there are no pixels (transparent) there, this will cause any
     * background underneath the content to become visible.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun stretchOverscroll_doesNotIncludeUnclippedPixels_verticalOverscroll() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.size(10.dp)
                            // Stretch overscroll will apply the stretch to the surrounding canvas,
                            // so add a graphics layer to get a canvas sized to the content. The
                            // expected usage is for this to be clipScrollableContainer() or
                            // similar, since a container that shows overscroll should clip the
                            // main axis, but we don't use that here as we want to test the
                            // implicit clipping behavior.
                            .graphicsLayer()
                            .overscroll(controller)
                            // This green background will be drawn fully occluding the red
                            // background of the parent box with the same size
                            .background(Color.Green)
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Stretch vertically down
        rule.runOnIdle {
            val offset = Offset(x = 0f, y = 50f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // We don't want to assert that the content is entirely green as the stretch effect will
        // change this a bit, so instead we assert that no red from the parent box is visible.
        rule.onNodeWithTag(tag).captureToImage().assertHasNoColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun stretchOverscroll_doesNotIncludeUnclippedPixels_horizontalOverscroll() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.size(10.dp)
                            // Stretch overscroll will apply the stretch to the surrounding canvas,
                            // so add a graphics layer to get a canvas sized to the content. The
                            // expected usage is for this to be clipScrollableContainer() or
                            // similar, since a container that shows overscroll should clip the
                            // main axis, but we don't use that here as we want to test the
                            // implicit clipping behavior.
                            .graphicsLayer()
                            .overscroll(controller)
                            // This green background will be drawn fully occluding the red
                            // background of the parent box with the same size
                            .background(Color.Green)
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Stretch horizontally right
        rule.runOnIdle {
            val offset = Offset(x = 50f, y = 0f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // We don't want to assert that the content is entirely green as the stretch effect will
        // change this a bit, so instead we assert that no red from the parent box is visible.
        rule.onNodeWithTag(tag).captureToImage().assertHasNoColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun stretchOverscroll_doesNotIncludeUnclippedPixels_overscrollInBothDirections() {
        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier.size(10.dp)
                            // Stretch overscroll will apply the stretch to the surrounding canvas,
                            // so add a graphics layer to get a canvas sized to the content. The
                            // expected usage is for this to be clipScrollableContainer() or
                            // similar, since a container that shows overscroll should clip the
                            // main axis, but we don't use that here as we want to test the
                            // implicit clipping behavior.
                            .graphicsLayer()
                            .overscroll(controller)
                            // This green background will be drawn fully occluding the red
                            // background of the parent box with the same size
                            .background(Color.Green)
                    )
                }
            }
        }

        // Overscroll is not displayed, so the content should be entirely green
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Green }

        // Stretch horizontally and vertically to the bottom right
        rule.runOnIdle {
            val offset = Offset(x = 50f, y = 50f)
            controller.applyToScroll(offset, source = NestedScrollSource.UserInput) { Offset.Zero }
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        // We don't want to assert that the content is entirely green as the stretch effect will
        // change this a bit, so instead we assert that no red from the parent box is visible.
        rule.onNodeWithTag(tag).captureToImage().assertHasNoColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun zeroSizedEffectIsNotConsumingOffsetsAndVelocity() {
        lateinit var effect: OverscrollEffect
        rule.setContent {
            Box {
                effect = rememberOverscrollEffect()!!
                Box(Modifier.overscroll(effect).size(0.dp))
            }
        }

        rule.runOnIdle {
            repeat(2) {
                val offset = Offset(-10f, -10f)
                var offsetConsumed: Offset? = null

                effect.applyToScroll(offset, NestedScrollSource.UserInput) {
                    offsetConsumed = offset - it
                    Offset.Zero
                }
                assertThat(offsetConsumed).isEqualTo(Offset.Zero)
            }
            val velocity = Velocity(-5f, -5f)
            runBlocking {
                var velocityConsumed: Velocity? = null

                effect.applyToFling(velocity) {
                    velocityConsumed = velocity - it
                    Velocity.Zero
                }
                assertThat(velocityConsumed!!).isEqualTo(Velocity.Zero)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun notAttachedEffectIsNotConsumingOffsetsAndVelocity() {
        lateinit var effect: OverscrollEffect
        rule.setContent { effect = rememberOverscrollEffect()!! }

        rule.runOnIdle {
            repeat(2) {
                val offset = Offset(0f, 10f)
                var offsetConsumed: Offset? = null

                effect.applyToScroll(offset, NestedScrollSource.UserInput) {
                    offsetConsumed = offset - it
                    Offset.Zero
                }
                assertThat(offsetConsumed).isEqualTo(Offset.Zero)
            }

            val velocity = Velocity(0f, 5f)
            runBlocking {
                var velocityConsumed: Velocity? = null

                effect.applyToFling(velocity) {
                    velocityConsumed = velocity - it
                    Velocity.Zero
                }
                assertThat(velocityConsumed!!).isEqualTo(Velocity.Zero)
            }
        }
    }

    @Test
    fun horizontalOverscrollEnabled_verifyOverscrollReceivedSingleAxisValues() {
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { 0f }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller
        )

        rule.waitUntil { controller.drawCallsCount == 1 }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(center, centerRight, endVelocity = 3000f)
        }

        rule.runOnIdle {
            with(controller) {
                // presented on consume pre scroll
                assertSingleAxisValue(lastPreScrollDelta.x, lastPreScrollDelta.y)

                // presented on consume post scroll
                assertSingleAxisValue(lastOverscrollDelta.x, lastOverscrollDelta.y)
                assertSingleAxisValue(lastInitialDragDelta.x, lastInitialDragDelta.y)

                // presented on pre fling
                assertSingleAxisValue(preFlingVelocity.x, preFlingVelocity.y)

                // presented on post fling
                assertSingleAxisValue(lastVelocity.x, lastVelocity.y)
            }
        }
    }

    @Test
    fun verticalOverscrollEnabled_verifyOverscrollReceivedSingleAxisValues() {
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { 0f }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller,
            orientation = Orientation.Vertical
        )

        rule.waitUntil { controller.drawCallsCount == 1 }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(center, bottomCenter, endVelocity = 3000f)
        }

        rule.runOnIdle {
            with(controller) {
                // presented on consume pre scroll
                assertSingleAxisValue(lastPreScrollDelta.y, lastPreScrollDelta.x)

                // presented on consume post scroll
                assertSingleAxisValue(lastOverscrollDelta.y, lastOverscrollDelta.x)
                assertSingleAxisValue(lastInitialDragDelta.y, lastInitialDragDelta.x)

                // presented on pre fling
                assertSingleAxisValue(preFlingVelocity.y, preFlingVelocity.x)

                // presented on post fling
                assertSingleAxisValue(lastVelocity.y, lastVelocity.x)
            }
        }
    }

    @Test
    fun verticalOverscrollEnabled_notTriggered_verifyCrossAxisIsCorrectlyPropagated() {
        val controller = TestOverscrollEffect()
        val inspectableConnection = InspectableConnection()
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = ScrollableState { 0f },
            overscrollEffect = controller,
            orientation = Orientation.Vertical,
            inspectableConnection = inspectableConnection
        )

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }

        rule.runOnIdle { assertThat(inspectableConnection.preScrollOffset.x).isEqualTo(0f) }
    }

    @Test
    fun verticalOverscrollEnabled_notTriggered_verifyCrossAxisVelocityIsCorrectlyPropagated() {
        val controller = TestOverscrollEffect()
        val inspectableConnection = InspectableConnection()
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = ScrollableState { 0f },
            overscrollEffect = controller,
            orientation = Orientation.Vertical,
            inspectableConnection = inspectableConnection
        )

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(center, center + Offset(100f, 100f), endVelocity = 1000f)
        }

        rule.runOnIdle { assertThat(inspectableConnection.preScrollVelocity.x).isEqualTo(0) }
    }

    @Test
    fun horizontalOverscrollEnabled_notTriggered_verifyCrossAxisIsCorrectlyPropagated() {
        val controller = TestOverscrollEffect()
        val inspectableConnection = InspectableConnection()
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = ScrollableState { 0f },
            overscrollEffect = controller,
            orientation = Orientation.Horizontal,
            inspectableConnection = inspectableConnection
        )

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }

        rule.runOnIdle { assertThat(inspectableConnection.preScrollOffset.y).isEqualTo(0f) }
    }

    @Test
    fun horizontalOverscrollEnabled_notTriggered_verifyCrossAxisVelocityIsCorrectlyPropagated() {
        val controller = TestOverscrollEffect()
        val inspectableConnection = InspectableConnection()
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = ScrollableState { 0f },
            overscrollEffect = controller,
            orientation = Orientation.Horizontal,
            inspectableConnection = inspectableConnection
        )

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            rule.onNodeWithTag(boxTag).performTouchInput {
                swipeWithVelocity(center, center + Offset(100f, 100f), endVelocity = 1000f)
            }
        }

        rule.runOnIdle { assertThat(inspectableConnection.preScrollVelocity.y).isEqualTo(0) }
    }

    private fun assertSingleAxisValue(mainAxis: Float, crossAxis: Float) {
        assertThat(abs(mainAxis)).isGreaterThan(0)
        assertThat(crossAxis).isEqualTo(0)
    }

    class TestOverscrollEffect(
        private val consumePreCycles: Boolean = false,
        var animationRunning: Boolean = false
    ) : OverscrollEffect {
        var drawCallsCount = 0
        var isInProgressCallCount = 0

        var lastVelocity = Velocity.Zero
        var lastInitialDragDelta = Offset.Zero
        var lastOverscrollDelta = Offset.Zero
        var lastNestedScrollSource: NestedScrollSource? = null

        var lastPreScrollDelta = Offset.Zero
        var preScrollSource: NestedScrollSource? = null

        var preFlingVelocity = Velocity.Zero

        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset
        ): Offset {
            lastPreScrollDelta = delta
            preScrollSource = source

            val consumed = if (consumePreCycles) delta / 10f else Offset.Zero

            val consumedByScroll = performScroll(delta - consumed)

            lastInitialDragDelta = delta
            lastOverscrollDelta = delta - consumedByScroll - consumed
            lastNestedScrollSource = source

            return delta - lastOverscrollDelta
        }

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity
        ) {
            preFlingVelocity = velocity
            val consumed = if (consumePreCycles) velocity / 10f else Velocity.Zero
            performFling(velocity - consumed)
            lastVelocity = velocity
        }

        override val isInProgress: Boolean
            get() {
                isInProgressCallCount += 1
                return animationRunning
            }

        override val effectModifier: Modifier = Modifier.drawBehind { drawCallsCount += 1 }
    }

    fun testDrag(reverseDirection: Boolean) {
        var consumeOnlyHalf = false
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { delta ->
            if (consumeOnlyHalf) {
                delta / 2
            } else {
                delta
            }
        }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller,
            reverseDirection = reverseDirection
        )

        rule.waitUntil { controller.drawCallsCount == 1 }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            assertThat(controller.lastInitialDragDelta.x).isGreaterThan(0f)
            assertThat(controller.lastInitialDragDelta.y).isZero()
            // consuming all, so overscroll is 0
            assertThat(controller.lastOverscrollDelta).isEqualTo(Offset.Zero)
        }

        rule.onNodeWithTag(boxTag).performTouchInput { up() }

        rule.runOnIdle { consumeOnlyHalf = true }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            assertThat(controller.lastInitialDragDelta.x).isGreaterThan(0f)
            assertThat(controller.lastInitialDragDelta.y).isZero()
            assertThat(controller.lastOverscrollDelta.x)
                .isEqualTo(controller.lastInitialDragDelta.x / 2)
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.UserInput)
        }

        rule.onNodeWithTag(boxTag).performTouchInput { up() }

        rule.runOnIdle {
            assertThat(controller.lastVelocity.x).isWithin(0.01f).of(0f)
            assertThat(controller.lastVelocity.y).isWithin(0.01f).of(0f)
        }
    }

    @MediumTest
    @Test
    fun testOverscrollCallbacks_verticalSwipeUp_shouldTriggerCallbacks() {
        val overscrollController = OffsetOverscrollEffectCounter()

        rule.setContent {
            val scrollableState = ScrollableState { delta -> delta }
            Box(Modifier.nestedScroll(NoOpConnection)) {
                Box(
                    Modifier.testTag(boxTag)
                        .size(300.dp)
                        .overscroll(overscrollController)
                        .scrollable(
                            state = scrollableState,
                            orientation = Orientation.Vertical,
                            overscrollEffect = overscrollController,
                            flingBehavior = ScrollableDefaults.flingBehavior()
                        )
                )
            }
        }
        rule.onNodeWithTag(boxTag).performTouchInput { swipeUp(bottom, centerY) }

        rule.runOnIdle {
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
        }
    }

    @MediumTest
    @Test
    fun testOverscrollModifierDrawsOnce() {
        var drawCount = 0
        rule.setContent {
            Spacer(
                modifier =
                    Modifier.testTag(boxTag)
                        .size(100.dp)
                        .overscroll(rememberOverscrollEffect())
                        .drawBehind { drawCount++ }
            )
        }
        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCount == 1 }
    }

    @MediumTest
    @Test
    fun testOverscrollCallbacks_verticalScrollMouse_shouldNotTriggerCallbacks() {
        val overscrollController = OffsetOverscrollEffectCounter()

        rule.setContent {
            val scrollableState = ScrollableState { delta -> delta }
            Box(Modifier.nestedScroll(NoOpConnection)) {
                Box(
                    Modifier.testTag(boxTag)
                        .size(300.dp)
                        .overscroll(overscrollController)
                        .scrollable(
                            state = scrollableState,
                            orientation = Orientation.Vertical,
                            overscrollEffect = overscrollController,
                            flingBehavior = ScrollableDefaults.flingBehavior()
                        )
                )
            }
        }

        // For the mouse scroll to work, you need to
        // - 1. place the test tag directly on the LazyColumn
        // - 2. Call moveTo() before scroll()
        rule.onNodeWithTag(boxTag).performMouseInput {
            moveTo(Offset.Zero)
            scroll(100f)
        }

        rule.runOnIdle {
            assertThat(overscrollController.applyToFlingCount).isEqualTo(0)
            assertThat(overscrollController.applyToScrollCount).isEqualTo(0)
        }
    }
}

private fun ComposeContentTestRule.setOverscrollContentAndReturnViewConfig(
    scrollableState: ScrollableState,
    overscrollEffect: OverscrollEffect,
    flingBehavior: FlingBehavior? = null,
    reverseDirection: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    inspectableConnection: NestedScrollConnection = NoOpConnection
): ViewConfiguration {
    var viewConfiguration: ViewConfiguration? = null
    setContent {
        viewConfiguration = LocalViewConfiguration.current
        Box(Modifier.nestedScroll(inspectableConnection)) {
            Box(
                Modifier.testTag("box")
                    .size(300.dp)
                    .overscroll(overscrollEffect)
                    .scrollable(
                        state = scrollableState,
                        orientation = orientation,
                        overscrollEffect = overscrollEffect,
                        flingBehavior = flingBehavior ?: ScrollableDefaults.flingBehavior(),
                        reverseDirection = reverseDirection
                    )
            )
        }
    }
    return viewConfiguration!!
}

private fun ImageBitmap.assertHasNoColor(color: Color) {
    val pixel = toPixelMap()
    for (x in 0 until width) {
        for (y in 0 until height) {
            assertWithMessage("Pixel at [$x,$y] was equal to $color")
                .that(pixel[x, y])
                .isNotEqualTo(color)
        }
    }
}

private val NoOpConnection = object : NestedScrollConnection {}

private class InspectableConnection : NestedScrollConnection {
    var preScrollOffset = Offset.Zero
    var preScrollVelocity = Velocity.Zero

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        preScrollOffset += available
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        preScrollVelocity += consumed
        preScrollVelocity += available
        return Velocity.Zero
    }
}

// Custom offset overscroll that only counts the number of times each callback is triggered.
private class OffsetOverscrollEffectCounter : OverscrollEffect {
    var applyToScrollCount: Int = 0
        private set

    var applyToFlingCount: Int = 0
        private set

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        applyToScrollCount++
        return Offset.Zero
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        applyToFlingCount++
    }

    override val isInProgress: Boolean = false
    override val effectModifier: Modifier = Modifier.offset { IntOffset(x = 0, y = 0) }
}

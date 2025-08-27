/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material.ripple

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [createRippleModifierNode]. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(
    // Below P the press ripple is split into two layers with half alpha, and we multiply the alpha
    // first so each layer will have the expected alpha to ensure that the minimum contrast in
    // areas where the ripples don't overlap is still correct - as a result the colors aren't
    // exactly what we expect here so we can't really reliably assert
    minSdkVersion = Build.VERSION_CODES.P
)
class RippleModifierNodeTest {

    @get:Rule val rule = createComposeRule()

    private val TestRipple = TestIndicationNodeFactory({ TestRippleColor }, { TestRippleAlpha })

    private class TestIndicationNodeFactory(
        private val color: ColorProducer,
        private val rippleAlpha: () -> RippleAlpha,
    ) : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode {
            return createRippleModifierNode(
                interactionSource = interactionSource,
                bounded = true,
                radius = Dp.Unspecified,
                color = color,
                rippleAlpha = rippleAlpha,
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestIndicationNodeFactory

            if (color != other.color) return false
            if (rippleAlpha !== other.rippleAlpha) return false

            return true
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + rippleAlpha.hashCode()
            return result
        }
    }

    @Test
    fun pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, TestRipple, bounded = true)
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.pressedAlpha,
            )

        assertRippleMatches(
            scope!!,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            expectedColor,
        )
    }

    /** Regression test for b/329693006 */
    @Test
    fun pressed_rippleCreatedBeforeDraw() {
        // Add a static press interaction so that when the ripple is added, it will add a ripple
        // immediately before the node is drawn
        val interactionSource =
            object : MutableInteractionSource {
                override val interactions: Flow<Interaction> =
                    flowOf(PressInteraction.Press(Offset.Zero))

                override suspend fun emit(interaction: Interaction) {}

                override fun tryEmit(interaction: Interaction): Boolean {
                    return true
                }
            }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, TestRipple, bounded = true)
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.pressedAlpha,
            )

        assertRippleMatches(
            scope!!,
            interactionSource,
            // Unused
            PressInteraction.Press(Offset(10f, 10f)),
            expectedColor,
        )
    }

    /**
     * Regression test for b/329693006, similar to [pressed_rippleCreatedBeforeDraw], but delegating
     * to the ripple node later in time to simulate clickable behavior.
     */
    @Test
    fun pressed_rippleLazilyDelegatedTo() {
        // Add a static press interaction so that when the ripple is added, it will add a ripple
        // immediately before the node is drawn
        val interactionSource =
            object : MutableInteractionSource {
                override val interactions: Flow<Interaction> =
                    flowOf(PressInteraction.Press(Offset.Zero))

                override suspend fun emit(interaction: Interaction) {}

                override fun tryEmit(interaction: Interaction): Boolean {
                    return true
                }
            }

        class TestRippleNode : DelegatingNode() {
            fun attachRipple() {
                delegate(TestRipple.create(interactionSource))
            }
        }

        val node = TestRippleNode()

        val element =
            object : ModifierNodeElement<TestRippleNode>() {
                override fun create(): TestRippleNode = node

                override fun update(node: TestRippleNode) {}

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = -1
            }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                    Box(Modifier.padding(25.dp).background(RippleBoxBackgroundColor)) {
                        val shape = RoundedCornerShape(20)
                        val clip = Modifier.clip(shape)
                        Box(
                            Modifier.padding(25.dp)
                                .width(40.dp)
                                .height(40.dp)
                                .border(BorderStroke(2.dp, Color.Black), shape)
                                .background(color = RippleBoxBackgroundColor, shape = shape)
                                .then(clip)
                                .then(element)
                        ) {}
                    }
                }
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.pressedAlpha,
            )

        // Add the ripple node to the hierarchy, which should then create a ripple before the node
        // has been drawn
        rule.runOnIdle { node.attachRipple() }

        assertRippleMatches(
            scope!!,
            interactionSource,
            // Unused
            PressInteraction.Press(Offset(10f, 10f)),
            expectedColor,
        )
    }

    @Test
    fun hovered() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, TestRipple, bounded = true)
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.hoveredAlpha,
            )

        assertRippleMatches(scope!!, interactionSource, HoverInteraction.Enter(), expectedColor)
    }

    @Test
    fun focused() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, TestRipple, bounded = true)
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.focusedAlpha,
            )

        assertRippleMatches(scope!!, interactionSource, FocusInteraction.Focus(), expectedColor)
    }

    @Test
    fun focusedHoveredThenUnhovered() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, TestRipple, bounded = true)
            }
        }

        scope!!

        // Focus
        assertRippleMatches(
            scope,
            interactionSource = interactionSource,
            interaction = FocusInteraction.Focus(),
            expectedCenterPixelColor =
                calculateResultingRippleColor(
                    TestRippleColor,
                    rippleOpacity = TestRippleAlpha.focusedAlpha,
                ),
        )

        // Hover
        val enter = HoverInteraction.Enter()
        assertRippleMatches(
            scope,
            interactionSource = interactionSource,
            interaction = enter,
            expectedCenterPixelColor =
                calculateResultingRippleColor(
                    TestRippleColor,
                    rippleOpacity = TestRippleAlpha.hoveredAlpha,
                ),
        )

        // Unhover
        assertRippleMatches(
            scope,
            interactionSource = interactionSource,
            interaction = HoverInteraction.Exit(enter),
            expectedCenterPixelColor =
                calculateResultingRippleColor(
                    TestRippleColor,
                    rippleOpacity = TestRippleAlpha.focusedAlpha,
                ),
        )
    }

    @Test
    fun dragged() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, TestRipple, bounded = true)
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.draggedAlpha,
            )

        assertRippleMatches(scope!!, interactionSource, DragInteraction.Start(), expectedColor)
    }

    /**
     * Test case for changing a color captured by the color lambda during an existing ripple effect
     *
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * color of currently active ripples unless they are being drawn on the UI thread (which should
     * only happen if the target radius also changes, which only happens for a bounds change when
     * the ripple radius is calculated by the framework).
     */
    @Test
    fun colorChangeDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        val initialColor = Color.Red
        var themeColor by mutableStateOf(initialColor)

        var scope: CoroutineScope? = null

        val ripple = TestIndicationNodeFactory({ themeColor }, { TestRippleAlpha })

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, ripple, bounded = true)
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(
                    initialColor,
                    rippleOpacity = TestRippleAlpha.draggedAlpha,
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newColor = Color.Green

        rule.runOnUiThread { themeColor = newColor }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(
                    newColor,
                    rippleOpacity = TestRippleAlpha.draggedAlpha,
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    /**
     * Test case for changing the ripple alpha captured by the ripple alpha lambda. Currently this
     * is only reflected when moving to a new state layer, we don't dynamically retarget existing
     * interactions (this should be a very rare case).
     *
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * alpha of currently active ripples unless they are being drawn on the UI thread (which should
     * only happen if the target radius also changes).
     */
    @Test
    fun rippleAlphaChange() {
        val interactionSource = MutableInteractionSource()

        var rippleAlpha by mutableStateOf(TestRippleAlpha)

        var scope: CoroutineScope? = null

        val ripple = TestIndicationNodeFactory({ TestRippleColor }, { rippleAlpha })

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, ripple, bounded = true)
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(
                    TestRippleColor,
                    rippleOpacity = TestRippleAlpha.draggedAlpha,
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newRippleAlpha = RippleAlpha(0.5f, 0.5f, 0.5f, 0.5f)

        rule.runOnUiThread { rippleAlpha = newRippleAlpha }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // Alpha shouldn't have changed, since we don't retarget existing interactions
            val expectedColor =
                calculateResultingRippleColor(
                    TestRippleColor,
                    rippleOpacity = TestRippleAlpha.draggedAlpha,
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(FocusInteraction.Focus()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // Now that we animate to a new state layer, the new ripple alpha should be used
            val expectedColor =
                calculateResultingRippleColor(
                    TestRippleColor,
                    rippleOpacity = newRippleAlpha.focusedAlpha,
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    /**
     * Test case for increasing the bounds of a ripple while pressed. Above S, the ripple should
     * expand to fill the expanding bounds, even though the radius was initially calculated with the
     * original smaller bounds.
     *
     * Note: no corresponding test for bounds decreasing, since there is no issue in such a case:
     * the radius is already big enough to fill the smaller space.
     */
    // Below S bounds changes won't update an existing ripple with an explicitly set radius, so the
    // ripple will not expand - we can only support this functionality on S+.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun boundsIncreaseDuringRipple_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        var size by mutableStateOf(100)

        val ripple = TestIndicationNodeFactory({ TestRippleColor }, { TestRippleAlpha })

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(
                Modifier.fillMaxSize().background(RippleBoxBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(with(LocalDensity.current) { size.toDp() })
                        .semantics(mergeDescendants = true) {}
                        .testTag(Tag)
                        .clip(RectangleShape)
                        .indication(interactionSource = interactionSource, indication = ripple)
                )
            }
        }

        rule.runOnIdle {
            scope!!.launch { interactionSource.emit(PressInteraction.Press(Offset(10f, 10f))) }
        }
        rule.waitForIdle()

        @Suppress("BanThreadSleep")
        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        fun assertPixelColors(expectedSize: Int) {
            with(rule.onNodeWithTag(Tag)) {
                val bitmap = captureToImage().asAndroidBitmap()
                Truth.assertThat(bitmap.width).isEqualTo(expectedSize)
                Truth.assertThat(bitmap.height).isEqualTo(expectedSize)
                with(bitmap) {
                    val center = Color(getPixel(width / 2, height / 2))
                    val topLeft = Color(getPixel(20, 20))
                    val topRight = Color(getPixel(width - 20, 20))
                    val bottomLeft = Color(getPixel(20, height - 20))
                    val bottomRight = Color(getPixel(width - 20, height - 20))

                    // On S and above, the press ripple is patterned and has inconsistent behaviour
                    // in terms of alpha, so it doesn't behave according to our expectations - we
                    // can't explicitly assert on the color. Instead we just assert that it is not
                    // the background color, to make sure that the ripple is rendering something
                    // over the whole background.
                    Truth.assertThat(center).isNotEqualTo(RippleBoxBackgroundColor)
                    // Important to assert the corners, as that is where the ripple needs to expand
                    // to fill.
                    Truth.assertThat(topLeft).isNotEqualTo(RippleBoxBackgroundColor)
                    Truth.assertThat(topRight).isNotEqualTo(RippleBoxBackgroundColor)
                    Truth.assertThat(bottomLeft).isNotEqualTo(RippleBoxBackgroundColor)
                    Truth.assertThat(bottomRight).isNotEqualTo(RippleBoxBackgroundColor)
                }
            }
        }

        assertPixelColors(size)

        val newSize = 200

        rule.runOnUiThread { size = newSize }
        rule.waitForIdle()

        assertPixelColors(newSize)
    }

    /**
     * Test case for increasing the bounds of a ripple while dragged - the state layer should fill
     * the new bounds.
     */
    @Test
    fun boundsIncreaseDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        var size by mutableStateOf(100)

        val ripple = TestIndicationNodeFactory({ TestRippleColor }, { TestRippleAlpha })

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(
                Modifier.fillMaxSize().background(RippleBoxBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(with(LocalDensity.current) { size.toDp() })
                        .semantics(mergeDescendants = true) {}
                        .testTag(Tag)
                        .clip(RectangleShape)
                        .indication(interactionSource = interactionSource, indication = ripple)
                )
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        fun assertPixelColors(expectedSize: Int) {
            with(rule.onNodeWithTag(Tag)) {
                val bitmap = captureToImage().asAndroidBitmap()
                Truth.assertThat(bitmap.width).isEqualTo(expectedSize)
                Truth.assertThat(bitmap.height).isEqualTo(expectedSize)
                with(bitmap) {
                    val center = Color(getPixel(width / 2, height / 2))
                    val topLeft = Color(getPixel(20, 20))
                    val topRight = Color(getPixel(width - 20, 20))
                    val bottomLeft = Color(getPixel(20, height - 20))
                    val bottomRight = Color(getPixel(width - 20, height - 20))

                    val expectedColor =
                        calculateResultingRippleColor(
                            TestRippleColor,
                            rippleOpacity = TestRippleAlpha.draggedAlpha,
                        )

                    Truth.assertThat(center).isEqualTo(expectedColor)
                    Truth.assertThat(topLeft).isEqualTo(expectedColor)
                    Truth.assertThat(topRight).isEqualTo(expectedColor)
                    Truth.assertThat(bottomLeft).isEqualTo(expectedColor)
                    Truth.assertThat(bottomRight).isEqualTo(expectedColor)
                }
            }
        }

        assertPixelColors(size)

        val newSize = 200

        rule.runOnUiThread { size = newSize }
        rule.waitForIdle()

        assertPixelColors(newSize)
    }

    /**
     * Asserts that the resultant color of the ripple on screen matches [expectedCenterPixelColor].
     *
     * @param interactionSource the [MutableInteractionSource] driving the ripple
     * @param interaction the [Interaction] to assert for
     * @param expectedCenterPixelColor the expected color for the pixel at the center of the
     *   [RippleBoxWithBackground]
     */
    private fun assertRippleMatches(
        scope: CoroutineScope,
        interactionSource: MutableInteractionSource,
        interaction: Interaction,
        expectedCenterPixelColor: Color,
    ) {
        // Pause the clock if we are drawing a state layer
        if (interaction !is PressInteraction) {
            rule.mainClock.autoAdvance = false
        }

        // Start ripple
        rule.runOnIdle { scope.launch { interactionSource.emit(interaction) } }

        // Advance to the end of the ripple / state layer animation
        rule.waitForIdle()
        @Suppress("BanThreadSleep")
        if (interaction is PressInteraction) {
            // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
            // synchronization. Instead just wait until after the ripples are finished animating.
            Thread.sleep(300)
        } else {
            rule.mainClock.advanceTimeBy(milliseconds = 300)
        }

        // Compare expected and actual pixel color
        val centerPixel =
            Color(
                rule.onNodeWithTag(Tag).captureToImage().asAndroidBitmap().run {
                    getPixel(width / 2, height / 2)
                }
            )

        // On S and above, the press ripple is patterned and has inconsistent behaviour in terms of
        // alpha, so it doesn't behave according to our expectations - we can't explicitly assert on
        // the color. Instead we just assert that it is not the background color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && interaction is PressInteraction) {
            Truth.assertThat(centerPixel).isNotEqualTo(RippleBoxBackgroundColor)
        } else {
            Truth.assertThat(centerPixel).isEqualTo(expectedCenterPixelColor)
        }
    }
}

/**
 * Generic Button like component with a border that allows injecting an [Indication], and has a
 * background with the same color around it - this makes the ripple contrast better and make it more
 * visible in screenshots.
 *
 * @param interactionSource the [MutableInteractionSource] that is used to drive the ripple state
 * @param ripple ripple [Indication] placed inside the surface
 * @param bounded whether [ripple] is bounded or not - this controls the clipping behavior
 */
@Composable
private fun RippleBoxWithBackground(
    interactionSource: MutableInteractionSource,
    ripple: Indication,
    bounded: Boolean,
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        Box(Modifier.padding(25.dp).background(RippleBoxBackgroundColor)) {
            val shape = RoundedCornerShape(20)
            // If the ripple is bounded, we want to clip to the shape, otherwise don't clip as
            // the ripple should draw outside the bounds.
            val clip = if (bounded) Modifier.clip(shape) else Modifier
            Box(
                Modifier.padding(25.dp)
                    .width(40.dp)
                    .height(40.dp)
                    .border(BorderStroke(2.dp, Color.Black), shape)
                    .background(color = RippleBoxBackgroundColor, shape = shape)
                    .then(clip)
                    .indication(interactionSource = interactionSource, indication = ripple)
            ) {}
        }
    }
}

/**
 * Blends ([contentColor] with [rippleOpacity]) on top of [RippleBoxBackgroundColor] to provide the
 * resulting RGB color that can be used for pixel comparison.
 */
private fun calculateResultingRippleColor(contentColor: Color, rippleOpacity: Float) =
    contentColor.copy(alpha = rippleOpacity).compositeOver(RippleBoxBackgroundColor)

private val TestRippleColor = Color.Red

private val TestRippleAlpha =
    RippleAlpha(draggedAlpha = 0.1f, focusedAlpha = 0.2f, hoveredAlpha = 0.3f, pressedAlpha = 0.4f)

private val RippleBoxBackgroundColor = Color.White

private const val Tag = "Ripple"

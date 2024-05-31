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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for (the deprecated) [rememberRipple] and [RippleTheme] APIs to ensure no regressions. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(
    // Below P the press ripple is split into two layers with half alpha, and we multiply the alpha
    // first so each layer will have the expected alpha to ensure that the minimum contrast in
    // areas where the ripples don't overlap is still correct - as a result the colors aren't
    // exactly what we expect here so we can't really reliably assert
    minSdkVersion = Build.VERSION_CODES.P,
    // On S and above, the press ripple is patterned and has inconsistent behaviour in terms of
    // alpha, so it doesn't behave according to our expectations - we can't explicitly assert on the
    // color.
    maxSdkVersion = Build.VERSION_CODES.R
)
@Suppress("DEPRECATION_ERROR")
class RememberRippleTest {

    @get:Rule val rule = createComposeRule()

    private val TestRippleColor = Color.Red

    private val TestRippleAlpha =
        RippleAlpha(
            draggedAlpha = 0.1f,
            focusedAlpha = 0.2f,
            hoveredAlpha = 0.3f,
            pressedAlpha = 0.4f
        )

    private val TestRippleTheme =
        object : RippleTheme {
            @Deprecated("Super method is deprecated")
            @Composable
            override fun defaultColor() = TestRippleColor

            @Deprecated("Super method is deprecated")
            @Composable
            override fun rippleAlpha() = TestRippleAlpha
        }

    @Test
    fun pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalRippleTheme provides TestRippleTheme) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(), bounded = true)
                }
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.pressedAlpha
            )

        assertRippleMatches(
            scope!!,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            expectedColor
        )
    }

    @Test
    fun hovered() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalRippleTheme provides TestRippleTheme) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(), bounded = true)
                }
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.hoveredAlpha
            )

        assertRippleMatches(scope!!, interactionSource, HoverInteraction.Enter(), expectedColor)
    }

    @Test
    fun focused() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalRippleTheme provides TestRippleTheme) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(), bounded = true)
                }
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.focusedAlpha
            )

        assertRippleMatches(scope!!, interactionSource, FocusInteraction.Focus(), expectedColor)
    }

    @Test
    fun dragged() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalRippleTheme provides TestRippleTheme) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(), bounded = true)
                }
            }
        }

        val expectedColor =
            calculateResultingRippleColor(
                TestRippleColor,
                rippleOpacity = TestRippleAlpha.draggedAlpha
            )

        assertRippleMatches(scope!!, interactionSource, DragInteraction.Start(), expectedColor)
    }

    /**
     * Test case for changing LocalRippleTheme during an existing ripple effect
     *
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * color of currently active ripples unless they are being drawn on the UI thread (which should
     * only happen if the target radius also changes).
     */
    @Test
    fun themeChangeDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        fun createRippleTheme(color: Color, alpha: Float) =
            object : RippleTheme {
                val rippleAlpha = RippleAlpha(alpha, alpha, alpha, alpha)

                @Deprecated("Super method is deprecated")
                @Composable
                override fun defaultColor() = color

                @Deprecated("Super method is deprecated")
                @Composable
                override fun rippleAlpha() = rippleAlpha
            }

        val initialColor = Color.Red
        val initialAlpha = 0.5f

        var rippleTheme by mutableStateOf(createRippleTheme(initialColor, initialAlpha))

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalRippleTheme provides rippleTheme) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(), bounded = true)
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(initialColor, rippleOpacity = initialAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newColor = Color.Green
        // TODO: changing alpha for existing state layers is not currently supported
        val newAlpha = 0.5f

        rule.runOnUiThread { rippleTheme = createRippleTheme(newColor, newAlpha) }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor = calculateResultingRippleColor(newColor, rippleOpacity = newAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    /**
     * Test case for changing a CompositionLocal consumed by the RippleTheme during an existing
     * ripple effect
     *
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * color of currently active ripples unless they are being drawn on the UI thread (which should
     * only happen if the target radius also changes).
     */
    @Test
    fun compositionLocalChangeDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        val initialColor = Color.Red
        var themeColor by mutableStateOf(initialColor)

        val expectedAlpha = 0.5f
        val rippleAlpha = RippleAlpha(expectedAlpha, expectedAlpha, expectedAlpha, expectedAlpha)

        val localThemeColor = compositionLocalOf { Color.Unspecified }

        val rippleTheme =
            object : RippleTheme {
                @Deprecated("Super method is deprecated")
                @Composable
                override fun defaultColor() = localThemeColor.current

                @Deprecated("Super method is deprecated")
                @Composable
                override fun rippleAlpha() = rippleAlpha
            }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(
                LocalRippleTheme provides rippleTheme,
                localThemeColor provides themeColor
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(), bounded = true)
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(initialColor, rippleOpacity = expectedAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newColor = Color.Green

        rule.runOnUiThread { themeColor = newColor }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(newColor, rippleOpacity = expectedAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    /**
     * Test case to ensure that CompositionLocals are queried at the place where the ripple is
     * consumed, not where it is created (i.e, inside the component applying indication).
     */
    @Test
    fun compositionLocalProvidedAfterRipple() {
        val interactionSource = MutableInteractionSource()

        val alpha = 0.5f
        val rippleAlpha = RippleAlpha(alpha, alpha, alpha, alpha)
        val expectedRippleColor = Color.Red

        val localThemeColor = compositionLocalOf { Color.Unspecified }

        val rippleTheme =
            object : RippleTheme {
                @Deprecated("Super method is deprecated")
                @Composable
                override fun defaultColor() = localThemeColor.current

                @Deprecated("Super method is deprecated")
                @Composable
                override fun rippleAlpha() = rippleAlpha
            }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(
                LocalRippleTheme provides rippleTheme,
                localThemeColor provides Color.Black
            ) {
                // Create ripple where localThemeColor is black
                val ripple = rememberRipple()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(localThemeColor provides expectedRippleColor) {
                        // Ripple is used where localThemeColor is red, so the instance
                        // should get the red color when it is created
                        RippleBoxWithBackground(interactionSource, ripple, bounded = true)
                    }
                }
            }
        }

        rule.runOnIdle {
            scope!!.launch { interactionSource.emit(PressInteraction.Press(Offset(10f, 10f))) }
        }

        rule.waitForIdle()
        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        @Suppress("BanThreadSleep") Thread.sleep(300)

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(expectedRippleColor, rippleOpacity = alpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
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
        expectedCenterPixelColor: Color
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
            rule.onNodeWithTag(Tag).captureToImage().asAndroidBitmap().run {
                getPixel(width / 2, height / 2)
            }

        Truth.assertThat(Color(centerPixel)).isEqualTo(expectedCenterPixelColor)
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
    bounded: Boolean
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

private val RippleBoxBackgroundColor = Color.Blue

private const val Tag = "Ripple"

/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material

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
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.test.junit4.ComposeContentTestRule
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

/** Test for [ripple], to verify colors and opacity in different configurations. */
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
class RippleTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun bounded_lightTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun bounded_lightTheme_highLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_lightTheme_highLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun bounded_lightTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.04f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.04f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.04f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = true,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.04f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = true,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.04f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_hovered() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            HoverInteraction.Enter(),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.04f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_focused() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            FocusInteraction.Focus(),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope =
            rule.setRippleContent(
                interactionSource = interactionSource,
                bounded = false,
                lightTheme = false,
                contentColor = contentColor
            )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.08f)
        )
    }

    /**
     * Test case for changing content color during an existing ripple effect
     *
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * color of currently active ripples unless they are being drawn on the UI thread (which should
     * only happen if the target radius also changes).
     */
    @Test
    fun contentColorChangeDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        val initialColor = Color.Red
        var contentColor by mutableStateOf(initialColor)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface(contentColor = contentColor) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        RippleBoxWithBackground(interactionSource, ripple(), bounded = true)
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor = calculateResultingRippleColor(initialColor, rippleOpacity = 0.08f)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newColor = Color.Green

        rule.runOnUiThread { contentColor = newColor }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor = calculateResultingRippleColor(newColor, rippleOpacity = 0.08f)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    @Test
    fun rippleConfiguration_color_dragged() {
        val interactionSource = MutableInteractionSource()

        val rippleConfiguration = RippleConfiguration(color = Color.Red)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface {
                    CompositionLocalProvider(
                        LocalRippleConfiguration provides rippleConfiguration
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBoxWithBackground(interactionSource, ripple(), bounded = true)
                        }
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(
                    // Color from the ripple configuration should be used
                    rippleConfiguration.color,
                    // Default alpha should be used
                    rippleOpacity = 0.08f
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    @Test
    fun rippleConfiguration_color_explicitColorSet_dragged() {
        val interactionSource = MutableInteractionSource()

        val rippleConfiguration = RippleConfiguration(color = Color.Red)

        val explicitColor = Color.Green

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface {
                    CompositionLocalProvider(
                        LocalRippleConfiguration provides rippleConfiguration
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBoxWithBackground(
                                interactionSource,
                                ripple(color = explicitColor),
                                bounded = true
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(
                    // Color explicitly set on the ripple should 'win' over the configuration color
                    explicitColor,
                    // Default alpha should be used
                    rippleOpacity = 0.08f
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    @Test
    fun rippleConfiguration_alpha_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val rippleConfiguration =
            RippleConfiguration(rippleAlpha = RippleAlpha(0.5f, 0.5f, 0.5f, 0.5f))

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface(contentColor = contentColor) {
                    CompositionLocalProvider(
                        LocalRippleConfiguration provides rippleConfiguration
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBoxWithBackground(interactionSource, ripple(), bounded = true)
                        }
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor =
                calculateResultingRippleColor(
                    // Default color should be used
                    contentColor,
                    // Alpha from the ripple configuration should be used
                    rippleOpacity = rippleConfiguration.rippleAlpha!!.draggedAlpha
                )

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    @Test
    fun rippleConfiguration_disabled_dragged() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface {
                    CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBoxWithBackground(interactionSource, ripple(), bounded = true)
                        }
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // No ripple should be showing
            Truth.assertThat(Color(centerPixel)).isEqualTo(RippleBoxBackgroundColor)
        }
    }

    /**
     * Test case for changing RippleConfiguration during an existing ripple effect
     *
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * color of currently active ripples unless they are being drawn on the UI thread (which should
     * only happen if the target radius also changes).
     */
    @Test
    fun rippleConfigurationChangeDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        var rippleConfiguration: RippleConfiguration? by mutableStateOf(RippleConfiguration())

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface(contentColor = contentColor) {
                    CompositionLocalProvider(
                        LocalRippleConfiguration provides rippleConfiguration
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBoxWithBackground(interactionSource, ripple(), bounded = true)
                        }
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // Ripple should use default values
            val expectedColor = calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newConfiguration =
            RippleConfiguration(
                color = Color.Red,
                rippleAlpha = RippleAlpha(0.5f, 0.5f, 0.5f, 0.5f)
            )

        rule.runOnUiThread { rippleConfiguration = newConfiguration }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // The ripple should now use the new configuration value for color. Ripple alpha
            // is not currently updated during an existing effect, so it should still use the old
            // value.
            val expectedColor =
                calculateResultingRippleColor(newConfiguration.color, rippleOpacity = 0.08f)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        rule.runOnUiThread { rippleConfiguration = null }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // The ripple should now be removed
            Truth.assertThat(Color(centerPixel)).isEqualTo(RippleBoxBackgroundColor)
        }
    }

    /**
     * Regression test for b/348379457 : going from enabled -> disabled -> enabled should show a
     * valid ripple, and going to disabled after that should not crash.
     */
    @Test
    fun rippleConfigurationToggleBetweenEnabledAndDisabled() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        var rippleConfiguration: RippleConfiguration? by mutableStateOf(RippleConfiguration())
        val dragStart1 = DragInteraction.Start()
        val dragStop1 = DragInteraction.Stop(dragStart1)
        val dragStart2 = DragInteraction.Start()
        val dragStop2 = DragInteraction.Stop(dragStart2)
        val dragStart3 = DragInteraction.Start()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                Surface(contentColor = contentColor) {
                    CompositionLocalProvider(
                        LocalRippleConfiguration provides rippleConfiguration
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBoxWithBackground(interactionSource, ripple(), bounded = true)
                        }
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(dragStart1) } }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor = calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(dragStop1) } }
        // Disable the ripple
        rule.runOnIdle { rippleConfiguration = null }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(dragStart2) } }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            // There should not be a ripple
            Truth.assertThat(Color(centerPixel)).isEqualTo(RippleBoxBackgroundColor)
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(dragStop2) } }
        // Enable the ripple again
        rule.runOnIdle { rippleConfiguration = RippleConfiguration() }

        // The ripple should show again
        rule.runOnIdle { scope!!.launch { interactionSource.emit(dragStart3) } }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            val expectedColor = calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        // Disable the ripple again
        rule.runOnIdle { rippleConfiguration = null }
        // Should not crash
        rule.waitForIdle()
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
        Surface(Modifier.padding(25.dp), color = RippleBoxBackgroundColor) {
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
 * Sets the content to a [RippleBoxWithBackground] with a [MaterialTheme] and surrounding [Surface]
 *
 * @param interactionSource [MutableInteractionSource] used to drive the ripple inside the
 *   [RippleBoxWithBackground]
 * @param bounded whether the ripple inside the [RippleBoxWithBackground] is bounded
 * @param lightTheme whether the theme is light or dark
 * @param contentColor the contentColor that will be used for the ripple color
 */
private fun ComposeContentTestRule.setRippleContent(
    interactionSource: MutableInteractionSource,
    bounded: Boolean,
    lightTheme: Boolean,
    contentColor: Color
): CoroutineScope {
    var scope: CoroutineScope? = null

    setContent {
        scope = rememberCoroutineScope()
        val colors = if (lightTheme) lightColors() else darkColors()

        MaterialTheme(colors) {
            Surface(contentColor = contentColor) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, ripple(bounded), bounded)
                }
            }
        }
    }
    waitForIdle()
    return scope!!
}

/**
 * Blends ([contentColor] with [rippleOpacity]) on top of [RippleBoxBackgroundColor] to provide the
 * resulting RGB color that can be used for pixel comparison.
 */
private fun calculateResultingRippleColor(contentColor: Color, rippleOpacity: Float) =
    contentColor.copy(alpha = rippleOpacity).compositeOver(RippleBoxBackgroundColor)

private val RippleBoxBackgroundColor = Color.Blue

private const val Tag = "Ripple"

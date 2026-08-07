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

package androidx.xr.glimmer

import android.os.Build
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.internal.color.withToneAndChroma
import androidx.xr.glimmer.testutils.assertGlimmerSurfaceShape
import androidx.xr.glimmer.testutils.captureToImage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ToggleButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun toggleButton_defaultColors() {
        rule.setGlimmerThemeContent {
            val colors = ToggleButtonDefaults.colors()
            assertThat(colors.backgroundColor).isEqualTo(GlimmerTheme.colors.surface)
            assertThat(colors.checkedBackgroundColor)
                .isEqualTo(
                    GlimmerTheme.colors.primary.withToneAndChroma(newTone = 70f, newChroma = 50f)
                )
        }
    }

    @Test
    fun toggleButton_semantics() {
        val checked = mutableStateOf(false)

        rule.setGlimmerThemeContent {
            ToggleButton(
                modifier = Modifier.testTag("toggle_button"),
                checked = checked.value,
                onCheckedChange = {},
            ) {
                Text("Toggle")
            }
        }

        rule
            .onNodeWithTag("toggle_button")
            .assertToggleableSemantics(checked = false)
            .assertIsEnabled()

        // Toggle the button.
        checked.value = true
        rule.waitForIdle()

        rule
            .onNodeWithTag("toggle_button")
            .assertToggleableSemantics(checked = true)
            .assertIsEnabled()
    }

    @Test
    fun toggleButton_findByTextAndClick() {
        var checked by mutableStateOf(false)
        rule.setGlimmerThemeContent {
            ToggleButton(checked = checked, onCheckedChange = { checked = it }) { Text("Toggle") }
        }

        rule.onNodeWithText("Toggle").performClick()
        rule.runOnIdle { assertThat(checked).isTrue() }
        rule.onNodeWithText("Toggle").performClick()
        rule.runOnIdle { assertThat(checked).isFalse() }
    }

    @Test
    fun toggleButton_changesShapeAndColor_whenCheckedStateChanges() {
        lateinit var expectedUncheckedShape: Shape
        val checked = mutableStateOf(false)

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            expectedUncheckedShape = GlimmerTheme.shapes.large
            Box {
                ToggleButton(
                    checked = checked.value,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("toggle_button"),
                ) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        // Unchecked state.
        rule
            .onNodeWithTag("toggle_button")
            .captureToImage()
            .assertGlimmerSurfaceShape(
                density = rule.density,
                shape = expectedUncheckedShape,
                backgroundColor = Color.Black,
            )

        // Toggle the button.
        checked.value = true
        rule.waitForIdle()

        // Checked state.
        rule
            .onNodeWithTag("toggle_button")
            .captureToImage()
            .assertGlimmerSurfaceShape(
                density = rule.density,
                shape = ToggleButtonDefaults.CheckedShape,
                backgroundColor = Color.Black,
            )
    }

    @Test
    fun toggleButton_changesCustomShapeAndColor_whenCheckedStateChanges() {
        val checked = mutableStateOf(false)
        val expectedCheckedShape = RoundedCornerShape(1)
        val expectedUncheckedShape = RoundedCornerShape(20)
        val expectedColors =
            ToggleButtonColors(
                backgroundColor = Color.Red,
                checkedBackgroundColor = Color.Green,
                contentColor = Color.Black,
                checkedContentColor = Color.Black,
            )

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Box {
                ToggleButton(
                    checked = checked.value,
                    colors = expectedColors,
                    shape =
                        ToggleButtonDefaults.shape(
                            checked = checked.value,
                            checkedShape = expectedCheckedShape,
                            uncheckedShape = expectedUncheckedShape,
                        ),
                    onCheckedChange = {},
                    modifier = Modifier.testTag("icon_toggle_button"),
                ) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        // Unchecked state (Red background).
        val uncheckedImage = rule.onNodeWithTag("icon_toggle_button").captureToImage()
        uncheckedImage.assertGlimmerSurfaceShape(
            density = rule.density,
            shape = expectedUncheckedShape,
            backgroundColor = Color.Black,
        )
        val uncheckedCenterColor = uncheckedImage.toPixelMap().run { get(width / 2, height / 2) }
        assertThat(uncheckedCenterColor).isEqualTo(expectedColors.backgroundColor)

        // Toggle the button.
        checked.value = true
        rule.waitForIdle()

        // Checked state (Green background).
        val checkedImage = rule.onNodeWithTag("icon_toggle_button").captureToImage()
        checkedImage.assertGlimmerSurfaceShape(
            density = rule.density,
            shape = expectedCheckedShape,
            backgroundColor = Color.Black,
        )
        val checkedCenterColor = checkedImage.toPixelMap().run { get(width / 2, height / 2) }
        assertThat(checkedCenterColor).isEqualTo(expectedColors.checkedBackgroundColor)
    }

    @Test
    fun toggleButton_defaultAnimation_graduallyChangesCornerSizes() {
        rule.mainClock.autoAdvance = false
        val checked = mutableStateOf(false)
        val checkedBackgroundColor = Color.Red
        val density = Density(1f)

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true, density = density) {
            ToggleButton(
                checked = checked.value,
                onCheckedChange = {},
                colors =
                    ToggleButtonDefaults.colors(checkedBackgroundColor = checkedBackgroundColor),
                modifier = Modifier.testTag("icon_toggle_button"),
            ) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }

        // Use the spec to find the animation's progress based on time.
        val initialValue = AnimationVector1D(0f)
        val targetValue = AnimationVector1D(1f)
        val initialVelocity = AnimationVector1D(0f)
        val vectorizedSpec = ToggleButtonAnimationSpec.vectorize(Float.VectorConverter)
        // Spring animation is not time based, so its total time can't be set explicitly.
        val totalAnimationDuration =
            vectorizedSpec.getDurationNanos(
                initialValue = initialValue,
                targetValue = targetValue,
                initialVelocity = initialVelocity,
            )
        // Usually spring animation hits the 50% value mark at 30% of the total animation time.
        val testAnimationDurationNs = (totalAnimationDuration * 0.30).toLong()
        val testAnimationValue =
            vectorizedSpec.getValueFromNanos(
                playTimeNanos = testAnimationDurationNs,
                initialValue = initialValue,
                targetValue = targetValue,
                initialVelocity = initialVelocity,
            )
        // Sanity check to make sure the value actually gets animated.
        assertThat(testAnimationValue.value).isNotEqualTo(0f)
        assertThat(testAnimationValue.value).isNotEqualTo(1f)

        // Start the animation and advance the time to the pre-calculated value.
        checked.value = true
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(testAnimationDurationNs / 1_000_000)

        // Calculate intermediate shape based on pre-calculated animation value.
        val uncheckedShape = RoundedCornerShape(UncheckedCornerSize)
        val checkedShape = RoundedCornerShape(CheckedCornerSize)
        val expectedShape =
            uncheckedShape.lerp(checkedShape, testAnimationValue.value) as RoundedCornerShape

        rule
            .onNodeWithTag("icon_toggle_button")
            .captureToImage()
            .assertGlimmerSurfaceShape(
                density = density,
                shape = expectedShape,
                backgroundColor = Color.Black,
            )
    }

    @Test
    fun toggleButton_updatesColor_whenThemePrimaryColorChanges() {
        var themeColors by mutableStateOf(Colors(primary = Color.Red))
        rule.setContent {
            GlimmerTheme(colors = themeColors) {
                ToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    modifier = Modifier.size(100.dp).testTag("toggle_button"),
                ) {
                    Text("Toggle")
                }
            }
        }

        val expectedRed = Color.Red.withToneAndChroma(newTone = 70f, newChroma = 50f)
        rule.onNodeWithTag("toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 8, height / 2)).isEqualTo(expectedRed)
        }

        themeColors = Colors(primary = Color.Blue)
        rule.waitForIdle()

        val expectedBlue = Color.Blue.withToneAndChroma(newTone = 70f, newChroma = 50f)
        rule.onNodeWithTag("toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 8, height / 2)).isEqualTo(expectedBlue)
        }
    }

    private fun SemanticsNodeInteraction.assertToggleableSemantics(
        checked: Boolean
    ): SemanticsNodeInteraction {
        return assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState(checked),
                )
            )
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDataType,
                    ContentDataType.Toggle,
                )
            )
    }
}

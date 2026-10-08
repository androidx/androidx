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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.internal.color.withTone
import androidx.xr.glimmer.testutils.assertGlimmerSurfaceShape
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class IconToggleButtonTest {
    @get:Rule(0)
    val rule = createComposeRule(config = ComposeUiTestConfig(inputMode = InputMode.Keyboard))
    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun iconToggleButton_defaultColors() {
        rule.setGlimmerThemeContent {
            val colors = IconToggleButtonDefaults.colors()
            val expectedCheckedBackgroundColor = GlimmerTheme.colors.primary.withTone(newTone = 70f)
            val expectedFocusedBackgroundColor =
                SurfaceDefaults.focusedColor(GlimmerTheme.colors.surface)
            val expectedFocusedCheckedBackgroundColor =
                GlimmerTheme.colors.primary.withTone(newTone = 82f)
            assertThat(colors.backgroundColor).isEqualTo(GlimmerTheme.colors.surface)
            assertThat(colors.focusedBackgroundColor).isEqualTo(expectedFocusedBackgroundColor)
            assertThat(colors.checkedBackgroundColor).isEqualTo(expectedCheckedBackgroundColor)
            assertThat(colors.focusedCheckedBackgroundColor)
                .isEqualTo(expectedFocusedCheckedBackgroundColor)
            assertThat(colors.contentColor).isEqualTo(calculateContentColor(colors.backgroundColor))
            assertThat(colors.focusedContentColor)
                .isEqualTo(calculateContentColor(expectedFocusedBackgroundColor))
            assertThat(colors.checkedContentColor)
                .isEqualTo(calculateContentColor(expectedCheckedBackgroundColor))
            assertThat(colors.focusedCheckedContentColor)
                .isEqualTo(calculateContentColor(expectedFocusedCheckedBackgroundColor))

            val customColors =
                IconToggleButtonDefaults.colors(
                    backgroundColor = Color.Red,
                    checkedBackgroundColor = Color.Green,
                )
            assertThat(customColors.focusedBackgroundColor)
                .isEqualTo(SurfaceDefaults.focusedColor(Color.Red))
            assertThat(customColors.focusedCheckedBackgroundColor)
                .isEqualTo(Color.Green.withTone(newTone = 82f))
        }
    }

    @Test
    fun iconToggleButtonDefaults_focusedBackgroundColor() {
        rule.setGlimmerThemeContent {
            val expectedFocusedBackgroundColor =
                SurfaceDefaults.focusedColor(GlimmerTheme.colors.surface)
            assertThat(IconToggleButtonDefaults.focusedBackgroundColor())
                .isEqualTo(expectedFocusedBackgroundColor)

            val customColor = Color.Red
            assertThat(IconToggleButtonDefaults.focusedBackgroundColor(customColor))
                .isEqualTo(SurfaceDefaults.focusedColor(customColor))
        }
    }

    @Test
    fun iconToggleButtonDefaults_checkedBackgroundColor() {
        rule.setGlimmerThemeContent {
            val expectedCheckedBackgroundColor = GlimmerTheme.colors.primary.withTone(newTone = 70f)
            assertThat(IconToggleButtonDefaults.checkedBackgroundColor())
                .isEqualTo(expectedCheckedBackgroundColor)

            val customColor = Color.Red
            assertThat(IconToggleButtonDefaults.checkedBackgroundColor(customColor))
                .isEqualTo(customColor.withTone(newTone = 70f))
        }
    }

    @Test
    fun iconToggleButtonDefaults_focusedCheckedBackgroundColor() {
        rule.setGlimmerThemeContent {
            val expectedFocusedCheckedBackgroundColor =
                GlimmerTheme.colors.primary.withTone(newTone = 82f)
            assertThat(IconToggleButtonDefaults.focusedCheckedBackgroundColor())
                .isEqualTo(expectedFocusedCheckedBackgroundColor)

            val customColor = Color.Red
            assertThat(IconToggleButtonDefaults.focusedCheckedBackgroundColor(customColor))
                .isEqualTo(customColor.withTone(newTone = 82f))
        }
    }

    @Test
    fun iconToggleButton_semantics() {
        val checked = mutableStateOf(false)

        rule.setGlimmerThemeContent {
            IconToggleButton(
                modifier = Modifier.testTag("icon_toggle_button"),
                checked = checked.value,
                onCheckedChange = {},
            ) {
                Icon(FavoriteIcon, null)
            }
        }

        rule
            .onNodeWithTag("icon_toggle_button")
            .assertToggleableSemantics(checked = false)
            .assertIsEnabled()

        // Toggle the button.
        checked.value = true
        rule.waitForIdle()

        rule
            .onNodeWithTag("icon_toggle_button")
            .assertToggleableSemantics(checked = true)
            .assertIsEnabled()
    }

    @Test
    fun iconToggleButton_findByContentDescriptionAndClick() {
        var checked by mutableStateOf(false)
        rule.setGlimmerThemeContent {
            IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(FavoriteIcon, "icon_toggle_button")
            }
        }

        rule.onNodeWithContentDescription("icon_toggle_button").performClick()
        rule.runOnIdle { assertThat(checked).isTrue() }
        rule.onNodeWithContentDescription("icon_toggle_button").performClick()
        rule.runOnIdle { assertThat(checked).isFalse() }
    }

    @Test
    fun iconToggleButton_changesShapeAndColor_whenCheckedStateChanges() {
        val checked = mutableStateOf(false)

        rule.setGlimmerThemeContent {
            Box {
                IconToggleButton(
                    checked = checked.value,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("icon_toggle_button"),
                ) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        // Unchecked state.
        rule
            .onNodeWithTag("icon_toggle_button")
            .captureToImage()
            .assertGlimmerSurfaceShape(
                density = rule.density,
                shape = CircleShape,
                backgroundColor = Color.Black,
            )

        // Toggle the button.
        checked.value = true
        rule.waitForIdle()

        // Checked state.
        rule
            .onNodeWithTag("icon_toggle_button")
            .captureToImage()
            .assertGlimmerSurfaceShape(
                density = rule.density,
                shape = ToggleButtonDefaults.CheckedShape,
                backgroundColor = Color.Black,
            )
    }

    @Test
    fun iconToggleButton_changesCustomShapeAndColor_whenCheckedStateChanges() {
        val checked = mutableStateOf(false)
        val expectedCheckedShape = RoundedCornerShape(1)
        val expectedUncheckedShape = RoundedCornerShape(20)
        val expectedColors =
            IconToggleButtonColors(
                backgroundColor = Color.Red,
                focusedBackgroundColor = Color.Red,
                checkedBackgroundColor = Color.Green,
                focusedCheckedBackgroundColor = Color.Green,
                contentColor = Color.Black,
                focusedContentColor = Color.Black,
                checkedContentColor = Color.Black,
                focusedCheckedContentColor = Color.Black,
            )

        rule.setGlimmerThemeContent {
            Box {
                IconToggleButton(
                    checked = checked.value,
                    colors = expectedColors,
                    shape =
                        IconToggleButtonDefaults.shape(
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
    fun iconToggleButton_defaultAnimation_graduallyChangesCornerSizes() {
        rule.mainClock.autoAdvance = false
        val checked = mutableStateOf(false)
        val checkedBackgroundColor = Color.Red
        val density = Density(1f)

        rule.setGlimmerThemeContent(density = density) {
            IconToggleButton(
                checked = checked.value,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        checkedBackgroundColor = checkedBackgroundColor
                    ),
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
    fun iconToggleButton_updatesColor_whenThemePrimaryColorChanges() {
        var themeColors by mutableStateOf(Colors(primary = Color.Red))
        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            GlimmerTheme(colors = themeColors) {
                IconToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    modifier = Modifier.size(100.dp).testTag("icon_toggle_button"),
                ) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        val expectedRed = Color.Red.withTone(newTone = 70f)
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedRed)
        }

        themeColors = Colors(primary = Color.Blue)
        rule.waitForIdle()

        val expectedBlue = Color.Blue.withTone(newTone = 70f)
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedBlue)
        }
    }

    @Test
    fun iconToggleButton_updatesColor_whenThemePrimaryColorChanges_whenFocusedAndChecked() {
        rule.mainClock.autoAdvance = false
        val focusRequester = FocusRequester()
        var themeColors by mutableStateOf(Colors(primary = Color.Red))
        rule.setContent {
            GlimmerTheme(colors = themeColors) {
                IconToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    modifier =
                        Modifier.focusRequester(focusRequester)
                            .size(100.dp)
                            .testTag("icon_toggle_button"),
                ) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.mainClock.advanceTimeBy(1000)

        val expectedRed = Color.Red.withTone(newTone = 82f)
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedRed)
        }

        val darkBlue = Color(0xFF0000CC)
        themeColors = Colors(primary = darkBlue)
        rule.mainClock.advanceTimeByFrame()

        val expectedDarkBlue = darkBlue.withTone(newTone = 82f)
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedDarkBlue)
        }
    }

    @Test
    fun iconToggleButton_updatesColor_whenThemeSurfaceColorChanges_whenFocusedAndUnchecked() {
        rule.mainClock.autoAdvance = false
        val focusRequester = FocusRequester()
        var themeColors by mutableStateOf(Colors(surface = Color.Red))
        rule.setContent {
            GlimmerTheme(colors = themeColors) {
                IconToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    modifier =
                        Modifier.focusRequester(focusRequester)
                            .size(100.dp)
                            .testTag("icon_toggle_button"),
                ) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.mainClock.advanceTimeBy(1000)

        val expectedRed = Color.Red.withTone(newTone = 34f)
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedRed)
        }

        themeColors = Colors(surface = Color.Blue)
        rule.mainClock.advanceTimeByFrame()

        val expectedBlue = Color.Blue.withTone(newTone = 34f)
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedBlue)
        }
    }

    @Test
    fun iconToggleButton_focusedBackgroundColor_usedWhenFocused() {
        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()
        val backgroundColor = Color.Red
        val focusedBackgroundColor = Color.Yellow

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        backgroundColor = backgroundColor,
                        focusedBackgroundColor = focusedBackgroundColor,
                    ),
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }

        // Center of button should be unfocused background color
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(backgroundColor)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Advance past enter animation
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(focusedBackgroundColor)
        }
    }

    @Test
    fun iconToggleButton_focusedCheckedBackgroundColor_usedWhenFocusedAndChecked() {
        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()
        val checkedBackgroundColor = Color.Red
        val focusedCheckedBackgroundColor = Color.Yellow

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        checkedBackgroundColor = checkedBackgroundColor,
                        focusedCheckedBackgroundColor = focusedCheckedBackgroundColor,
                    ),
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }

        // Center of button should be unfocused checked background color
        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(checkedBackgroundColor)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Advance past enter animation
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(focusedCheckedBackgroundColor)
        }
    }

    @Test
    fun iconToggleButton_defaultColors_whenChecked() {
        rule.mainClock.autoAdvance = false

        var checked by mutableStateOf(false)
        var expectedUncheckedColor = Color.Unspecified
        var expectedCheckedColor = Color.Unspecified

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            expectedUncheckedColor = GlimmerTheme.colors.surface
            expectedCheckedColor = IconToggleButtonDefaults.checkedBackgroundColor()

            IconToggleButton(
                checked = checked,
                onCheckedChange = {},
                modifier = Modifier.testTag("icon_toggle_button"),
            ) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedUncheckedColor)
        }

        checked = true
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedCheckedColor)
        }
    }

    @Test
    fun iconToggleButton_defaultColors_whenFocused() {
        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()
        var expectedUnfocusedColor = Color.Unspecified
        var expectedFocusedColor = Color.Unspecified

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            expectedUnfocusedColor = GlimmerTheme.colors.surface
            expectedFocusedColor =
                IconToggleButtonDefaults.focusedBackgroundColor(expectedUnfocusedColor)

            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedUnfocusedColor)
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedFocusedColor)
        }
    }

    @Test
    fun iconToggleButton_defaultColors_whenFocusedAndChecked() {
        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()
        var checked by mutableStateOf(false)
        var expectedUncheckedUnfocusedColor = Color.Unspecified
        var expectedCheckedFocusedColor = Color.Unspecified

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            expectedUncheckedUnfocusedColor = GlimmerTheme.colors.surface
            expectedCheckedFocusedColor = IconToggleButtonDefaults.focusedCheckedBackgroundColor()

            IconToggleButton(
                checked = checked,
                onCheckedChange = {},
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedUncheckedUnfocusedColor)
        }

        checked = true
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("icon_toggle_button").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedCheckedFocusedColor)
        }
    }

    @Test
    fun iconToggleButton_customContentColor_appliedWhenUnfocused() {
        val customContentColor = Color.Magenta
        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                colors = IconToggleButtonDefaults.colors(contentColor = customContentColor),
                modifier = Modifier.testTag("icon_toggle_button"),
            ) {
                Box(Modifier.then(DelegatableNodeProviderElement { node = it }))
            }
        }

        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(customContentColor) }
    }

    @Test
    fun iconToggleButton_customCheckedContentColor_appliedWhenCheckedAndUnfocused() {
        val customCheckedContentColor = Color.Cyan
        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        checkedContentColor = customCheckedContentColor
                    ),
                modifier = Modifier.testTag("icon_toggle_button"),
            ) {
                Box(Modifier.then(DelegatableNodeProviderElement { node = it }))
            }
        }

        rule.runOnIdle {
            assertThat(node!!.currentContentColor()).isEqualTo(customCheckedContentColor)
        }
    }

    @Test
    fun iconToggleButton_focusedColor_resolvesFocusedContentColor() {
        val focusRequester = FocusRequester()
        val backgroundColor = Color.Black
        val focusedBackgroundColor = Color.White

        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        backgroundColor = backgroundColor,
                        focusedBackgroundColor = focusedBackgroundColor,
                    ),
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.then(DelegatableNodeProviderElement { node = it }))
            }
        }

        // Initial background is black, so calculated content color should be white
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.White) }

        rule.runOnIdle { focusRequester.requestFocus() }
        // Focused background is white, so calculated content color should be black
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Black) }
    }

    @Test
    fun iconToggleButton_focusedCheckedColor_resolvesFocusedCheckedContentColor() {
        val focusRequester = FocusRequester()
        val checkedBackgroundColor = Color.Black
        val focusedCheckedBackgroundColor = Color.White

        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        checkedBackgroundColor = checkedBackgroundColor,
                        focusedCheckedBackgroundColor = focusedCheckedBackgroundColor,
                    ),
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.then(DelegatableNodeProviderElement { node = it }))
            }
        }

        // Initial checked background is black, so calculated content color should be white
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.White) }

        rule.runOnIdle { focusRequester.requestFocus() }
        // Focused checked background is white, so calculated content color should be black
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Black) }
    }

    @Test
    fun iconToggleButton_customFocusedContentColor_appliedWhenFocused() {
        val focusRequester = FocusRequester()
        val customFocusedContentColor = Color.Magenta
        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        focusedContentColor = customFocusedContentColor
                    ),
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.then(DelegatableNodeProviderElement { node = it }))
            }
        }

        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.White) }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(node!!.currentContentColor()).isEqualTo(customFocusedContentColor)
        }
    }

    @Test
    fun iconToggleButton_customFocusedCheckedContentColor_appliedWhenFocusedAndChecked() {
        val focusRequester = FocusRequester()
        val customFocusedCheckedContentColor = Color.Cyan
        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                colors =
                    IconToggleButtonDefaults.colors(
                        focusedCheckedContentColor = customFocusedCheckedContentColor
                    ),
                modifier = Modifier.focusRequester(focusRequester).testTag("icon_toggle_button"),
            ) {
                Box(Modifier.then(DelegatableNodeProviderElement { node = it }))
            }
        }

        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Black) }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(node!!.currentContentColor()).isEqualTo(customFocusedCheckedContentColor)
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

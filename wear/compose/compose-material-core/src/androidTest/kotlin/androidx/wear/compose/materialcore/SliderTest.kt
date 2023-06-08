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

package androidx.wear.compose.materialcore

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

public class SliderTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    public fun slider_button_supports_testtag() {
        rule.setContent {
            SliderButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun slider_button_has_click_action_when_enabled() {
        rule.setContent {
            SliderButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    public fun slider_button_has_click_action_when_disabled() {
        rule.setContent {
            SliderButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    public fun slider_button_is_correctly_enabled() {
        rule.setContent {
            SliderButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    public fun slider_button_is_correctly_disabled() {
        rule.setContent {
            SliderButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    public fun slider_button_responds_to_click_when_enabled() {
        var clicked = false
        rule.setContent {
            SliderButtonWithDefaults(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            Assert.assertEquals(true, clicked)
        }
    }

    @Test
    public fun slider_button_does_not_respond_to_click_when_disabled() {
        var clicked = false
        rule.setContent {
            SliderButtonWithDefaults(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            Assert.assertEquals(false, clicked)
        }
    }

    @Test
    public fun sets_icon_content_description_for_slider_button() {
        val testContentDescription = "testContentDescription"

        rule.setContent {
            SliderButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                content = { TestImage(iconLabel = testContentDescription) }
            )
        }

        rule.onNodeWithTag(TEST_TAG, true)
            .onChild()
            .assertContentDescriptionContains(testContentDescription)
    }

    @Test
    public fun slider_progress_bar_supports_testtag() {
        rule.setContent {
            ProgressBarWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun slider_progress_bar_draws_bar_separator_when_segments_greater_than_one() {
        rule.setContent {
            ProgressBarWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                barSeparatorColor = mutableStateOf(BarSeparatorColor),
                visibleSegments = 5
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor = BarSeparatorColor, 0.1f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun slider_progress_bar_does_not_draw_bar_separator_when_segments_equals_one() {
        rule.setContent {
            ProgressBarWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                barSeparatorColor = mutableStateOf(BarSeparatorColor),
                visibleSegments = 1
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(unexpectedColor = BarSeparatorColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun draws_selected_and_unselected_bar_when_ratio_between_zero_and_one() {
        rule.setContent {
            ProgressBarWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selectedBarColor = mutableStateOf(SelectedBarColor),
                unselectedBarColor = mutableStateOf(UnselectedBarColor),
                valueRatio = 0.4f
            )
        }

        val progressBarImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        progressBarImage.assertContainsColor(expectedColor = SelectedBarColor, 1f)
        progressBarImage.assertContainsColor(expectedColor = UnselectedBarColor, 1f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun draws_selected_bar_only_when_ratio_equals_one() {
        rule.setContent {
            ProgressBarWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selectedBarColor = mutableStateOf(SelectedBarColor),
                unselectedBarColor = mutableStateOf(UnselectedBarColor),
                valueRatio = 1f
            )
        }

        val progressBarImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        progressBarImage.assertContainsColor(expectedColor = SelectedBarColor, 1f)
        progressBarImage.assertDoesNotContainColor(unexpectedColor = UnselectedBarColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun draws_unselected_bar_only_when_ratio_equals_zero() {
        rule.setContent {
            ProgressBarWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selectedBarColor = mutableStateOf(SelectedBarColor),
                unselectedBarColor = mutableStateOf(UnselectedBarColor),
                valueRatio = 0f
            )
        }

        val progressBarImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        progressBarImage.assertContainsColor(expectedColor = UnselectedBarColor, 1f)
        progressBarImage.assertDoesNotContainColor(unexpectedColor = SelectedBarColor)
    }

    @Test
    public fun directed_value_correct_for_ltr() {
        val expectedValue = -1

        val value = directedValue(LayoutDirection.Ltr, -1, +1)

        Assert.assertEquals(expectedValue, value)
    }

    @Test
    public fun directed_value_correct_for_rtl() {
        val expectedValue = +1

        val value = directedValue(LayoutDirection.Rtl, -1, +1)

        Assert.assertEquals(expectedValue, value)
    }

    @Composable
    internal fun SliderButtonWithDefaults(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
        contentAlignment: Alignment = Alignment.Center,
        buttonControlSize: Dp = ButtonControlSize,
        content: @Composable () -> Unit = { TestImage() }
    ) {
        InlineSliderButton(
            enabled = enabled,
            onClick = onClick,
            contentAlignment = contentAlignment,
            buttonControlSize = buttonControlSize,
            modifier = modifier,
            content = content
        )
    }

    @Composable
    internal fun ProgressBarWithDefaults(
        modifier: Modifier = Modifier,
        selectedBarColor: State<Color> = mutableStateOf(SelectedBarColor),
        unselectedBarColor: State<Color> = mutableStateOf(UnselectedBarColor),
        barSeparatorColor: State<Color> = mutableStateOf(BarSeparatorColor),
        visibleSegments: Int = 1,
        valueRatio: Float = 0.4f
    ) {
        val layoutDirection = LayoutDirection.Ltr
        val drawSelectedProgressBar =
            { color: Color, ratio: Float, direction: LayoutDirection, drawScope: DrawScope ->
                drawScope.drawTestSelectedProgressBar(color, ratio, direction)
            }
        val drawUnselectedProgressBar =
            { color: Color, ratio: Float, direction: LayoutDirection, drawScope: DrawScope ->
                drawScope.drawTestUnselectedProgressBar(color, ratio, direction)
            }
        val drawProgressBarSeparator = { color: Color, position: Float, drawScope: DrawScope ->
            drawScope.drawTestProgressBarSeparator(color, position)
        }

        Box(
            modifier = modifier
                .height(ProgressBarHeight)
                .fillMaxWidth()
                .drawProgressBar(
                    selectedBarColor = selectedBarColor,
                    unselectedBarColor = unselectedBarColor,
                    barSeparatorColor = barSeparatorColor,
                    visibleSegments = visibleSegments,
                    valueRatio = valueRatio,
                    direction = layoutDirection,
                    drawSelectedProgressBar = drawSelectedProgressBar,
                    drawUnselectedProgressBar = drawUnselectedProgressBar,
                    drawProgressBarSeparator = drawProgressBarSeparator
                )
        )
    }

    private fun DrawScope.drawTestSelectedProgressBar(
        color: Color,
        valueRatio: Float,
        direction: LayoutDirection
    ) {
        drawLine(
            color,
            Offset(
                directedValue(direction, 0f, size.width * (1 - valueRatio)), size.height / 2
            ),
            Offset(
                directedValue(direction, size.width * valueRatio, size.width), size.height / 2
            ),
            strokeWidth = size.height
        )
    }

    private fun DrawScope.drawTestUnselectedProgressBar(
        color: Color,
        valueRatio: Float,
        direction: LayoutDirection
    ) {
        drawLine(
            color,
            Offset(
                directedValue(direction, size.width * valueRatio, 0f), size.height / 2
            ),
            Offset(
                directedValue(direction, size.width, size.width * (1 - valueRatio)), size.height / 2
            ),
            strokeWidth = size.height
        )
    }

    private fun DrawScope.drawTestProgressBarSeparator(color: Color, position: Float) {
        drawLine(
            color,
            Offset(position, 0f),
            Offset(position, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }

    private val ButtonControlSize = 36.dp
    private val ProgressBarHeight = 10.dp
    private val SelectedBarColor = Color.Red
    private val UnselectedBarColor = Color.Green
    private val BarSeparatorColor = Color.Blue
}

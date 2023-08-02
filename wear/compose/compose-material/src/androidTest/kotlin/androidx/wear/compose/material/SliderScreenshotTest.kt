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

package androidx.wear.compose.material

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SliderScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun inlineslider_not_segmented() {
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                segmented = false,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun inlineslider_segmented() {
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                segmented = true,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun inlineslider_segmented_with_custom_colors() {
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                segmented = true,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                colors = InlineSliderDefaults.colors(
                    backgroundColor = Color.Green,
                    spacerColor = Color.Yellow,
                    selectedBarColor = Color.Magenta,
                    unselectedBarColor = Color.White,
                    disabledBackgroundColor = Color.DarkGray,
                    disabledSpacerColor = Color.LightGray,
                    disabledSelectedBarColor = Color.Red,
                    disabledUnselectedBarColor = Color.Blue
                ),
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun inlineslider_custom_icons() {
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                onValueChange = { },
                valueRange = 1f..4f,
                decreaseIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                },
                increaseIcon = {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = ""
                    )
                },
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun inlineslider_segmented_with_custom_colors_disabled() {
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                enabled = false,
                segmented = true,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                colors = InlineSliderDefaults.colors(
                    backgroundColor = Color.Green,
                    spacerColor = Color.Yellow,
                    selectedBarColor = Color.Magenta,
                    unselectedBarColor = Color.White,
                    disabledBackgroundColor = Color.DarkGray,
                    disabledSpacerColor = Color.LightGray,
                    disabledSelectedBarColor = Color.Red,
                    disabledUnselectedBarColor = Color.Blue
                ),
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun inlineslider_custom_icons_disabled() {
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                enabled = false,
                onValueChange = { },
                valueRange = 1f..4f,
                decreaseIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                },
                increaseIcon = {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = ""
                    )
                },
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

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

package androidx.wear.compose.material3.test

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.InlineSlider
import androidx.wear.compose.material3.InlineSliderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalWearMaterial3Api::class)
class SliderScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun inlineslider_not_segmented() {
        verifyScreenshot {
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
    }

    @Test
    fun inlineslider_not_segmented_disabled() {
        verifyScreenshot {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                segmented = false,
                enabled = false,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }
    }

    @Test
    fun inlineslider_segmented() {
        verifyScreenshot {
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
    }

    @Test
    fun inlineslider_segmented_disabled() {
        verifyScreenshot {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                segmented = true,
                enabled = false,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }
    }

    @Test
    public fun inlineslider_rtl() {
        verifyScreenshot {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                InlineSlider(
                    modifier = Modifier.testTag(TEST_TAG),
                    value = 2f,
                    increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                    decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                    onValueChange = { },
                    valueRange = 1f..4f,
                    steps = 2
                )
            }
        }
    }

    @Test
    public fun inlineslider_with_increase_button_disabled() {
        verifyScreenshot {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 4f,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                steps = 3,
                onValueChange = {}
            )
        }
    }

    @Test
    public fun inlineslider_with_decrease_button_disabled() {
        verifyScreenshot {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 0f,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                steps = 3,
                onValueChange = {}
            )
        }
    }

    @Test
    fun inlineslider_segmented_with_custom_colors() {
        verifyScreenshot {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                segmented = true,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                colors = InlineSliderDefaults.colors(
                    containerColor = Color.Green,
                    buttonIconColor = Color.Yellow,
                    selectedBarColor = Color.Magenta,
                    unselectedBarColor = Color.White,
                    barSeparatorColor = Color.Cyan,
                    disabledContainerColor = Color.DarkGray,
                    disabledButtonIconColor = Color.LightGray,
                    disabledSelectedBarColor = Color.Red,
                    disabledUnselectedBarColor = Color.Blue,
                    disabledBarSeparatorColor = Color.Gray
                ),
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }
    }

    @Test
    fun inlineslider_custom_icons() {
        verifyScreenshot {
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
    }

    @Test
    fun inlineslider_segmented_with_custom_colors_disabled() {
        verifyScreenshot {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                enabled = false,
                segmented = true,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                colors = InlineSliderDefaults.colors(
                    containerColor = Color.Green,
                    buttonIconColor = Color.Yellow,
                    selectedBarColor = Color.Magenta,
                    unselectedBarColor = Color.White,
                    barSeparatorColor = Color.Cyan,
                    disabledContainerColor = Color.DarkGray,
                    disabledButtonIconColor = Color.LightGray,
                    disabledSelectedBarColor = Color.Red,
                    disabledUnselectedBarColor = Color.Blue,
                    disabledBarSeparatorColor = Color.Gray
                ),
                onValueChange = { },
                valueRange = 1f..4f,
                steps = 2
            )
        }
    }

    @Test
    fun inlineslider_custom_icons_disabled() {
        verifyScreenshot {
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
    }

    private fun verifyScreenshot(
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

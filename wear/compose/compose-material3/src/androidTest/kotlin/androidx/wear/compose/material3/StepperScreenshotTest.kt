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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.ScreenConfiguration
import androidx.wear.compose.material3.ScreenSize
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.StepperDefaults.IconSize
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.setContentWithTheme
import androidx.wear.compose.material3.verifyScreenshot
import androidx.wear.compose.materialcore.RangeIcons
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
class StepperScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun stepper_no_content(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
                increaseIcon = { IncreaseIcon() },
                decreaseIcon = { DecreaseIcon() },
            ) {}
        }
    }

    @Test
    fun stepper_custom_icons(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
                decreaseIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = "") },
                increaseIcon = { Icon(imageVector = Icons.Filled.ThumbUp, contentDescription = "") },
            ) {}
        }
    }

    @Test
    fun stepper_with_content(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
                increaseIcon = { IncreaseIcon() },
                decreaseIcon = { DecreaseIcon() },
            ) {
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier.width(146.dp),
                    label = { Text(text = "Demo", modifier = Modifier.fillMaxWidth()) },
                )
            }
        }
    }

    @Test
    fun stepper_with_custom_colors(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
                increaseIcon = { IncreaseIcon() },
                decreaseIcon = { DecreaseIcon() },
                colors =
                    StepperDefaults.colors(
                        buttonContainerColor = Color.Green,
                        contentColor = Color.Yellow,
                        buttonIconColor = Color.Magenta,
                    ),
            ) {
                Text("Demo")
            }
        }
    }

    @Test
    fun stepper_with_increase_button_disabled(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 4f,
                steps = 3,
                increaseIcon = { IncreaseIcon() },
                decreaseIcon = { DecreaseIcon() },
                onValueChange = {},
            ) {}
        }
    }

    @Test
    fun stepper_with_decrease_button_disabled(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 0f,
                steps = 3,
                increaseIcon = { IncreaseIcon() },
                decreaseIcon = { DecreaseIcon() },
                onValueChange = {},
            ) {}
        }
    }

    @Test
    fun stepper_disabled(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize = screenSize) {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 1f,
                steps = 3,
                onValueChange = {},
                enabled = false,
                increaseIcon = { IncreaseIcon() },
                decreaseIcon = { DecreaseIcon() },
            ) {}
        }
    }

    @Test
    fun stepper_increase_button_morphs_when_pressed(@TestParameter screenSize: ScreenSize) {
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Stepper(
                        modifier = Modifier.testTag(TEST_TAG),
                        value = 2f,
                        steps = 3,
                        onValueChange = {},
                        increaseIcon = {
                            Icon(
                                modifier = Modifier.testTag("increase_icon"),
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Increase",
                            )
                        },
                        decreaseIcon = { DecreaseIcon() },
                    ) {}
                }
            }
        }

        rule.onNodeWithTag("increase_icon", true).performTouchInput { down(center) }
        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun stepper_decrease_button_morphs_when_pressed(@TestParameter screenSize: ScreenSize) {
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Stepper(
                        modifier = Modifier.testTag(TEST_TAG),
                        value = 2f,
                        steps = 3,
                        onValueChange = {},
                        increaseIcon = { IncreaseIcon() },
                        decreaseIcon = {
                            Icon(
                                modifier = Modifier.testTag("decrease_icon"),
                                imageVector = RangeIcons.Minus,
                                contentDescription = "Decrease",
                            )
                        },
                    ) {}
                }
            }
        }

        rule.onNodeWithTag("decrease_icon", true).performTouchInput { down(center) }
        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }

    private fun verifyScreenshot(screenSize: ScreenSize, content: @Composable () -> Unit) {
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    content()
                }
            }
        }

        rule.verifyScreenshot(testName, screenshotRule)
    }
}

@Composable
private fun IncreaseIcon() =
    Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = "Increase",
        modifier = Modifier.size(IconSize),
    )

@Composable
private fun DecreaseIcon() =
    Icon(
        imageVector = RangeIcons.Minus,
        contentDescription = "Decrease",
        modifier = Modifier.size(IconSize),
    )

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalWearMaterial3Api::class)
public class StepperScreenshotTest {
    @get:Rule
    public val rule = createComposeRule()

    @get:Rule
    public val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    public val testName = TestName()

    @Test
    public fun stepper_no_content() {
        verifyScreenshot {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                steps = 3,
                onValueChange = {}
            ) {}
        }
    }

    @Test
    public fun stepper_custom_icons() {
        verifyScreenshot {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
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
            ) {}
        }
    }

    @Test
    public fun stepper_with_content() {
        verifyScreenshot {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                onValueChange = {}
            ) {
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier.width(146.dp),
                    label = {
                        Text(
                            text = "Demo",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        }
    }

    @Test
    public fun stepper_with_custom_colors() {
        verifyScreenshot {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                backgroundColor = Color.Green,
                contentColor = Color.Yellow,
                iconColor = Color.Magenta,
            ) {
                Text("Demo")
            }
        }
    }

    @Test
    public fun stepper_with_increase_button_disabled() {
        verifyScreenshot {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 4f,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                steps = 3,
                onValueChange = {}
            ) {}
        }
    }

    @Test
    public fun stepper_with_decrease_button_disabled() {
        verifyScreenshot {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                valueRange = 0f..4f,
                value = 0f,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                steps = 3,
                onValueChange = {}
            ) {}
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

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
public class StepperScreenshotTest {
    @get:Rule
    public val rule = createComposeRule()

    @get:Rule
    public val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    public val testName = TestName()

    @Test
    public fun stepper_no_content() {
        rule.setContentWithThemeAndBackground {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {}
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    public fun stepper_custom_icons() {
        rule.setContentWithThemeAndBackground {
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

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    public fun stepper_with_content() {
        rule.setContentWithThemeAndBackground {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {}
            ) {
                Chip(
                    onClick = {},
                    modifier = Modifier.width(146.dp),
                    colors = ChipDefaults.secondaryChipColors(),
                    label = { Text("Demo", modifier = Modifier.align(Alignment.Center)) }
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    public fun stepper_with_custom_colors() {
        rule.setContentWithThemeAndBackground {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 2f,
                steps = 3,
                onValueChange = {},
                backgroundColor = Color.Green,
                contentColor = Color.Yellow,
                iconTintColor = Color.Magenta,
            ) {
                Text("Demo")
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

private fun ComposeContentTestRule.setContentWithThemeAndBackground(
    composable: @Composable () -> Unit
) {
    setContentWithTheme {
        Box(modifier = Modifier.background(Color.Black)) {
            composable()
        }
    }
}

/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Confirmation
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class DialogScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun alert_title_body_and_buttons() = verifyScreenshot {
        Alert(
            title = {
                Text(
                    text = "Power off",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground
                )
            },
            negativeButton = {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("No")
                }
            },
            positiveButton = {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("Yes")
                }
            },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            Text(
                text = "Are you sure?",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
        }
    }

    @Test
    fun alert_icon_title_and_buttons() = verifyScreenshot {
        Alert(
            icon = { TestIcon() },
            title = {
                Text(
                    text = "Allow access to location?",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground
                )
            },
            negativeButton = {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("No")
                }
            },
            positiveButton = {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("Yes")
                }
            },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Test
    fun alert_icon_title_and_chip() = verifyScreenshot {
        Alert(
            icon = { TestIcon() },
            title = { Text(
                text = "Grant location permission to use this app",
                textAlign = TextAlign.Center
            ) },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            item {
                Chip(
                    icon = { TestIcon() },
                    label = { Text("Settings") },
                    onClick = {},
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
    }

    @Test
    fun alert_title_message_and_chip() = verifyScreenshot {
        Alert(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            title = {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    Text(text = "Title that is quite long", textAlign = TextAlign.Center)
                }
            },
            message = {
                Text(
                    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2
                )
            },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            item {
                Chip(
                    icon = { TestIcon() },
                    label = { Text("Allow access") },
                    onClick = {},
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
        }
    }

    @Test
    fun confirmation() = verifyScreenshot {
        Confirmation(
            onTimeout = {},
            icon = { TestIcon() },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            Text(
                text = "Success",
                textAlign = TextAlign.Center
            )
        }
    }

    private fun verifyScreenshot(
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
             content()
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

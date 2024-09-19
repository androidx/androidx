/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.ConfirmationDefaults.curvedText
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
class ConfirmationScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun confirmation_icon_linearText(@TestParameter screenSize: ScreenSize) {

        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            Confirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                text = { Text("Your message has been sent") }
            ) {
                DefaultSmallIcon()
            }
        }
    }

    @Test
    fun confirmation_icon_curvedText(@TestParameter screenSize: ScreenSize) {
        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            Confirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                curvedText = curvedText("Confirmed")
            ) {
                DefaultIcon()
            }
        }
    }

    @Test
    fun confirmation_icon_noText(@TestParameter screenSize: ScreenSize) {
        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            Confirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                curvedText = null
            ) {
                DefaultIcon()
            }
        }
    }

    @Test
    fun successConfirmation_icon_text(@TestParameter screenSize: ScreenSize) {
        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            SuccessConfirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                curvedText = curvedText("Success")
            )
        }
    }

    @Test
    fun successConfirmation_icon_noText(@TestParameter screenSize: ScreenSize) {
        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            SuccessConfirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                curvedText = null
            )
        }
    }

    @Test
    fun failureConfirmation_icon_text(@TestParameter screenSize: ScreenSize) {
        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            FailureConfirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                curvedText = curvedText("Failure")
            )
        }
    }

    @Test
    fun failureConfirmation_icon_noText(@TestParameter screenSize: ScreenSize) {
        rule.verifyConfirmationScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize
        ) { modifier ->
            FailureConfirmation(
                show = true,
                modifier = modifier,
                onDismissRequest = {},
                curvedText = null,
            )
        }
    }

    private fun ComposeContentTestRule.verifyConfirmationScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        screenSize: ScreenSize,
        content: @Composable (modifier: Modifier) -> Unit
    ) {
        setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                content(Modifier.size(screenSize.size.dp).testTag(TEST_TAG))
            }
        }

        onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }

    @Composable
    private fun DefaultIcon() {
        Icon(
            Icons.Filled.Add,
            modifier = Modifier.size(ConfirmationDefaults.IconSize),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
    }

    @Composable
    private fun DefaultSmallIcon() {
        Icon(
            Icons.Filled.Add,
            modifier = Modifier.size(ConfirmationDefaults.SmallIconSize),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
    }
}

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
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
class OpenOnPhoneDialogScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun openOnPhone_50_percent_progress(@TestParameter screenSize: ScreenSize) {
        rule.verifyOpenOnPhoneScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            advanceTimeBy = OpenOnPhoneDialogDefaults.DurationMillis / 2,
            screenSize = screenSize
        )
    }

    @Test
    fun openOnPhone_100_percent_progress(@TestParameter screenSize: ScreenSize) {
        rule.verifyOpenOnPhoneScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            advanceTimeBy = OpenOnPhoneDialogDefaults.DurationMillis,
            screenSize = screenSize
        )
    }

    private fun ComposeContentTestRule.verifyOpenOnPhoneScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        screenSize: ScreenSize,
        advanceTimeBy: Long,
    ) {
        rule.mainClock.autoAdvance = false
        setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                OpenOnPhoneDialog(
                    show = true,
                    modifier = Modifier.size(screenSize.size.dp).testTag(TEST_TAG),
                    onDismissRequest = {},
                )
            }
        }

        rule.mainClock.advanceTimeBy(advanceTimeBy)
        onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }
}

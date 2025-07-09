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

package androidx.wear.compose.material3

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ListHeaderScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun listheader() =
        rule.verifyScreenshot(testName = testName, screenshotRule = screenshotRule) {
            ListHeader(modifier = Modifier.testTag(TEST_TAG)) { Text("Header") }
        }

    @Test
    fun listheader_multiline() =
        rule.verifyScreenshot(testName = testName, screenshotRule = screenshotRule) {
            ListHeader(modifier = Modifier.testTag(TEST_TAG)) {
                Text(
                    "Header should be center-aligned over multiple lines and so this heading is quite long"
                )
            }
        }

    @Test
    fun listsubheader_textonly() =
        rule.verifyScreenshot(testName = testName, screenshotRule = screenshotRule) {
            ListSubHeader(modifier = Modifier.testTag(TEST_TAG)) { Text("Subheader") }
        }

    @Test
    fun listsubheader_textonly_multiline() =
        rule.verifyScreenshot(testName = testName, screenshotRule = screenshotRule) {
            ListSubHeader(modifier = Modifier.testTag(TEST_TAG)) {
                Text(
                    "Subheader should be start-aligned over multiple lines and so this heading is quite long"
                )
            }
        }

    @Test
    fun listsubheader_textonly_rtl() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            ListSubHeader(modifier = Modifier.testTag(TEST_TAG)) { Text("Subheader") }
        }

    @Test
    fun listsubheader_text_and_icon() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
        ) {
            ListSubHeader(
                modifier = Modifier.testTag(TEST_TAG),
                label = { Text(text = "Subheader") },
                icon = { Icon(imageVector = Icons.Outlined.Home, "home") },
            )
        }

    @Test
    fun listsubheader_text_and_icon_rtl() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            ListSubHeader(
                modifier = Modifier.testTag(TEST_TAG),
                label = { Text(text = "Subheader") },
                icon = { Icon(imageVector = Icons.Outlined.Home, "home") },
            )
        }
}

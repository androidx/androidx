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

import android.os.Build
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ListHeaderScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun listheader() =
        rule.verifyScreenshot(methodName = testName.methodName, screenshotRule = screenshotRule) {
            ListHeader(modifier = Modifier.testTag(TEST_TAG)) { Text("Header") }
        }

    @Test
    fun listsubheader_textonly() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
        ) {
            ListSubHeader(modifier = Modifier.testTag(TEST_TAG)) { Text("Subheader") }
        }

    @Test
    fun listsubheader_textonly_rtl() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            ListSubHeader(modifier = Modifier.testTag(TEST_TAG)) { Text("Subheader") }
        }

    @Test
    fun listsubheader_text_and_icon() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
        ) {
            ListSubHeader(
                modifier = Modifier.testTag(TEST_TAG),
                label = { Text(text = "Subheader") },
                icon = { Icon(imageVector = Icons.Outlined.Home, "home") }
            )
        }

    @Test
    fun listsubheader_text_and_icon_rtl() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ListSubHeader(
                modifier = Modifier.testTag(TEST_TAG),
                label = { Text(text = "Subheader") },
                icon = { Icon(imageVector = Icons.Outlined.Home, "home") }
            )
        }
}

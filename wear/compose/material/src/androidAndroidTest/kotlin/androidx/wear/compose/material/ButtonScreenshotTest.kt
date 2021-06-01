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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
@OptIn(ExperimentalTestApi::class)
class ButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    val testTag = "test-item"

    @Test
    fun button_ltr() =
        verifyScreenshot(LayoutDirection.Ltr) {
            Button(onClick = {}, modifier = Modifier.testTag(testTag)) {
                Text("abc")
            }
        }

    @Test
    fun button_rtl() =
        verifyScreenshot(LayoutDirection.Rtl) {
            Button(onClick = {}, modifier = Modifier.testTag(testTag)) {
                Text("abc")
            }
        }

    @Test
    fun button_disabled() =
        verifyScreenshot {
            Button(onClick = {}, enabled = false, modifier = Modifier.testTag(testTag)) {
                Text("abc")
            }
        }

    @Test
    fun compactbutton_ltr() =
        verifyScreenshot(LayoutDirection.Ltr) {
            CompactButton(onClick = {}, modifier = Modifier.testTag(testTag)) {
                Text("xs")
            }
        }

    @Test
    fun compactbutton_rtl() =
        verifyScreenshot(LayoutDirection.Rtl) {
            CompactButton(onClick = {}, modifier = Modifier.testTag(testTag)) {
                Text("xs")
            }
        }

    @Test
    fun compactbutton_disabled() =
        verifyScreenshot {
            CompactButton(onClick = {}, enabled = false, modifier = Modifier.testTag(testTag)) {
                Text("xs")
            }
        }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                content()
            }
        }

        rule.onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

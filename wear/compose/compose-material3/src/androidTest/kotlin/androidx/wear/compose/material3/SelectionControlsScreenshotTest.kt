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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
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
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SelectionControlsScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun checkbox_checked_enabled() =
        verifyScreenshot {
            Checkbox(checked = true, enabled = true, modifier = testBackgroundModifier())
        }

    @Test
    fun checkbox_unchecked_enabled() =
        verifyScreenshot {
            Checkbox(checked = false, enabled = true, modifier = testBackgroundModifier())
        }

    @Test
    fun checkbox_checked_disabled() =
        verifyScreenshot {
            Checkbox(checked = true, enabled = false, modifier = testBackgroundModifier())
        }

    @Test
    fun checkbox_unchecked_disabled() =
        verifyScreenshot {
            Checkbox(checked = false, enabled = false, modifier = testBackgroundModifier())
        }

    @Test
    fun switch_checked_enabled() =
        verifyScreenshot {
            Switch(checked = true, enabled = true, modifier = testBackgroundModifier())
        }

    @Test
    fun switch_unchecked_enabled() =
        verifyScreenshot {
            Switch(checked = false, enabled = true, modifier = testBackgroundModifier())
        }

    @Test
    fun switch_checked_disabled() =
        verifyScreenshot {
            Switch(checked = true, enabled = false, modifier = testBackgroundModifier())
        }

    @Test
    fun switch_unchecked_disabled() =
        verifyScreenshot {
            Switch(checked = false, enabled = false, modifier = testBackgroundModifier())
        }

    @Test
    fun switch_rtl_checked_enabled() =
        verifyScreenshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Switch(checked = true, enabled = true, modifier = testBackgroundModifier())
            }
        }

    @Test
    fun switch_rtl_unchecked_enabled() =
        verifyScreenshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Switch(checked = false, enabled = true, modifier = testBackgroundModifier())
            }
        }

    @Test
    fun switch_rtl_checked_disabled() =
        verifyScreenshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Switch(checked = true, enabled = false, modifier = testBackgroundModifier())
            }
        }

    @Test
    fun switch_rtl_unchecked_disabled() =
        verifyScreenshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Switch(checked = false, enabled = false, modifier = testBackgroundModifier())
            }
        }

    @Test
    fun radiobutton_checked_enabled() =
        verifyScreenshot {
            RadioButton(selected = true, enabled = true, modifier = testBackgroundModifier())
        }

    @Test
    fun radiobutton_unchecked_enabled() =
        verifyScreenshot {
            RadioButton(selected = false, enabled = true, modifier = testBackgroundModifier())
        }

    @Test
    fun radiobutton_checked_disabled() =
        verifyScreenshot {
            RadioButton(selected = true, enabled = false, modifier = testBackgroundModifier())
        }

    @Test
    fun radiobutton_unchecked_disabled() =
        verifyScreenshot {
            RadioButton(selected = false, enabled = false, modifier = testBackgroundModifier())
        }

    private fun verifyScreenshot(
        threshold: Double = 0.98,
        content: @Composable BoxScope.() -> Unit
    ) {
        rule.setContentWithTheme(composable = content)
        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName, MSSIMMatcher(threshold))
    }

    @Composable
    private fun testBackgroundModifier(): Modifier =
        Modifier
            .testTag(TEST_TAG)
            .background(
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.5f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            )
}

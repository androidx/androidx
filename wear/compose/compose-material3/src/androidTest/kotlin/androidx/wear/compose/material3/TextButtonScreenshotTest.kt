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
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.setContentWithTheme
import androidx.wear.compose.material3.touchTargetAwareSize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun filled_text_button_enabled() = verifyScreenshot {
        sampleFilledTextButton(enabled = true, isCompact = false)
    }

    @Test
    fun filled_text_button_disabled() =
        verifyScreenshot("text_button_disabled") {
            sampleFilledTextButton(enabled = false, isCompact = false)
        }

    @Test
    fun filled_tonal_text_button_enabled() = verifyScreenshot {
        sampleFilledTonalTextButton(enabled = true, isCompact = false)
    }

    @Test
    fun filled_tonal_text_button_disabled() =
        verifyScreenshot("text_button_disabled") {
            sampleFilledTonalTextButton(enabled = false, isCompact = false)
        }

    @Test
    fun outlined_text_button_enabled() = verifyScreenshot {
        sampleOutlinedTextButton(enabled = true, isCompact = false)
    }

    @Test
    fun outlined_text_button_disabled() = verifyScreenshot {
        sampleOutlinedTextButton(enabled = false, isCompact = false)
    }

    @Test
    fun text_button_enabled() = verifyScreenshot {
        sampleTextButton(enabled = true, isCompact = false)
    }

    @Test
    fun text_button_disabled() = verifyScreenshot {
        sampleTextButton(enabled = false, isCompact = false)
    }

    @Test
    fun filled_compact_text_button_enabled() = verifyScreenshot {
        sampleFilledTextButton(enabled = true, isCompact = true)
    }

    @Test
    fun filled_compact_text_button_disabled() =
        verifyScreenshot("compact_text_button_disabled") {
            sampleFilledTextButton(enabled = false, isCompact = true)
        }

    @Test
    fun filled_tonal_compact_text_button_enabled() = verifyScreenshot {
        sampleFilledTonalTextButton(enabled = true, isCompact = true)
    }

    @Test
    fun filled_tonal_compact_text_button_disabled() =
        verifyScreenshot("compact_text_button_disabled") {
            sampleFilledTonalTextButton(enabled = false, isCompact = true)
        }

    @Test
    fun outlined_compact_text_button_enabled() = verifyScreenshot {
        sampleOutlinedTextButton(enabled = true, isCompact = true)
    }

    @Test
    fun outlined_compact_text_button_disabled() = verifyScreenshot {
        sampleOutlinedTextButton(enabled = false, isCompact = true)
    }

    @Test
    fun compact_text_button_enabled() = verifyScreenshot {
        sampleTextButton(enabled = true, isCompact = true)
    }

    @Test
    fun compact_text_button_disabled() = verifyScreenshot {
        sampleTextButton(enabled = false, isCompact = true)
    }

    @Composable
    private fun sampleFilledTextButton(enabled: Boolean, isCompact: Boolean) {
        TextButton(
            onClick = {},
            colors = TextButtonDefaults.filledTextButtonColors(),
            enabled = enabled,
            modifier = Modifier
                .testTag(TEST_TAG)
                .then(
                    if (isCompact)
                        Modifier.touchTargetAwareSize(TextButtonDefaults.ExtraSmallButtonSize)
                    else Modifier
                )
        ) {
            Text(text = if (isCompact) "TB" else "ABC")
        }
    }

    @Composable
    private fun sampleFilledTonalTextButton(enabled: Boolean, isCompact: Boolean) {
        TextButton(
            onClick = {},
            colors = TextButtonDefaults.filledTonalTextButtonColors(),
            enabled = enabled,
            modifier = Modifier
                .testTag(TEST_TAG)
                .then(
                    if (isCompact)
                        Modifier.touchTargetAwareSize(TextButtonDefaults.ExtraSmallButtonSize)
                    else Modifier
                )
        ) {
            Text(text = if (isCompact) "TB" else "ABC")
        }
    }

    @Composable
    private fun sampleOutlinedTextButton(enabled: Boolean, isCompact: Boolean) {
        TextButton(
            onClick = {},
            colors = TextButtonDefaults.outlinedTextButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder(enabled),
            enabled = enabled,
            modifier = Modifier
                .testTag(TEST_TAG)
                .then(
                    if (isCompact)
                        Modifier.touchTargetAwareSize(TextButtonDefaults.ExtraSmallButtonSize)
                    else Modifier
                )
        ) {
            Text(text = if (isCompact) "O" else "ABC")
        }
    }

    @Composable
    private fun sampleTextButton(enabled: Boolean, isCompact: Boolean) {
        TextButton(
            onClick = {},
            enabled = enabled,
            modifier = Modifier
                .testTag(TEST_TAG)
                .then(
                    if (isCompact)
                        Modifier.touchTargetAwareSize(TextButtonDefaults.ExtraSmallButtonSize)
                    else Modifier
                )
        ) {
            Text(text = if (isCompact) "TB" else "ABC")
        }
    }

    private fun verifyScreenshot(
        methodName: String = testName.methodName,
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

        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertAgainstGolden(screenshotRule, methodName)
    }
}

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

package androidx.wear.compose.material3.test

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
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
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextConfiguration
import androidx.wear.compose.material3.TextConfigurationDefaults
import androidx.wear.compose.material3.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun text_with_default_start_alignment() = verifyScreenshot { sampleText() }

    @Test
    fun text_align_follows_composition_local_center_alignment() = verifyScreenshot {
        CompositionLocalProvider(
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    overflow = TextConfigurationDefaults.Overflow,
                    maxLines = TextConfigurationDefaults.MaxLines,
                )
        ) {
            sampleText()
        }
    }

    @Test
    fun text_align_follows_composition_local_end_alignment() = verifyScreenshot {
        CompositionLocalProvider(
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.End,
                    overflow = TextConfigurationDefaults.Overflow,
                    maxLines = TextConfigurationDefaults.MaxLines,
                )
        ) {
            sampleText()
        }
    }

    @Composable
    private fun sampleText() {
        Text(
            text = "abcdefg",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG)
        )
    }

    private fun verifyScreenshot(
        methodName: String = testName.methodName,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            Box(
                modifier =
                    Modifier.width(300.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                content()
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, methodName)
    }
}

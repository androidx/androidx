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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class AnimatedTextScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun animatedText_0f() = verifyScreenshot { BaseAnimatedText(0f) }

    @Test fun animatedText_0_25f() = verifyScreenshot { BaseAnimatedText(0.25f) }

    @Test fun animatedText_0_5f() = verifyScreenshot { BaseAnimatedText(0.5f) }

    @Test fun animatedText_0_75f() = verifyScreenshot { BaseAnimatedText(0.75f) }

    @Test fun animatedText_1f() = verifyScreenshot { BaseAnimatedText(1f) }

    @Composable
    private fun BaseAnimatedText(progressFraction: Float) {
        val animatedTextFontRegistry =
            rememberAnimatedTextFontRegistry(
                // Variation axes at the start of the animation, width 10, weight 200
                startFontVariationSettings =
                    FontVariation.Settings(
                        FontVariation.width(10f),
                        FontVariation.weight(200),
                    ),
                // Variation axes at the end of the animation, width 100, weight 500
                endFontVariationSettings =
                    FontVariation.Settings(
                        FontVariation.width(100f),
                        FontVariation.weight(500),
                    ),
                startFontSize = 10.sp,
                endFontSize = 50.sp,
            )
        AnimatedText(
            text = "Hello!",
            fontRegistry = animatedTextFontRegistry,
            contentAlignment = Alignment.Center,
            progressFraction = { progressFraction },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    private fun verifyScreenshot(content: @Composable () -> Unit) {
        rule.setContentWithTheme {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

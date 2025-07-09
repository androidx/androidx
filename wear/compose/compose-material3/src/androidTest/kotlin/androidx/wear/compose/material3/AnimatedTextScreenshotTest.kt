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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextDirection
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class AnimatedTextScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun animatedText_0f() = verifyScreenshot { BaseAnimatedText(0f) }

    @Test fun animatedText_0_25f() = verifyScreenshot { BaseAnimatedText(0.25f) }

    @Test fun animatedText_0_5f() = verifyScreenshot { BaseAnimatedText(0.5f) }

    @Test fun animatedText_0_75f() = verifyScreenshot { BaseAnimatedText(0.75f) }

    @Test fun animatedText_1f() = verifyScreenshot { BaseAnimatedText(1f) }

    @Test
    fun animatedText_rtl_language_unspecified_text_direction_0f() = verifyScreenshot {
        BaseAnimatedText(0f, "\u0641\u0627\u0631\u0633\u06cc")
    }

    @Test
    fun animatedText_rtl_language_unspecified_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(0.5f, "\u0641\u0627\u0631\u0633\u06cc")
    }

    @Test
    fun animatedText_rtl_language_unspecified_text_direction_1f() = verifyScreenshot {
        BaseAnimatedText(1f, "\u0641\u0627\u0631\u0633\u06cc")
    }

    @Test
    fun animatedText_rtl_language_rtl_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(0.5f, "\u0641\u0627\u0631\u0633\u06cc", textDirection = TextDirection.Rtl)
    }

    @Test
    fun animatedText_rtl_language_ltr_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(0.5f, "\u0641\u0627\u0631\u0633\u06cc", textDirection = TextDirection.Ltr)
    }

    @Test
    fun animatedText_rtl_language_content_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(
            0.5f,
            "\u0641\u0627\u0631\u0633\u06cc",
            textDirection = TextDirection.Content,
        )
    }

    @Test
    fun animatedText_ltr_language_rtl_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(progressFraction = 0.5f, textDirection = TextDirection.Rtl)
    }

    @Test
    fun animatedText_ltr_language_ltr_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(progressFraction = 0.5f, textDirection = TextDirection.Ltr)
    }

    @Test
    fun animatedText_ltr_language_content_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(progressFraction = 0.5f, textDirection = TextDirection.Content)
    }

    @Test
    fun animatedText_bidi_language_rtl_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(
            progressFraction = 0.5f,
            textDirection = TextDirection.Rtl,
            text = "Text: \u0641\u0627\u0631\u0633\u06cc",
        )
    }

    @Test
    fun animatedText_bidi_language_ltr_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(
            progressFraction = 0.5f,
            textDirection = TextDirection.Ltr,
            text = "Text: \u0641\u0627\u0631\u0633\u06cc",
        )
    }

    @Test
    fun animatedText_bidi_language_content_text_direction_0_5f() = verifyScreenshot {
        BaseAnimatedText(
            progressFraction = 0.5f,
            textDirection = TextDirection.Content,
            text = "Text: \u0641\u0627\u0631\u0633\u06cc",
        )
    }

    @Composable
    private fun BaseAnimatedText(
        progressFraction: Float,
        text: String = "Hello!",
        textDirection: TextDirection = TextDirection.Unspecified,
    ) {
        val animatedTextFontRegistry =
            rememberAnimatedTextFontRegistry(
                // Variation axes at the start of the animation, width 10, weight 200
                startFontVariationSettings =
                    FontVariation.Settings(FontVariation.width(10f), FontVariation.weight(200)),
                // Variation axes at the end of the animation, width 100, weight 500
                endFontVariationSettings =
                    FontVariation.Settings(FontVariation.width(100f), FontVariation.weight(500)),
                startFontSize = 10.sp,
                endFontSize = 50.sp,
                textStyle = LocalTextStyle.current.copy(textDirection = textDirection),
            )
        AnimatedText(
            text = text,
            fontRegistry = animatedTextFontRegistry,
            contentAlignment = Alignment.Center,
            progressFraction = { progressFraction },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    private fun verifyScreenshot(content: @Composable () -> Unit) {
        rule.setContentWithTheme {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }

        rule.verifyScreenshot(testName, screenshotRule)
    }
}

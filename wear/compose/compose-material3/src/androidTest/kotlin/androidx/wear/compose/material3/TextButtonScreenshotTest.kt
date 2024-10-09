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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
import androidx.wear.compose.material3.TextButtonShapes
import androidx.wear.compose.material3.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextButtonScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun filled_text_button_enabled() = verifyScreenshot { sampleFilledTextButton(enabled = true) }

    @Test
    fun filled_text_button_disabled() = verifyScreenshot { sampleFilledTextButton(enabled = false) }

    @Test
    fun filled_tonal_text_button_enabled() = verifyScreenshot {
        sampleFilledTonalTextButton(enabled = true)
    }

    @Test
    fun filled_tonal_text_button_disabled() = verifyScreenshot {
        sampleFilledTonalTextButton(enabled = false)
    }

    @Test
    fun outlined_text_button_enabled() = verifyScreenshot {
        sampleOutlinedTextButton(enabled = true)
    }

    @Test
    fun outlined_text_button_disabled() = verifyScreenshot {
        sampleOutlinedTextButton(enabled = false)
    }

    @Test fun text_button_enabled() = verifyScreenshot { sampleTextButton(enabled = true) }

    @Test fun text_button_disabled() = verifyScreenshot { sampleTextButton(enabled = false) }

    @Test
    fun text_button_with_offset() = verifyScreenshot {
        sampleTextButton(enabled = true, modifier = Modifier.offset(10.dp))
    }

    @Test
    fun text_button_with_corner_animation() = verifyScreenshot {
        sampleTextButton(
            shapes = TextButtonDefaults.animatedShapes(),
        )
    }

    @Test
    fun text_button_with_morph_animation() = verifyScreenshot {
        sampleTextButton(
            shapes =
                TextButtonDefaults.animatedShapes(
                    shape = CutCornerShape(15.dp),
                    pressedShape = RoundedCornerShape(15.dp)
                ),
        )
    }

    @Composable
    private fun sampleFilledTextButton(enabled: Boolean) {
        TextButton(
            onClick = {},
            colors = TextButtonDefaults.filledTextButtonColors(),
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        ) {
            Text(text = "ABC")
        }
    }

    @Composable
    private fun sampleFilledTonalTextButton(enabled: Boolean) {
        TextButton(
            onClick = {},
            colors = TextButtonDefaults.filledTonalTextButtonColors(),
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        ) {
            Text(text = "ABC")
        }
    }

    @Composable
    private fun sampleOutlinedTextButton(enabled: Boolean) {
        TextButton(
            onClick = {},
            colors = TextButtonDefaults.outlinedTextButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder(enabled),
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        ) {
            Text(text = "ABC")
        }
    }

    @Composable
    private fun sampleTextButton(
        enabled: Boolean = true,
        shapes: TextButtonShapes = TextButtonDefaults.shapes(),
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource? = null
    ) {
        TextButton(
            onClick = {},
            enabled = enabled,
            shapes = shapes,
            modifier = modifier.testTag(TEST_TAG),
            interactionSource = interactionSource
        ) {
            Text(text = "ABC")
        }
    }

    private fun verifyScreenshot(
        methodName: String = testName.methodName,
        content: @Composable () -> Unit
    ) {
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
            .assertAgainstGolden(screenshotRule, methodName)
    }
}

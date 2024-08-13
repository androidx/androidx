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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CenteredText
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.TestIcon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun button_enabled() = verifyScreenshot() { BaseButton() }

    @Test fun button_disabled() = verifyScreenshot() { BaseButton(enabled = false) }

    @Test
    fun three_slot_button_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { ThreeSlotButton() }

    @Test
    fun three_slot_button_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) { ThreeSlotButton() }

    @Test fun button_outlined_enabled() = verifyScreenshot() { OutlinedButton() }

    @Test fun button_outlined_disabled() = verifyScreenshot() { OutlinedButton(enabled = false) }

    @Test
    fun button_image_background_enabled() = verifyScreenshot {
        ImageBackgroundButton(size = Size.Unspecified)
    }

    @Test
    fun button_image_background_disabled() = verifyScreenshot {
        ImageBackgroundButton(enabled = false, size = Size.Unspecified)
    }

    @Test
    fun button_image_background_with_intrinsic_size() = verifyScreenshot {
        ImageBackgroundButton(size = null)
    }

    @Test
    fun button_label_only_center_aligned() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) }
        )
    }

    @Test
    fun button_icon_and_label_start_aligned() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { TestIcon() },
        )
    }

    @Test
    fun filled_tonal_button_label_only_center_aligned() = verifyScreenshot {
        FilledTonalButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) }
        )
    }

    @Test
    fun filled_tonal_button_icon_and_label_start_aligned() = verifyScreenshot {
        FilledTonalButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { TestIcon() },
        )
    }

    @Test
    fun outlined_tonal_button_label_only_center_aligned() = verifyScreenshot {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) }
        )
    }

    @Test
    fun outlined_tonal_button_icon_and_label_start_aligned() = verifyScreenshot {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { TestIcon() },
        )
    }

    @Test
    fun child_button_label_only_center_aligned() = verifyScreenshot {
        ChildButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) }
        )
    }

    @Test
    fun child_button_icon_and_label_start_aligned() = verifyScreenshot {
        ChildButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { TestIcon() },
        )
    }

    @Test fun compact_button_enabled() = verifyScreenshot { CompactButton() }

    @Test fun compact_button_disabled() = verifyScreenshot { CompactButton(enabled = false) }

    @Test
    fun compact_button_label_only_center_aligned() = verifyScreenshot {
        CompactButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) }
        )
    }

    @Test
    fun compact_button_icon_and_label_start_aligned() = verifyScreenshot {
        CompactButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { TestIcon() },
        )
    }

    @Composable
    private fun BaseButton(enabled: Boolean = true) {
        Button(enabled = enabled, onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {
            CenteredText("Base Button")
        }
    }

    @Composable
    private fun ThreeSlotButton(enabled: Boolean = true) {
        Button(
            enabled = enabled,
            onClick = {},
            label = { Text("Three Slot Button") },
            secondaryLabel = { Text("Secondary Label") },
            icon = { TestIcon() },
            modifier = Modifier.testTag(TEST_TAG)
        )
    }

    @Composable
    private fun OutlinedButton(enabled: Boolean = true) {
        OutlinedButton(enabled = enabled, onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {
            CenteredText("Outlined Button")
        }
    }

    @Composable
    private fun ImageBackgroundButton(enabled: Boolean = true, size: Size?) {
        Button(
            enabled = enabled,
            onClick = {},
            label = { Text("Image Button") },
            secondaryLabel = { Text("Secondary Label") },
            colors =
                ButtonDefaults.imageBackgroundButtonColors(
                    backgroundImagePainter = painterResource(R.drawable.backgroundimage1),
                    forcedSize = size
                ),
            icon = { TestIcon() },
            modifier = Modifier.testTag(TEST_TAG)
        )
    }

    @Composable
    private fun CompactButton(enabled: Boolean = true) {
        CompactButton(
            onClick = {},
            label = { Text("Compact Button") },
            icon = { TestIcon() },
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        )
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

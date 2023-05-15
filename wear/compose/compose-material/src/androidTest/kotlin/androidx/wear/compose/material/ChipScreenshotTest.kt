/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.wear.compose.material.test

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.OutlinedChip
import androidx.wear.compose.material.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material.TEST_TAG
import androidx.wear.compose.material.TestIcon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ChipScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun chip_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleChip()
    }

    @Test
    fun chip_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleChip()
    }

    @Test
    fun chip_secondary_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleChip(colors = ChipDefaults.secondaryChipColors())
    }

    @Test
    fun chip_secondary_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleChip(colors = ChipDefaults.secondaryChipColors())
    }

    @Test
    fun chip_outlined_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleOutlinedChip()
    }

    @Test
    fun chip_outlined_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleOutlinedChip()
    }

    @Test
    fun chip_disabled() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleChip(enabled = false)
    }

    @Test
    fun chip_gradient_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleChip(colors = ChipDefaults.gradientBackgroundChipColors())
    }

    @Test
    fun chip_gradient_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleChip(colors = ChipDefaults.gradientBackgroundChipColors())
    }

    @Test
    fun chip_image_background() = verifyScreenshot {
        sampleChip(colors = ChipDefaults.imageBackgroundChipColors(
            backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1)))
    }

    @Test
    fun compact_chip_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleCompactChip()
    }

    @Test
    fun compact_chip_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleCompactChip()
    }

    @Test
    fun compact_chip_disabled() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleCompactChip(enabled = false)
    }

    @Composable
    private fun sampleChip(
        enabled: Boolean = true,
        colors: ChipColors = ChipDefaults.primaryChipColors()
    ) {
        Chip(
            enabled = enabled,
            colors = colors,
            onClick = {},
            label = { Text("Standard chip") },
            secondaryLabel = { Text("Secondary text") },
            icon = { TestIcon() },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleOutlinedChip(
        enabled: Boolean = true,
        colors: ChipColors = ChipDefaults.outlinedChipColors()
    ) {
        OutlinedChip(
            enabled = enabled,
            colors = colors,
            onClick = {},
            label = { Text("Standard chip") },
            secondaryLabel = { Text("Secondary text") },
            icon = { TestIcon() },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleCompactChip(enabled: Boolean = true) {
        CompactChip(
            enabled = enabled,
            onClick = {},
            label = { Text("Compact chip") },
            icon = { TestIcon() },
            modifier = Modifier.testTag(TEST_TAG),
        )
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

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

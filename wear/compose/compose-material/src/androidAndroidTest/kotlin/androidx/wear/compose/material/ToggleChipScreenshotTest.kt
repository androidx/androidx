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
package androidx.wear.compose.material

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextOverflow
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
class ToggleChipScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun toggle_chip_checkbox() = verifyScreenshot {
        sampleToggleChip(checked = true)
    }

    @Test
    fun toggle_chip_checkbox_unchecked() = verifyScreenshot {
        sampleToggleChip(checked = false)
    }

    @Test
    fun toggle_chip_checkbox_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleToggleChip()
    }

    @Test
    fun toggle_chip_radio() = verifyScreenshot {
        val checked = true
        sampleToggleChip(
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.radioIcon(checked),
                    contentDescription = ""
                )
            },
            checked = checked
        )
    }

    @Test
    fun toggle_chip_radio_unchecked() = verifyScreenshot {
        val checked = false
        sampleToggleChip(
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.radioIcon(checked),
                    contentDescription = ""
                )
            },
            checked = checked,
        )
    }

    @Test
    fun toggle_chip_switch_checked() = verifyScreenshot {
        val checked = true
        sampleToggleChip(
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked),
                    contentDescription = ""
                )
            },
            checked = checked,
        )
    }

    @Test
    fun toggle_chip_switch_unchecked() = verifyScreenshot {
        val checked = false
        sampleToggleChip(
            colors = ToggleChipDefaults.toggleChipColors(
                uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
            ),
            toggleControl = {
                // For Switch  toggle controls the Wear Material UX guidance is to set the
                // unselected toggle control color to ToggleChipDefaults.switchUncheckedIconColor()
                // rather than the default.
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked),
                    contentDescription = ""
                )
            },
            checked = checked,
        )
    }

    @Test
    fun toggle_chip_disabled() = verifyScreenshot {
        sampleToggleChip(
            enabled = false,
        )
    }

    @Test
    fun split_toggle_chip() = verifyScreenshot {
        sampleSplitToggleChip()
    }

    @Test
    fun split_toggle_chip_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleSplitToggleChip()
    }

    @Test
    fun split_toggle_chip_disabled() = verifyScreenshot {
        sampleSplitToggleChip(enabled = false)
    }

    @Composable
    private fun sampleToggleChip(
        colors: ToggleChipColors = ToggleChipDefaults.toggleChipColors(),
        enabled: Boolean = true,
        checked: Boolean = true,
        toggleControl: @Composable () -> Unit = {
            Icon(
                imageVector = ToggleChipDefaults.checkboxIcon(checked = checked),
                contentDescription = ""
            )
        },
    ) {
        ToggleChip(
            appIcon = { TestIcon() },
            label = {
                Text("Standard toggle chip", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                Text("Secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            checked = checked,
            enabled = enabled,
            colors = colors,
            toggleControl = toggleControl,
            onCheckedChange = {},
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleSplitToggleChip(
        enabled: Boolean = true,
    ) {
        SplitToggleChip(
            label = {
                Text("Split chip", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                Text("Secondary", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            checked = true,
            enabled = enabled,
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.checkboxIcon(checked = true),
                    contentDescription = ""
                )
            },
            onCheckedChange = {},
            onClick = {},
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

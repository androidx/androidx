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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
class ToggleButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun toggle_button_checked_checkbox() = verifyScreenshot {
        sampleCheckboxButton(enabled = true, checked = true)
    }

    @Test
    fun toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleCheckboxButton(enabled = true, checked = false)
    }

    @Test
    fun toggle_button_checked_switch() = verifyScreenshot { sampleSwitchButton(checked = true) }

    @Test
    fun toggle_button_unchecked_switch() = verifyScreenshot { sampleSwitchButton(checked = false) }

    @Test
    fun toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleCheckboxButton(enabled = true, checked = true)
        }

    @Test
    fun toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleCheckboxButton(enabled = true, checked = false)
        }

    @Test
    fun toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSwitchButton(checked = true)
        }

    @Test
    fun toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSwitchButton(checked = false)
        }

    @Test
    fun disabled_toggle_button_checked_checkbox() = verifyScreenshot {
        sampleCheckboxButton(checked = true, enabled = false)
    }

    @Test
    fun disabled_toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleCheckboxButton(checked = false, enabled = false)
    }

    @Test
    fun disabled_toggle_button_checked_switch() = verifyScreenshot {
        sampleSwitchButton(checked = true, enabled = false)
    }

    @Test
    fun disabled_toggle_button_unchecked_switch() = verifyScreenshot {
        sampleSwitchButton(checked = false, enabled = false)
    }

    @Test
    fun disabled_toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleCheckboxButton(checked = true, enabled = false)
        }

    @Test
    fun disabled_toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleCheckboxButton(checked = false, enabled = false)
        }

    @Test
    fun disabled_toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSwitchButton(checked = true, enabled = false)
        }

    @Test
    fun disabled_toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSwitchButton(checked = false, enabled = false)
        }

    @Test
    fun split_toggle_button_checked_checkbox() = verifyScreenshot {
        sampleSplitCheckboxButton(checked = true)
    }

    @Test
    fun split_toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleSplitCheckboxButton(checked = false)
    }

    @Test
    fun split_toggle_button_checked_switch() = verifyScreenshot {
        sampleSplitSwitchButton(checked = true)
    }

    @Test
    fun split_toggle_button_unchecked_switch() = verifyScreenshot {
        sampleSplitSwitchButton(checked = false)
    }

    @Test
    fun split_toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitCheckboxButton(checked = true)
        }

    @Test
    fun split_toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitCheckboxButton(checked = false)
        }

    @Test
    fun split_toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitSwitchButton(checked = true)
        }

    @Test
    fun split_toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitSwitchButton(checked = false)
        }

    @Test
    fun disabled_split_toggle_button_checked_checkbox() = verifyScreenshot {
        sampleSplitCheckboxButton(checked = true, enabled = false)
    }

    @Test
    fun disabled_split_toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleSplitCheckboxButton(checked = false, enabled = false)
    }

    @Test
    fun disabled_split_toggle_button_checked_switch() = verifyScreenshot {
        sampleSplitSwitchButton(checked = true, enabled = false)
    }

    @Test
    fun disabled_split_toggle_button_unchecked_switch() = verifyScreenshot {
        sampleSplitSwitchButton(checked = false, enabled = false)
    }

    @Test
    fun disabled_split_toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitCheckboxButton(checked = true, enabled = false)
        }

    @Test
    fun disabled_split_toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitCheckboxButton(checked = false, enabled = false)
        }

    @Test
    fun disabled_split_toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitSwitchButton(checked = true, enabled = false)
        }

    @Test
    fun disabled_split_toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitSwitchButton(checked = false, enabled = false)
        }

    @Test
    fun split_checkbox_button_customized_padding_6dp() = verifyScreenshot {
        sampleSplitCheckboxButton(contentPadding = PaddingValues(6.dp))
    }

    @Test
    fun split_checkbox_button_customized_horizontal_padding_24dp() = verifyScreenshot {
        sampleSplitCheckboxButton(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        )
    }

    @Test
    fun split_switch_button_customized_padding_6dp() = verifyScreenshot {
        sampleSplitSwitchButton(contentPadding = PaddingValues(6.dp))
    }

    @Test
    fun split_switch_button_customized_horizontal_padding_24dp() = verifyScreenshot {
        sampleSplitSwitchButton(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp))
    }

    @Test
    fun toggle_button_checkbox_inset_focus_ring() {
        val checkedInteractionSource = MutableInteractionSource()
        val uncheckedInteractionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalRippleThemeConfiguration provides
                    RippleDefaults.InsetFocusRingThemeConfiguration
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.testTag(TEST_TAG),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CheckboxButton(
                            icon = { TestIcon() },
                            label = { Text("ToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = true,
                            onCheckedChange = {},
                            interactionSource = checkedInteractionSource,
                        )
                        CheckboxButton(
                            icon = { TestIcon() },
                            label = { Text("ToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = false,
                            onCheckedChange = {},
                            interactionSource = uncheckedInteractionSource,
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            checkedInteractionSource.tryEmit(FocusInteraction.Focus())
            uncheckedInteractionSource.tryEmit(FocusInteraction.Focus())
        }
        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun split_toggle_button_checkbox_inset_focus_ring() {
        val containerInteractionSource = MutableInteractionSource()
        val toggleInteractionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalRippleThemeConfiguration provides
                    RippleDefaults.InsetFocusRingThemeConfiguration
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.testTag(TEST_TAG),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SplitCheckboxButton(
                            label = { Text("SplitToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = true,
                            onCheckedChange = {},
                            onContainerClick = {},
                            toggleContentDescription = "",
                            containerInteractionSource = containerInteractionSource,
                        )
                        SplitCheckboxButton(
                            label = { Text("SplitToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = false,
                            onCheckedChange = {},
                            onContainerClick = {},
                            toggleContentDescription = "",
                            toggleInteractionSource = toggleInteractionSource,
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            containerInteractionSource.tryEmit(FocusInteraction.Focus())
            toggleInteractionSource.tryEmit(FocusInteraction.Focus())
        }
        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun toggle_button_switch_inset_focus_ring() {
        val checkedInteractionSource = MutableInteractionSource()
        val uncheckedInteractionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalRippleThemeConfiguration provides
                    RippleDefaults.InsetFocusRingThemeConfiguration
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.testTag(TEST_TAG),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SwitchButton(
                            icon = { TestIcon() },
                            label = { Text("ToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = true,
                            onCheckedChange = {},
                            interactionSource = checkedInteractionSource,
                        )
                        SwitchButton(
                            icon = { TestIcon() },
                            label = { Text("ToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = false,
                            onCheckedChange = {},
                            interactionSource = uncheckedInteractionSource,
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            checkedInteractionSource.tryEmit(FocusInteraction.Focus())
            uncheckedInteractionSource.tryEmit(FocusInteraction.Focus())
        }
        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun split_toggle_button_switch_inset_focus_ring() {
        val containerInteractionSource = MutableInteractionSource()
        val toggleInteractionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalRippleThemeConfiguration provides
                    RippleDefaults.InsetFocusRingThemeConfiguration
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.testTag(TEST_TAG),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SplitSwitchButton(
                            label = { Text("SplitToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = true,
                            onCheckedChange = {},
                            onContainerClick = {},
                            toggleContentDescription = "",
                            containerInteractionSource = containerInteractionSource,
                        )
                        SplitSwitchButton(
                            label = { Text("SplitToggleButton") },
                            secondaryLabel = { Text("Secondary label") },
                            checked = false,
                            onCheckedChange = {},
                            onContainerClick = {},
                            toggleContentDescription = "",
                            toggleInteractionSource = toggleInteractionSource,
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            containerInteractionSource.tryEmit(FocusInteraction.Focus())
            toggleInteractionSource.tryEmit(FocusInteraction.Focus())
        }
        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Composable
    private fun sampleCheckboxButton(enabled: Boolean = true, checked: Boolean = true) {
        CheckboxButton(
            icon = { TestIcon() },
            label = { Text("ToggleButton") },
            secondaryLabel = { Text("Secondary label") },
            checked = checked,
            enabled = enabled,
            onCheckedChange = {},
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleSplitCheckboxButton(
        checked: Boolean = true,
        enabled: Boolean = true,
        contentPadding: PaddingValues = CheckboxButtonDefaults.ContentPadding,
    ) {
        SplitCheckboxButton(
            label = { Text("SplitToggleButton") },
            secondaryLabel = { Text("Secondary label") },
            checked = checked,
            enabled = enabled,
            onCheckedChange = {},
            onContainerClick = {},
            toggleContentDescription = "",
            modifier = Modifier.testTag(TEST_TAG),
            contentPadding = contentPadding,
        )
    }

    @Composable
    private fun sampleSwitchButton(enabled: Boolean = true, checked: Boolean = true) {
        SwitchButton(
            icon = { TestIcon() },
            label = { Text("ToggleButton") },
            secondaryLabel = { Text("Secondary label") },
            checked = checked,
            enabled = enabled,
            onCheckedChange = {},
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleSplitSwitchButton(
        checked: Boolean = true,
        enabled: Boolean = true,
        contentPadding: PaddingValues = SwitchButtonDefaults.ContentPadding,
    ) {
        SplitSwitchButton(
            label = { Text("SplitToggleButton") },
            secondaryLabel = { Text("Secondary label") },
            checked = checked,
            enabled = enabled,
            onCheckedChange = {},
            onContainerClick = {},
            toggleContentDescription = "",
            modifier = Modifier.testTag(TEST_TAG),
            contentPadding = contentPadding,
        )
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(modifier = Modifier.background(Color.Black)) { content() }
            }
        }

        rule.verifyScreenshot(testName, screenshotRule)
    }
}

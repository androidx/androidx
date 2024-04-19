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
import androidx.annotation.RequiresApi
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ToggleControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun checkbox_supports_testtag() {
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Checkbox(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun checkbox_is_expected_size() {
        rule
            .setContentWithThemeForSizeAssertions {
                with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                    Checkbox(modifier = Modifier.testTag(TEST_TAG))
                }
            }
            .assertHeightIsEqualTo(TOGGLE_CONTROL_HEIGHT)
            .assertWidthIsEqualTo(TOGGLE_CONTROL_WIDTH)
    }

    @Test
    fun checkbox_has_role_checkbox_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Checkbox(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox
                )
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_checked_colors_are_customisable() {
        val boxColor = Color.Green
        val checkmarkColor = Color.Blue
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Checkbox(
                    colors = CheckboxDefaults.colors(
                        checkedBoxColor = boxColor,
                        checkedCheckmarkColor = checkmarkColor
                    ),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
        checkboxImage.assertContainsColor(checkmarkColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_unchecked_colors_are_customisable() {
        // NB checkmark is erased during animation, so we don't test uncheckedCheckmarkColor
        // as it is just used as a target color for the animation.
        val boxColor = Color.Green
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = false)) {
                Checkbox(
                    colors = CheckboxDefaults.colors(
                        uncheckedBoxColor = boxColor,
                    ),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
    }

    @Test
    fun switch_supports_testtag() {
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Switch(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun switch_is_expected_size() {
        rule
            .setContentWithThemeForSizeAssertions {
                with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                    Switch(modifier = Modifier.testTag(TEST_TAG))
                }
            }
            .assertHeightIsEqualTo(TOGGLE_CONTROL_HEIGHT)
            .assertWidthIsEqualTo(TOGGLE_CONTROL_WIDTH)
    }

    @Test
    fun switch_has_role_switch_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Switch(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Switch
                )
            )
    }

    @Test
    fun switch_has_no_clickaction_by_default() {
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Switch(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_checked_colors_are_customisable() {
        val thumbColor = Color.Green
        val thumbIconColor = Color.Yellow
        val trackColor = Color.Blue
        val trackStrokeColor = Color.Red
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = true)) {
                Switch(
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = thumbColor,
                        checkedThumbIconColor = thumbIconColor,
                        checkedTrackColor = trackColor,
                        checkedTrackBorderColor = trackStrokeColor
                    ),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(thumbColor)
        image.assertContainsColor(thumbIconColor)
        image.assertContainsColor(trackColor)
        image.assertContainsColor(trackStrokeColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_unchecked_colors_are_customisable() {
        val thumbColor = Color.Green
        val thumbIconColor = Color.Yellow
        val trackColor = Color.Blue
        val trackStrokeColor = Color.Red
        rule.setContentWithTheme {
            with(ToggleControlScope(isEnabled = true, isChecked = false)) {
                Switch(
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = thumbColor,
                        uncheckedThumbIconColor = thumbIconColor,
                        uncheckedTrackColor = trackColor,
                        uncheckedTrackBorderColor = trackStrokeColor
                    ),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.waitForIdle()
        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(thumbColor)
        // NB opacity of icon is decreased to 0 in unchecked state,
        // hence we are asserting that the thumbIconColor should not exist.
        image.assertDoesNotContainColor(thumbIconColor)
        image.assertContainsColor(trackColor)
    }
}

private val TOGGLE_CONTROL_WIDTH = 32.dp
private val TOGGLE_CONTROL_HEIGHT = 24.dp

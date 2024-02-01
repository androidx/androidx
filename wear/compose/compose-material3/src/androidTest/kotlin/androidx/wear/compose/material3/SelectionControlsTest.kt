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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class SelectionControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun radio_control_supports_testtag() {
        rule.setContentWithTheme {
            with(SelectionControlScope(isEnabled = true, isSelected = true)) {
                Radio(
                    Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun radio_control_is_expected_size() {
        rule
            .setContentWithThemeForSizeAssertions {
                with(SelectionControlScope(isEnabled = true, isSelected = true)) {
                    Radio(
                        modifier = Modifier.testTag(TEST_TAG)
                    )
                }
            }
            .assertHeightIsEqualTo(SELECTION_CONTROL_HEIGHT)
            .assertWidthIsEqualTo(SELECTION_CONTROL_WIDTH)
    }

    @Test
    fun radio_control_has_role_radiobutton() {
        rule.setContentWithTheme {
            with(SelectionControlScope(isEnabled = true, isSelected = true)) {
                Radio(
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton
                )
            )
    }

    @Test
    fun radio_control_is_correctly_enabled() {
        rule.setContentWithTheme {
            with(SelectionControlScope(isEnabled = true, isSelected = true)) {
                Radio(
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radio_control_selected_colors_are_customisable() {
        val color = Color.Green
        rule.setContentWithTheme {
            with(SelectionControlScope(isEnabled = true, isSelected = true)) {
                Radio(
                    colors = RadioDefaults.colors(
                        selectedColor = color
                    ),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(color)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radio_control_unselected_colors_are_customisable() {
        // NB Dot is erased during animation, so we don't test uncheckedDotColor
        // as it is just used as a target color for the animation.
        val color = Color.Green
        rule.setContentWithTheme {
            with(SelectionControlScope(isEnabled = true, isSelected = false)) {
                Radio(
                    colors = RadioDefaults.colors(
                        unselectedColor = color,
                    ),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(color)
    }
}

private val SELECTION_CONTROL_WIDTH = 32.dp
private val SELECTION_CONTROL_HEIGHT = 24.dp

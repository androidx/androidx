/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteCheckboxButtonColorsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun checkboxButtonDefaults_values() {
        assertThat(RemoteCheckboxButtonDefaults.Height.constantValue)
            .isEqualTo(52.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.IconSpacing.constantValue)
            .isEqualTo(6.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.IconSize.constantValue)
            .isEqualTo(24.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.LabelSpacerSize.constantValue)
            .isEqualTo(1.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.BoxSize.constantValue)
            .isEqualTo(18.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.BoxStroke.constantValue)
            .isEqualTo(2.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.BoxRadius.constantValue)
            .isEqualTo(2.rdp.constantValue)
        assertThat(RemoteCheckboxButtonDefaults.BoxOuterSize.constantValue)
            .isEqualTo(24.rdp.constantValue)
    }

    @Test
    fun checkboxButtonColors_resolvesCorrectly() {
        val checkedContainer = RemoteColor.Red
        val uncheckedContainer = RemoteColor.Blue
        val disabledCheckedContainer = RemoteColor.Gray
        val disabledUncheckedContainer = RemoteColor.DarkGray

        val colors =
            RemoteCheckboxButtonColors(
                checkedContainerColor = checkedContainer,
                checkedContentColor = RemoteColor.White,
                checkedSecondaryContentColor = RemoteColor.LightGray,
                checkedIconColor = RemoteColor.Yellow,
                checkedBoxColor = RemoteColor.Cyan,
                checkedCheckmarkColor = RemoteColor.Black,
                uncheckedContainerColor = uncheckedContainer,
                uncheckedContentColor = RemoteColor.White,
                uncheckedSecondaryContentColor = RemoteColor.LightGray,
                uncheckedIconColor = RemoteColor.Yellow,
                uncheckedBoxColor = RemoteColor.Magenta,
                disabledCheckedContainerColor = disabledCheckedContainer,
                disabledCheckedContentColor = RemoteColor.DarkGray,
                disabledCheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledCheckedIconColor = RemoteColor.DarkGray,
                disabledCheckedBoxColor = RemoteColor.DarkGray,
                disabledCheckedCheckmarkColor = RemoteColor.DarkGray,
                disabledUncheckedContainerColor = disabledUncheckedContainer,
                disabledUncheckedContentColor = RemoteColor.DarkGray,
                disabledUncheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUncheckedIconColor = RemoteColor.DarkGray,
                disabledUncheckedBoxColor = RemoteColor.DarkGray,
            )

        // Enabled + Checked
        val color1 = colors.containerColor(enabled = true.rb, checked = true.rb)
        assertThat(color1.constantValue).isEqualTo(checkedContainer.constantValue)

        // Enabled + Unchecked
        val color2 = colors.containerColor(enabled = true.rb, checked = false.rb)
        assertThat(color2.constantValue).isEqualTo(uncheckedContainer.constantValue)

        // Disabled + Checked
        val color3 = colors.containerColor(enabled = false.rb, checked = true.rb)
        assertThat(color3.constantValue).isEqualTo(disabledCheckedContainer.constantValue)

        // Disabled + Unchecked
        val color4 = colors.containerColor(enabled = false.rb, checked = false.rb)
        assertThat(color4.constantValue).isEqualTo(disabledUncheckedContainer.constantValue)
    }

    @Test
    fun checkboxButtonColors_equality() {
        val colors1 =
            RemoteCheckboxButtonColors(
                checkedContainerColor = RemoteColor.Red,
                checkedContentColor = RemoteColor.White,
                checkedSecondaryContentColor = RemoteColor.LightGray,
                checkedIconColor = RemoteColor.Yellow,
                checkedBoxColor = RemoteColor.Cyan,
                checkedCheckmarkColor = RemoteColor.Black,
                uncheckedContainerColor = RemoteColor.Blue,
                uncheckedContentColor = RemoteColor.White,
                uncheckedSecondaryContentColor = RemoteColor.LightGray,
                uncheckedIconColor = RemoteColor.Yellow,
                uncheckedBoxColor = RemoteColor.Magenta,
                disabledCheckedContainerColor = RemoteColor.Gray,
                disabledCheckedContentColor = RemoteColor.DarkGray,
                disabledCheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledCheckedIconColor = RemoteColor.DarkGray,
                disabledCheckedBoxColor = RemoteColor.DarkGray,
                disabledCheckedCheckmarkColor = RemoteColor.DarkGray,
                disabledUncheckedContainerColor = RemoteColor.DarkGray,
                disabledUncheckedContentColor = RemoteColor.DarkGray,
                disabledUncheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUncheckedIconColor = RemoteColor.DarkGray,
                disabledUncheckedBoxColor = RemoteColor.DarkGray,
            )

        val colors2 = colors1.copy()
        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())

        val colors3 = colors1.copy(checkedContainerColor = RemoteColor.Green)
        assertThat(colors3).isNotEqualTo(colors1)
    }

    @Test
    fun checkboxButtonColors_defaultsCheckmarkColorToPrimaryContainer() {
        lateinit var colors: RemoteCheckboxButtonColors
        lateinit var splitColors: RemoteSplitCheckboxButtonColors
        val expectedPrimaryContainer = RemoteColor.Green
        val expectedBackground = RemoteColor.Magenta
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme()
                        .copy(
                            primaryContainer = expectedPrimaryContainer,
                            background = expectedBackground,
                        )
            ) {
                colors = RemoteCheckboxButtonDefaults.checkboxButtonColors()
                splitColors = RemoteSplitCheckboxButtonDefaults.splitCheckboxButtonColors()
            }
        }

        assertThat(colors.checkedCheckmarkColor.constantValue)
            .isEqualTo(expectedPrimaryContainer.constantValue)
        assertThat(colors.disabledCheckedCheckmarkColor.constantValue)
            .isEqualTo(splitColors.disabledCheckedCheckmarkColor.constantValue)
    }

    @Test
    fun outlinedCardBorderColor_defaultsToOutline() {
        lateinit var borderColor: RemoteColor
        val expectedOutline = RemoteColor.Cyan
        composeTestRule.setContent {
            RemoteMaterialTheme(colorScheme = RemoteColorScheme().copy(outline = expectedOutline)) {
                borderColor = RemoteCardDefaults.outlinedCardBorderColor
            }
        }

        assertThat(borderColor.constantValue).isEqualTo(expectedOutline.constantValue)
    }
}

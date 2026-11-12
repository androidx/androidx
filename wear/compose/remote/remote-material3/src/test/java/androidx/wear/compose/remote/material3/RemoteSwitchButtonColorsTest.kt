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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteSwitchButtonColorsTest {

    @Test
    fun switchButtonDefaults_values() {
        assertThat(RemoteSwitchButtonDefaults.Height.constantValue).isEqualTo(52.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.IconSpacing.constantValue)
            .isEqualTo(6.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.IconSize.constantValue)
            .isEqualTo(24.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.LabelSpacerSize.constantValue)
            .isEqualTo(1.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.SwitchWidth.constantValue)
            .isEqualTo(32.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.SwitchHeight.constantValue)
            .isEqualTo(24.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.SwitchTrackWidth.constantValue)
            .isEqualTo(2.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.SwitchInnerHeight.constantValue)
            .isEqualTo(22.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.ThumbRadiusChecked.constantValue)
            .isEqualTo(9.rdp.constantValue)
        assertThat(RemoteSwitchButtonDefaults.ThumbRadiusUnchecked.constantValue)
            .isEqualTo(6.rdp.constantValue)
    }

    @Test
    fun switchButtonColors_resolvesCorrectly() {
        val checkedContainer = RemoteColor.Red
        val uncheckedContainer = RemoteColor.Blue
        val disabledCheckedContainer = RemoteColor.Gray
        val disabledUncheckedContainer = RemoteColor.DarkGray

        val colors =
            RemoteSwitchButtonColors(
                checkedContainerColor = checkedContainer,
                checkedContentColor = RemoteColor.White,
                checkedSecondaryContentColor = RemoteColor.LightGray,
                checkedIconColor = RemoteColor.Yellow,
                checkedThumbColor = RemoteColor.Cyan,
                checkedThumbIconColor = RemoteColor.Black,
                checkedTrackColor = RemoteColor.Blue,
                checkedTrackBorderColor = RemoteColor.White,
                uncheckedContainerColor = uncheckedContainer,
                uncheckedContentColor = RemoteColor.White,
                uncheckedSecondaryContentColor = RemoteColor.LightGray,
                uncheckedIconColor = RemoteColor.Yellow,
                uncheckedThumbColor = RemoteColor.Magenta,
                uncheckedTrackColor = RemoteColor.DarkGray,
                uncheckedTrackBorderColor = RemoteColor.Gray,
                disabledCheckedContainerColor = disabledCheckedContainer,
                disabledCheckedContentColor = RemoteColor.DarkGray,
                disabledCheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledCheckedIconColor = RemoteColor.DarkGray,
                disabledCheckedThumbColor = RemoteColor.DarkGray,
                disabledCheckedThumbIconColor = RemoteColor.DarkGray,
                disabledCheckedTrackColor = RemoteColor.DarkGray,
                disabledCheckedTrackBorderColor = RemoteColor.DarkGray,
                disabledUncheckedContainerColor = disabledUncheckedContainer,
                disabledUncheckedContentColor = RemoteColor.DarkGray,
                disabledUncheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUncheckedIconColor = RemoteColor.DarkGray,
                disabledUncheckedThumbColor = RemoteColor.DarkGray,
                disabledUncheckedTrackBorderColor = RemoteColor.DarkGray,
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
    fun switchButtonColors_equality() {
        val colors1 =
            RemoteSwitchButtonColors(
                checkedContainerColor = RemoteColor.Red,
                checkedContentColor = RemoteColor.White,
                checkedSecondaryContentColor = RemoteColor.LightGray,
                checkedIconColor = RemoteColor.Yellow,
                checkedThumbColor = RemoteColor.Cyan,
                checkedThumbIconColor = RemoteColor.Black,
                checkedTrackColor = RemoteColor.Blue,
                checkedTrackBorderColor = RemoteColor.White,
                uncheckedContainerColor = RemoteColor.Blue,
                uncheckedContentColor = RemoteColor.White,
                uncheckedSecondaryContentColor = RemoteColor.LightGray,
                uncheckedIconColor = RemoteColor.Yellow,
                uncheckedThumbColor = RemoteColor.Magenta,
                uncheckedTrackColor = RemoteColor.DarkGray,
                uncheckedTrackBorderColor = RemoteColor.Gray,
                disabledCheckedContainerColor = RemoteColor.Gray,
                disabledCheckedContentColor = RemoteColor.DarkGray,
                disabledCheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledCheckedIconColor = RemoteColor.DarkGray,
                disabledCheckedThumbColor = RemoteColor.DarkGray,
                disabledCheckedThumbIconColor = RemoteColor.DarkGray,
                disabledCheckedTrackColor = RemoteColor.DarkGray,
                disabledCheckedTrackBorderColor = RemoteColor.DarkGray,
                disabledUncheckedContainerColor = RemoteColor.DarkGray,
                disabledUncheckedContentColor = RemoteColor.DarkGray,
                disabledUncheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUncheckedIconColor = RemoteColor.DarkGray,
                disabledUncheckedThumbColor = RemoteColor.DarkGray,
                disabledUncheckedTrackBorderColor = RemoteColor.DarkGray,
            )

        val colors2 = colors1.copy()
        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())

        val colors3 = colors1.copy(checkedContainerColor = RemoteColor.Green)
        assertThat(colors3).isNotEqualTo(colors1)
    }
}

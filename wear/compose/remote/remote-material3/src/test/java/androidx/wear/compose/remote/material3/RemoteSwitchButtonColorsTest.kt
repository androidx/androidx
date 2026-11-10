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
import androidx.compose.ui.graphics.Color
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
        val checkedContainer = RemoteColor(Color.Red)
        val uncheckedContainer = RemoteColor(Color.Blue)
        val disabledCheckedContainer = RemoteColor(Color.Gray)
        val disabledUncheckedContainer = RemoteColor(Color.DarkGray)

        val colors =
            RemoteSwitchButtonColors(
                checkedContainerColor = checkedContainer,
                checkedContentColor = RemoteColor(Color.White),
                checkedSecondaryContentColor = RemoteColor(Color.LightGray),
                checkedIconColor = RemoteColor(Color.Yellow),
                checkedThumbColor = RemoteColor(Color.Cyan),
                checkedThumbIconColor = RemoteColor(Color.Black),
                checkedTrackColor = RemoteColor(Color.Blue),
                checkedTrackBorderColor = RemoteColor(Color.White),
                uncheckedContainerColor = uncheckedContainer,
                uncheckedContentColor = RemoteColor(Color.White),
                uncheckedSecondaryContentColor = RemoteColor(Color.LightGray),
                uncheckedIconColor = RemoteColor(Color.Yellow),
                uncheckedThumbColor = RemoteColor(Color.Magenta),
                uncheckedTrackColor = RemoteColor(Color.DarkGray),
                uncheckedTrackBorderColor = RemoteColor(Color.Gray),
                disabledCheckedContainerColor = disabledCheckedContainer,
                disabledCheckedContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedIconColor = RemoteColor(Color.DarkGray),
                disabledCheckedThumbColor = RemoteColor(Color.DarkGray),
                disabledCheckedThumbIconColor = RemoteColor(Color.DarkGray),
                disabledCheckedTrackColor = RemoteColor(Color.DarkGray),
                disabledCheckedTrackBorderColor = RemoteColor(Color.DarkGray),
                disabledUncheckedContainerColor = disabledUncheckedContainer,
                disabledUncheckedContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedIconColor = RemoteColor(Color.DarkGray),
                disabledUncheckedThumbColor = RemoteColor(Color.DarkGray),
                disabledUncheckedTrackBorderColor = RemoteColor(Color.DarkGray),
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
                checkedContainerColor = RemoteColor(Color.Red),
                checkedContentColor = RemoteColor(Color.White),
                checkedSecondaryContentColor = RemoteColor(Color.LightGray),
                checkedIconColor = RemoteColor(Color.Yellow),
                checkedThumbColor = RemoteColor(Color.Cyan),
                checkedThumbIconColor = RemoteColor(Color.Black),
                checkedTrackColor = RemoteColor(Color.Blue),
                checkedTrackBorderColor = RemoteColor(Color.White),
                uncheckedContainerColor = RemoteColor(Color.Blue),
                uncheckedContentColor = RemoteColor(Color.White),
                uncheckedSecondaryContentColor = RemoteColor(Color.LightGray),
                uncheckedIconColor = RemoteColor(Color.Yellow),
                uncheckedThumbColor = RemoteColor(Color.Magenta),
                uncheckedTrackColor = RemoteColor(Color.DarkGray),
                uncheckedTrackBorderColor = RemoteColor(Color.Gray),
                disabledCheckedContainerColor = RemoteColor(Color.Gray),
                disabledCheckedContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedIconColor = RemoteColor(Color.DarkGray),
                disabledCheckedThumbColor = RemoteColor(Color.DarkGray),
                disabledCheckedThumbIconColor = RemoteColor(Color.DarkGray),
                disabledCheckedTrackColor = RemoteColor(Color.DarkGray),
                disabledCheckedTrackBorderColor = RemoteColor(Color.DarkGray),
                disabledUncheckedContainerColor = RemoteColor(Color.DarkGray),
                disabledUncheckedContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedIconColor = RemoteColor(Color.DarkGray),
                disabledUncheckedThumbColor = RemoteColor(Color.DarkGray),
                disabledUncheckedTrackBorderColor = RemoteColor(Color.DarkGray),
            )

        val colors2 = colors1.copy()
        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())

        val colors3 = colors1.copy(checkedContainerColor = RemoteColor(Color.Green))
        assertThat(colors3).isNotEqualTo(colors1)
    }
}

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
class RemoteSplitCheckboxButtonColorsTest {

    @Test
    fun splitCheckboxButtonDefaults_values() {
        assertThat(RemoteSplitCheckboxButtonDefaults.Height.constantValue)
            .isEqualTo(52.rdp.constantValue)
    }

    @Test
    fun splitCheckboxButtonColors_resolvesCorrectly() {
        val checkedContainer = RemoteColor.Red
        val uncheckedContainer = RemoteColor.Blue
        val disabledCheckedContainer = RemoteColor.Gray
        val disabledUncheckedContainer = RemoteColor.DarkGray

        val colors =
            RemoteSplitCheckboxButtonColors(
                checkedContainerColor = checkedContainer,
                checkedContentColor = RemoteColor.White,
                checkedSecondaryContentColor = RemoteColor.LightGray,
                checkedSplitContainerColor = RemoteColor.Yellow,
                checkedBoxColor = RemoteColor.Cyan,
                checkedCheckmarkColor = RemoteColor.Black,
                uncheckedContainerColor = uncheckedContainer,
                uncheckedContentColor = RemoteColor.White,
                uncheckedSecondaryContentColor = RemoteColor.LightGray,
                uncheckedSplitContainerColor = RemoteColor.Yellow,
                uncheckedBoxColor = RemoteColor.Magenta,
                disabledCheckedContainerColor = disabledCheckedContainer,
                disabledCheckedContentColor = RemoteColor.DarkGray,
                disabledCheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledCheckedSplitContainerColor = RemoteColor.DarkGray,
                disabledCheckedBoxColor = RemoteColor.DarkGray,
                disabledCheckedCheckmarkColor = RemoteColor.DarkGray,
                disabledUncheckedContainerColor = disabledUncheckedContainer,
                disabledUncheckedContentColor = RemoteColor.DarkGray,
                disabledUncheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUncheckedSplitContainerColor = RemoteColor.DarkGray,
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
    fun splitCheckboxButtonColors_equality() {
        val colors1 =
            RemoteSplitCheckboxButtonColors(
                checkedContainerColor = RemoteColor.Red,
                checkedContentColor = RemoteColor.White,
                checkedSecondaryContentColor = RemoteColor.LightGray,
                checkedSplitContainerColor = RemoteColor.Yellow,
                checkedBoxColor = RemoteColor.Cyan,
                checkedCheckmarkColor = RemoteColor.Black,
                uncheckedContainerColor = RemoteColor.Blue,
                uncheckedContentColor = RemoteColor.White,
                uncheckedSecondaryContentColor = RemoteColor.LightGray,
                uncheckedSplitContainerColor = RemoteColor.Yellow,
                uncheckedBoxColor = RemoteColor.Magenta,
                disabledCheckedContainerColor = RemoteColor.Gray,
                disabledCheckedContentColor = RemoteColor.DarkGray,
                disabledCheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledCheckedSplitContainerColor = RemoteColor.DarkGray,
                disabledCheckedBoxColor = RemoteColor.DarkGray,
                disabledCheckedCheckmarkColor = RemoteColor.DarkGray,
                disabledUncheckedContainerColor = RemoteColor.DarkGray,
                disabledUncheckedContentColor = RemoteColor.DarkGray,
                disabledUncheckedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUncheckedSplitContainerColor = RemoteColor.DarkGray,
                disabledUncheckedBoxColor = RemoteColor.DarkGray,
            )

        val colors2 = colors1.copy()
        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())

        val colors3 = colors1.copy(checkedContainerColor = RemoteColor.Green)
        assertThat(colors3).isNotEqualTo(colors1)
    }
}

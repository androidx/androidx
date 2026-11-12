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
class RemoteSplitRadioButtonColorsTest {

    @Test
    fun splitRadioButtonDefaults_values() {
        assertThat(RemoteSplitRadioButtonDefaults.Height.constantValue)
            .isEqualTo(52.rdp.constantValue)
    }

    @Test
    fun splitRadioButtonColors_resolvesCorrectly() {
        val selectedContainer = RemoteColor.Red
        val unselectedContainer = RemoteColor.Blue
        val disabledSelectedContainer = RemoteColor.Gray
        val disabledUnselectedContainer = RemoteColor.DarkGray

        val colors =
            RemoteSplitRadioButtonColors(
                selectedContainerColor = selectedContainer,
                selectedContentColor = RemoteColor.White,
                selectedSecondaryContentColor = RemoteColor.LightGray,
                selectedSplitContainerColor = RemoteColor.Yellow,
                selectedControlColor = RemoteColor.Cyan,
                unselectedContainerColor = unselectedContainer,
                unselectedContentColor = RemoteColor.White,
                unselectedSecondaryContentColor = RemoteColor.LightGray,
                unselectedSplitContainerColor = RemoteColor.Yellow,
                unselectedControlColor = RemoteColor.Magenta,
                disabledSelectedContainerColor = disabledSelectedContainer,
                disabledSelectedContentColor = RemoteColor.DarkGray,
                disabledSelectedSecondaryContentColor = RemoteColor.DarkGray,
                disabledSelectedSplitContainerColor = RemoteColor.DarkGray,
                disabledSelectedControlColor = RemoteColor.DarkGray,
                disabledUnselectedContainerColor = disabledUnselectedContainer,
                disabledUnselectedContentColor = RemoteColor.DarkGray,
                disabledUnselectedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUnselectedSplitContainerColor = RemoteColor.DarkGray,
                disabledUnselectedControlColor = RemoteColor.DarkGray,
            )

        // Enabled + Selected
        val color1 = colors.containerColor(enabled = true.rb, selected = true.rb)
        assertThat(color1.constantValue).isEqualTo(selectedContainer.constantValue)

        // Enabled + Unselected
        val color2 = colors.containerColor(enabled = true.rb, selected = false.rb)
        assertThat(color2.constantValue).isEqualTo(unselectedContainer.constantValue)

        // Disabled + Selected
        val color3 = colors.containerColor(enabled = false.rb, selected = true.rb)
        assertThat(color3.constantValue).isEqualTo(disabledSelectedContainer.constantValue)

        // Disabled + Unselected
        val color4 = colors.containerColor(enabled = false.rb, selected = false.rb)
        assertThat(color4.constantValue).isEqualTo(disabledUnselectedContainer.constantValue)
    }

    @Test
    fun splitRadioButtonColors_equality() {
        val colors1 =
            RemoteSplitRadioButtonColors(
                selectedContainerColor = RemoteColor.Red,
                selectedContentColor = RemoteColor.White,
                selectedSecondaryContentColor = RemoteColor.LightGray,
                selectedSplitContainerColor = RemoteColor.Yellow,
                selectedControlColor = RemoteColor.Cyan,
                unselectedContainerColor = RemoteColor.Blue,
                unselectedContentColor = RemoteColor.White,
                unselectedSecondaryContentColor = RemoteColor.LightGray,
                unselectedSplitContainerColor = RemoteColor.Yellow,
                unselectedControlColor = RemoteColor.Magenta,
                disabledSelectedContainerColor = RemoteColor.Gray,
                disabledSelectedContentColor = RemoteColor.DarkGray,
                disabledSelectedSecondaryContentColor = RemoteColor.DarkGray,
                disabledSelectedSplitContainerColor = RemoteColor.DarkGray,
                disabledSelectedControlColor = RemoteColor.DarkGray,
                disabledUnselectedContainerColor = RemoteColor.DarkGray,
                disabledUnselectedContentColor = RemoteColor.DarkGray,
                disabledUnselectedSecondaryContentColor = RemoteColor.DarkGray,
                disabledUnselectedSplitContainerColor = RemoteColor.DarkGray,
                disabledUnselectedControlColor = RemoteColor.DarkGray,
            )

        val colors2 = colors1.copy()
        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())

        val colors3 = colors1.copy(selectedContainerColor = RemoteColor.Green)
        assertThat(colors3).isNotEqualTo(colors1)
    }
}

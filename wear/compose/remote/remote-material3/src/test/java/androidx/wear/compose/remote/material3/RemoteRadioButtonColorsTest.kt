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
class RemoteRadioButtonColorsTest {

    @Test
    fun radioButtonDefaults_values() {
        assertThat(RemoteRadioButtonDefaults.Height.constantValue).isEqualTo(52.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.IconSpacing.constantValue)
            .isEqualTo(6.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.IconSize.constantValue).isEqualTo(24.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.LabelSpacerSize.constantValue)
            .isEqualTo(1.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.CircleRadius.constantValue)
            .isEqualTo(9.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.CircleStroke.constantValue)
            .isEqualTo(2.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.DotRadius.constantValue).isEqualTo(5.rdp.constantValue)
        assertThat(RemoteRadioButtonDefaults.ControlSize.constantValue)
            .isEqualTo(24.rdp.constantValue)
    }

    @Test
    fun radioButtonColors_resolvesCorrectly() {
        val selectedContainer = RemoteColor(Color.Red)
        val unselectedContainer = RemoteColor(Color.Blue)
        val disabledSelectedContainer = RemoteColor(Color.Gray)
        val disabledUnselectedContainer = RemoteColor(Color.DarkGray)

        val colors =
            RemoteRadioButtonColors(
                selectedContainerColor = selectedContainer,
                selectedContentColor = RemoteColor(Color.White),
                selectedSecondaryContentColor = RemoteColor(Color.LightGray),
                selectedIconColor = RemoteColor(Color.Yellow),
                selectedControlColor = RemoteColor(Color.Cyan),
                unselectedContainerColor = unselectedContainer,
                unselectedContentColor = RemoteColor(Color.White),
                unselectedSecondaryContentColor = RemoteColor(Color.LightGray),
                unselectedIconColor = RemoteColor(Color.Yellow),
                unselectedControlColor = RemoteColor(Color.Magenta),
                disabledSelectedContainerColor = disabledSelectedContainer,
                disabledSelectedContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedIconColor = RemoteColor(Color.DarkGray),
                disabledSelectedControlColor = RemoteColor(Color.DarkGray),
                disabledUnselectedContainerColor = disabledUnselectedContainer,
                disabledUnselectedContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedIconColor = RemoteColor(Color.DarkGray),
                disabledUnselectedControlColor = RemoteColor(Color.DarkGray),
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
    fun radioButtonColors_equality() {
        val colors1 =
            RemoteRadioButtonColors(
                selectedContainerColor = RemoteColor(Color.Red),
                selectedContentColor = RemoteColor(Color.White),
                selectedSecondaryContentColor = RemoteColor(Color.LightGray),
                selectedIconColor = RemoteColor(Color.Yellow),
                selectedControlColor = RemoteColor(Color.Cyan),
                unselectedContainerColor = RemoteColor(Color.Blue),
                unselectedContentColor = RemoteColor(Color.White),
                unselectedSecondaryContentColor = RemoteColor(Color.LightGray),
                unselectedIconColor = RemoteColor(Color.Yellow),
                unselectedControlColor = RemoteColor(Color.Magenta),
                disabledSelectedContainerColor = RemoteColor(Color.Gray),
                disabledSelectedContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedIconColor = RemoteColor(Color.DarkGray),
                disabledSelectedControlColor = RemoteColor(Color.DarkGray),
                disabledUnselectedContainerColor = RemoteColor(Color.DarkGray),
                disabledUnselectedContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedIconColor = RemoteColor(Color.DarkGray),
                disabledUnselectedControlColor = RemoteColor(Color.DarkGray),
            )

        val colors2 = colors1.copy()
        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())

        val colors3 = colors1.copy(selectedContainerColor = RemoteColor(Color.Green))
        assertThat(colors3).isNotEqualTo(colors1)
    }
}

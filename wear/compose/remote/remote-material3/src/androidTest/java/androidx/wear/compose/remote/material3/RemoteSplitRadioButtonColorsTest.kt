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
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RemoteSplitRadioButtonColorsTest {

    @Test
    fun splitRadioButtonDefaults_values() {
        assertEquals(52.rdp.constantValue, RemoteSplitRadioButtonDefaults.Height.constantValue)
    }

    @Test
    fun splitRadioButtonColors_resolvesCorrectly() {
        val selectedContainer = RemoteColor(Color.Red)
        val unselectedContainer = RemoteColor(Color.Blue)
        val disabledSelectedContainer = RemoteColor(Color.Gray)
        val disabledUnselectedContainer = RemoteColor(Color.DarkGray)

        val colors =
            RemoteSplitRadioButtonColors(
                selectedContainerColor = selectedContainer,
                selectedContentColor = RemoteColor(Color.White),
                selectedSecondaryContentColor = RemoteColor(Color.LightGray),
                selectedSplitContainerColor = RemoteColor(Color.Yellow),
                selectedControlColor = RemoteColor(Color.Cyan),
                unselectedContainerColor = unselectedContainer,
                unselectedContentColor = RemoteColor(Color.White),
                unselectedSecondaryContentColor = RemoteColor(Color.LightGray),
                unselectedSplitContainerColor = RemoteColor(Color.Yellow),
                unselectedControlColor = RemoteColor(Color.Magenta),
                disabledSelectedContainerColor = disabledSelectedContainer,
                disabledSelectedContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledSelectedControlColor = RemoteColor(Color.DarkGray),
                disabledUnselectedContainerColor = disabledUnselectedContainer,
                disabledUnselectedContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledUnselectedControlColor = RemoteColor(Color.DarkGray),
            )

        // Enabled + Selected
        val color1 = colors.containerColor(enabled = true.rb, selected = true.rb)
        assertEquals(selectedContainer.constantValue, color1.constantValue)

        // Enabled + Unselected
        val color2 = colors.containerColor(enabled = true.rb, selected = false.rb)
        assertEquals(unselectedContainer.constantValue, color2.constantValue)

        // Disabled + Selected
        val color3 = colors.containerColor(enabled = false.rb, selected = true.rb)
        assertEquals(disabledSelectedContainer.constantValue, color3.constantValue)

        // Disabled + Unselected
        val color4 = colors.containerColor(enabled = false.rb, selected = false.rb)
        assertEquals(disabledUnselectedContainer.constantValue, color4.constantValue)
    }

    @Test
    fun splitRadioButtonColors_equality() {
        val colors1 =
            RemoteSplitRadioButtonColors(
                selectedContainerColor = RemoteColor(Color.Red),
                selectedContentColor = RemoteColor(Color.White),
                selectedSecondaryContentColor = RemoteColor(Color.LightGray),
                selectedSplitContainerColor = RemoteColor(Color.Yellow),
                selectedControlColor = RemoteColor(Color.Cyan),
                unselectedContainerColor = RemoteColor(Color.Blue),
                unselectedContentColor = RemoteColor(Color.White),
                unselectedSecondaryContentColor = RemoteColor(Color.LightGray),
                unselectedSplitContainerColor = RemoteColor(Color.Yellow),
                unselectedControlColor = RemoteColor(Color.Magenta),
                disabledSelectedContainerColor = RemoteColor(Color.Gray),
                disabledSelectedContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledSelectedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledSelectedControlColor = RemoteColor(Color.DarkGray),
                disabledUnselectedContainerColor = RemoteColor(Color.DarkGray),
                disabledUnselectedContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUnselectedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledUnselectedControlColor = RemoteColor(Color.DarkGray),
            )

        val colors2 = colors1.copy()
        assertEquals(colors1, colors2)
        assertEquals(colors1.hashCode(), colors2.hashCode())

        val colors3 = colors1.copy(selectedContainerColor = RemoteColor(Color.Green))
        assertNotEquals(colors1, colors3)
    }
}

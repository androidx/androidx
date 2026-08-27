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
class RemoteSplitCheckboxButtonColorsTest {

    @Test
    fun splitCheckboxButtonDefaults_values() {
        assertEquals(52.rdp.constantValue, RemoteSplitCheckboxButtonDefaults.Height.constantValue)
    }

    @Test
    fun splitCheckboxButtonColors_resolvesCorrectly() {
        val checkedContainer = RemoteColor(Color.Red)
        val uncheckedContainer = RemoteColor(Color.Blue)
        val disabledCheckedContainer = RemoteColor(Color.Gray)
        val disabledUncheckedContainer = RemoteColor(Color.DarkGray)

        val colors =
            RemoteSplitCheckboxButtonColors(
                checkedContainerColor = checkedContainer,
                checkedContentColor = RemoteColor(Color.White),
                checkedSecondaryContentColor = RemoteColor(Color.LightGray),
                checkedSplitContainerColor = RemoteColor(Color.Yellow),
                checkedBoxColor = RemoteColor(Color.Cyan),
                checkedCheckmarkColor = RemoteColor(Color.Black),
                uncheckedContainerColor = uncheckedContainer,
                uncheckedContentColor = RemoteColor(Color.White),
                uncheckedSecondaryContentColor = RemoteColor(Color.LightGray),
                uncheckedSplitContainerColor = RemoteColor(Color.Yellow),
                uncheckedBoxColor = RemoteColor(Color.Magenta),
                disabledCheckedContainerColor = disabledCheckedContainer,
                disabledCheckedContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledCheckedBoxColor = RemoteColor(Color.DarkGray),
                disabledCheckedCheckmarkColor = RemoteColor(Color.DarkGray),
                disabledUncheckedContainerColor = disabledUncheckedContainer,
                disabledUncheckedContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledUncheckedBoxColor = RemoteColor(Color.DarkGray),
            )

        // Enabled + Checked
        val color1 = colors.containerColor(enabled = true.rb, checked = true.rb)
        assertEquals(checkedContainer.constantValue, color1.constantValue)

        // Enabled + Unchecked
        val color2 = colors.containerColor(enabled = true.rb, checked = false.rb)
        assertEquals(uncheckedContainer.constantValue, color2.constantValue)

        // Disabled + Checked
        val color3 = colors.containerColor(enabled = false.rb, checked = true.rb)
        assertEquals(disabledCheckedContainer.constantValue, color3.constantValue)

        // Disabled + Unchecked
        val color4 = colors.containerColor(enabled = false.rb, checked = false.rb)
        assertEquals(disabledUncheckedContainer.constantValue, color4.constantValue)
    }

    @Test
    fun splitCheckboxButtonColors_equality() {
        val colors1 =
            RemoteSplitCheckboxButtonColors(
                checkedContainerColor = RemoteColor(Color.Red),
                checkedContentColor = RemoteColor(Color.White),
                checkedSecondaryContentColor = RemoteColor(Color.LightGray),
                checkedSplitContainerColor = RemoteColor(Color.Yellow),
                checkedBoxColor = RemoteColor(Color.Cyan),
                checkedCheckmarkColor = RemoteColor(Color.Black),
                uncheckedContainerColor = RemoteColor(Color.Blue),
                uncheckedContentColor = RemoteColor(Color.White),
                uncheckedSecondaryContentColor = RemoteColor(Color.LightGray),
                uncheckedSplitContainerColor = RemoteColor(Color.Yellow),
                uncheckedBoxColor = RemoteColor(Color.Magenta),
                disabledCheckedContainerColor = RemoteColor(Color.Gray),
                disabledCheckedContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledCheckedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledCheckedBoxColor = RemoteColor(Color.DarkGray),
                disabledCheckedCheckmarkColor = RemoteColor(Color.DarkGray),
                disabledUncheckedContainerColor = RemoteColor(Color.DarkGray),
                disabledUncheckedContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledUncheckedSplitContainerColor = RemoteColor(Color.DarkGray),
                disabledUncheckedBoxColor = RemoteColor(Color.DarkGray),
            )

        val colors2 = colors1.copy()
        assertEquals(colors1, colors2)
        assertEquals(colors1.hashCode(), colors2.hashCode())

        val colors3 = colors1.copy(checkedContainerColor = RemoteColor(Color.Green))
        assertNotEquals(colors1, colors3)
    }
}

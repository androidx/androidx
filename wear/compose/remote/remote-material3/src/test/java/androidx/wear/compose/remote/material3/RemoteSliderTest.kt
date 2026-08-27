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
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteSliderTest {

    @Test
    fun defaultValues() {
        assertEquals(52f, RemoteSliderDefaults.Height.value.constantValue, 0.001f)
        assertEquals(48f, RemoteSliderDefaults.ControlSize.value.constantValue, 0.001f)
        assertEquals(12f, RemoteSliderDefaults.BarHeight.value.constantValue, 0.001f)
        assertEquals(12f, RemoteSliderDefaults.SelectedBarHeight.value.constantValue, 0.001f)
        assertEquals(4f, RemoteSliderDefaults.UnselectedBarHeight.value.constantValue, 0.001f)
        assertEquals(2f, RemoteSliderDefaults.BarSeparatorRadius.value.constantValue, 0.001f)
        assertEquals(4f, RemoteSliderDefaults.SegmentBarPadding.value.constantValue, 0.001f)
        assertEquals(24f, RemoteSliderDefaults.IconSize.value.constantValue, 0.001f)
        assertEquals(8, RemoteSliderDefaults.MaxSegmentSteps)
    }

    @Test
    fun sliderColors_copy() {
        val original =
            RemoteSliderColors(
                containerColor = RemoteColor(Color.DarkGray),
                buttonIconColor = RemoteColor(Color.White),
                selectedBarColor = RemoteColor(Color.Blue),
                unselectedBarColor = RemoteColor(Color.LightGray),
                selectedBarSeparatorColor = RemoteColor(Color.Black),
                unselectedBarSeparatorColor = RemoteColor(Color.Gray),
                disabledContainerColor = RemoteColor(Color.Black),
                disabledButtonIconColor = RemoteColor(Color.DarkGray),
                disabledSelectedBarColor = RemoteColor(Color.Gray),
                disabledUnselectedBarColor = RemoteColor(Color.DarkGray),
                disabledSelectedBarSeparatorColor = RemoteColor(Color.Black),
                disabledUnselectedBarSeparatorColor = RemoteColor(Color.DarkGray),
            )

        val newSelectedBarColor = RemoteColor(Color.Green)
        val copy = original.copy(selectedBarColor = newSelectedBarColor)

        assertEquals(newSelectedBarColor, copy.selectedBarColor)
        assertEquals(original.containerColor, copy.containerColor)
        assertEquals(original.buttonIconColor, copy.buttonIconColor)
        assertEquals(original.unselectedBarColor, copy.unselectedBarColor)
        assertEquals(original.selectedBarSeparatorColor, copy.selectedBarSeparatorColor)
        assertEquals(original.unselectedBarSeparatorColor, copy.unselectedBarSeparatorColor)
    }

    @Test
    fun sliderColors_equalsAndHashCode() {
        val colors1 =
            RemoteSliderColors(
                containerColor = RemoteColor(Color.DarkGray),
                buttonIconColor = RemoteColor(Color.White),
                selectedBarColor = RemoteColor(Color.Blue),
                unselectedBarColor = RemoteColor(Color.LightGray),
                selectedBarSeparatorColor = RemoteColor(Color.Black),
                unselectedBarSeparatorColor = RemoteColor(Color.Gray),
                disabledContainerColor = RemoteColor(Color.Black),
                disabledButtonIconColor = RemoteColor(Color.DarkGray),
                disabledSelectedBarColor = RemoteColor(Color.Gray),
                disabledUnselectedBarColor = RemoteColor(Color.DarkGray),
                disabledSelectedBarSeparatorColor = RemoteColor(Color.Black),
                disabledUnselectedBarSeparatorColor = RemoteColor(Color.DarkGray),
            )
        val colors2 = colors1.copy()
        val colors3 = colors1.copy(selectedBarColor = RemoteColor(Color.Red))

        assertEquals(colors1, colors2)
        assertEquals(colors1.hashCode(), colors2.hashCode())
        assertNotEquals(colors1, colors3)
    }
}

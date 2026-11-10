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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteSliderTest {

    @Test
    fun defaultValues() {
        assertThat(RemoteSliderDefaults.Height.value.constantValue).isWithin(0.001f).of(52f)
        assertThat(RemoteSliderDefaults.ControlSize.value.constantValue).isWithin(0.001f).of(48f)
        assertThat(RemoteSliderDefaults.BarHeight.value.constantValue).isWithin(0.001f).of(12f)
        assertThat(RemoteSliderDefaults.SelectedBarHeight.value.constantValue)
            .isWithin(0.001f)
            .of(12f)
        assertThat(RemoteSliderDefaults.UnselectedBarHeight.value.constantValue)
            .isWithin(0.001f)
            .of(4f)
        assertThat(RemoteSliderDefaults.BarSeparatorRadius.value.constantValue)
            .isWithin(0.001f)
            .of(2f)
        assertThat(RemoteSliderDefaults.SegmentBarPadding.value.constantValue)
            .isWithin(0.001f)
            .of(4f)
        assertThat(RemoteSliderDefaults.IconSize.value.constantValue).isWithin(0.001f).of(24f)
        assertThat(RemoteSliderDefaults.MaxSegmentSteps).isEqualTo(8)
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

        assertThat(copy.selectedBarColor).isEqualTo(newSelectedBarColor)
        assertThat(copy.containerColor).isEqualTo(original.containerColor)
        assertThat(copy.buttonIconColor).isEqualTo(original.buttonIconColor)
        assertThat(copy.unselectedBarColor).isEqualTo(original.unselectedBarColor)
        assertThat(copy.selectedBarSeparatorColor).isEqualTo(original.selectedBarSeparatorColor)
        assertThat(copy.unselectedBarSeparatorColor).isEqualTo(original.unselectedBarSeparatorColor)
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

        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())
        assertThat(colors3).isNotEqualTo(colors1)
    }
}

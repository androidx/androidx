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

package androidx.text.vertical.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VerticalTextStyleTest {

    @Test
    fun constructor_defaultValues() {
        val style = VerticalTextStyle()
        assertThat(style.color).isEqualTo(Color.Unspecified)
        assertThat(style.fontSize).isEqualTo(TextUnit.Unspecified)
        assertThat(style.fontWeight).isNull()
        assertThat(style.fontStyle).isNull()
        assertThat(style.fontFamily).isNull()
    }

    @Test
    fun merge_overwritesSpecifiedValues() {
        val style1 = VerticalTextStyle(color = Color.Red, fontSize = 20.sp)
        val style2 = VerticalTextStyle(color = Color.Blue)

        val merged = style1.merge(style2)

        assertThat(merged.color).isEqualTo(Color.Blue)
        assertThat(merged.fontSize).isEqualTo(20.sp)
    }

    @Test
    fun merge_preservesOriginalValues_whenOtherHasUnspecified() {
        val style1 = VerticalTextStyle(color = Color.Red, fontSize = 20.sp)
        val style2 = VerticalTextStyle(fontSize = TextUnit.Unspecified)

        val merged = style1.merge(style2)

        assertThat(merged.color).isEqualTo(Color.Red)
        assertThat(merged.fontSize).isEqualTo(20.sp)
    }

    @Test
    fun plusOperator_mergesStyles() {
        val style1 = VerticalTextStyle(color = Color.Red)
        val style2 = VerticalTextStyle(fontSize = 20.sp)

        val result = style1 + style2

        assertThat(result.color).isEqualTo(Color.Red)
        assertThat(result.fontSize).isEqualTo(20.sp)
    }

    @Test
    fun copy_updatesRequestedValues() {
        val style = VerticalTextStyle(color = Color.Red, fontSize = 20.sp)
        val updated = style.copy(fontSize = 30.sp)

        assertThat(updated.color).isEqualTo(Color.Red)
        assertThat(updated.fontSize).isEqualTo(30.sp)
    }

    @Test
    fun equals_and_hashCode() {
        val style1 = VerticalTextStyle(color = Color.Red, fontWeight = FontWeight.Bold)
        val style2 = VerticalTextStyle(color = Color.Red, fontWeight = FontWeight.Bold)
        val style3 = VerticalTextStyle(color = Color.Blue)

        assertThat(style1).isEqualTo(style2)
        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
        assertThat(style1).isNotEqualTo(style3)
    }
}

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

package androidx.compose.material3

import androidx.compose.ui.Alignment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextFieldLabelPositionTest {

    @Suppress("DEPRECATION")
    @Test
    fun attached_equality() {
        val attached1 =
            TextFieldLabelPosition.Attached(
                alwaysMinimize = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val attached2 =
            TextFieldLabelPosition.Attached(
                alwaysMinimize = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val attached3 =
            TextFieldLabelPosition.Attached(
                alwaysMinimize = false,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val attached4 =
            TextFieldLabelPosition.Attached(
                alwaysMinimize = true,
                minimizedAlignment = Alignment.Start,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val attached5 =
            TextFieldLabelPosition.Attached(
                alwaysMinimize = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.Start,
            )

        assertThat(attached1).isEqualTo(attached2)
        assertThat(attached1.hashCode()).isEqualTo(attached2.hashCode())
        assertThat(attached1).isNotEqualTo(attached3)
        assertThat(attached1).isNotEqualTo(attached4)
        assertThat(attached1).isNotEqualTo(attached5)
    }

    @Suppress("DEPRECATION")
    @Test
    fun attached_toString() {
        val attached =
            TextFieldLabelPosition.Attached(
                alwaysMinimize = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        assertThat(attached.toString())
            .isEqualTo(
                "Attached(alwaysMinimize=true, " +
                    "minimizedAlignment=Horizontal(bias=1.0), " +
                    "expandedAlignment=Horizontal(bias=0.0))"
            )
    }

    @Test
    fun inside_equality() {
        val inside1 =
            TextFieldLabelPosition.Inside(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val inside2 =
            TextFieldLabelPosition.Inside(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val inside3 =
            TextFieldLabelPosition.Inside(
                isAlwaysMinimized = false,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val inside4 =
            TextFieldLabelPosition.Inside(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.Start,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val inside5 =
            TextFieldLabelPosition.Inside(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.Start,
            )

        assertThat(inside1).isEqualTo(inside2)
        assertThat(inside1.hashCode()).isEqualTo(inside2.hashCode())
        assertThat(inside1).isNotEqualTo(inside3)
        assertThat(inside1).isNotEqualTo(inside4)
        assertThat(inside1).isNotEqualTo(inside5)
    }

    @Test
    fun inside_toString() {
        val inside =
            TextFieldLabelPosition.Inside(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        assertThat(inside.toString())
            .isEqualTo(
                "Inside(isAlwaysMinimized=true, " +
                    "minimizedAlignment=Horizontal(bias=1.0), " +
                    "expandedAlignment=Horizontal(bias=0.0))"
            )
    }

    @Test
    fun cutout_equality() {
        val cutout1 =
            TextFieldLabelPosition.Cutout(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val cutout2 =
            TextFieldLabelPosition.Cutout(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val cutout3 =
            TextFieldLabelPosition.Cutout(
                isAlwaysMinimized = false,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val cutout4 =
            TextFieldLabelPosition.Cutout(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.Start,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        val cutout5 =
            TextFieldLabelPosition.Cutout(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.Start,
            )

        assertThat(cutout1).isEqualTo(cutout2)
        assertThat(cutout1.hashCode()).isEqualTo(cutout2.hashCode())
        assertThat(cutout1).isNotEqualTo(cutout3)
        assertThat(cutout1).isNotEqualTo(cutout4)
        assertThat(cutout1).isNotEqualTo(cutout5)
    }

    @Test
    fun cutout_toString() {
        val cutout =
            TextFieldLabelPosition.Cutout(
                isAlwaysMinimized = true,
                minimizedAlignment = Alignment.End,
                expandedAlignment = Alignment.CenterHorizontally,
            )
        assertThat(cutout.toString())
            .isEqualTo(
                "Cutout(isAlwaysMinimized=true, " +
                    "minimizedAlignment=Horizontal(bias=1.0), " +
                    "expandedAlignment=Horizontal(bias=0.0))"
            )
    }

    @Test
    fun above_equality() {
        val above1 = TextFieldLabelPosition.Above(alignment = Alignment.End)
        val above2 = TextFieldLabelPosition.Above(alignment = Alignment.End)
        val above3 = TextFieldLabelPosition.Above(alignment = Alignment.Start)

        assertThat(above1).isEqualTo(above2)
        assertThat(above1.hashCode()).isEqualTo(above2.hashCode())
        assertThat(above1).isNotEqualTo(above3)
    }

    @Test
    fun above_toString() {
        val above = TextFieldLabelPosition.Above(alignment = Alignment.End)
        assertThat(above.toString()).isEqualTo("Above(alignment=Horizontal(bias=1.0))")
    }

    @Suppress("DEPRECATION")
    @Test
    fun defaultValues() {
        val attached = TextFieldLabelPosition.Attached()
        assertThat(attached.alwaysMinimize).isFalse()
        assertThat(attached.minimizedAlignment).isEqualTo(Alignment.Start)
        assertThat(attached.expandedAlignment).isEqualTo(Alignment.Start)

        val inside = TextFieldLabelPosition.Inside()
        assertThat(inside.isAlwaysMinimized).isFalse()
        assertThat(inside.minimizedAlignment).isEqualTo(Alignment.Start)
        assertThat(inside.expandedAlignment).isEqualTo(Alignment.Start)

        val cutout = TextFieldLabelPosition.Cutout()
        assertThat(cutout.isAlwaysMinimized).isFalse()
        assertThat(cutout.minimizedAlignment).isEqualTo(Alignment.Start)
        assertThat(cutout.expandedAlignment).isEqualTo(Alignment.Start)

        val above = TextFieldLabelPosition.Above()
        assertThat(above.alignment).isEqualTo(Alignment.Start)
    }
}

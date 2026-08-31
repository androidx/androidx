/*
 * Copyright 2019 The Android Open Source Project
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
@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text.style

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LineHeightStyleTest {

    @Test
    fun equals_returns_false_for_different_alignment() {
        val lineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.None)
        val otherLineHeightStyle = LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.None)
        assertThat(lineHeightStyle).isNotEqualTo(otherLineHeightStyle)
    }

    @Test
    fun equals_returns_false_for_different_trim() {
        val lineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.None)
        val otherLineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.Both)
        assertThat(lineHeightStyle).isNotEqualTo(otherLineHeightStyle)
    }

    @Test
    fun equals_returns_false_for_different_mode() {
        val lineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.None,
                mode = LineHeightStyle.Mode.Fixed,
            )
        val otherLineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.None,
                mode = LineHeightStyle.Mode.Minimum,
            )
        assertThat(lineHeightStyle).isNotEqualTo(otherLineHeightStyle)
    }

    @Test
    fun equals_returns_true_for_same_attributes() {
        val lineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.FirstLineTop,
                mode = LineHeightStyle.Mode.Tight,
            )
        val otherLineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.FirstLineTop,
                mode = LineHeightStyle.Mode.Tight,
            )
        assertThat(lineHeightStyle).isEqualTo(otherLineHeightStyle)
    }

    @Test
    fun hashCode_is_different_for_different_alignment() {
        val lineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.None)
        val otherLineHeightStyle = LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.Both)
        assertThat(lineHeightStyle.hashCode()).isNotEqualTo(otherLineHeightStyle.hashCode())
    }

    @Test
    fun hashCode_is_different_for_different_trim() {
        val lineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.None)
        val otherLineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.Both)
        assertThat(lineHeightStyle.hashCode()).isNotEqualTo(otherLineHeightStyle.hashCode())
    }

    @Test
    fun hashCode_is_different_for_different_mode() {
        val lineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.None,
                mode = LineHeightStyle.Mode.Fixed,
            )
        val otherLineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.None,
                mode = LineHeightStyle.Mode.Minimum,
            )
        assertThat(lineHeightStyle.hashCode()).isNotEqualTo(otherLineHeightStyle.hashCode())
    }

    @Test
    fun hashCode_is_same_for_same_attributes() {
        val lineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.Both,
                mode = LineHeightStyle.Mode.Minimum,
            )
        val otherLineHeightStyle =
            LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.Both,
                mode = LineHeightStyle.Mode.Minimum,
            )
        assertThat(lineHeightStyle.hashCode()).isEqualTo(otherLineHeightStyle.hashCode())
    }

    @Test
    fun isTrimFirstLineTop() {
        assertThat(Trim.FirstLineTop.trimsFirstLineTop).isTrue()
        assertThat(Trim.Both.trimsFirstLineTop).isTrue()
        assertThat(Trim.LastLineBottom.trimsFirstLineTop).isFalse()
        assertThat(Trim.None.trimsFirstLineTop).isFalse()
        assertThat(Trim(0x01 or 0x80000000.toInt()).trimsFirstLineTop).isTrue()
        assertThat(Trim(0x80000000.toInt()).trimsFirstLineTop).isFalse()
    }

    @Test
    fun isTrimLastLineBottom() {
        assertThat(Trim.LastLineBottom.trimsLastLineBottom).isTrue()
        assertThat(Trim.Both.trimsLastLineBottom).isTrue()
        assertThat(Trim.FirstLineTop.trimsLastLineBottom).isFalse()
        assertThat(Trim.None.trimsLastLineBottom).isFalse()
        assertThat(Trim(0x10 or 0x80000000.toInt()).trimsLastLineBottom).isTrue()
        assertThat(Trim(0x80000000.toInt()).trimsLastLineBottom).isFalse()
    }
}

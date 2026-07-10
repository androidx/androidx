/*
 * Copyright 2022 The Android Open Source Project
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
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextAlignTest {

    @Test
    fun equals_returns_false_for_different_textaligns() {
        val textAlign = TextAlign.Justify
        val otherTextAlign = TextAlign.Center
        assertThat(textAlign).isNotEqualTo(otherTextAlign)
    }

    @Test
    fun equals_returns_true_for_same_textaligns() {
        val textAlign = TextAlign.Center
        val otherTextAlign = TextAlign.Center
        assertThat(textAlign).isEqualTo(otherTextAlign)
    }

    @Test
    fun hashCode_is_different_for_different_textaligns() {
        val textAlign = TextAlign.Justify
        val otherTextAlign = TextAlign.Center
        assertThat(textAlign.hashCode()).isNotEqualTo(otherTextAlign.hashCode())
    }

    @Test
    fun hashCode_is_same_for_same_textaligns() {
        val textAlign = TextAlign.Center
        val otherTextAlign = TextAlign.Center
        assertThat(textAlign.hashCode()).isEqualTo(otherTextAlign.hashCode())
    }

    @Test
    fun isSpecified_returns_true_for_specified_textaligns() {
        assertThat(TextAlign.Left.isSpecified).isTrue()
        assertThat(TextAlign.Right.isSpecified).isTrue()
        assertThat(TextAlign.Center.isSpecified).isTrue()
        assertThat(TextAlign.Justify.isSpecified).isTrue()
        assertThat(TextAlign.Start.isSpecified).isTrue()
        assertThat(TextAlign.End.isSpecified).isTrue()
        assertThat(TextAlign.Unspecified.isSpecified).isFalse()
    }

    @Test
    fun valueOf_throws_unknown_value() {
        assertFailsWith<IllegalArgumentException> { TextAlign.valueOf(-1) }

        assertFailsWith<IllegalArgumentException> { TextAlign.valueOf(7) }
    }

    @Test
    fun valueOf_reconstructs_textaligns() {
        assertThat(TextAlign.valueOf(TextAlign.Left.value)).isEqualTo(TextAlign.Left)
        assertThat(TextAlign.valueOf(TextAlign.Right.value)).isEqualTo(TextAlign.Right)
        assertThat(TextAlign.valueOf(TextAlign.Center.value)).isEqualTo(TextAlign.Center)
        assertThat(TextAlign.valueOf(TextAlign.Justify.value)).isEqualTo(TextAlign.Justify)
        assertThat(TextAlign.valueOf(TextAlign.Start.value)).isEqualTo(TextAlign.Start)
        assertThat(TextAlign.valueOf(TextAlign.End.value)).isEqualTo(TextAlign.End)
    }

    @Test
    fun takeOrElse_returns_this_if_isSpecified() {
        assertThat(TextAlign.Left.takeOrElse { TextAlign.End }).isEqualTo(TextAlign.Left)
        assertThat(TextAlign.Right.takeOrElse { TextAlign.End }).isEqualTo(TextAlign.Right)
        assertThat(TextAlign.Center.takeOrElse { TextAlign.End }).isEqualTo(TextAlign.Center)
        assertThat(TextAlign.Justify.takeOrElse { TextAlign.End }).isEqualTo(TextAlign.Justify)
        assertThat(TextAlign.Start.takeOrElse { TextAlign.End }).isEqualTo(TextAlign.Start)
        assertThat(TextAlign.End.takeOrElse { TextAlign.Start }).isEqualTo(TextAlign.End)
        assertThat(TextAlign.Unspecified.takeOrElse { TextAlign.End }).isEqualTo(TextAlign.End)
    }
}

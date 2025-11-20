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
class TextDirectionTest {

    @Test
    fun equals_returns_false_for_different_textdirections() {
        val textDirection = TextDirection.Ltr
        val otherTextDirection = TextDirection.Rtl
        assertThat(textDirection).isNotEqualTo(otherTextDirection)
    }

    @Test
    fun equals_returns_true_for_same_textdirections() {
        val textDirection = TextDirection.Rtl
        val otherTextDirection = TextDirection.Rtl
        assertThat(textDirection).isEqualTo(otherTextDirection)
    }

    @Test
    fun hashCode_is_different_for_different_textdirections() {
        val textDirection = TextDirection.Ltr
        val otherTextDirection = TextDirection.Rtl
        assertThat(textDirection.hashCode()).isNotEqualTo(otherTextDirection.hashCode())
    }

    @Test
    fun hashCode_is_same_for_same_textdirections() {
        val textDirection = TextDirection.Rtl
        val otherTextDirection = TextDirection.Rtl
        assertThat(textDirection.hashCode()).isEqualTo(otherTextDirection.hashCode())
    }

    @Test
    fun isSpecified_returns_true_for_specified_textdirections() {
        assertThat(TextDirection.Ltr.isSpecified).isTrue()
        assertThat(TextDirection.Rtl.isSpecified).isTrue()
        assertThat(TextDirection.Content.isSpecified).isTrue()
        assertThat(TextDirection.ContentOrLtr.isSpecified).isTrue()
        assertThat(TextDirection.ContentOrRtl.isSpecified).isTrue()
        assertThat(TextDirection.Unspecified.isSpecified).isFalse()
    }

    @Test
    fun valueOf_throws_unknown_value() {
        assertFailsWith<IllegalArgumentException> { TextDirection.valueOf(-1) }

        assertFailsWith<IllegalArgumentException> { TextDirection.valueOf(6) }
    }

    @Test
    fun valueOf_reconstructs_textdirections() {
        assertThat(TextDirection.valueOf(TextDirection.Ltr.value)).isEqualTo(TextDirection.Ltr)
        assertThat(TextDirection.valueOf(TextDirection.Rtl.value)).isEqualTo(TextDirection.Rtl)
        assertThat(TextDirection.valueOf(TextDirection.Content.value))
            .isEqualTo(TextDirection.Content)
        assertThat(TextDirection.valueOf(TextDirection.ContentOrLtr.value))
            .isEqualTo(TextDirection.ContentOrLtr)
        assertThat(TextDirection.valueOf(TextDirection.ContentOrRtl.value))
            .isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun takeOrElse_returns_this_if_isSpecified() {
        assertThat(TextDirection.Ltr.takeOrElse { TextDirection.Rtl }).isEqualTo(TextDirection.Ltr)
        assertThat(TextDirection.Rtl.takeOrElse { TextDirection.Ltr }).isEqualTo(TextDirection.Rtl)
        assertThat(TextDirection.Content.takeOrElse { TextDirection.Ltr })
            .isEqualTo(TextDirection.Content)
        assertThat(TextDirection.ContentOrLtr.takeOrElse { TextDirection.Ltr })
            .isEqualTo(TextDirection.ContentOrLtr)
        assertThat(TextDirection.ContentOrRtl.takeOrElse { TextDirection.Ltr })
            .isEqualTo(TextDirection.ContentOrRtl)
        assertThat(TextDirection.Unspecified.takeOrElse { TextDirection.Ltr })
            .isEqualTo(TextDirection.Ltr)
    }
}

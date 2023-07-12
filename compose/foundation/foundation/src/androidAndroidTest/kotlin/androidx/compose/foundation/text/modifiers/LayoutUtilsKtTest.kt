/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class LayoutUtilsKtTest {

    @Test
    fun finalConstraints_returnsTightWidth() {
        val subject = finalConstraints(
            Constraints(500, 500, 0, 50),
            true,
            TextOverflow.Ellipsis,
            42f
        )
        assertThat(subject.maxWidth).isEqualTo(500)
    }

    @Test
    fun finalConstraints_returnsMaxIntrinsicWhenUnbound() {
        val subject = finalConstraints(
            Constraints(500, 500, 0, 50),
            false,
            TextOverflow.Clip,
            1234.1f
        )
        assertThat(subject.maxWidth).isEqualTo(1235)
    }

    @Test
    fun finalMaxWidth_returnsTightWidth() {
        val subject = finalMaxWidth(
            Constraints(500, 500, 0, 50),
            true,
            TextOverflow.Ellipsis,
            42f
        )
        assertThat(subject).isEqualTo(500)
    }

    @Test
    fun finalMaxWidth_returnsMaxIntrinsicWhenUnbound() {
        val subject = finalMaxWidth(
            Constraints(500, 500, 0, 50),
            false,
            TextOverflow.Clip,
            1234.1f
        )
        assertThat(subject).isEqualTo(1235)
    }

    @Test
    fun finalMaxLines_negative() {
        val subject = finalMaxLines(true, TextOverflow.Clip, -1)
        assertThat(subject).isEqualTo(1)
    }

    @Test
    fun finalMaxLines_positive_noOverride() {
        val subject = finalMaxLines(true, TextOverflow.Clip, 4)
        assertThat(subject).isEqualTo(4)
    }

    @Test
    fun finalMaxLines_overrideOn_TextOverflowEllipsis_andSoftwrapFalse() {
        val subject = finalMaxLines(false, TextOverflow.Ellipsis, 4)
        assertThat(subject).isEqualTo(1)
    }
}
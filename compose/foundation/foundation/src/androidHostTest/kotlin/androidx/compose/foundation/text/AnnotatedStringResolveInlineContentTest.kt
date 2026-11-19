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

package androidx.compose.foundation.text

import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringResolveInlineContentTest {

    private val sampleInlineContent =
        mapOf(
            "box" to
                InlineTextContent(
                    Placeholder(10.sp, 10.sp, PlaceholderVerticalAlign.AboveBaseline)
                ) {}
        )

    @Test
    fun hasInlineContent_returnsFalse_whenInlineContentIsNull() {
        val text = buildAnnotatedString {
            append("Hello")
            appendInlineContent("box")
        }

        assertThat(text.hasInlineContent(null)).isFalse()
    }

    @Test
    fun hasInlineContent_returnsFalse_whenInlineContentIsEmpty() {
        val text = buildAnnotatedString {
            append("Hello")
            appendInlineContent("box")
        }

        assertThat(text.hasInlineContent(emptyMap())).isFalse()
    }

    @Test
    fun hasInlineContent_returnsFalse_whenTextHasNoInlineContentAnnotation() {
        val text = buildAnnotatedString { append("Hello") }

        assertThat(text.hasInlineContent(sampleInlineContent)).isFalse()
    }

    @Test
    fun hasInlineContent_returnsTrue_whenTextAndInlineContentHaveEntries() {
        val text = buildAnnotatedString {
            append("Hello")
            appendInlineContent("box")
        }

        assertThat(text.hasInlineContent(sampleInlineContent)).isTrue()
    }
}

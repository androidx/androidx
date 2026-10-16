/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.insert
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class SingleLineOutputTransformationTest {

    @Test
    fun lineFeed_isReplacedWithWhitespace() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello\nworld"))
        with(SingleLineOutputTransformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("hello world")
    }

    @Test
    fun carriageReturn_isReplacedWithZeroWidthSpace() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello\rworld"))
        with(SingleLineOutputTransformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("hello\uFEFFworld")
    }

    @Test
    fun lineFeed_isReplaced_aroundEmojis() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("$COMPLEX_EMOJI\n$REGULAR_EMOJI"))
        with(SingleLineOutputTransformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("$COMPLEX_EMOJI $REGULAR_EMOJI")
    }

    @Test
    fun wrapAppliesOtherOutputTransformationFirst() {
        val other = OutputTransformation { insert(3, "\n") }
        val wrapped = SingleLineOutputTransformation.wrap(other)
        val buffer = TextFieldBuffer(TextFieldCharSequence("abcdef"))
        with(wrapped) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("abc def")
    }

    companion object {
        // Rainbow flag (🏳️‍🌈): white flag (U+1F3F3), variation selector-16 (U+FE0F),
        // zero-width joiner (U+200D), rainbow (U+1F308). Multiple codepoints.
        private const val COMPLEX_EMOJI = "\ud83c\udff3\ufe0f\u200d\ud83c\udf08"
        // Grinning face (😀, U+1F600). Single codepoint (surrogate pair in UTF-16).
        private const val REGULAR_EMOJI = "\uD83D\uDE00"
    }
}

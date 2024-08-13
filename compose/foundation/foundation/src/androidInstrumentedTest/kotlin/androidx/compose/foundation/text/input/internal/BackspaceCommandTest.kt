/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BackspaceCommandTest {

    // Test sample surrogate pair characters.
    private val SP1 = "\uD83D\uDE00" // U+1F600: GRINNING FACE
    private val SP2 = "\uD83D\uDE01" // U+1F601: GRINNING FACE WITH SMILING EYES
    private val SP3 = "\uD83D\uDE02" // U+1F602: FACE WITH TEARS OF JOY
    private val SP4 = "\uD83D\uDE03" // U+1F603: SMILING FACE WITH OPEN MOUTH
    private val SP5 = "\uD83D\uDE04" // U+1F604: SMILING FACE WITH OPEN MOUTH AND SMILING EYES

    // Family ZWJ Emoji: U+1F468 U+200D U+1F469 U+200D U+1F467 U+200D U+1F466
    private val ZWJ_EMOJI = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"

    @Test
    fun test_delete() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("BCDE")
        Truth.assertThat(eb.selection.start).isEqualTo(0)
        Truth.assertThat(eb.selection.end).isEqualTo(0)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_from_offset0() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("ABCDE")
        Truth.assertThat(eb.selection.start).isEqualTo(0)
        Truth.assertThat(eb.selection.end).isEqualTo(0)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_with_selection() {
        val eb = TextFieldBuffer("ABCDE", TextRange(2, 3))

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("ABDE")
        Truth.assertThat(eb.selection.start).isEqualTo(2)
        Truth.assertThat(eb.selection.end).isEqualTo(2)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_with_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))
        eb.setComposition(2, 3)

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("ABDE")
        Truth.assertThat(eb.selection.start).isEqualTo(1)
        Truth.assertThat(eb.selection.end).isEqualTo(1)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_surrogate_pair() {
        val eb = TextFieldBuffer("$SP1$SP2$SP3$SP4$SP5", TextRange(2))

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("$SP2$SP3$SP4$SP5")
        Truth.assertThat(eb.selection.start).isEqualTo(0)
        Truth.assertThat(eb.selection.end).isEqualTo(0)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_with_selection_surrogate_pair() {
        val eb = TextFieldBuffer("$SP1$SP2$SP3$SP4$SP5", TextRange(4, 6))

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("$SP1$SP2$SP4$SP5")
        Truth.assertThat(eb.selection.start).isEqualTo(4)
        Truth.assertThat(eb.selection.end).isEqualTo(4)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_with_composition_surrogate_pair() {
        val eb = TextFieldBuffer("$SP1$SP2$SP3$SP4$SP5", TextRange(2))
        eb.setComposition(4, 6)

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo("$SP1$SP2$SP4$SP5")
        Truth.assertThat(eb.selection.start).isEqualTo(2)
        Truth.assertThat(eb.selection.end).isEqualTo(2)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun test_delete_with_composition_zwj_emoji() {
        val eb = TextFieldBuffer("$ZWJ_EMOJI$ZWJ_EMOJI", TextRange(ZWJ_EMOJI.length))

        eb.backspace()

        Truth.assertThat(eb.toString()).isEqualTo(ZWJ_EMOJI)
        Truth.assertThat(eb.selection.start).isEqualTo(0)
        Truth.assertThat(eb.selection.end).isEqualTo(0)
        Truth.assertThat(eb.hasComposition()).isFalse()
    }
}

internal fun TextFieldBuffer(
    initialValue: String = "",
    initialSelection: TextRange = TextRange.Zero
) = TextFieldBuffer(TextFieldCharSequence(initialValue, initialSelection))

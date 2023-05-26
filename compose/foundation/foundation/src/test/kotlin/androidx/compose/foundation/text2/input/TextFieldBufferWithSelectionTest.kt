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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import java.text.ParseException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TextFieldBufferWithSelectionTest {

    @Test
    fun initialSelection() {
        val state = TextFieldBufferWithSelection(TextFieldCharSequence())
        assertThat(state.selectionInChars).isEqualTo(TextRange(0))
        assertThat(state.hasSelection).isFalse()
    }

    @Test
    fun selectionAdjusted_empty_textInserted() {
        testSelectionAdjustment("", { append("hello") }, "hello_")
    }

    @Test
    fun selectionAdjusted_whenCursorAtStart_textInsertedAtCursor() {
        testSelectionAdjustment("_hello", { insert(0, "world") }, "world_hello")
    }

    @Test
    fun selectionAdjusted_whenCursorAtStart_textInsertedAfterCursor() {
        testSelectionAdjustment("_hello", { append("world") }, "_helloworld")
    }

    @Test
    fun selectionAdjusted_whenCursorAtStart_textReplacedAroundCursor() {
        testSelectionAdjustment("_hello", { replace(0, length, "foo") }, "_foo")
    }

    @Test
    fun selectionAdjusted_whenCursorAtEnd_textInsertedAtCursor() {
        testSelectionAdjustment("hello_", { append("world") }, "helloworld_")
    }

    @Test
    fun selectionAdjusted_whenCursorAtEnd_textInsertedBeforeCursor() {
        testSelectionAdjustment("hello_", { insert(0, "world") }, "worldhello_")
    }

    @Test
    fun selectionAdjusted_whenCursorAtEnd_textReplacedAroundCursor() {
        testSelectionAdjustment("hello_", { replace(0, length, "foo") }, "foo_")
    }

    @Test
    fun selectionAdjusted_whenCursorInMiddle_textInsertedAtCursor() {
        testSelectionAdjustment("he_llo", { insert(2, "foo") }, "hefoo_llo")
    }

    @Test
    fun selectionAdjusted_whenCursorInMiddle_textReplacedJustBeforeCursor() {
        testSelectionAdjustment("he_llo", { replace(0, 2, "foo") }, "foo_llo")
    }

    @Test
    fun selectionAdjusted_whenCursorInMiddle_textReplacedJustAfterCursor() {
        testSelectionAdjustment("he_llo", { replace(2, 3, "foo") }, "he_foolo")
    }

    @Test
    fun selectionAdjusted_whenCursorInMiddle_textReplacedAroundCursor() {
        testSelectionAdjustment("he_llo", { replace(1, 3, "foo") }, "hfoo_lo")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_allReplacedWithShorter() {
        testSelectionAdjustment("_hello_", { replace(0, length, "foo") }, "_foo_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_allReplacedWithLonger() {
        testSelectionAdjustment("_hello_", { replace(0, length, "abracadabra") }, "_abracadabra_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_textReplacedInsideSelection_withLonger() {
        testSelectionAdjustment("_hello_", { replace(1, 4, "world") }, "_hworldo_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_textReplacedInsideSelection_withShorter() {
        testSelectionAdjustment("_hello_", { replace(1, 4, "w") }, "_hwo_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_textReplacedInsideFromStart_withLonger() {
        testSelectionAdjustment("_hello_", { replace(0, 3, "world") }, "_worldlo_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_textReplacedInsideFromStart_withShorter() {
        testSelectionAdjustment("_hello_", { replace(1, 3, "w") }, "_hwlo_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_textReplacedInsideToEnd_withLonger() {
        testSelectionAdjustment("_hello_", { replace(length - 3, length, "world") }, "_heworld_")
    }

    @Test
    fun selectionAdjusted_whenAllSelected_textReplacedInsideToEnd_withShorter() {
        testSelectionAdjustment("_hello_", { replace(length - 3, length, "w") }, "_hew_")
    }

    @Test
    fun selectionAdjusted_whenInsideSelected_textReplacedJustBeforeSelection() {
        testSelectionAdjustment("hel_lo_", { replace(0, 3, "world") }, "world_lo_")
    }

    @Test
    fun selectionAdjusted_whenInsideSelected_textReplacedJustAfterSelection() {
        testSelectionAdjustment("_he_llo", { replace(2, length, "world") }, "_he_world")
    }

    @Test
    fun selectionAdjusted_whenInsideSelected_textReplacedAroundStart() {
        testSelectionAdjustment("h_ello_", { replace(0, 3, "world") }, "world_lo_")
    }

    @Test
    fun selectionAdjusted_whenInsideSelected_textReplacedAroundEnd() {
        testSelectionAdjustment("_hell_o", { replace(2, length, "world") }, "_he_world")
    }

    @Test
    fun resetTo_copiesTextAndSelection() {
        val expectedValue = TextFieldCharSequence("world", TextRange(5))
        val state = TextFieldBufferWithSelection(
            value = TextFieldCharSequence("hello", TextRange(2)),
            sourceValue = expectedValue
        )
        state.revertAllChanges()
        assertThat(state.toTextFieldCharSequence()).isEqualTo(expectedValue)
        assertThat(state.changes.changeCount).isEqualTo(0)
    }

    @Test
    fun testConvertTextFieldValueToAndFromString() {
        assertThat("".parseAsTextEditState()).isEqualTo(TextFieldCharSequence())
        assertThat("hello".parseAsTextEditState()).isEqualTo(TextFieldCharSequence("hello"))
        assertThat("_hello".parseAsTextEditState()).isEqualTo(TextFieldCharSequence("hello"))
        assertThat("h_ello".parseAsTextEditState())
            .isEqualTo(TextFieldCharSequence("hello", selection = TextRange(1)))
        assertThat("hello_".parseAsTextEditState())
            .isEqualTo(TextFieldCharSequence("hello", selection = TextRange(5)))
        assertThat("_hello_".parseAsTextEditState())
            .isEqualTo(TextFieldCharSequence("hello", selection = TextRange(0, 5)))
        assertThat("he__llo".parseAsTextEditState())
            .isEqualTo(TextFieldCharSequence("hello", selection = TextRange(2)))
        assertThat("he_l_lo".parseAsTextEditState())
            .isEqualTo(TextFieldCharSequence("hello", selection = TextRange(2, 3)))
        assertFailsWith<ParseException> {
            "_he_llo_".parseAsTextEditState()
        }

        listOf("", "_hello", "h_ello", "hello_", "_hello_", "he_ll_o").forEach {
            val value = it.parseAsTextEditState()
            assertThat(value.toParsableString()).isEqualTo(it)
        }
    }

    private fun testSelectionAdjustment(
        initial: String,
        transform: TextFieldBufferWithSelection.() -> Unit,
        expected: String
    ) {
        val state = TextFieldBufferWithSelection(initial.parseAsTextEditState())
        state.transform()
        assertThat(state.toTextFieldCharSequence().toParsableString()).isEqualTo(expected)
    }

    /**
     * Parses this string into a [TextFieldValue], replacing a single underscore with the cursor, or
     * two underscores with a selection.
     */
    private fun String.parseAsTextEditState(): TextFieldCharSequence {
        var firstMark = -1
        var secondMark = -1
        val source = this
        val text = buildString {
            source.forEachIndexed { i, char ->
                if (char == '_') {
                    when {
                        firstMark == -1 -> firstMark = i
                        secondMark == -1 -> secondMark = i - 1
                        else -> throw ParseException("Unexpected underscore in \"$this\"", i)
                    }
                } else {
                    append(char)
                }
            }
        }

        return TextFieldCharSequence(
            text = text,
            selection = when {
                firstMark == -1 -> TextRange.Zero
                secondMark == -1 -> TextRange(firstMark)
                else -> TextRange(firstMark, secondMark)
            }
        )
    }

    private fun TextFieldCharSequence.toParsableString(): String = buildString {
        append(this@toParsableString)
        if (isNotEmpty()) {
            insert(selectionInChars.min, '_')
            if (!selectionInChars.collapsed) {
                insert(selectionInChars.max + 1, '_')
            }
        }
    }
}
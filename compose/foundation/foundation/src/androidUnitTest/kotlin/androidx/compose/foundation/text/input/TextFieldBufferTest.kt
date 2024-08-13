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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.text.ParseException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TextFieldBufferTest {

    @Test
    fun initialSelection() {
        val state = TextFieldBuffer(TextFieldCharSequence())
        assertThat(state.selection).isEqualTo(TextRange(0))
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
        val state =
            TextFieldBuffer(
                initialValue = TextFieldCharSequence("hello", TextRange(2)),
                originalValue = expectedValue
            )
        state.revertAllChanges()
        assertThat(state.toTextFieldCharSequence()).isEqualTo(expectedValue)
        assertThat(state.changes.changeCount).isEqualTo(0)
    }

    @Test
    fun placeCursorBeforeCharAt_emptyBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))

        assertFailsWith<IllegalArgumentException> { buffer.placeCursorBeforeCharAt(-1) }

        buffer.placeCursorBeforeCharAt(0)
        assertThat(buffer.selection).isEqualTo(TextRange(0))

        assertFailsWith<IllegalArgumentException> { buffer.placeCursorBeforeCharAt(1) }
    }

    @Test
    fun placeCursorBeforeCharAt_nonEmptyBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))
        assertFailsWith<IllegalArgumentException> { buffer.placeCursorBeforeCharAt(-1) }

        buffer.placeCursorBeforeCharAt(0)
        assertThat(buffer.selection).isEqualTo(TextRange(0))

        buffer.placeCursorBeforeCharAt(1)
        assertThat(buffer.selection).isEqualTo(TextRange(1))

        buffer.placeCursorBeforeCharAt(5)
        assertThat(buffer.selection).isEqualTo(TextRange(5))

        assertFailsWith<IllegalArgumentException> { buffer.placeCursorBeforeCharAt(6) }
    }

    @Test
    fun placeCursorAfterCharAt_emptyBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))

        buffer.placeCursorAfterCharAt(-1)
        assertThat(buffer.selection).isEqualTo(TextRange(0))

        assertFailsWith<IllegalArgumentException> { buffer.placeCursorAfterCharAt(0) }

        assertFailsWith<IllegalArgumentException> { buffer.placeCursorAfterCharAt(1) }
    }

    @Test
    fun placeCursorAfterCharAt_nonEmptyBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))

        buffer.placeCursorAfterCharAt(-1)
        assertThat(buffer.selection).isEqualTo(TextRange(0))

        buffer.placeCursorAfterCharAt(0)
        assertThat(buffer.selection).isEqualTo(TextRange(1))

        buffer.placeCursorAfterCharAt(1)
        assertThat(buffer.selection).isEqualTo(TextRange(2))

        buffer.placeCursorAfterCharAt(4)
        assertThat(buffer.selection).isEqualTo(TextRange(5))

        assertFailsWith<IllegalArgumentException> { buffer.placeCursorAfterCharAt(5) }
    }

    @Test
    fun selectCharsIn_emptyBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))

        buffer.selection = TextRange(0)
        assertThat(buffer.selection).isEqualTo(TextRange(0))

        assertFailsWith<IllegalArgumentException> { buffer.selection = TextRange(0, 1) }
    }

    @Test
    fun selectCharsIn_nonEmptyBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))

        buffer.selection = TextRange(0)
        assertThat(buffer.selection).isEqualTo(TextRange(0))

        buffer.selection = TextRange(0, 1)
        assertThat(buffer.selection).isEqualTo(TextRange(0, 1))

        buffer.selection = TextRange(0, 5)
        assertThat(buffer.selection).isEqualTo(TextRange(0, 5))

        buffer.selection = TextRange(4, 5)
        assertThat(buffer.selection).isEqualTo(TextRange(4, 5))

        buffer.selection = TextRange(5, 5)
        assertThat(buffer.selection).isEqualTo(TextRange(5, 5))

        assertFailsWith<IllegalArgumentException> { buffer.selection = TextRange(5, 6) }

        assertFailsWith<IllegalArgumentException> { buffer.selection = TextRange(6, 6) }
    }

    @Test
    fun setTextIfChanged_updatesText_whenChanged() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("world")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("world")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 5))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun setTextIfChanged_updatesText_whenPrefixChanged() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("1ello")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("1ello")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 1))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 1))
    }

    @Test
    fun setTextIfChanged_updatesText_whenPrefixAdded() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("1hello")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("1hello")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 1))
    }

    @Test
    fun setTextIfChanged_updatesText_whenPrefixRemoved() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("ello")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("ello")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 1))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0))
    }

    @Test
    fun setTextIfChanged_updatesText_whenSuffixChanged() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("hell1")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("hell1")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(4, 5))
    }

    @Test
    fun setTextIfChanged_updatesText_whenSuffixAdded() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("hello1")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("hello1")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(5))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(5, 6))
    }

    @Test
    fun setTextIfChanged_updatesText_whenSuffixRemoved() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("hell")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("hell")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(4))
    }

    @Test
    fun setTextIfChanged_updatesText_whenMiddleChanged_once() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("h1llo")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("h1llo")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(1, 2))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(1, 2))
    }

    @Test
    fun setTextIfChanged_updatesText_whenMiddleAdded_once() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("he1llo")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("he1llo")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(2))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(2, 3))
    }

    @Test
    fun setTextIfChanged_updatesText_whenMiddleRemoved_once() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("helo")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("helo")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(2, 3))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(2))
    }

    @Test
    fun setTextIfChanged_updatesText_whenMiddleChanged_multiple() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("h1l2o")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("h1l2o")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(1, 4))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun setTextIfChanged_updatesText_whenMiddleAdded_multiple() {
        val text = "hello"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("he1ll2o")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("he1ll2o")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(2, 4))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(2, 6))
    }

    @Test
    fun setTextIfChanged_updatesText_whenMiddleRemoved_multiple() {
        val text = "abcde"
        val buffer = TextFieldBuffer(TextFieldCharSequence(text))

        buffer.setTextIfChanged("ace")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.toString()).isEqualTo("ace")
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(1, 4))
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(1, 2))
    }

    @Test
    fun setTextIfChanged_doesNotUpdateTextIfEqual() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))

        buffer.setTextIfChanged("hello")

        assertThat(buffer.changes.changeCount).isEqualTo(0)
    }

    @Test
    fun setTextIfChanged_doesNotUpdateTextIfEqual_afterChange() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))
        buffer.append(" world")

        buffer.setTextIfChanged("hello world")

        assertThat(buffer.changes.changeCount).isEqualTo(1)
    }

    @Test
    fun charAt_throws_whenEmpty() {
        val buffer = TextFieldBuffer(TextFieldCharSequence())

        assertFailsWith<IndexOutOfBoundsException> { buffer.charAt(0) }
    }

    @Test
    fun charAt_throws_whenOutOfBounds() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("a"))

        assertFailsWith<IndexOutOfBoundsException> { buffer.charAt(1) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.charAt(-1) }
    }

    @Test
    fun charAt_returnsChars() {
        val buffer = TextFieldBuffer(TextFieldCharSequence("ab"))
        assertThat(buffer.charAt(0)).isEqualTo('a')
        assertThat(buffer.charAt(1)).isEqualTo('b')
    }

    @Test
    fun asCharSequence_isViewOfBuffer() {
        val buffer = TextFieldBuffer(TextFieldCharSequence())
        val charSequence = buffer.asCharSequence()

        assertThat(charSequence.toString()).isEmpty()

        buffer.append("hello")

        assertThat(charSequence.toString()).isEqualTo("hello")
    }

    @Test
    fun replace_withSubSequence_crossedOffsets() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        val error = assertFailsWith<IllegalArgumentException> { buffer.replace(0, 0, "hi", 2, 0) }
        assertThat(error.message).isEqualTo("Expected textStart=2 <= textEnd=0")
    }

    @Test
    fun replace_withSubSequence_startTooSmall() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        assertFailsWith<IllegalArgumentException> { buffer.replace(0, 0, "hi", -1, 0) }
    }

    @Test
    fun replace_withSubSequence_endTooBig() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        assertFailsWith<IndexOutOfBoundsException> { buffer.replace(0, 0, "hi", 2, 3) }
    }

    @Test
    fun replace_withSubSequence_empty() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        buffer.replace(0, 0, "hi", 0, 0)
        assertThat(buffer.toString()).isEqualTo("")
    }

    @Test
    fun replace_withSubSequence_singleCharFromStart() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        buffer.replace(0, 0, "hi", 0, 1)
        assertThat(buffer.toString()).isEqualTo("h")
    }

    @Test
    fun replace_withSubSequence_singleCharFromEnd() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        buffer.replace(0, 0, "hi", 1, 2)
        assertThat(buffer.toString()).isEqualTo("i")
    }

    @Test
    fun replace_withSubSequence_middle() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        buffer.replace(0, 0, "abcd", 1, 3)
        assertThat(buffer.toString()).isEqualTo("bc")
    }

    @Test
    fun replace_withSubSequence_full() {
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        buffer.replace(0, 0, "abcd", 0, 4)
        assertThat(buffer.toString()).isEqualTo("abcd")
    }

    @Test
    fun changeList_alwaysReturnsTheLatestChanges() {
        val buffer = TextFieldBuffer(TextFieldCharSequence())
        val changeList = buffer.changes
        buffer.insert(0, "hello")
        assertThat(changeList.changeCount).isEqualTo(1)
        assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(0))
        assertThat(changeList.getRange(0)).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun findCommonPrefixAndSuffix_works() {
        assertCommonPrefixAndSuffix("", "", null)
        assertCommonPrefixAndSuffix("a", "a", null)
        assertCommonPrefixAndSuffix("abc", "abc", null)
        assertCommonPrefixAndSuffix("", "b", TextRange(0) to TextRange(0, 1))
        assertCommonPrefixAndSuffix("a", "", TextRange(0, 1) to TextRange(0))
        assertCommonPrefixAndSuffix("ab", "ac", TextRange(1, 2) to TextRange(1, 2))
        assertCommonPrefixAndSuffix("abb", "ac", TextRange(1, 3) to TextRange(1, 2))
        assertCommonPrefixAndSuffix("ab", "acc", TextRange(1, 2) to TextRange(1, 3))
        assertCommonPrefixAndSuffix("az", "bz", TextRange(0, 1) to TextRange(0, 1))
        assertCommonPrefixAndSuffix("cba", "za", TextRange(0, 2) to TextRange(0, 1))
        assertCommonPrefixAndSuffix("za", "cba", TextRange(0, 1) to TextRange(0, 2))
        assertCommonPrefixAndSuffix("aoz", "apz", TextRange(1, 2) to TextRange(1, 2))
        assertCommonPrefixAndSuffix("amnoz", "az", TextRange(1, 4) to TextRange(1, 1))
        assertCommonPrefixAndSuffix("az", "amnoz", TextRange(1, 1) to TextRange(1, 4))
        assertCommonPrefixAndSuffix("amnoz", "axz", TextRange(1, 4) to TextRange(1, 2))
        assertCommonPrefixAndSuffix("axz", "amnoz", TextRange(1, 2) to TextRange(1, 4))
    }

    /** Tests of private testing helper code. */
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
        assertFailsWith<ParseException> { "_he_llo_".parseAsTextEditState() }

        listOf("", "_hello", "h_ello", "hello_", "_hello_", "he_ll_o").forEach {
            val value = it.parseAsTextEditState()
            assertThat(value.toParsableString()).isEqualTo(it)
        }
    }

    @Test
    fun createFromTextFieldState() {
        val state = TextFieldState("Hello", TextRange(3))
        val buffer = state.toTextFieldBuffer()

        assertThat(buffer.asCharSequence().toString()).isEqualTo("Hello")
        assertThat(buffer.selection).isEqualTo(TextRange(3))
        assertThat(buffer.changes.changeCount).isEqualTo(0)

        // guarding against future changes
        assertThat(buffer.originalValue).isEqualTo(state.value)
    }

    @Test
    fun changesToBuffer_doesNotPropagateToTextFieldState() {
        val state = TextFieldState("Hello", TextRange(3))
        val buffer = state.toTextFieldBuffer()

        buffer.replace(0, 5, "World")

        assertThat(buffer.asCharSequence().toString()).isEqualTo("World")
        assertThat(state.text.toString()).isEqualTo("Hello")
    }

    private fun testSelectionAdjustment(
        initial: String,
        transform: TextFieldBuffer.() -> Unit,
        expected: String
    ) {
        val state = TextFieldBuffer(initial.parseAsTextEditState())
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
            selection =
                when {
                    firstMark == -1 -> TextRange.Zero
                    secondMark == -1 -> TextRange(firstMark)
                    else -> TextRange(firstMark, secondMark)
                }
        )
    }

    private fun TextFieldCharSequence.toParsableString(): String = buildString {
        append(this@toParsableString)
        if (isNotEmpty()) {
            insert(selection.min, '_')
            if (!selection.collapsed) {
                insert(selection.max + 1, '_')
            }
        }
    }

    private fun assertCommonPrefixAndSuffix(
        a: CharSequence,
        b: CharSequence,
        expectedRanges: Pair<TextRange, TextRange>?
    ) {
        var result: Pair<TextRange, TextRange>? = null
        findCommonPrefixAndSuffix(a, b) { aStart, aEnd, bStart, bEnd ->
            result = Pair(TextRange(aStart, aEnd), TextRange(bStart, bEnd))
        }
        assertWithMessage("Expected findCommonPrefixAndSuffix(\"$a\", \"$b\") to report")
            .that(result)
            .isEqualTo(expectedRanges)
    }
}

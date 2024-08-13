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

import androidx.compose.foundation.text.input.PlacedAnnotation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextHighlightType
import androidx.compose.foundation.text.input.internal.matchers.assertThat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextFieldBufferUseFromImeTest {

    @Test
    fun insert() {
        val eb = TextFieldBuffer("", TextRange.Zero)

        eb.imeReplace(0, 0, "A")

        assertThat(eb).hasChars("A")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        // Keep inserting text to the end of string. Cursor should follow.
        eb.imeReplace(1, 1, "BC")
        assertThat(eb).hasChars("ABC")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        // Insert into middle position. Cursor should be end of inserted text.
        eb.imeReplace(1, 1, "D")
        assertThat(eb).hasChars("ADBC")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
    }

    @Test
    fun delete() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.imeReplace(0, 1, "")

        // Delete the left character at the cursor.
        assertThat(eb).hasChars("BCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        // Delete the text before the cursor
        eb.imeReplace(0, 2, "")
        assertThat(eb).hasChars("DE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        // Delete end of the text.
        eb.imeReplace(1, 2, "")
        assertThat(eb).hasChars("D")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
    }

    @Test
    fun setSelection() {
        val eb = TextFieldBuffer("ABCDE", TextRange(0, 3))
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.selection = TextRange(0, 5) // Change the selection
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.imeReplace(0, 3, "X") // replace function cancel the selection and place cursor.
        assertThat(eb).hasChars("XDE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.setSelection(0, 2) // Set the selection again
        assertThat(eb).hasChars("XDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
    }

    @Test
    fun imeReplace_cancels_composition() {
        val tfb =
            TextFieldBuffer(
                TextFieldCharSequence(
                    text = "ABCDE",
                    selection = TextRange(1, 4),
                    composition = TextRange(0, 5)
                )
            )

        tfb.imeReplace(0, 3, "FGH")

        assertThat(tfb).hasChars("FGHDE")
        assertThat(tfb.selection.start).isEqualTo(3)
        assertThat(tfb.selection.end).isEqualTo(3)
        assertThat(tfb.hasComposition()).isFalse()
        assertThat(tfb.composition).isNull()
    }

    @Test
    fun setSelection_coerces_whenNegativeStart() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(-1, 1)

        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(1)
    }

    @Test
    fun setSelection_coerces_whenNegativeEnd() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(1, -1)

        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun setSelection_allowReversedSelection() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)
        eb.setSelection(4, 2)

        assertThat(eb.selection).isEqualTo(TextRange(4, 2))
    }

    @Test
    fun replace_reversedRegion() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)
        eb.imeReplace(3, 1, "FGHI")

        assertThat(eb).hasChars("AFGHIDE")
        assertThat(eb.selection.start).isEqualTo(5)
        assertThat(eb.selection.end).isEqualTo(5)
    }

    @Test
    fun setComposition_and_cancelComposition() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(5)

        eb.imeReplace(2, 3, "X") // replace function cancel the composition text.
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.setComposition(2, 4) // set composition again
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(4)
    }

    @Test
    fun setComposition_and_commitComposition() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(5)

        eb.imeReplace(2, 3, "X") // replace function cancel the composition text.
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.setComposition(2, 4) // set composition again
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(4)

        eb.commitComposition() // commit the composition
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
    }

    @Test
    fun setComposition_and_annotationList() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        val annotations: List<PlacedAnnotation> =
            listOf(
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 0, 5)
            )
        eb.setComposition(0, 5, annotations)
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(5)
        assertThat(eb.composingAnnotations?.size).isEqualTo(1)
        assertThat(eb.composingAnnotations?.first()).isEqualTo(annotations.first())

        eb.imeReplace(2, 3, "X") // replace function cancel the composition text.
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
        assertThat(eb.composingAnnotations?.isEmpty()).isTrue()

        eb.setComposition(2, 4) // set composition again
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(4)
        assertThat(eb.composingAnnotations?.isEmpty()).isTrue()

        eb.setComposition(0, 5, annotations)
        assertThat(eb.composingAnnotations?.size).isEqualTo(1)
        assertThat(eb.composingAnnotations?.first()).isEqualTo(annotations.first())

        eb.commitComposition() // commit the composition
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
        assertThat(eb.composingAnnotations?.isEmpty()).isTrue()
    }

    @Test
    fun setCursor_and_get_cursor() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.selection = TextRange(1)
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.selection = TextRange(2)
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()

        eb.selection = TextRange(5)
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.selection.start).isEqualTo(5)
        assertThat(eb.selection.end).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.composition).isNull()
    }

    @Test
    fun delete_preceding_cursor_no_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.imeDelete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_trailing_cursor_no_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.imeDelete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_preceding_selection_no_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(0, 1))

        eb.imeDelete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_trailing_selection_no_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(4, 5))

        eb.imeDelete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_covered_cursor() {
        // AB[]CDE
        val eb = TextFieldBuffer("ABCDE", TextRange(2, 2))

        eb.imeDelete(1, 3)
        // A[]DE
        assertThat(eb).hasChars("ADE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
    }

    @Test
    fun delete_covered_selection() {
        // A[BC]DE
        val eb = TextFieldBuffer("ABCDE", TextRange(1, 3))

        eb.imeDelete(0, 4)
        // []E
        assertThat(eb).hasChars("E")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun delete_covered_reversedSelection() {
        // A[BC]DE
        val eb = TextFieldBuffer("ABCDE", TextRange(3, 1))

        eb.imeDelete(0, 4)
        // []E
        assertThat(eb).hasChars("E")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun delete_intersects_first_half_of_selection() {
        // AB[CD]E
        val eb = TextFieldBuffer("ABCDE", TextRange(2, 4))

        eb.imeDelete(1, 3)
        // A[D]E
        assertThat(eb).hasChars("ADE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(2)
    }

    @Test
    fun delete_intersects_first_half_of_reversedSelection() {
        // AB[CD]E
        val eb = TextFieldBuffer("ABCDE", TextRange(4, 2))

        eb.imeDelete(3, 1)
        // A[D]E
        assertThat(eb).hasChars("ADE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(2)
    }

    @Test
    fun delete_intersects_second_half_of_selection() {
        // A[BCD]EFG
        val eb = TextFieldBuffer("ABCDEFG", TextRange(1, 4))

        eb.imeDelete(3, 5)
        // A[BC]FG
        assertThat(eb).hasChars("ABCFG")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(3)
    }

    @Test
    fun delete_intersects_second_half_of_reversedSelection() {
        // A[BCD]EFG
        val eb = TextFieldBuffer("ABCDEFG", TextRange(4, 1))

        eb.imeDelete(5, 3)
        // A[BC]FG
        assertThat(eb).hasChars("ABCFG")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(3)
    }

    @Test
    fun delete_preceding_composition_no_intersection() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 2)
        eb.imeDelete(2, 3)

        assertThat(eb).hasChars("ABDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun delete_trailing_composition_no_intersection() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.imeDelete(2, 3)

        assertThat(eb).hasChars("ABDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun delete_preceding_composition_intersection() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)
        eb.imeDelete(2, 4)

        assertThat(eb).hasChars("ABE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun delete_trailing_composition_intersection() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 5)
        eb.imeDelete(2, 4)

        assertThat(eb).hasChars("ABE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun delete_composition_contains_delrange() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(2, 5)
        eb.imeDelete(3, 4)

        assertThat(eb).hasChars("ABCE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(4)
    }

    @Test
    fun delete_delrange_contains_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.imeDelete(2, 5)

        assertThat(eb).hasChars("AB")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun setHighlight_clearHighlight() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setHighlight(TextHighlightType.HandwritingSelectPreview, 1, 3)
        assertThat(eb.highlight)
            .isEqualTo(Pair(TextHighlightType.HandwritingSelectPreview, TextRange(1, 3)))

        eb.setHighlight(TextHighlightType.HandwritingDeletePreview, 2, 4)
        assertThat(eb.highlight)
            .isEqualTo(Pair(TextHighlightType.HandwritingDeletePreview, TextRange(2, 4)))

        eb.clearHighlight()
        assertThat(eb.highlight).isNull()
    }

    @Test
    fun setHighlight_setSelection_clearsHighlight() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setHighlight(TextHighlightType.HandwritingSelectPreview, 1, 3)
        assertThat(eb.highlight)
            .isEqualTo(Pair(TextHighlightType.HandwritingSelectPreview, TextRange(1, 3)))

        eb.setSelection(0, 1)
        assertThat(eb.highlight).isNull()
    }

    @Test
    fun setHighlight_replace_clearsHighlight() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setHighlight(TextHighlightType.HandwritingSelectPreview, 1, 3)
        assertThat(eb.highlight)
            .isEqualTo(Pair(TextHighlightType.HandwritingSelectPreview, TextRange(1, 3)))

        eb.imeReplace(3, 5, "F")
        assertThat(eb.highlight).isNull()
    }

    @Test
    fun setHighlight_delete_clearsHighlight() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setHighlight(TextHighlightType.HandwritingSelectPreview, 1, 3)
        assertThat(eb.highlight)
            .isEqualTo(Pair(TextHighlightType.HandwritingSelectPreview, TextRange(1, 3)))

        eb.imeDelete(0, 1)
        assertThat(eb.highlight).isNull()
    }
}

internal fun TextFieldBuffer(
    initialValue: String = "",
    initialSelection: TextRange = TextRange.Zero
) = TextFieldBuffer(TextFieldCharSequence(initialValue, initialSelection))

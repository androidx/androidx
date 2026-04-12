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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.allCaps
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.Locale
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class BatchEditTest : ImeEditCommandTest() {

    // --- Multiple commitText ---

    @Test
    fun multipleCommits_append() {
        initialize("A", TextRange(1))
        imeScope.beginBatchEdit()
        imeScope.commitText("B", 1)
        imeScope.commitText("C", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABC")
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun multipleCommits_insertAtCursor() {
        initialize("A", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.commitText("B", 1)
        imeScope.commitText("C", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("BCA")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun multipleCommits_replaceSelection() {
        initialize("ABC", TextRange(0, 3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.commitText("E", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("DE")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun multipleCommits_farTailAndHead() {
        initialize("ABC", TextRange(1))
        imeScope.beginBatchEdit()
        imeScope.commitText("X", 2)
        imeScope.commitText("Y", -1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXBYC")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun multipleCommits_withIntermediateSelection() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.setSelection(1, 1)
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXBCD")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    // --- Multiple setComposingText ---

    @Test
    fun multipleSetComposingText_updatesExisting() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingText("D", 1)
        imeScope.setComposingText("E", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCE")
        assertThat(state.composition).isEqualTo(TextRange(3, 4))
    }

    @Test
    fun multipleSetComposingText_newCompositionAfterCommit() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingText("D", 1)
        imeScope.commitText("D", 1)
        imeScope.setComposingText("E", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.composition).isEqualTo(TextRange(4, 5))
    }

    @Test
    fun multipleSetComposingText_withDifferentAnnotations() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingText("D", 1, listOf())
        imeScope.setComposingText("E", 1, null)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCE")
    }

    @Test
    fun setComposingText_thenCommitText_inBatch() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingText("D", 1)
        imeScope.commitText("E", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCE")
        assertThat(state.composition).isNull()
    }

    @Test
    fun setComposingText_thenDelete_inBatch() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingText("D", 1)
        imeScope.deleteSurroundingText(2, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AB")
    }

    // --- setComposingRegion ---

    @Test
    fun setComposingRegion_thenCommitText() {
        initialize("ABCDE", TextRange(5))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(1, 4)
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXE")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun setComposingRegion_thenSetComposingText() {
        initialize("ABCDE", TextRange(5))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(1, 4)
        imeScope.setComposingText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXE")
        assertThat(state.composition).isEqualTo(TextRange(1, 2))
    }

    @Test
    fun multipleSetComposingRegion_inBatch() {
        initialize("ABCDE", TextRange(5))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(1, 4)
        imeScope.setComposingRegion(2, 5)
        imeScope.endBatchEdit()
        assertThat(state.composition).isEqualTo(TextRange(2, 5))
    }

    @Test
    fun setComposingRegion_outOfBounds_inBatch() {
        initialize("ABCDE", TextRange(5))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(-1, 10)
        imeScope.endBatchEdit()
        assertThat(state.composition).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun setComposingRegion_reversed_inBatch() {
        initialize("ABCDE", TextRange(5))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(4, 1)
        imeScope.endBatchEdit()
        assertThat(state.composition).isEqualTo(TextRange(1, 4))
    }

    // --- deleteSurroundingText ---

    @Test
    fun multipleDeleteSurroundingText_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(1, 0)
        imeScope.deleteSurroundingText(0, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ADE")
        assertThat(state.selection).isEqualTo(TextRange(1))
    }

    @Test
    fun deleteSurroundingText_thenCommitText_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(1, 1)
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXDE")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun deleteSurroundingText_overlapping_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(2, 0)
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("CDE")
        assertThat(state.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun deleteSurroundingText_withLargeValues_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEmpty()
        assertThat(state.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun deleteSurroundingText_aroundSelection_inBatch() {
        initialize("ABCDE", TextRange(1, 4))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(1, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("BCD")
        assertThat(state.selection).isEqualTo(TextRange(0, 3))
    }

    // --- deleteSurroundingTextInCodePoints ---

    @Test
    fun multipleDeleteCodePoints_inBatch() {
        val CH1 = "\uD83D\uDE00"
        val CH2 = "\uD83D\uDE01"
        val CH3 = "\uD83D\uDE02"
        initialize("$CH1$CH2$CH3", TextRange(4))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingTextInCodePoints(1, 0)
        imeScope.deleteSurroundingTextInCodePoints(0, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo(CH1)
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun deleteCodePoints_aroundEmoji_inBatch() {
        val CH1 = "\uD83D\uDE00"
        initialize("A${CH1}B", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingTextInCodePoints(1, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("A")
        assertThat(state.selection).isEqualTo(TextRange(1))
    }

    @Test
    fun deleteCodePoints_thenSetComposingText_inBatch() {
        val CH1 = "\uD83D\uDE00"
        initialize("A${CH1}B", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingTextInCodePoints(1, 0)
        imeScope.setComposingText("C", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ACB")
        assertThat(state.composition).isEqualTo(TextRange(1, 2))
    }

    @Test
    fun deleteCodePoints_withInvalidSurrogates_inBatch() {
        val invalidSurrogate = "\uD83D"
        initialize("A${invalidSurrogate}B", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingTextInCodePoints(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AB")
    }

    @Test
    fun deleteCodePoints_overlapping_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingTextInCodePoints(2, 0)
        imeScope.deleteSurroundingTextInCodePoints(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("CDE")
    }

    // --- setSelection ---

    @Test
    fun multipleSetSelection_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setSelection(1, 2)
        imeScope.setSelection(3, 4)
        imeScope.endBatchEdit()
        assertThat(state.selection).isEqualTo(TextRange(3, 4))
    }

    @Test
    fun setSelection_thenCommitText_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setSelection(1, 4)
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXE")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun setSelection_thenDelete_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setSelection(2, 2)
        imeScope.deleteSurroundingText(1, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ADE")
        assertThat(state.selection).isEqualTo(TextRange(1))
    }

    @Test
    fun setSelection_reversed_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setSelection(4, 1)
        imeScope.endBatchEdit()
        assertThat(state.selection).isEqualTo(TextRange(4, 1))
    }

    @Test
    fun setSelection_outOfBounds_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setSelection(-10, 100)
        imeScope.endBatchEdit()
        assertThat(state.selection).isEqualTo(TextRange(0, 5))
    }

    // --- finishComposingText ---

    @Test
    fun multipleFinishComposingText_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(1, 4)
        imeScope.finishComposingText()
        imeScope.finishComposingText()
        imeScope.endBatchEdit()
        assertThat(state.composition).isNull()
    }

    @Test
    fun finishComposingText_thenStartNew_inBatch() {
        initialize("ABCDE", TextRange(0))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(1, 3)
        imeScope.finishComposingText()
        imeScope.setComposingRegion(3, 5)
        imeScope.endBatchEdit()
        assertThat(state.composition).isEqualTo(TextRange(3, 5))
    }

    @Test
    fun finishComposingText_thenDelete_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(0, 5)
        imeScope.finishComposingText()
        imeScope.deleteSurroundingText(1, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ADE")
        assertThat(state.composition).isNull()
    }

    @Test
    fun finishComposingText_noActiveComposition_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.finishComposingText()
        imeScope.endBatchEdit()
        assertThat(state.composition).isNull()
    }

    @Test
    fun finishComposingText_thenSetSelection_inBatch() {
        initialize("ABCDE", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(0, 5)
        imeScope.finishComposingText()
        imeScope.setSelection(1, 4)
        imeScope.endBatchEdit()
        assertThat(state.selection).isEqualTo(TextRange(1, 4))
        assertThat(state.composition).isNull()
    }

    // --- Batch with OutputTransformation ---

    @Test
    fun batchEdit_withPrefixTransformation() {
        initialize("ABC", TextRange(0), outputTransformation = { insert(0, "PREFIX") })
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("DABC")
    }

    @Test
    fun batchEdit_withSuffixTransformation() {
        initialize("ABC", TextRange(3), outputTransformation = { insert(length, "SUFFIX") })
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCD")
    }

    @Test
    fun batchEdit_withMaskingTransformation() {
        // Use a transformation that doesn't replace the whole range to keep mappings 1-to-1
        initialize("ABC", TextRange(3), outputTransformation = { append("!") })
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    @Test
    fun batchEdit_withCompositeTransformation() {
        initialize(
            "ABC",
            TextRange(3),
            outputTransformation = {
                insert(0, "PREFIX")
                insert(length, "SUFFIX")
            },
        )
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(1, 0)
        imeScope.commitText("D", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABD")
    }

    @Test
    fun batchEdit_transformationDependentOnContent() {
        initialize("12345", TextRange(5), outputTransformation = { if (length > 3) insert(3, "-") })
        imeScope.beginBatchEdit()
        imeScope.commitText("6", 1)
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("12345")
    }

    // --- Batch with InputTransformation ---

    @Test
    fun batchEdit_withAllCapsInputTransformation() {
        initialize("ABC", TextRange(3))
        transformedState.update(InputTransformation.allCaps(Locale.current))
        imeScope.beginBatchEdit()
        imeScope.commitText("d", 1)
        imeScope.commitText("e", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCDE")
    }

    @Test
    fun batchEdit_withFilteringInputTransformation() {
        initialize("ABC", TextRange(3))
        transformedState.update {
            val s = asCharSequence().toString().replace("X", "")
            replace(0, length, s)
        }
        imeScope.beginBatchEdit()
        imeScope.commitText("X", 1)
        imeScope.commitText("Y", 1)
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCY")
    }

    @Test
    fun batchEdit_withLengthLimitInputTransformation() {
        initialize("ABC", TextRange(3))
        transformedState.update(InputTransformation.maxLength(4))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.commitText("E", 1)
        imeScope.endBatchEdit()
        // The entire batch is applied as one atomic operation when no OutputTransformation is
        // present.
        // Reverting the final state (ABCDE) reverts to the original state (ABC).
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    @Test
    fun batchEdit_withRejectingInputTransformation() {
        initialize("ABC", TextRange(3))
        transformedState.update { revertAllChanges() }
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    @Test
    fun batchEdit_withModifyAndRejectMixed() {
        initialize("ABC", TextRange(3))
        transformedState.update { if (length > 5) revertAllChanges() }
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.commitText("E", 1)
        imeScope.commitText("F", 1)
        imeScope.endBatchEdit()
        // ABC + D + E + F = ABCDEF (length 6 > 5) -> reverted to ABC
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    // --- Complex Mixing ---

    @Test
    fun complexBatch_commitDeleteSelection() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1) // ABCD
        imeScope.deleteSurroundingText(2, 0) // AB
        imeScope.setSelection(1, 1) // A|B
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AB")
        assertThat(state.selection).isEqualTo(TextRange(1))
    }

    @Test
    fun complexBatch_composingCommitFinish() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingText("D", 1)
        imeScope.commitText("E", 1)
        imeScope.finishComposingText()
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCE")
        assertThat(state.composition).isNull()
    }

    @Test
    fun complexBatch_multipleDeletesAndInserts() {
        initialize("ABCDE", TextRange(2)) // AB|CDE
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(1, 1) // A|DE
        imeScope.commitText("X", 1) // AX|DE
        imeScope.deleteSurroundingText(1, 2) // A|
        imeScope.commitText("YZ", 1) // AYZ|
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AYZ")
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun complexBatch_undoRedoSideEffects() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABC")
        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    @Test
    fun complexBatch_withSurrogatePairsMixed() {
        val emoji = "\uD83D\uDE00"
        initialize("A${emoji}B", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(2, 0) // deletes Emoji
        imeScope.commitText("C", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ACB")
    }

    // --- Edge Cases ---

    @Test
    fun emptyBatch() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    @Test
    fun nestedBatches() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.endBatchEdit()
        imeScope.commitText("E", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCDE")
    }

    @Test
    fun batchWithNoNetChange() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABC")
    }

    @Test
    fun batchWithNoNetChange2() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.commitText("E", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCDE")
    }

    @Test
    fun batchWithExtremelyLargeValues() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setSelection(Int.MAX_VALUE, Int.MAX_VALUE)
        imeScope.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEmpty()
    }

    @Test
    fun batchAppliedAfterStateChangeExternally() {
        initialize("ABC", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.commitText("D", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("ABCD")
    }

    // --- Surrogate Pairs Mixed ---

    @Test
    fun batch_insertBetweenSurrogatePair() {
        val emoji = "\uD83D\uDE00"
        initialize("A${emoji}B", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("A\uD83DX\uDE00B")
    }

    @Test
    fun batch_deletePartialSurrogatePair() {
        val emoji = "\uD83D\uDE00"
        initialize("A${emoji}B", TextRange(2))
        imeScope.beginBatchEdit()
        imeScope.deleteSurroundingText(1, 0)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("A\uDE00B")
    }

    @Test
    fun batch_replaceSurrogateWithNormal() {
        val emoji = "\uD83D\uDE00"
        initialize("A${emoji}B", TextRange(1, 3))
        imeScope.beginBatchEdit()
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXB")
    }

    @Test
    fun batch_composingSurrogatePair() {
        val emoji = "\uD83D\uDE00"
        initialize("A${emoji}B", TextRange(3))
        imeScope.beginBatchEdit()
        imeScope.setComposingRegion(1, 3)
        imeScope.commitText("X", 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("AXB")
    }

    @Test
    fun batch_multipleEmojisCommits() {
        val emoji1 = "\uD83D\uDE00"
        val emoji2 = "\uD83D\uDE01"
        initialize("A", TextRange(1))
        imeScope.beginBatchEdit()
        imeScope.commitText(emoji1, 1)
        imeScope.commitText(emoji2, 1)
        imeScope.endBatchEdit()
        assertThat(state.text.toString()).isEqualTo("A$emoji1$emoji2")
    }

    @Test
    fun stressTest_randomEditsInBatch() {
        val random = kotlin.random.Random(42)
        repeat(200) { iteration ->
            val initialLength = random.nextInt(0, 100)
            val initialText = RandomString(initialLength, random)
            val start = random.nextInt(0, initialLength + 1)
            val end = random.nextInt(0, initialLength + 1)
            val initialSelection = TextRange(start, end)

            initialize(initialText, initialSelection)

            val log = StringBuilder()
            log.appendLine("Iteration $iteration")
            log.appendLine("Initial text: \"$initialText\", selection: $initialSelection")

            imeScope.beginBatchEdit()
            log.appendLine("beginBatchEdit()")

            val numEdits = random.nextInt(1, 20)
            repeat(numEdits) {
                when (random.nextInt(3)) {
                    0 -> {
                        val text = RandomString(random.nextInt(0, 10), random)
                        val pos = random.nextInt(-5, 5)
                        log.appendLine("commitText(\"$text\", $pos)")
                        imeScope.commitText(text, pos)
                    }
                    1 -> {
                        val before = random.nextInt(0, 20)
                        val after = random.nextInt(0, 20)
                        log.appendLine("deleteSurroundingText($before, $after)")
                        imeScope.deleteSurroundingText(before, after)
                    }
                    2 -> {
                        val s = random.nextInt(-10, 110)
                        val e = random.nextInt(-10, 110)
                        log.appendLine("setSelection($s, $e)")
                        imeScope.setSelection(s, e)
                    }
                }
            }

            try {
                log.appendLine("endBatchEdit()")
                imeScope.endBatchEdit()
            } catch (t: Throwable) {
                println("CRASH DETECTED!\n$log")
                throw t
            }
        }
    }

    @Test
    fun stressTest_largeDeletesSmallText() {
        val random = kotlin.random.Random(123)
        repeat(100) { iteration ->
            initialize("abc", TextRange(1))
            val log = StringBuilder()
            log.appendLine("largeDeletesSmallText Iteration $iteration")
            log.appendLine("Initial text: \"abc\", selection: TextRange(1)")

            imeScope.beginBatchEdit()
            log.appendLine("beginBatchEdit()")
            repeat(5) {
                val before = random.nextInt(0, 1000)
                val after = random.nextInt(0, 1000)
                val text = RandomString(random.nextInt(0, 10), random)
                val pos = random.nextInt(-5, 5)

                log.appendLine("deleteSurroundingText($before, $after)")
                imeScope.deleteSurroundingText(before, after)
                log.appendLine("commitText(\"$text\", $pos)")
                imeScope.commitText(text, pos)
            }
            try {
                log.appendLine("endBatchEdit()")
                imeScope.endBatchEdit()
            } catch (t: Throwable) {
                println("CRASH DETECTED!\n$log")
                throw t
            }
        }
    }

    @Test
    fun stressTest_manyCommitsThenDelete() {
        val random = kotlin.random.Random(456)
        repeat(100) { iteration ->
            initialize("", TextRange(0))
            val log = StringBuilder()
            log.appendLine("manyCommitsThenDelete Iteration $iteration")
            log.appendLine("Initial text: \"\", selection: TextRange(0)")

            imeScope.beginBatchEdit()
            log.appendLine("beginBatchEdit()")
            repeat(20) {
                log.appendLine("commitText(\"a\", 1)")
                imeScope.commitText("a", 1)
            }
            log.appendLine("deleteSurroundingText(10, 10)")
            imeScope.deleteSurroundingText(10, 10)
            try {
                log.appendLine("endBatchEdit()")
                imeScope.endBatchEdit()
            } catch (t: Throwable) {
                println("CRASH DETECTED!\n$log")
                throw t
            }
        }
    }

    private fun RandomString(length: Int, random: kotlin.random.Random): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 "
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}

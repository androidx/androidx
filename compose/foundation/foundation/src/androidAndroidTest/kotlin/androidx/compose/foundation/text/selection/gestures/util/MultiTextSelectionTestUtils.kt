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

package androidx.compose.foundation.text.selection.gestures.util

import androidx.compose.foundation.text.selection.Selection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection.Ltr
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import kotlin.math.max
import kotlin.math.min

internal class MultiSelectionSubject(
    failureMetadata: FailureMetadata?,
    private val subject: Selection?,
    private val texts: List<Pair<String, String>>,
) : Subject(failureMetadata, subject) {

    private val textContentIndices = texts.textContentIndices()

    companion object {
        fun withContent(
            texts: List<Pair<String, String>>
        ): Factory<MultiSelectionSubject, Selection?> =
            Factory { failureMetadata, subject ->
                MultiSelectionSubject(failureMetadata, subject, texts)
            }
    }

    fun hasSelection(expected: TextRange?) {
        if (expected == null) {
            Truth.assertThat(subject).isNull()
            return
        }

        check("selection").that(subject).isNotNull()

        val startSelectableId = textContentIndices.offsetToSelectableId(expected.start) + 1
        val startOffset = textContentIndices.offsetToLocalOffset(expected.start)
        val endSelectableId = textContentIndices.offsetToSelectableId(expected.end) + 1
        val endOffset = textContentIndices.offsetToLocalOffset(expected.end)

        val expectedSelection = Selection(
            start = Selection.AnchorInfo(Ltr, startOffset, startSelectableId.toLong()),
            end = Selection.AnchorInfo(Ltr, endOffset, endSelectableId.toLong()),
            handlesCrossed = startSelectableId > endSelectableId ||
                (startSelectableId == endSelectableId && startOffset > endOffset),
        )

        if (subject!! != expectedSelection) {
            failWithActual(
                Fact.simpleFact("expected equal selections"),
                Fact.fact("expected", expectedSelection.multiTextToString(texts)),
            )
        }
    }

    override fun actualCustomStringRepresentation(): String =
        subject?.multiTextToString(texts) ?: "null"

    private val Selection.AnchorInfo.stringIndex
        get() = textContentIndices[selectableId.toInt() - 1].first + offset

    private val Selection.minStringIndex get() = min(start.stringIndex, end.stringIndex)
    private val Selection.maxStringIndex get() = max(start.stringIndex, end.stringIndex)

    private fun Selection.multiTextToString(texts: List<Pair<String, String>>): String {
        val content = texts.joinToString(separator = "\n") { it.first }
        val collapsedSelection = start.stringIndex == end.stringIndex
        val selectionString = content
            .map { if (it == '\n') '\n' else '.' }
            .joinToString("")
            .let {
                if (collapsedSelection) {
                    if (start.stringIndex == content.length) {
                        // edge case of selection being at end of text,
                        // so append the marker instead of replacing
                        "$it|"
                    } else if (content[start.stringIndex] == '\n') {
                        it.replaceRange(start.stringIndex..start.stringIndex, "|\n")
                    } else {
                        it.replaceRange(start.stringIndex..start.stringIndex, "|")
                    }
                } else {
                    val selectionRange = minStringIndex until maxStringIndex
                    it.replaceRange(selectionRange, content.substring(selectionRange))
                }
            }
        return """
                |Collapsed = $collapsedSelection
                |Selection = $this
                |$selectionString
            """.trimMargin().trim()
    }
}

internal fun List<Pair<String, String>>.textContentIndices() =
    runningFold(0) { runningIndex, (str, _) -> runningIndex + str.length + 1 }
        .zipWithNext()
        .map { (prev, next) -> prev until next }

internal fun List<IntRange>.offsetToSelectableId(i: Int) = getIndexRange(i).index
internal fun List<IntRange>.offsetToLocalOffset(i: Int): Int = i - getIndexRange(i).value.first

private fun List<IntRange>.getIndexRange(i: Int): IndexedValue<IntRange> =
    withIndex().first { (_, range) -> i in range }

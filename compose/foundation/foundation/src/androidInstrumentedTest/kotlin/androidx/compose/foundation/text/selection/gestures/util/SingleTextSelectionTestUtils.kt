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
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import kotlin.math.max
import kotlin.math.min

private val Selection.min get() = min(start.offset, end.offset)
private val Selection.max get() = max(start.offset, end.offset)

internal class SelectionSubject constructor(
    failureMetadata: FailureMetadata?,
    private val subject: Selection?,
    private val content: String,
) : Subject(failureMetadata, subject) {

    companion object {
        fun withContent(content: String): Factory<SelectionSubject, Selection?> =
            Factory { failureMetadata, subject ->
                SelectionSubject(failureMetadata, subject, content)
            }
    }

    fun hasSelection(
        expected: TextRange?,
        startTextDirection: ResolvedTextDirection,
        endTextDirection: ResolvedTextDirection,
    ) {
        if (expected == null) {
            Truth.assertThat(subject).isNull()
            return
        }

        check("selection").that(subject).isNotNull()
        subject!! // smart cast to non-nullable

        val startHandle = Selection.AnchorInfo(startTextDirection, expected.start, 1)
        val endHandle = Selection.AnchorInfo(endTextDirection, expected.end, 1)
        val expectedSelection = Selection(
            start = startHandle,
            end = endHandle,
            handlesCrossed = expected.start > expected.end,
        )
        if (subject != expectedSelection) {
            failWithActual(
                Fact.simpleFact("expected equal selections"),
                Fact.fact("expected", expectedSelection.textToString(content)),
            )
        }
    }

    override fun actualCustomStringRepresentation(): String =
        subject?.textToString(content) ?: "null"
}

private fun Selection.textToString(content: String): String {
    val collapsedSelection = start.offset == end.offset
    val selectionString = content
        .map { if (it == '\n') '\n' else '.' }
        .joinToString("")
        .let {
            if (collapsedSelection) {
                if (start.offset == content.length) {
                    // edge case of selection being at end of text,
                    // so append the marker instead of replacing
                    "$it|"
                } else if (content[start.offset] == '\n') {
                    it.replaceRange(start.offset..start.offset, "|\n")
                } else {
                    it.replaceRange(start.offset..start.offset, "|")
                }
            } else {
                val selectionRange = min until max
                it.replaceRange(selectionRange, content.substring(selectionRange))
            }
        }
    return """
                |Collapsed = $collapsedSelection
                |Selection = $this
                |$selectionString
            """.trimMargin().trim()
}

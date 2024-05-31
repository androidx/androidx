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

import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

internal abstract class TextFieldSelectionAsserter<T>(
    textContent: String,
    private val rule: ComposeTestRule,
    textToolbar: TextToolbar,
    hapticFeedback: FakeHapticFeedback,
    getActual: () -> T,
) : SelectionAsserter<T>(textContent, rule, textToolbar, hapticFeedback, getActual) {
    var selection: TextRange = 0.collapsed
    var cursorHandleShown = false

    override fun assert() {
        super.assert()
        assertCursorHandleShown(cursorHandleShown)
    }

    private fun assertCursorHandleShown(shown: Boolean) {
        val cursorHandle = rule.onNode(isSelectionHandle(Handle.Cursor))
        if (shown) cursorHandle.assertExists() else cursorHandle.assertDoesNotExist()
    }
}

internal class TextFieldValueSubject(
    failureMetadata: FailureMetadata?,
    private val subject: TextFieldValue,
    private val textContent: String,
) : Subject(failureMetadata, subject) {

    companion object {
        fun withContent(content: String): Factory<TextFieldValueSubject, TextFieldValue> =
            Factory { failureMetadata, subject ->
                TextFieldValueSubject(failureMetadata, subject, content)
            }
    }

    fun hasSelection(textRange: TextRange) {
        check("text").that(subject.text).isEqualTo(textContent)
        if (subject.selection != textRange) {
            failWithActual(
                Fact.simpleFact("expected equal selections"),
                Fact.fact("expected", TextFieldValue(textContent, textRange).customToString()),
            )
        }
    }

    override fun actualCustomStringRepresentation(): String = subject.customToString()

    private fun TextFieldValue.customToString(): String {
        val selectionString =
            text
                .map { if (it == '\n') '\n' else '.' }
                .joinToString("")
                .let {
                    if (selection.collapsed) {
                        if (selection.start == text.length) {
                            // edge case of selection being at end of text,
                            // so append the marker instead of replacing
                            "$it|"
                        } else if (it[selection.start] == '\n') {
                            // edge case of selection being at end of a line,
                            // so append the marker to the EOL and then add the new line
                            it.replaceRange(selection.start..selection.start, "|\n")
                        } else {
                            it.replaceRange(selection.start..selection.start, "|")
                        }
                    } else {
                        it.replaceRange(selection.min until selection.max, getSelectedText().text)
                    }
                }
        return """
                /Selection = ${selection.start} to ${selection.end}
                /$selectionString
            """
            .trimMargin(marginPrefix = "/")
            .trim()
    }
}

internal class TextField1SelectionAsserter(
    textContent: String,
    rule: ComposeTestRule,
    textToolbar: TextToolbar,
    hapticFeedback: FakeHapticFeedback,
    getActual: () -> TextFieldValue,
) :
    TextFieldSelectionAsserter<TextFieldValue>(
        textContent,
        rule,
        textToolbar,
        hapticFeedback,
        getActual
    ) {
    override fun subAssert() {
        Truth.assertAbout(TextFieldValueSubject.withContent(textContent))
            .that(getActual())
            .hasSelection(selection)
    }
}

internal class TextFieldStateSubject(
    failureMetadata: FailureMetadata?,
    private val subject: TextFieldState,
    private val textContent: String,
) : Subject(failureMetadata, subject) {

    companion object {
        fun withContent(content: String): Factory<TextFieldStateSubject, TextFieldState> =
            Factory { failureMetadata, subject ->
                TextFieldStateSubject(failureMetadata, subject, content)
            }
    }

    fun hasSelection(textRange: TextRange) {
        check("text").that(subject.text).isEqualTo(textContent)
        if (subject.selection != textRange) {
            failWithActual(
                Fact.simpleFact("expected equal selections"),
                Fact.fact("expected", TextFieldState(textContent, textRange).customToString()),
            )
        }
    }

    override fun actualCustomStringRepresentation(): String = subject.customToString()

    private fun TextFieldState.customToString(): String {
        val selectionString =
            text
                .map { if (it == '\n') '\n' else '.' }
                .joinToString("")
                .let {
                    if (selection.collapsed) {
                        if (selection.start == text.length) {
                            // edge case of selection being at end of text,
                            // so append the marker instead of replacing
                            "$it|"
                        } else if (it[selection.start] == '\n') {
                            // edge case of selection being at end of a line,
                            // so append the marker to the EOL and then add the new line
                            it.replaceRange(selection.start..selection.start, "|\n")
                        } else {
                            it.replaceRange(selection.start..selection.start, "|")
                        }
                    } else {
                        val selectedText = text.subSequence(selection.min, selection.max)
                        it.replaceRange(selection.min until selection.max, selectedText)
                    }
                }
        return """
                /Selection = ${selection.start} to ${selection.end}
                /$selectionString
            """
            .trimMargin(marginPrefix = "/")
            .trim()
    }
}

internal class TextField2SelectionAsserter(
    textContent: String,
    rule: ComposeTestRule,
    textToolbar: TextToolbar,
    hapticFeedback: FakeHapticFeedback,
    getActual: () -> TextFieldState,
) :
    TextFieldSelectionAsserter<TextFieldState>(
        textContent,
        rule,
        textToolbar,
        hapticFeedback,
        getActual
    ) {
    override fun subAssert() {
        Truth.assertAbout(TextFieldStateSubject.withContent(textContent))
            .that(getActual())
            // BTF2 doesn't allow reversed selection, but it should contain the same min/max.
            .hasSelection(selection.run { TextRange(min, max) })
    }
}

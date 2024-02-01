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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.OutputTransformation
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.insert
import androidx.compose.foundation.text2.input.internal.selection.calculateAdjacentCursorPosition
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TransformedTextSelectionMovementTest {

    @Test
    fun calculateNextCursorPosition_aroundReplacement() {
        val state = TextFieldState("abc", initialSelectionInChars = TextRange(1))
        val outputTransformation = OutputTransformation {
            replace(1, 2, "zz") // "azzc"
        }
        val transformedState =
            TransformedTextFieldState(state, outputTransformation = outputTransformation)

        calculateNextCursorPosition(transformedState)

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(3))
    }

    @Test
    fun calculatePreviousCursorPosition_aroundReplacement() {
        val state = TextFieldState("abc", initialSelectionInChars = TextRange(2))
        val outputTransformation = OutputTransformation {
            replace(1, 2, "zz") // "azzc"
        }
        val transformedState =
            TransformedTextFieldState(state, outputTransformation = outputTransformation)
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(3))

        calculatePreviousCursorPosition(transformedState)

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(1))
    }

    @Test
    fun calculateNextCursorPosition_aroundInsertion() {
        val state = TextFieldState("ab", initialSelectionInChars = TextRange(0))
        val outputTransformation = OutputTransformation {
            insert(1, "zz") // "azzb"
        }
        val transformedState =
            TransformedTextFieldState(state, outputTransformation = outputTransformation)

        calculateNextCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.Start))

        calculateNextCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(3))
        assertThat(transformedState.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.End))

        calculateNextCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(4))
        assertThat(transformedState.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.End))
    }

    @Test
    fun calculatePreviousCursorPosition_aroundInsertion() {
        val state = TextFieldState("ab", initialSelectionInChars = TextRange(2))
        val outputTransformation = OutputTransformation {
            insert(1, "zz") // "azzb"
        }
        val transformedState =
            TransformedTextFieldState(state, outputTransformation = outputTransformation)
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(4))

        calculatePreviousCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(3))
        assertThat(transformedState.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.End))

        calculatePreviousCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(1))
        assertThat(transformedState.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.Start))

        calculatePreviousCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(0))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(0))
        assertThat(transformedState.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.Start))
    }

    @Test
    fun calculateNextCursorPosition_aroundDeletion() {
        val state = TextFieldState("abcd", initialSelectionInChars = TextRange(0))
        val outputTransformation = OutputTransformation {
            delete(1, 3) // "ad"
        }
        val transformedState =
            TransformedTextFieldState(state, outputTransformation = outputTransformation)

        calculateNextCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 3))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(1))

        calculateNextCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(4))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(2))
    }

    @Test
    fun calculatePreviousCursorPosition_aroundDeletion() {
        val state = TextFieldState("abcd", initialSelectionInChars = TextRange(4))
        val outputTransformation = OutputTransformation {
            delete(1, 3) // "ad"
        }
        val transformedState =
            TransformedTextFieldState(state, outputTransformation = outputTransformation)
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(2))

        calculatePreviousCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 3))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(1))

        calculatePreviousCursorPosition(transformedState)
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(0))
        assertThat(transformedState.visualText.selectionInChars).isEqualTo(TextRange(0))
    }

    private fun calculateNextCursorPosition(state: TransformedTextFieldState) {
        val newCursor = calculateAdjacentCursorPosition(
            state.visualText.toString(),
            state.visualText.selectionInChars.end,
            forward = true,
            state
        )
        state.placeCursorBeforeCharAt(newCursor)
    }

    private fun calculatePreviousCursorPosition(state: TransformedTextFieldState) {
        val newCursor = calculateAdjacentCursorPosition(
            state.visualText.toString(),
            state.visualText.selectionInChars.end,
            forward = false,
            state
        )
        state.placeCursorBeforeCharAt(newCursor)
    }
}

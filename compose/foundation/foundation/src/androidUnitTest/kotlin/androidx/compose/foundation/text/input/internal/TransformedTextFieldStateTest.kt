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

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.PlacedAnnotation
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TransformedTextFieldStateTest {

    @Test
    fun outputTransformationAffectsPresentedAndVisualText() {
        val state = TextFieldState("hello")
        val outputTransformation = OutputTransformation { append("world") }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.untransformedText.toString()).isEqualTo("hello")
        assertThat(transformedState.outputText.toString()).isEqualTo("helloworld")
        assertThat(transformedState.visualText.toString()).isEqualTo("helloworld")
    }

    @Test
    fun outputTransformationDoesNotRemoveComposingAnnotations() {
        val state = TextFieldState()
        val outputTransformation = OutputTransformation { append(" world") }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )
        val annotations: List<PlacedAnnotation> =
            listOf(AnnotatedString.Range(SpanStyle(background = Color.Blue), 0, 5))
        DefaultImeEditCommandScope(transformedState)
            .setComposingText(text = "hello", newCursorPosition = 1, annotations = annotations)

        assertThat(transformedState.visualText.composingAnnotations).isEqualTo(annotations)
    }

    @Test
    fun mapToTransformed_insertions() {
        val state = TextFieldState("zzzz")
        val outputTransformation = OutputTransformation {
            insert(2, "bb")
            insert(0, "aa")
            append("cc")
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.outputText.toString()).isEqualTo("aazzbbzzcc")
        assertThat(transformedState.mapToTransformed(0)).isEqualTo(TextRange(0, 2))
        assertThat(transformedState.mapToTransformed(1)).isEqualTo(TextRange(3))
        assertThat(transformedState.mapToTransformed(2)).isEqualTo(TextRange(4, 6))
        assertThat(transformedState.mapToTransformed(3)).isEqualTo(TextRange(7))
        assertThat(transformedState.mapToTransformed(4)).isEqualTo(TextRange(8, 10))
        // Past the end.
        assertThat(transformedState.mapToTransformed(5)).isEqualTo(TextRange(11))
    }

    @Test
    fun mapToTransformed_deletions() {
        val state = TextFieldState("aazzbbzzcc")
        val outputTransformation = OutputTransformation {
            delete(8, 10)
            delete(4, 6)
            delete(0, 2)
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.outputText.toString()).isEqualTo("zzzz")
        assertThat(transformedState.mapToTransformed(0)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapToTransformed(1)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapToTransformed(2)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapToTransformed(3)).isEqualTo(TextRange(1))
        assertThat(transformedState.mapToTransformed(4)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapToTransformed(5)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapToTransformed(6)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapToTransformed(7)).isEqualTo(TextRange(3))
        assertThat(transformedState.mapToTransformed(8)).isEqualTo(TextRange(4))
        assertThat(transformedState.mapToTransformed(9)).isEqualTo(TextRange(4))
        assertThat(transformedState.mapToTransformed(10)).isEqualTo(TextRange(4))
        // Past the end.
        assertThat(transformedState.mapToTransformed(11)).isEqualTo(TextRange(5))
    }

    @Test
    fun mapToTransformed_replacements() {
        val state = TextFieldState("aabb")
        val outputTransformation = OutputTransformation {
            replace(2, 4, "ddd")
            replace(0, 2, "c")
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.outputText.toString()).isEqualTo("cddd")
        assertThat(transformedState.mapToTransformed(0)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapToTransformed(1)).isEqualTo(TextRange(0, 1))
        assertThat(transformedState.mapToTransformed(2)).isEqualTo(TextRange(1))
        assertThat(transformedState.mapToTransformed(3)).isEqualTo(TextRange(1, 4))
        assertThat(transformedState.mapToTransformed(4)).isEqualTo(TextRange(4))
        // Past the end.
        assertThat(transformedState.mapToTransformed(5)).isEqualTo(TextRange(5))
    }

    @Test
    fun mapFromTransformed_insertions() {
        val state = TextFieldState("zzzz")
        val outputTransformation = OutputTransformation {
            insert(2, "bb")
            insert(0, "aa")
            append("cc")
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.outputText.toString()).isEqualTo("aazzbbzzcc")
        assertThat(transformedState.mapFromTransformed(0)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapFromTransformed(1)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapFromTransformed(2)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapFromTransformed(3)).isEqualTo(TextRange(1))
        assertThat(transformedState.mapFromTransformed(4)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapFromTransformed(5)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapFromTransformed(6)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapFromTransformed(7)).isEqualTo(TextRange(3))
        assertThat(transformedState.mapFromTransformed(8)).isEqualTo(TextRange(4))
        assertThat(transformedState.mapFromTransformed(9)).isEqualTo(TextRange(4))
        assertThat(transformedState.mapFromTransformed(10)).isEqualTo(TextRange(4))
        // Past the end.
        assertThat(transformedState.mapFromTransformed(11)).isEqualTo(TextRange(5))
    }

    @Test
    fun mapFromTransformed_deletions() {
        val state = TextFieldState("aazzbbzzcc")
        val outputTransformation = OutputTransformation {
            delete(8, 10)
            delete(4, 6)
            delete(0, 2)
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.outputText.toString()).isEqualTo("zzzz")
        assertThat(transformedState.mapFromTransformed(0)).isEqualTo(TextRange(0, 2))
        assertThat(transformedState.mapFromTransformed(1)).isEqualTo(TextRange(3))
        assertThat(transformedState.mapFromTransformed(2)).isEqualTo(TextRange(4, 6))
        assertThat(transformedState.mapFromTransformed(3)).isEqualTo(TextRange(7))
        assertThat(transformedState.mapFromTransformed(4)).isEqualTo(TextRange(8, 10))
        // Past the end.
        assertThat(transformedState.mapFromTransformed(5)).isEqualTo(TextRange(11))
    }

    @Test
    fun mapFromTransformed_replacements() {
        val state = TextFieldState("aabb")
        val outputTransformation = OutputTransformation {
            replace(2, 4, "ddd")
            replace(0, 2, "c")
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        assertThat(transformedState.outputText.toString()).isEqualTo("cddd")
        assertThat(transformedState.mapFromTransformed(0)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapFromTransformed(1)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapFromTransformed(2)).isEqualTo(TextRange(2, 4))
        assertThat(transformedState.mapFromTransformed(3)).isEqualTo(TextRange(2, 4))
        assertThat(transformedState.mapFromTransformed(4)).isEqualTo(TextRange(4))
        // Past the end.
        assertThat(transformedState.mapFromTransformed(5)).isEqualTo(TextRange(5))
    }

    @Test
    fun textFieldStateSelection_isTransformed_toPresentedText() {
        val state = TextFieldState("hello")
        val outputTransformation = OutputTransformation { insert(0, "aa") }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )
        assertThat(transformedState.outputText.toString()).isEqualTo("aahello")

        state.edit { selection = TextRange(0, 2) }
        assertThat(transformedState.outputText.selection).isEqualTo(TextRange(0, 4))
        // Rest of indices and wedge affinity are covered by mapToTransformed tests.
    }

    @Test
    fun collectImeNotifications_usesVisualText() = runTest {
        val state = TextFieldState("hello")
        val outputTransformation = OutputTransformation {
            insert(0, "a")
            insert(length, "a")
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        val collectedOldValues = mutableListOf<TextFieldCharSequence>()
        val collectedNewValues = mutableListOf<TextFieldCharSequence>()
        val collectedRestartImes = mutableListOf<Boolean>()
        val job = launch {
            transformedState.collectImeNotifications { oldValue, newValue, restartIme ->
                collectedOldValues += oldValue
                collectedNewValues += newValue
                collectedRestartImes += restartIme
            }
        }

        testScheduler.advanceUntilIdle()

        transformedState.editUntransformedTextAsUser(restartImeIfContentChanges = false) {
            append(" world")
        }

        testScheduler.advanceUntilIdle()

        assertThat(collectedOldValues)
            .containsExactly(TextFieldCharSequence("ahelloa", selection = TextRange(6)))
        assertThat(collectedNewValues)
            .containsExactly(TextFieldCharSequence("ahello worlda", selection = TextRange(12)))
        assertThat(collectedRestartImes).containsExactly(false)
        job.cancel()
    }

    @Test
    fun collectImeNotifications_carriesRestartImeUnchanged() = runTest {
        val state = TextFieldState("hello").apply { editAsUser(null) { setComposition(0, 5) } }
        val outputTransformation = OutputTransformation {
            insert(0, "a")
            insert(length, "a")
        }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = outputTransformation
            )

        val collectedOldValues = mutableListOf<TextFieldCharSequence>()
        val collectedNewValues = mutableListOf<TextFieldCharSequence>()
        val collectedRestartImes = mutableListOf<Boolean>()
        val job = launch {
            transformedState.collectImeNotifications { oldValue, newValue, restartIme ->
                collectedOldValues += oldValue
                collectedNewValues += newValue
                collectedRestartImes += restartIme
            }
        }

        testScheduler.advanceUntilIdle()

        transformedState.editUntransformedTextAsUser(restartImeIfContentChanges = true) {
            append(" world")
        }

        testScheduler.advanceUntilIdle()

        assertThat(collectedOldValues)
            .containsExactly(
                TextFieldCharSequence(
                    "ahelloa",
                    selection = TextRange(6),
                    composition = TextRange(0, 7)
                )
            )
        assertThat(collectedNewValues)
            .containsExactly(
                TextFieldCharSequence(
                    "ahello worlda",
                    selection = TextRange(12),
                    composition = TextRange(0, 6)
                )
            )
        assertThat(collectedRestartImes).containsExactly(true)
        job.cancel()
    }
}

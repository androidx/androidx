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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.OutputTransformation
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.insert
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TransformedTextFieldStateTest {

    @Test
    fun outputTransformationAffectsPresentedAndVisualText() {
        val state = TextFieldState("hello")
        val outputTransformation = OutputTransformation {
            append("world")
        }
        val transformedState = TransformedTextFieldState(
            textFieldState = state,
            outputTransformation = outputTransformation
        )

        assertThat(transformedState.untransformedText.toString()).isEqualTo("hello")
        assertThat(transformedState.outputText.toString()).isEqualTo("helloworld")
        assertThat(transformedState.visualText.toString()).isEqualTo("helloworld")
    }

    @Test
    fun mapToTransformed_insertions() {
        val state = TextFieldState("zzzz")
        val outputTransformation = OutputTransformation {
            insert(2, "bb")
            insert(0, "aa")
            append("cc")
        }
        val transformedState = TransformedTextFieldState(
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
        val transformedState = TransformedTextFieldState(
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
        val transformedState = TransformedTextFieldState(
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
        val transformedState = TransformedTextFieldState(
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
        val transformedState = TransformedTextFieldState(
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
        val transformedState = TransformedTextFieldState(
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
        val outputTransformation = OutputTransformation {
            insert(0, "aa")
        }
        val transformedState = TransformedTextFieldState(
            textFieldState = state,
            outputTransformation = outputTransformation
        )
        assertThat(transformedState.outputText.toString()).isEqualTo("aahello")

        state.edit { selectCharsIn(TextRange(0, 2)) }
        assertThat(transformedState.outputText.selectionInChars).isEqualTo(TextRange(0, 4))
        // Rest of indices and wedge affinity are covered by mapToTransformed tests.
    }
}

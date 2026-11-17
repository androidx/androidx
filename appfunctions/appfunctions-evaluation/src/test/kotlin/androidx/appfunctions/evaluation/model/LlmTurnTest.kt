/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.evaluation.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class LlmTurnTest {

    private val parts =
        listOf(LlmPart.TextPart("Create a note"), LlmPart.TextPart("Titled Grocery"))
    private val otherParts = listOf(LlmPart.TextPart("Create a note"))

    // UserTurn

    @Test
    fun userTurnConstructor_givenParts_areExposedInOrder() {
        val turn = LlmTurn.UserTurn(parts)

        assertThat(turn.parts)
            .containsExactly(LlmPart.TextPart("Create a note"), LlmPart.TextPart("Titled Grocery"))
            .inOrder()
    }

    @Test
    fun userTurnEquals_sameParts_isEqualWithSameHashCode() {
        val turn = LlmTurn.UserTurn(parts)
        val same = LlmTurn.UserTurn(parts.toList())

        assertThat(turn).isEqualTo(same)
        assertThat(turn.hashCode()).isEqualTo(same.hashCode())
    }

    @Test
    fun userTurnEquals_differentParts_isNotEqual() {
        val turn = LlmTurn.UserTurn(parts)
        val other = LlmTurn.UserTurn(otherParts)

        assertThat(turn).isNotEqualTo(other)
    }

    @Test
    fun userTurnEquals_modelTurnWithSameParts_isNotEqual() {
        val turn = LlmTurn.UserTurn(parts)
        val modelTurn = LlmTurn.ModelTurn(parts)

        assertThat(turn).isNotEqualTo(modelTurn)
    }

    @Test
    fun userTurnToString_turnWithParts_listsParts() {
        val turn = LlmTurn.UserTurn(otherParts)

        assertThat(turn.toString()).isEqualTo("UserTurn(parts=[TextPart(text='Create a note')])")
    }

    // ModelTurn

    @Test
    fun modelTurnConstructor_givenValues_areExposedAsProperties() {
        val turn = LlmTurn.ModelTurn(parts, inputTokens = 10, outputTokens = 5)

        assertThat(turn.parts)
            .containsExactly(LlmPart.TextPart("Create a note"), LlmPart.TextPart("Titled Grocery"))
            .inOrder()
        assertThat(turn.inputTokens).isEqualTo(10)
        assertThat(turn.outputTokens).isEqualTo(5)
    }

    @Test
    fun modelTurnConstructor_partsOnly_hasUnknownTokenCounts() {
        val turn = LlmTurn.ModelTurn(parts)

        assertThat(turn.inputTokens).isEqualTo(-1)
        assertThat(turn.outputTokens).isEqualTo(-1)
    }

    @Test
    fun modelTurnConstructor_zeroTokenCounts_areAccepted() {
        val turn = LlmTurn.ModelTurn(parts, inputTokens = 0, outputTokens = 0)

        assertThat(turn.inputTokens).isEqualTo(0)
        assertThat(turn.outputTokens).isEqualTo(0)
    }

    @Test
    fun modelTurnConstructor_inputTokensBelowMinusOne_throws() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                LlmTurn.ModelTurn(parts, inputTokens = -2)
            }

        assertThat(error).hasMessageThat().isEqualTo("inputTokens must be >= -1, got -2")
    }

    @Test
    fun modelTurnConstructor_outputTokensBelowMinusOne_throws() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                LlmTurn.ModelTurn(parts, outputTokens = -2)
            }

        assertThat(error).hasMessageThat().isEqualTo("outputTokens must be >= -1, got -2")
    }

    @Test
    fun modelTurnEquals_sameValues_isEqualWithSameHashCode() {
        val turn = LlmTurn.ModelTurn(parts, inputTokens = 10, outputTokens = 5)
        val same = LlmTurn.ModelTurn(parts.toList(), inputTokens = 10, outputTokens = 5)

        assertThat(turn).isEqualTo(same)
        assertThat(turn.hashCode()).isEqualTo(same.hashCode())
    }

    @Test
    fun modelTurnEquals_differentParts_isNotEqual() {
        val turn = LlmTurn.ModelTurn(parts, inputTokens = 10, outputTokens = 5)
        val other = LlmTurn.ModelTurn(otherParts, inputTokens = 10, outputTokens = 5)

        assertThat(turn).isNotEqualTo(other)
    }

    @Test
    fun modelTurnEquals_differentInputTokens_isNotEqual() {
        val turn = LlmTurn.ModelTurn(parts, inputTokens = 10, outputTokens = 5)
        val other = LlmTurn.ModelTurn(parts, inputTokens = -1, outputTokens = 5)

        assertThat(turn).isNotEqualTo(other)
    }

    @Test
    fun modelTurnEquals_differentOutputTokens_isNotEqual() {
        val turn = LlmTurn.ModelTurn(parts, inputTokens = 10, outputTokens = 5)
        val other = LlmTurn.ModelTurn(parts, inputTokens = 10, outputTokens = 6)

        assertThat(turn).isNotEqualTo(other)
    }

    @Test
    fun modelTurnEquals_userTurnWithSameParts_isNotEqual() {
        val turn = LlmTurn.ModelTurn(parts)
        val userTurn = LlmTurn.UserTurn(parts)

        assertThat(turn).isNotEqualTo(userTurn)
    }

    @Test
    fun modelTurnToString_partsOnly_showsUnknownTokenCounts() {
        val turn = LlmTurn.ModelTurn(otherParts)

        assertThat(turn.toString())
            .isEqualTo(
                "ModelTurn(parts=[TextPart(text='Create a note')], inputTokens=-1, outputTokens=-1)"
            )
    }

    @Test
    fun modelTurnToString_allValues_listsEveryProperty() {
        val turn = LlmTurn.ModelTurn(otherParts, inputTokens = 10, outputTokens = 5)

        assertThat(turn.toString())
            .isEqualTo(
                "ModelTurn(parts=[TextPart(text='Create a note')], inputTokens=10, outputTokens=5)"
            )
    }
}

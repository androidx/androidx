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

import androidx.annotation.IntRange

/**
 * Holds one conversational turn in `LlmResponse.turns`.
 *
 * @property parts ordered [LlmPart]s of this turn
 */
public abstract class LlmTurn private constructor(public val parts: List<LlmPart>) {
    /**
     * Holds a user turn.
     *
     * @param parts ordered [LlmPart]s of this user turn
     * @constructor creates a user turn with [parts]
     */
    public class UserTurn(parts: List<LlmPart>) : LlmTurn(parts) {
        override fun equals(other: Any?): Boolean = other is UserTurn && parts == other.parts

        override fun hashCode(): Int = parts.hashCode()

        override fun toString(): String = "UserTurn(parts=$parts)"
    }

    /**
     * Holds a model turn.
     *
     * @param parts ordered [LlmPart]s of this model turn
     * @param inputTokens tokens of the whole prompt that produced this turn, e.g. the system
     *   instruction, the tool declarations and all earlier turns, including tokens that the
     *   provider read from a cache; -1 if unknown
     * @param outputTokens tokens that the model generated in this turn, including any reasoning or
     *   thinking tokens the provider bills as output; -1 if unknown
     * @constructor creates a model turn with [parts] and token counts
     * @throws IllegalArgumentException if [inputTokens] or [outputTokens] is less than -1
     */
    public class ModelTurn(
        parts: List<LlmPart>,
        @IntRange(from = -1) @get:IntRange(from = -1) public val inputTokens: Int = -1,
        @IntRange(from = -1) @get:IntRange(from = -1) public val outputTokens: Int = -1,
    ) : LlmTurn(parts) {
        init {
            require(inputTokens >= -1) { "inputTokens must be >= -1, got $inputTokens" }
            require(outputTokens >= -1) { "outputTokens must be >= -1, got $outputTokens" }
        }

        override fun equals(other: Any?): Boolean =
            other is ModelTurn &&
                parts == other.parts &&
                inputTokens == other.inputTokens &&
                outputTokens == other.outputTokens

        override fun hashCode(): Int {
            var result = parts.hashCode()
            result = 31 * result + inputTokens
            result = 31 * result + outputTokens
            return result
        }

        override fun toString(): String =
            "ModelTurn(parts=$parts, inputTokens=$inputTokens, outputTokens=$outputTokens)"
    }
}

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

import androidx.appfunctions.AppFunctionName
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse

/** Holds a single content item in `LlmTurn.parts`. */
public abstract class LlmPart private constructor() {
    /**
     * Holds a natural-language text item.
     *
     * @property text text content
     * @constructor creates a text part with [text]
     */
    public class TextPart(public val text: String) : LlmPart() {
        override fun equals(other: Any?): Boolean = other is TextPart && text == other.text

        override fun hashCode(): Int = text.hashCode()

        override fun toString(): String = "TextPart(text='$text')"
    }

    /**
     * Holds an [ExecuteAppFunctionRequest] requested by the model, typically in an
     * `LlmTurn.ModelTurn`.
     *
     * @property request [ExecuteAppFunctionRequest] to execute
     * @constructor creates an AppFunction call request part with [request]
     */
    public class AppFunctionRequestPart(public val request: ExecuteAppFunctionRequest) : LlmPart() {
        override fun equals(other: Any?): Boolean =
            other is AppFunctionRequestPart && request == other.request

        override fun hashCode(): Int = request.hashCode()

        override fun toString(): String = "AppFunctionRequestPart(request=$request)"
    }

    /**
     * Holds the [ExecuteAppFunctionResponse] from executing the AppFunction [functionName],
     * typically in the `LlmTurn.UserTurn` that follows the model's request.
     *
     * @property functionName [AppFunctionName] of the executed AppFunction: the
     *   [ExecuteAppFunctionRequest.targetPackageName] and
     *   [ExecuteAppFunctionRequest.functionIdentifier] of the [AppFunctionRequestPart] it answers
     * @property response [ExecuteAppFunctionResponse] from the execution
     * @constructor creates an AppFunction execution response part for [functionName]
     */
    public class AppFunctionResponsePart(
        public val functionName: AppFunctionName,
        public val response: ExecuteAppFunctionResponse,
    ) : LlmPart() {
        override fun equals(other: Any?): Boolean =
            other is AppFunctionResponsePart &&
                functionName == other.functionName &&
                response == other.response

        override fun hashCode(): Int = 31 * functionName.hashCode() + response.hashCode()

        override fun toString(): String =
            "AppFunctionResponsePart(functionName=$functionName, response=$response)"
    }
}

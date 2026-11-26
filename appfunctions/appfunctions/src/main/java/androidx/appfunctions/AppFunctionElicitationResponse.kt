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

package androidx.appfunctions

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appfunctions.internal.requireBundle

/**
 * An [AppFunctionInputResponse] answering an [AppFunctionElicitationRequest].
 *
 * Callers include each response in [ExecuteAppFunctionRequest.inputResponses] under the same key as
 * the corresponding request in [AppFunctionInputRequiredException.inputRequests].
 */
public sealed class AppFunctionElicitationResponse : AppFunctionInputResponse() {

    final override fun writeToBundle(bundle: Bundle) {
        bundle.putInt(KEY_RESPONSE_TYPE, RESPONSE_TYPE_ELICITATION)
        writeElicitationToBundle(bundle)
    }

    /** Writes [KEY_ELICITATION_TYPE] and subclass-specific properties into [bundle]. */
    internal abstract fun writeElicitationToBundle(bundle: Bundle)

    /**
     * Answers an [AppFunctionElicitationRequest.Form] with structured [data].
     *
     * Build [data] using [AppFunctionData.Builder] initialized with the
     * [AppFunctionElicitationRequest.Form] request, setting the value under
     * [PROPERTY_ELICITATION_VALUE].
     *
     * App functions must treat [data] as unvalidated input from the caller.
     *
     * Example:
     * ```kotlin
     * val responseData =
     *     AppFunctionData.Builder(formRequest)
     *         .setString(
     *             AppFunctionElicitationResponse.FormData.PROPERTY_ELICITATION_VALUE,
     *             "Groceries",
     *         )
     *         .build()
     * val response = AppFunctionElicitationResponse.FormData(responseData)
     * ```
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public class FormData(
        /**
         * The elicited data, storing the value under [PROPERTY_ELICITATION_VALUE] matching
         * [AppFunctionElicitationRequest.Form.responseDataTypeMetadata].
         */
        public val data: AppFunctionData
    ) : AppFunctionElicitationResponse() {

        override fun writeElicitationToBundle(bundle: Bundle) {
            bundle.putInt(KEY_ELICITATION_TYPE, TYPE_FORM_DATA)
            bundle.putBundle(KEY_DATA, data.toBundle())
        }

        override fun toString(): String = "FormData(data=$data)"

        public companion object {
            /**
             * The property key used to store and retrieve the elicited value inside
             * [FormData.data].
             */
            public const val PROPERTY_ELICITATION_VALUE: String =
                "androidAppfunctionsElicitationValue"

            private const val KEY_DATA = "data"

            internal fun fromBundle(bundle: Bundle): FormData =
                FormData(AppFunctionData.fromBundle(bundle.requireBundle(KEY_DATA)))
        }
    }

    /**
     * Answers an [AppFunctionElicitationRequest.PendingAction].
     *
     * Callers send this response after launching
     * [AppFunctionElicitationRequest.PendingAction.pendingIntent] and the launched activity
     * finishes, ending the user's interaction with the target app's UI.
     *
     * This response carries no user input and isn't proof that the user completed the action. App
     * functions must determine the outcome from their own app state.
     */
    public class ActionFinished() : AppFunctionElicitationResponse() {
        override fun writeElicitationToBundle(bundle: Bundle) {
            bundle.putInt(KEY_ELICITATION_TYPE, TYPE_ACTION_FINISHED)
        }

        override fun equals(other: Any?): Boolean = other is ActionFinished

        override fun hashCode(): Int = javaClass.hashCode()

        override fun toString(): String = "ActionFinished()"
    }

    /**
     * Reserves room for new elicitation response types, so that app functions handle unknown types
     * with an `else` branch.
     */
    internal class HiddenElicitation : AppFunctionElicitationResponse() {
        override fun writeElicitationToBundle(bundle: Bundle) {
            throw UnsupportedOperationException("Hidden response can't be converted to a Bundle")
        }
    }

    internal companion object {
        private const val KEY_ELICITATION_TYPE = "elicitationType"

        private const val TYPE_FORM_DATA = 1
        private const val TYPE_ACTION_FINISHED = 2

        /**
         * Creates an [AppFunctionElicitationResponse] from a [Bundle] created by [toBundle].
         *
         * Returns `null` if the bundle isn't an elicitation response of a known type.
         *
         * @throws IllegalArgumentException if [bundle] is malformed.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun fromBundle(bundle: Bundle): AppFunctionElicitationResponse? =
            when (bundle.getInt(KEY_ELICITATION_TYPE)) {
                TYPE_FORM_DATA -> FormData.fromBundle(bundle)
                TYPE_ACTION_FINISHED -> ActionFinished()
                else -> null
            }
    }
}

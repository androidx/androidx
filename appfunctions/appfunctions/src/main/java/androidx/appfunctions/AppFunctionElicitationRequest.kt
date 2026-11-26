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

import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import androidx.appfunctions.internal.requireBundle
import androidx.appfunctions.internal.requireCharSequence
import androidx.appfunctions.internal.requireParcelable
import androidx.appfunctions.internal.requireString
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import java.util.Objects

/**
 * An [AppFunctionInputRequest] that asks the caller to elicit additional input before an app
 * function can complete execution.
 *
 * App functions throw [AppFunctionInputRequiredException] carrying one or more elicitation requests
 * when they need extra information (such as disambiguation or confirmation) to proceed.
 */
public sealed class AppFunctionElicitationRequest : AppFunctionInputRequest() {

    final override fun writeToBundle(bundle: Bundle) {
        bundle.putInt(KEY_REQUEST_TYPE, REQUEST_TYPE_ELICITATION)
        writeElicitationToBundle(bundle)
    }

    /** Writes [KEY_ELICITATION_TYPE] and subclass-specific properties into [bundle]. */
    internal abstract fun writeElicitationToBundle(bundle: Bundle)

    /**
     * Asks the caller for structured input matching [responseDataTypeMetadata].
     *
     * Use [Form] for non-sensitive input (such as disambiguation or preferences) that the caller
     * collects in-band and returns as an [AppFunctionElicitationResponse.FormData] on the next
     * call. For sensitive interactions, use [PendingAction].
     *
     * Example:
     * ```kotlin
     * throw AppFunctionInputRequiredException(
     *     inputRequests =
     *         mapOf(
     *             "choose_note" to
     *                 AppFunctionElicitationRequest.Form(
     *                     instruction = "Which note should be deleted?",
     *                     responseDataTypeMetadata =
     *                         AppFunctionStringTypeMetadata(
     *                             isNullable = false,
     *                             enumValues = setOf("Groceries", "Packing List"),
     *                         ),
     *                 )
     *         )
     * )
     * ```
     *
     * @throws IllegalArgumentException if [responseDataTypeMetadata] is not a supported primitive
     *   type.
     */
    public class Form(
        /** Explains what input is needed, for the caller to understand the request. */
        public val instruction: String,
        /**
         * The shape of the data that the app function expects in the response.
         *
         * Only primitive types are supported: [AppFunctionBooleanTypeMetadata],
         * [AppFunctionIntTypeMetadata], [AppFunctionLongTypeMetadata],
         * [AppFunctionFloatTypeMetadata], [AppFunctionDoubleTypeMetadata], and
         * [AppFunctionStringTypeMetadata].
         */
        public val responseDataTypeMetadata: AppFunctionDataTypeMetadata,
    ) : AppFunctionElicitationRequest() {

        init {
            require(isSupportedResponseDataType(responseDataTypeMetadata)) {
                "Unsupported responseDataTypeMetadata: " +
                    "${responseDataTypeMetadata::class.simpleName}. " +
                    "Only primitive types (boolean, int, long, float, double, string) are supported."
            }
        }

        override fun writeElicitationToBundle(bundle: Bundle) {
            bundle.putInt(KEY_ELICITATION_TYPE, TYPE_FORM)
            bundle.putString(KEY_INSTRUCTION, instruction)
            bundle.putBundle(KEY_RESPONSE_DATA_TYPE_METADATA, responseDataTypeMetadata.toBundle())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Form) return false
            return instruction == other.instruction &&
                responseDataTypeMetadata == other.responseDataTypeMetadata
        }

        override fun hashCode(): Int = Objects.hash(instruction, responseDataTypeMetadata)

        override fun toString(): String =
            "Form(instruction=$instruction, responseDataTypeMetadata=$responseDataTypeMetadata)"

        internal companion object {
            private const val KEY_INSTRUCTION = "instruction"
            private const val KEY_RESPONSE_DATA_TYPE_METADATA = "responseDataTypeMetadata"

            private fun isSupportedResponseDataType(
                metadata: AppFunctionDataTypeMetadata
            ): Boolean =
                when (metadata) {
                    is AppFunctionBooleanTypeMetadata,
                    is AppFunctionIntTypeMetadata,
                    is AppFunctionLongTypeMetadata,
                    is AppFunctionFloatTypeMetadata,
                    is AppFunctionDoubleTypeMetadata,
                    is AppFunctionStringTypeMetadata -> true
                    else -> false
                }

            fun fromBundle(bundle: Bundle): Form =
                Form(
                    instruction = bundle.requireString(KEY_INSTRUCTION),
                    responseDataTypeMetadata =
                        AppFunctionDataTypeMetadata.fromBundle(
                            bundle.requireBundle(KEY_RESPONSE_DATA_TYPE_METADATA)
                        ),
                )
        }
    }

    /**
     * Asks the caller to launch [pendingIntent] for out-of-band user input in the app's own UI.
     *
     * Once the interaction finishes, the caller retries the function with
     * [AppFunctionElicitationResponse.ActionFinished]. Use [PendingAction] for sensitive
     * interactions such as authentication, payment authorization.
     *
     * Example:
     * ```kotlin
     * val intent = Intent(context, ConfirmDeleteActivity::class.java).putExtra("id", requestId)
     * val pendingIntent =
     *     PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
     * throw AppFunctionInputRequiredException(
     *     inputRequests =
     *         mapOf(
     *             "confirm_delete" to
     *                 AppFunctionElicitationRequest.PendingAction(
     *                     displayMessage = "Open Notes to confirm deleting the note.",
     *                     pendingIntent = pendingIntent,
     *                 )
     *         )
     * )
     * ```
     *
     * @throws IllegalArgumentException if [pendingIntent] does not target an activity.
     */
    public class PendingAction(
        /**
         * Localized message explaining to the user why the app's UI is being opened.
         *
         * The target app should provide a localized string, which the caller may display to the
         * user as provided.
         */
        public val displayMessage: CharSequence,
        /**
         * Opens the app's UI for the user to provide input.
         *
         * Must be an activity [PendingIntent] (for example, created via [PendingIntent.getActivity]
         * or [PendingIntent.getActivities]). The launched activity should set a result (such as
         * [android.app.Activity.RESULT_OK] or [android.app.Activity.RESULT_CANCELED]) and finish
         * when the interaction completes, recording any outcome in its own state. The caller should
         * launch this [PendingIntent] for a result (for example, using
         * `ActivityResultContracts.StartIntentSenderForResult` or
         * [android.app.Activity.startIntentSenderForResult]) and wait for the result before calling
         * the function again.
         */
        public val pendingIntent: PendingIntent,
    ) : AppFunctionElicitationRequest() {

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                require(pendingIntent.isActivity) { "pendingIntent must target an activity." }
            }
        }

        override fun writeElicitationToBundle(bundle: Bundle) {
            bundle.putInt(KEY_ELICITATION_TYPE, TYPE_PENDING_ACTION)
            bundle.putCharSequence(KEY_DISPLAY_MESSAGE, displayMessage)
            bundle.putParcelable(KEY_PENDING_INTENT, pendingIntent)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PendingAction) return false
            return displayMessage == other.displayMessage && pendingIntent == other.pendingIntent
        }

        override fun hashCode(): Int = Objects.hash(displayMessage, pendingIntent)

        override fun toString(): String =
            "PendingAction(displayMessage=$displayMessage, pendingIntent=$pendingIntent)"

        internal companion object {
            private const val KEY_DISPLAY_MESSAGE = "displayMessage"
            private const val KEY_PENDING_INTENT = "pendingIntent"

            fun fromBundle(bundle: Bundle): PendingAction =
                PendingAction(
                    displayMessage = bundle.requireCharSequence(KEY_DISPLAY_MESSAGE),
                    pendingIntent = bundle.requireParcelable(KEY_PENDING_INTENT),
                )
        }
    }

    /**
     * Reserves room for new request types, so that callers handle unknown types with an `else`
     * branch.
     */
    internal class HiddenElicitation : AppFunctionElicitationRequest() {
        override fun writeElicitationToBundle(bundle: Bundle) {
            throw UnsupportedOperationException("Hidden request can't be converted to a Bundle")
        }
    }

    internal companion object {
        private const val KEY_ELICITATION_TYPE = "elicitationType"

        private const val TYPE_FORM = 1
        private const val TYPE_PENDING_ACTION = 2

        /**
         * Creates an [AppFunctionElicitationRequest] from a [Bundle] created by [toBundle].
         *
         * Returns `null` if the bundle isn't an elicitation request of a known type.
         *
         * @throws IllegalArgumentException if [bundle] is malformed.
         */
        fun fromBundle(bundle: Bundle): AppFunctionElicitationRequest? =
            when (bundle.getInt(KEY_ELICITATION_TYPE)) {
                TYPE_FORM -> Form.fromBundle(bundle)
                TYPE_PENDING_ACTION -> PendingAction.fromBundle(bundle)
                else -> null
            }
    }
}

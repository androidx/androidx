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

import android.os.Bundle

/**
 * A request from an app function for input that the caller must resolve before calling the function
 * again, sent through [AppFunctionInputRequiredException].
 *
 * @see AppFunctionElicitationRequest
 */
public sealed class AppFunctionInputRequest {

    /** Converts this request to a [Bundle] that can be sent across processes. */
    internal fun toBundle(): Bundle = Bundle().apply { writeToBundle(this) }

    /** Writes [KEY_REQUEST_TYPE] and subclass-specific properties into [bundle]. */
    internal abstract fun writeToBundle(bundle: Bundle)

    /**
     * Reserves room for new request types, so that callers handle unknown types with an `else`
     * branch.
     */
    internal class Hidden : AppFunctionInputRequest() {
        override fun writeToBundle(bundle: Bundle) {
            throw UnsupportedOperationException("Hidden request can't be converted to a Bundle")
        }
    }

    internal companion object {
        internal const val KEY_REQUEST_TYPE = "requestType"
        internal const val REQUEST_TYPE_ELICITATION = 1

        /**
         * Creates an [AppFunctionInputRequest] from a [Bundle] created by [toBundle].
         *
         * Returns `null` if the request type is unknown, for example because the request was
         * created by a newer version of the library.
         *
         * @throws IllegalArgumentException if [bundle] is malformed.
         */
        fun fromBundle(bundle: Bundle): AppFunctionInputRequest? =
            when (bundle.getInt(KEY_REQUEST_TYPE)) {
                REQUEST_TYPE_ELICITATION -> AppFunctionElicitationRequest.fromBundle(bundle)
                else -> null
            }
    }
}

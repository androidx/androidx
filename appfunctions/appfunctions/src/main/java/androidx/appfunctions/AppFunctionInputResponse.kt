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

/**
 * A response to an [AppFunctionInputRequest], passed by the caller in
 * [ExecuteAppFunctionRequest.inputResponses] when retrying an app function after receiving
 * [AppFunctionInputRequiredException].
 *
 * @see AppFunctionElicitationResponse
 */
public sealed class AppFunctionInputResponse {

    /** Converts this response to a [Bundle] that can be sent across processes. */
    internal fun toBundle(): Bundle = Bundle().apply { writeToBundle(this) }

    /** Writes [KEY_RESPONSE_TYPE] and subclass-specific properties into [bundle]. */
    internal abstract fun writeToBundle(bundle: Bundle)

    /**
     * Reserves room for new response types, so that app functions handle unknown types with an
     * `else` branch.
     */
    internal class Hidden : AppFunctionInputResponse() {
        override fun writeToBundle(bundle: Bundle) {
            throw UnsupportedOperationException("Hidden response can't be converted to a Bundle")
        }
    }

    internal companion object {
        internal const val KEY_RESPONSE_TYPE = "responseType"
        internal const val RESPONSE_TYPE_ELICITATION = 1

        /**
         * Creates an [AppFunctionInputResponse] from a [Bundle] created by [toBundle].
         *
         * Returns `null` if the response type is unknown, for example because the response was
         * created by a newer version of the library.
         *
         * @throws IllegalArgumentException if [bundle] is malformed.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun fromBundle(bundle: Bundle): AppFunctionInputResponse? =
            when (bundle.getInt(KEY_RESPONSE_TYPE)) {
                RESPONSE_TYPE_ELICITATION -> AppFunctionElicitationResponse.fromBundle(bundle)
                else -> null
            }
    }
}

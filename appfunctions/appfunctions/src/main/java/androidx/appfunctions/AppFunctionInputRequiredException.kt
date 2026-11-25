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
import android.util.Log
import androidx.appfunctions.internal.AppFunctionLibraryConfiguration.isLoggingEnabled
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG

// TODO(b/586898710): Update KDoc to link ExecuteAppFunctionRequest.inputResponses and
// AppFunctionInputResponse once response APIs are added.
/**
 * Thrown by an app function when it requires input from the caller before it can proceed.
 *
 * To proceed, the caller resolves each request in [inputRequests] and calls the same function again
 * with the responses.
 */
public class AppFunctionInputRequiredException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionAppException(ERROR_INPUT_REQUIRED, errorMessage, extras) {

    // TODO(b/586898710): Update KDoc to link ExecuteAppFunctionRequest.inputResponses once response
    // APIs are added.
    /**
     * The input requests that the caller must resolve before calling the function again.
     *
     * Each request is keyed by a key that the app function assigns and that is unique within the
     * exception. The caller returns the response to a request under the same key.
     */
    public val inputRequests: Map<String, AppFunctionInputRequest> = readInputRequests(extras)

    /**
     * Creates an [AppFunctionInputRequiredException].
     *
     * @param inputRequests The input requests that the caller must resolve before calling the
     *   function again, keyed by a key that is unique within the exception.
     * @param errorMessage The error message for the caller to understand why the input is required.
     * @throws IllegalArgumentException if [inputRequests] is empty.
     */
    public constructor(
        inputRequests: Map<String, AppFunctionInputRequest>,
        errorMessage: String? = null,
    ) : this(errorMessage, createExtras(inputRequests))

    private companion object {
        private const val EXTRA_INPUT_REQUESTS = "androidx.appfunctions.extra.INPUT_REQUESTS"

        private fun createExtras(inputRequests: Map<String, AppFunctionInputRequest>): Bundle {
            require(inputRequests.isNotEmpty()) { "inputRequests must not be empty" }
            val requestsBundle = Bundle()
            for ((key, request) in inputRequests) {
                requestsBundle.putBundle(key, request.toBundle())
            }
            return Bundle().apply { putBundle(EXTRA_INPUT_REQUESTS, requestsBundle) }
        }

        /**
         * Reads the input requests from [extras].
         *
         * The extras may come from another process, so requests that are malformed or of an unknown
         * type are skipped instead of failing the whole exception.
         */
        private fun readInputRequests(extras: Bundle): Map<String, AppFunctionInputRequest> {
            val requestsBundle =
                try {
                    extras.getBundle(EXTRA_INPUT_REQUESTS)
                } catch (e: RuntimeException) {
                    if (isLoggingEnabled) {
                        Log.w(APP_FUNCTIONS_TAG, "Malformed input requests extras", e)
                    }
                    return emptyMap()
                } ?: return emptyMap()
            val keys =
                try {
                    requestsBundle.keySet()
                } catch (e: RuntimeException) {
                    if (isLoggingEnabled) {
                        Log.w(APP_FUNCTIONS_TAG, "Malformed input requests bundle", e)
                    }
                    return emptyMap()
                }
            return buildMap {
                for (key in keys) {
                    if (key == null) {
                        if (isLoggingEnabled) {
                            Log.w(APP_FUNCTIONS_TAG, "Skipping input request with null key")
                        }
                        continue
                    }
                    val request =
                        try {
                            val requestBundle = requestsBundle.getBundle(key)
                            if (requestBundle != null) {
                                AppFunctionInputRequest.fromBundle(requestBundle)
                            } else {
                                null
                            }
                        } catch (e: RuntimeException) {
                            if (isLoggingEnabled) {
                                Log.w(APP_FUNCTIONS_TAG, "Malformed input request $key", e)
                            }
                            continue
                        }
                    if (request != null) {
                        put(key, request)
                    } else if (isLoggingEnabled) {
                        Log.w(APP_FUNCTIONS_TAG, "Skipping unsupported input request $key")
                    }
                }
            }
        }
    }
}

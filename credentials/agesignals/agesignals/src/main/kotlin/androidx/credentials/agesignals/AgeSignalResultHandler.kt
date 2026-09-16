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

package androidx.credentials.agesignals

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.agesignals.exceptions.GetAgeRangeException

/**
 * Converts age range results to and from the [Bundle] representation exchanged between an age
 * signal provider and the [AgeSignalProvider] implementation that surfaces the result to the
 * calling app.
 *
 * Providers write a result with [toResponseBundle] or [toExceptionBundle]. The resulting [Bundle]
 * can be returned directly when a provider is able to fulfill a request without user interaction,
 * or attached to the result [Intent] of a consent [Activity]:
 * ```kotlin
 * setResult(
 *     Activity.RESULT_OK,
 *     Intent().putExtras(AgeSignalResultHandler.toResponseBundle(response)),
 * )
 * ```
 *
 * [AgeSignalProvider] implementations read results back with [retrieveGetAgeRangeResponse] and
 * [retrieveGetAgeRangeException], passing either the bundle received directly from the provider or
 * `intent.extras` from the consent activity's result.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AgeSignalResultHandler private constructor() {
    public companion object {
        @VisibleForTesting
        internal const val EXTRA_GET_AGE_RANGE_RESPONSE =
            "androidx.credentials.agesignals.extra.GET_AGE_RANGE_RESPONSE"

        @VisibleForTesting
        internal const val EXTRA_GET_AGE_RANGE_EXCEPTION =
            "androidx.credentials.agesignals.extra.GET_AGE_RANGE_EXCEPTION"

        /**
         * Converts a successful [GetAgeRangeResponse] into a [Bundle] that can be sent across a
         * process, to be read back with [retrieveGetAgeRangeResponse].
         *
         * @param response the age range result to return to the calling app
         */
        @JvmStatic
        public fun toResponseBundle(response: GetAgeRangeResponse): Bundle {
            val bundle = Bundle()
            bundle.putBundle(EXTRA_GET_AGE_RANGE_RESPONSE, GetAgeRangeResponse.asBundle(response))
            return bundle
        }

        /**
         * Converts a [GetAgeRangeException] into a [Bundle] that can be sent across a process, to
         * be read back with [retrieveGetAgeRangeException].
         *
         * @param exception the failure to return to the calling app
         */
        @JvmStatic
        public fun toExceptionBundle(exception: GetAgeRangeException): Bundle {
            val bundle = Bundle()
            bundle.putBundle(
                EXTRA_GET_AGE_RANGE_EXCEPTION,
                GetAgeRangeException.asBundle(exception),
            )
            return bundle
        }

        /**
         * Retrieves the [GetAgeRangeResponse] written by a provider through [toResponseBundle].
         *
         * @param bundle the bundle returned directly by the provider, or `intent.extras` of the
         *   result intent of the provider's consent activity
         * @return the parsed response, or `null` if [bundle] carries no response or carries one
         *   that this version of the library cannot interpret
         */
        @JvmStatic
        public fun retrieveGetAgeRangeResponse(bundle: Bundle): GetAgeRangeResponse? {
            val responseBundle = bundle.getBundle(EXTRA_GET_AGE_RANGE_RESPONSE) ?: return null
            return GetAgeRangeResponse.fromBundle(responseBundle)
        }

        /**
         * Retrieves the [GetAgeRangeException] written by a provider through [toExceptionBundle].
         *
         * @param bundle the bundle returned directly by the provider, or `intent.extras` of the
         *   result intent of the provider's consent activity
         * @return the parsed exception, or `null` if [bundle] carries no exception. An exception
         *   entry that this version of the library cannot interpret is reported as a
         *   [androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException], so a `null`
         *   return always means the provider reported no failure.
         */
        @JvmStatic
        public fun retrieveGetAgeRangeException(bundle: Bundle): GetAgeRangeException? {
            val exceptionBundle = bundle.getBundle(EXTRA_GET_AGE_RANGE_EXCEPTION) ?: return null
            return GetAgeRangeException.fromBundle(exceptionBundle)
        }
    }
}

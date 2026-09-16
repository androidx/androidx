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

package androidx.credentials.agesignals.internal

import android.util.Log
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.credentials.agesignals.exceptions.GetAgeRangeInterruptedException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnavailableException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException

private const val TAG = "AgeSignalConversionUtil"

/**
 * Reconstructs the concrete [GetAgeRangeException] subclass matching [errorType], falling back to
 * [GetAgeRangeUnknownException] for types this version of the library does not recognize.
 */
internal fun toJetpackGetAgeRangeException(
    errorType: String,
    errorMsg: CharSequence?,
): GetAgeRangeException {
    return when (errorType) {
        GetAgeRangeUnavailableException.TYPE_GET_AGE_RANGE_UNAVAILABLE_EXCEPTION ->
            GetAgeRangeUnavailableException(errorMsg)
        GetAgeRangeInterruptedException.TYPE_GET_AGE_RANGE_INTERRUPTED_EXCEPTION ->
            GetAgeRangeInterruptedException(errorMsg)
        GetAgeRangeProviderConfigurationException
            .TYPE_GET_AGE_RANGE_PROVIDER_CONFIGURATION_EXCEPTION ->
            GetAgeRangeProviderConfigurationException(errorMsg)
        GetAgeRangeUnknownException.TYPE_GET_AGE_RANGE_UNKNOWN_EXCEPTION ->
            GetAgeRangeUnknownException(errorMsg)
        else -> {
            Log.w(
                TAG,
                "Unrecognized age range exception type '$errorType'; reporting the failure as " +
                    "unknown.",
            )
            GetAgeRangeUnknownException(errorMsg)
        }
    }
}

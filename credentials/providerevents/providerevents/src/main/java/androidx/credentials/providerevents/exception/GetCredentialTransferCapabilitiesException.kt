/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.exception

import androidx.annotation.RestrictTo

/**
 * Represents an error thrown when the provider is requested to return the transfer capabilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GetCredentialTransferCapabilitiesException(public val type: String, message: String) :
    Exception(message) {
    public companion object {
        /** The request cannot be trusted because the caller is unknown */
        public const val UNKNOWN_CALLER_TYPE: String =
            "androidx.credentials.providerevents.exception.GetCredentialTransferCapabilitiesException.UNKNOWN_CALLER_TYPE"

        /** Used by the system when the request fails to reach the provider */
        public const val SYSTEM_ERROR_TYPE: String =
            "androidx.credentials.providerevents.exception.GetCredentialTransferCapabilitiesException.SYSTEM_ERROR_TYPE"

        /** The request cannot be processed due to unknown error */
        public const val UNKNOWN_ERROR_TYPE: String =
            "androidx.credentials.providerevents.exception.GetCredentialTransferCapabilitiesException.UNKNOWN_ERROR_TYPE"
    }
}

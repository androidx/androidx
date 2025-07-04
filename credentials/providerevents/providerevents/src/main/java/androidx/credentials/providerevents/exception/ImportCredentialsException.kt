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

/** Represents an error thrown during the import flow. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImportCredentialsException(public val type: String, message: String) :
    Exception(message) {
    public companion object {
        /** The request json cannot be read because it is written in invalid format */
        public const val INVALID_JSON_TYPE: String =
            "androidx.credentials.providerevents.exception.ImportCredentialsException.INVALID_JSON_TYPE"

        /** The request cannot be trusted because the caller is unknown */
        public const val UNKNOWN_CALLER_TYPE: String =
            "androidx.credentials.providerevents.exception.ImportCredentialsException.UNKNOWN_CALLER_TYPE"

        /** Used by the system when the request fails to reach the provider */
        public const val SYSTEM_ERROR_TYPE: String =
            "androidx.credentials.providerevents.exception.ImportCredentialsException.SYSTEM_ERROR_TYPE"

        /** The credential json cannot be returned due to unknown error */
        public const val UNKNOWN_ERROR_TYPE: String =
            "androidx.credentials.providerevents.exception.ImportCredentialsException.UNKNOWN_ERROR_TYPE"
    }
}

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

package androidx.credentials.providerevents.exception

import androidx.annotation.RestrictTo

/**
 * Reported by a credential provider through
 * [androidx.credentials.providerevents.service.CredentialProviderEventsService.onBeginBatchCreateCredentialRequest]
 * when it cannot supply create entries for the batch request.
 *
 * @property errorMessage a human-readable message describing the failure, or `null`
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BeginBatchCreateCredentialException
@JvmOverloads
public constructor(public val errorMessage: CharSequence? = null) :
    Exception(errorMessage?.toString()) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val type: String = TYPE_BEGIN_BATCH_CREATE_CREDENTIAL_EXCEPTION

    internal companion object {
        internal const val TYPE_BEGIN_BATCH_CREATE_CREDENTIAL_EXCEPTION =
            "androidx.credentials.providerevents.exception.BeginBatchCreateCredentialException.TYPE_UNKNOWN"
    }
}

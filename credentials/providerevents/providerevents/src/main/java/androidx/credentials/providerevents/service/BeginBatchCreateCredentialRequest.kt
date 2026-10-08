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

package androidx.credentials.providerevents.service

import androidx.annotation.RestrictTo
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.providerevents.internal.RequestValidationHelper

/**
 * Asks a credential provider which accounts it can create credentials under for a batch of relying
 * parties, delivered to [CredentialProviderEventsService.onBatchCreateCredentialCandidatesRequest].
 *
 * @property requests one request per relying party, each carrying that relying party as its
 *   [callingAppInfo][BeginCreateCredentialRequest.callingAppInfo]
 * @throws IllegalArgumentException if a request has no `callingAppInfo`, or two requests have the
 *   same calling package name
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BeginBatchCreateCredentialRequest(requests: List<BeginCreateCredentialRequest>) {
    public val requests: List<BeginCreateCredentialRequest> = requests.toList()

    init {
        RequestValidationHelper.requireUniquePackageNames(
            this.requests.map {
                requireNotNull(it.callingAppInfo) { "Each request must have a callingAppInfo" }
                    .packageName
            }
        )
    }
}

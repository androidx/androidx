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
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.providerevents.internal.RequestValidationHelper

/**
 * The batch of credential creation requests delivered to a credential provider's fulfillment
 * activity, one per relying party.
 *
 * Each request is identified by the package name in its
 * [callingAppInfo][ProviderCreateCredentialRequest.callingAppInfo], which is the relying party as
 * verified by the system. The provider returns one [CreateCredentialResult] per request in a
 * [BatchCreateCredentialResponse], matched by that package name.
 *
 * @property requests the relying parties' credential creation requests
 * @throws IllegalArgumentException if two requests have the same calling package name
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProviderBatchCreateCredentialRequest(requests: List<ProviderCreateCredentialRequest>) {
    public val requests: List<ProviderCreateCredentialRequest> = requests.toList()

    init {
        RequestValidationHelper.requireUniquePackageNames(
            this.requests.map { it.callingAppInfo.packageName }
        )
    }
}

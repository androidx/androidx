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
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException

/**
 * The outcome of creating a credential for one relying party in a batch: either a [response] or an
 * [exception].
 *
 * @property packageName the package name of the relying party this result is for, matching the
 *   [callingAppInfo][androidx.credentials.provider.ProviderCreateCredentialRequest.callingAppInfo]
 *   of its request in the [ProviderBatchCreateCredentialRequest]
 * @property response the created credential, or `null` if creation failed
 * @property exception why creation failed, or `null` if it succeeded
 * @throws IllegalArgumentException if [packageName] is empty
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CreateCredentialResult
private constructor(
    public val packageName: String,
    public val response: CreateCredentialResponse?,
    public val exception: CreateCredentialException?,
) {
    init {
        require(packageName.isNotEmpty()) { "packageName must not be empty" }
    }

    /** Constructs a successful result. */
    public constructor(
        packageName: String,
        response: CreateCredentialResponse,
    ) : this(packageName, response, null)

    /** Constructs a failed result. */
    public constructor(
        packageName: String,
        exception: CreateCredentialException,
    ) : this(packageName, null, exception)
}

/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider

import android.os.Bundle
import android.service.credentials.CallingAppInfo

/**
 * Base custom begin create request class for registering a credential.
 *
 * If you get a [BeginCreateCustomCredentialRequest] instead of a type-safe request class such as
 * [BeginCreatePasswordCredentialRequest], [BeginCreatePublicKeyCredentialRequest], etc., then
 * as a credential provider, you should check if you have any other library at interest that
 * supports this custom [type] of credential request,
 * and if so use its parsing utilities to resolve to a type-safe class within that library.
 *
 * Note: The Bundle keys for [candidateQueryData] should not be in the form
 * of androidx.credentials.*` as they are reserved for internal use by this androidx library.
 *
 * @param type the credential type determined by the credential-type-specific subclass for
 * custom use cases
 * @param candidateQueryData the partial request data in the [Bundle] format that will be sent
 * to the provider during the initial candidate query stage, which should not contain sensitive
 * user credential information (note: bundle keys in the form of `androidx.credentials.*` are
 * reserved for internal library use)
 * @param callingAppInfo info pertaining to the app that is requesting for credentials
 * retrieval or creation
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [type], or [candidateQueryData] is null
 */
open class BeginCreateCustomCredentialRequest constructor(
    type: String,
    candidateQueryData: Bundle,
    callingAppInfo: CallingAppInfo?
) : BeginCreateCredentialRequest(type, candidateQueryData, callingAppInfo)
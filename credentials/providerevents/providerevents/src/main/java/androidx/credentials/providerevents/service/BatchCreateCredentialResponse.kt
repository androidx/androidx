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
import androidx.credentials.providerevents.internal.RequestValidationHelper

/**
 * The results a credential provider returns for a [ProviderBatchCreateCredentialRequest] through
 * [androidx.credentials.providerevents.IntentHandler.setBatchCreateCredentialResponse].
 *
 * Results are matched to requests by [CreateCredentialResult.packageName], never by position. A
 * response should carry exactly one result per request. The system ignores results for package
 * names it did not request, and reports each request without a result to its relying party as a
 * failure.
 *
 * @property results one result per relying party
 * @throws IllegalArgumentException if two results have the same package name
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BatchCreateCredentialResponse(results: List<CreateCredentialResult>) {
    public val results: List<CreateCredentialResult> = results.toList()

    init {
        RequestValidationHelper.requireUniquePackageNames(this.results.map { it.packageName })
    }
}

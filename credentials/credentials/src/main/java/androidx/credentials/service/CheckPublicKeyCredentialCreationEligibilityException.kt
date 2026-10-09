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

package androidx.credentials.service

import androidx.annotation.RestrictTo

/**
 * Reported by a relying party through
 * [RelyingPartyEventsService.onCheckPublicKeyCredentialCreationEligibility] when it cannot
 * determine whether the user is eligible for passkey creation.
 *
 * @property errorMessage a human-readable message describing the failure, or `null`
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPublicKeyCredentialCreationEligibilityException
@JvmOverloads
public constructor(public val errorMessage: CharSequence? = null) :
    Exception(errorMessage?.toString()) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val type: String = TYPE_CHECK_PUBLIC_KEY_CREDENTIAL_CREATION_ELIGIBILITY_EXCEPTION

    internal companion object {
        internal const val TYPE_CHECK_PUBLIC_KEY_CREDENTIAL_CREATION_ELIGIBILITY_EXCEPTION =
            "androidx.credentials.service.CheckPublicKeyCredentialCreationEligibilityException.TYPE_UNKNOWN"
    }
}

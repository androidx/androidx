/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials

import android.content.ComponentName
import android.os.Bundle

/**
 * A request to retrieve the user's saved application password from their password provider.
 *
 * @param isAutoSelectAllowed false by default, allows auto selecting a password if there is
 * only one available
 * @param allowedProviders a set of provider service [ComponentName] allowed to receive this
 * option (Note: a [SecurityException] will be thrown if it is set as non-empty but your app does
 * not have android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS; for API level < 34,
 * this property will not take effect and you should control the allowed provider via
 * [library dependencies](https://developer.android.com/training/sign-in/passkeys#add-dependencies))
 */
class GetPasswordOption @JvmOverloads constructor(
    isAutoSelectAllowed: Boolean = false,
    allowedProviders: Set<ComponentName> = emptySet(),
) : CredentialOption(
    type = PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    requestData = Bundle(),
    candidateQueryData = Bundle(),
    isSystemProviderRequired = false,
    isAutoSelectAllowed = isAutoSelectAllowed,
    allowedProviders,
) {

    /** @hide */
    companion object {
        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        internal fun createFrom(
            data: Bundle,
            allowedProviders: Set<ComponentName>,
        ): GetPasswordOption {
            return GetPasswordOption(
                data.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false),
                allowedProviders
            )
        }
    }
}
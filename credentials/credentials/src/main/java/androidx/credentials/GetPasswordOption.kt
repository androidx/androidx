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
 * @property allowedUserIds a optional set of user ids with which the credentials associated are
 * requested; leave as empty if you want to request all the available user credentials
 * @property typePriorityHint always sets the priority of this entry to
 * [PriorityHints.PRIORITY_PASSWORD_OR_SIMILAR], which defines how it appears in the credential
 * selector, with less precedence than account ordering but more precedence than last used time;
 * see [PriorityHints] and [CredentialOption] for more information
 */
class GetPasswordOption private constructor(
    val allowedUserIds: Set<String>,
    isAutoSelectAllowed: Boolean,
    allowedProviders: Set<ComponentName>,
    requestData: Bundle,
    candidateQueryData: Bundle,
    typePriorityHint: @PriorityHints Int =
        PASSWORD_OPTION_PRIORITY_CATEGORY,
) : CredentialOption(
    type = PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    requestData = requestData,
    candidateQueryData = candidateQueryData,
    isSystemProviderRequired = false,
    isAutoSelectAllowed = isAutoSelectAllowed,
    allowedProviders = allowedProviders,
    typePriorityHint = typePriorityHint,
) {

    /**
     * Constructs a [GetPasswordOption].
     *
     * @param allowedUserIds a optional set of user ids with which the credentials associated are
     * requested; leave as empty if you want to request all the available user credentials
     * @param isAutoSelectAllowed false by default, allows auto selecting a password if there is
     * only one available
     * @param allowedProviders a set of provider service [ComponentName] allowed to receive this
     * option (Note: a [SecurityException] will be thrown if it is set as non-empty but your app does
     * not have android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS; for API level < 34,
     * this property will not take effect and you should control the allowed provider via
     * [library dependencies](https://developer.android.com/training/sign-in/passkeys#add-dependencies))
     */
    @JvmOverloads constructor(
        allowedUserIds: Set<String> = emptySet(),
        isAutoSelectAllowed: Boolean = false,
        allowedProviders: Set<ComponentName> = emptySet(),
    ) : this(
        allowedUserIds = allowedUserIds,
        isAutoSelectAllowed = isAutoSelectAllowed,
        allowedProviders = allowedProviders,
        requestData = toBundle(allowedUserIds),
        candidateQueryData = toBundle(allowedUserIds)
    )

    internal companion object {
        internal const val BUNDLE_KEY_ALLOWED_USER_IDS =
            "androidx.credentials.BUNDLE_KEY_ALLOWED_USER_IDS"

        internal const val PASSWORD_OPTION_PRIORITY_CATEGORY =
            PriorityHints.PRIORITY_PASSWORD_OR_SIMILAR

        @JvmStatic
        internal fun createFrom(
            data: Bundle,
            allowedProviders: Set<ComponentName>,
            candidateQueryData: Bundle,
        ): GetPasswordOption {
            val allowUserIdList = data.getStringArrayList(BUNDLE_KEY_ALLOWED_USER_IDS)
            return GetPasswordOption(
                allowedUserIds = allowUserIdList?.toSet() ?: emptySet(),
                isAutoSelectAllowed = data.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false),
                allowedProviders = allowedProviders,
                requestData = data,
                candidateQueryData = candidateQueryData,
                typePriorityHint = data.getInt(BUNDLE_KEY_TYPE_PRIORITY_VALUE,
                    PASSWORD_OPTION_PRIORITY_CATEGORY),
            )
        }

        @JvmStatic
        internal fun toBundle(allowUserIds: Set<String>): Bundle {
            val bundle = Bundle()
            bundle.putStringArrayList(BUNDLE_KEY_ALLOWED_USER_IDS, ArrayList(allowUserIds))
            return bundle
        }
    }
}

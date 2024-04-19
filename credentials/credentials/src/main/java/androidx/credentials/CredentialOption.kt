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
import androidx.annotation.RestrictTo
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base class for getting a specific type of credentials.
 *
 * [GetCredentialRequest] will be composed of a list of [CredentialOption] subclasses to indicate
 * the specific credential types and configurations that your app accepts.
 *
 * The [typePriorityHint] bit helps decide where the credential will be displayed on the
 * selector. It is used with more importance than signals like 'last recently used' but with less
 * importance than other signals, such as the ordering of displayed accounts.
 * It is expected to be one of the defined [PriorityHints] constants. By default,
 * [GetCustomCredentialOption] will have [PriorityHints.PRIORITY_DEFAULT], [GetPasswordOption] will
 * have [PriorityHints.PRIORITY_PASSWORD_OR_SIMILAR] and [GetPublicKeyCredentialOption] will have
 * [PriorityHints.PRIORITY_PASSKEY_OR_SIMILAR]. It is expected that [GetCustomCredentialOption]
 * types will remain unchanged unless strong reasons arise and cannot ever have
 * [PriorityHints.PRIORITY_PASSKEY_OR_SIMILAR]. Given passkeys prevent many security threats that
 * other credentials do not, we enforce that nothing is shown higher than
 * passkey types in order to provide end users with the safest credentials first. See the spec
 * [here](https://w3c.github.io/webauthn/) for more information on passkeys.
 *
 * @property type the credential type determined by the credential-type-specific subclass (e.g.
 * the type for [GetPasswordOption] is [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] and for
 * [GetPublicKeyCredentialOption] is [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL])
 * @property requestData the request data in the [Bundle] format
 * @property candidateQueryData the partial request data in the [Bundle] format that will be sent to
 * the provider during the initial candidate query stage, which will not contain sensitive user
 * information
 * @property isSystemProviderRequired true if must only be fulfilled by a system provider and false
 * otherwise
 * @property isAutoSelectAllowed whether a credential entry will be automatically chosen if it is
 * the only one available option
 * @property allowedProviders a set of provider service [ComponentName] allowed to receive this
 * option (Note: a [SecurityException] will be thrown if it is set as non-empty but your app does
 * not have android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS; for API level < 34,
 * this property will not take effect and you should control the allowed provider via
 * [library dependencies](https://developer.android.com/training/sign-in/passkeys#add-dependencies))
 * @property typePriorityHint sets the priority of this entry, which defines how it appears in
 * the credential selector, with less precedence than account ordering but more precedence than last
 * used time; see [PriorityHints] for more information
 */
abstract class CredentialOption internal constructor(
    val type: String,
    val requestData: Bundle,
    val candidateQueryData: Bundle,
    val isSystemProviderRequired: Boolean,
    val isAutoSelectAllowed: Boolean,
    val allowedProviders: Set<ComponentName>,
    val typePriorityHint: @PriorityHints Int,
) {

    init {
        requestData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, isAutoSelectAllowed)
        candidateQueryData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, isAutoSelectAllowed)
        requestData.putInt(BUNDLE_KEY_TYPE_PRIORITY_VALUE,
            typePriorityHint);
        candidateQueryData.putInt(BUNDLE_KEY_TYPE_PRIORITY_VALUE, typePriorityHint)
    }

    internal companion object {
        internal const val BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED =
            "androidx.credentials.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED"

        internal const val BUNDLE_KEY_TYPE_PRIORITY_VALUE =
            "androidx.credentials.BUNDLE_KEY_TYPE_PRIORITY_VALUE"

        internal fun extractAutoSelectValue(data: Bundle): Boolean {
            return data.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        fun createFrom(
            type: String,
            requestData: Bundle,
            candidateQueryData: Bundle,
            requireSystemProvider: Boolean,
            allowedProviders: Set<ComponentName>,
        ): CredentialOption {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        GetPasswordOption.createFrom(
                            requestData, allowedProviders, candidateQueryData)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        when (requestData.getString(PublicKeyCredential.BUNDLE_KEY_SUBTYPE)) {
                            GetPublicKeyCredentialOption
                                .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION ->
                                GetPublicKeyCredentialOption.createFrom(
                                    requestData, allowedProviders, candidateQueryData)
                            else -> throw FrameworkClassParsingException()
                        }
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a request with
                // the raw framework values.
                GetCustomCredentialOption(
                    requestData,
                    type,
                    candidateQueryData = candidateQueryData,
                    isSystemProviderRequired = requireSystemProvider,
                    isAutoSelectAllowed = requestData.getBoolean(
                        BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false),
                    allowedProviders = allowedProviders,
                    typePriorityHint = requestData.getInt(BUNDLE_KEY_TYPE_PRIORITY_VALUE,
                        GetCustomCredentialOption.CUSTOM_OPTION_PRIORITY_CATEGORY
                    ),
                )
            }
        }
    }
}

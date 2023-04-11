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
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Encapsulates a request to get a user credential.
 *
 * An application can construct such a request by adding one or more types of [CredentialOption],
 * and then call [CredentialManager.getCredential] to launch framework UI flows to allow the user
 * to consent to using a previously saved credential for the given application.
 *
 * @property credentialOptions the list of [CredentialOption] from which the user can choose
 * one to authenticate to the app
 * @property origin the origin of a different application if the request is being made on behalf of
 * that application. For API level >=34, setting a non-null value for this parameter, will throw
 * a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present.
 * @property preferIdentityDocUi the value which signals if the UI should be tailored to display an
 * identity document like driver license etc.
 * @property preferUiBrandingComponentName a service [ComponentName] from which the Credential
 * Selector UI will pull its label and icon to render top level branding. Your app must have the
 * permission android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this, or
 * it would not take effect. Notice that this bit may not take effect for Android API level
 * 33 and below, depending on the pre-34 provider(s) you have chosen.
 * @throws IllegalArgumentException If [credentialOptions] is empty
 */
class GetCredentialRequest
@JvmOverloads constructor(
    val credentialOptions: List<CredentialOption>,
    val origin: String? = null,
    val preferIdentityDocUi: Boolean = false,
    val preferUiBrandingComponentName: ComponentName? = null,
) {

    init {
        require(credentialOptions.isNotEmpty()) { "credentialOptions should not be empty" }
    }

    /** A builder for [GetCredentialRequest]. */
    class Builder {
        private var credentialOptions: MutableList<CredentialOption> = mutableListOf()
        private var origin: String? = null
        private var preferIdentityDocUi: Boolean = false
        private var preferUiBrandingComponentName: ComponentName? = null

        /** Adds a specific type of [CredentialOption]. */
        fun addCredentialOption(credentialOption: CredentialOption): Builder {
            credentialOptions.add(credentialOption)
            return this
        }

        /** Sets the list of [CredentialOption]. */
        fun setCredentialOptions(credentialOptions: List<CredentialOption>): Builder {
            this.credentialOptions = credentialOptions.toMutableList()
            return this
        }

        /**
         * Sets the [origin] of a different application if the request is being made on behalf of
         * that application. For API level >=34, setting a non-null value for this parameter, will
         * throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not
         * present.
         */
        fun setOrigin(origin: String): Builder {
            this.origin = origin
            return this
        }

        /**
         * Sets service [ComponentName] from which the Credential Selector UI will pull its label
         * and icon to render top level branding. Your app must have the
         * permission android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this,
         * or it would not take effect. Notice that this bit may not take effect for Android API
         * level 33 and below, depending on the pre-34 provider(s) you have chosen.
         */
        fun setPreferUiBrandingComponentName(component: ComponentName?): Builder {
            this.preferUiBrandingComponentName = component
            return this
        }

        /**
         * Sets the [Boolean] preferIdentityDocUi to true if the requester wants to prefer using a
         * UI suited for Identity Documents like mDocs, Driving License etc.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setPreferIdentityDocUi(preferIdentityDocUi: Boolean): Builder {
            this.preferIdentityDocUi = preferIdentityDocUi
            return this
        }

        /**
         * Builds a [GetCredentialRequest].
         *
         * @throws IllegalArgumentException If [credentialOptions] is empty
         */
        fun build(): GetCredentialRequest {
            return GetCredentialRequest(
                credentialOptions.toList(),
                origin,
                preferIdentityDocUi,
                preferUiBrandingComponentName
            )
        }
    }

    /** @hide */
    companion object {
        internal const val BUNDLE_KEY_PREFER_IDENTITY_DOC_UI =
            "androidx.credentials.BUNDLE_KEY_PREFER_IDENTITY_DOC_UI"
        internal const val BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME =
            "androidx.credentials.BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME"

        /** @hide */
        @JvmStatic
        fun toRequestDataBundle(
            request: GetCredentialRequest
        ): Bundle {
            val bundle = Bundle()
            bundle.putBoolean(BUNDLE_KEY_PREFER_IDENTITY_DOC_UI, request.preferIdentityDocUi)
            bundle.putParcelable(
                BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME, request.preferUiBrandingComponentName)
            return bundle
        }

        /** @hide */
        @JvmStatic
        fun createFrom(
            credentialOptions: List<CredentialOption>,
            origin: String?,
            data: Bundle
        ): GetCredentialRequest {
            try {
                val preferIdentityDocUi = data.getBoolean(BUNDLE_KEY_PREFER_IDENTITY_DOC_UI)
                @Suppress("DEPRECATION")
                val preferUiBrandingComponentName = data.getParcelable<ComponentName>(
                    BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME)
                var getCredentialBuilder = Builder().setCredentialOptions(credentialOptions)
                    .setPreferIdentityDocUi(preferIdentityDocUi)
                    .setPreferUiBrandingComponentName(preferUiBrandingComponentName)
                if (origin != null) {
                    getCredentialBuilder.setOrigin(origin)
                }
                return getCredentialBuilder.build()
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
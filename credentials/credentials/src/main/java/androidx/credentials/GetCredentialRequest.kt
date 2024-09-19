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
import androidx.annotation.RequiresApi
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Encapsulates a request to get a user credential.
 *
 * An application can construct such a request by adding one or more types of [CredentialOption],
 * and then call [CredentialManager.getCredential] to launch framework UI flows to allow the user to
 * consent to using a previously saved credential for the given application.
 *
 * @param credentialOptions the list of [CredentialOption] from which the user can choose one to
 *   authenticate to the app
 * @param origin the origin of a different application if the request is being made on behalf of
 *   that application (Note: for API level >=34, setting a non-null value for this parameter, will
 *   throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
 * @param preferIdentityDocUi the value which signals if the UI should be tailored to display an
 *   identity document like driver license etc
 * @param preferUiBrandingComponentName a service [ComponentName] from which the Credential Selector
 *   UI will pull its label and icon to render top level branding (Note: your app must have the
 *   permission android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this, or it
 *   would not take effect; also this bit may not take effect for Android API level 33 and below,
 *   depending on the pre-34 provider(s) you have chosen
 * @param preferImmediatelyAvailableCredentials true if you prefer the operation to return
 *   immediately when there is no available credentials instead of falling back to discovering
 *   remote options, and false (default) otherwise
 * @property credentialOptions the list of [CredentialOption] from which the user can choose one to
 *   authenticate to the app
 * @property origin the origin of a different application if the request is being made on behalf of
 *   that application. For API level >=34, setting a non-null value for this parameter, will throw a
 *   SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present.
 * @property preferIdentityDocUi the value which signals if the UI should be tailored to display an
 *   identity document like driver license etc.
 * @property preferUiBrandingComponentName a service [ComponentName] from which the Credential
 *   Selector UI will pull its label and icon to render top level branding. Your app must have the
 *   permission android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this, or it
 *   would not take effect. Notice that this bit may not take effect for Android API level 33 and
 *   below, depending on the pre-34 provider(s) you have chosen.
 * @property preferImmediatelyAvailableCredentials true if you prefer the operation to return
 *   immediately when there is no available credentials instead of falling back to discovering
 *   remote options, and false (default) otherwise
 * @throws IllegalArgumentException If [credentialOptions] is empty or contains
 *   [GetRestoreCredentialOption] with another option (i.e. [GetPasswordOption] or
 *   [GetPublicKeyCredentialOption]).
 */
class GetCredentialRequest
@JvmOverloads
constructor(
    val credentialOptions: List<CredentialOption>,
    val origin: String? = null,
    val preferIdentityDocUi: Boolean = false,
    val preferUiBrandingComponentName: ComponentName? = null,
    @get:JvmName("preferImmediatelyAvailableCredentials")
    val preferImmediatelyAvailableCredentials: Boolean = false,
) {

    init {
        require(credentialOptions.isNotEmpty()) { "credentialOptions should not be empty" }
        if (credentialOptions.size > 1) {
            for (option in credentialOptions) {
                if (option is GetRestoreCredentialOption) {
                    throw IllegalArgumentException(
                        "Only a single GetRestoreCredentialOption should be provided."
                    )
                }
            }
        }
    }

    /** A builder for [GetCredentialRequest]. */
    class Builder {
        private var credentialOptions: MutableList<CredentialOption> = mutableListOf()
        private var origin: String? = null
        private var preferIdentityDocUi: Boolean = false
        private var preferImmediatelyAvailableCredentials: Boolean = false
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
         * Sets whether you prefer the operation to return immediately when there is no available
         * credentials instead of falling back to discovering remote options. The default value is
         * false.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setPreferImmediatelyAvailableCredentials(
            preferImmediatelyAvailableCredentials: Boolean
        ): Builder {
            this.preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials
            return this
        }

        /**
         * Sets service [ComponentName] from which the Credential Selector UI will pull its label
         * and icon to render top level branding. Your app must have the permission
         * android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this, or it would
         * not take effect. Notice that this bit may not take effect for Android API level 33 and
         * below, depending on the pre-34 provider(s) you have chosen.
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
                preferUiBrandingComponentName,
                preferImmediatelyAvailableCredentials
            )
        }
    }

    companion object {
        internal const val BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS =
            "androidx.credentials.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS"
        private const val BUNDLE_KEY_PREFER_IDENTITY_DOC_UI =
            "androidx.credentials.BUNDLE_KEY_PREFER_IDENTITY_DOC_UI"
        private const val BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME =
            "androidx.credentials.BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME"

        /**
         * Returns the request metadata as a `Bundle`.
         *
         * This API should only be used by OEM services and library groups.
         *
         * Note: this is not the equivalent of the complete request itself. For example, it does not
         * include the request's `credentialOptions` or `origin`.
         */
        @JvmStatic
        fun getRequestMetadataBundle(request: GetCredentialRequest): Bundle {
            val bundle = Bundle()
            bundle.putBoolean(BUNDLE_KEY_PREFER_IDENTITY_DOC_UI, request.preferIdentityDocUi)
            bundle.putBoolean(
                BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                request.preferImmediatelyAvailableCredentials
            )
            bundle.putParcelable(
                BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME,
                request.preferUiBrandingComponentName
            )
            return bundle
        }

        /**
         * Parses the [request] into an instance of [GetCredentialRequest].
         *
         * It is recommended to construct a GetCredentialRequest by direct constructor calls,
         * instead of using this API. This API should only be used by a small subset of system apps
         * that reconstruct an existing object for user interactions such as collecting consents.
         *
         * @param request the framework GetCredentialRequest object
         */
        @RequiresApi(34)
        @JvmStatic
        fun createFrom(request: android.credentials.GetCredentialRequest): GetCredentialRequest {
            return createFrom(
                request.credentialOptions.map { CredentialOption.createFrom(it) },
                request.origin,
                request.data
            )
        }

        /**
         * Parses the raw data into an instance of [GetCredentialRequest].
         *
         * It is recommended to construct a GetCredentialRequest by direct constructor calls,
         * instead of using this API. This API should only be used by a small subset of system apps
         * that reconstruct an existing object for user interactions such as collecting consents.
         *
         * @param credentialOptions matches [GetCredentialRequest.credentialOptions]
         * @param origin matches [GetCredentialRequest.origin]
         * @param metadata request metadata serialized as a Bundle using [getRequestMetadataBundle]
         */
        @JvmStatic
        fun createFrom(
            credentialOptions: List<CredentialOption>,
            origin: String?,
            metadata: Bundle
        ): GetCredentialRequest {
            try {
                val preferIdentityDocUi = metadata.getBoolean(BUNDLE_KEY_PREFER_IDENTITY_DOC_UI)
                val preferImmediatelyAvailableCredentials =
                    metadata.getBoolean(BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS)
                @Suppress("DEPRECATION")
                val preferUiBrandingComponentName =
                    metadata.getParcelable<ComponentName>(
                        BUNDLE_KEY_PREFER_UI_BRANDING_COMPONENT_NAME
                    )
                val getCredentialBuilder =
                    Builder()
                        .setCredentialOptions(credentialOptions)
                        .setPreferIdentityDocUi(preferIdentityDocUi)
                        .setPreferUiBrandingComponentName(preferUiBrandingComponentName)
                        .setPreferImmediatelyAvailableCredentials(
                            preferImmediatelyAvailableCredentials
                        )
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

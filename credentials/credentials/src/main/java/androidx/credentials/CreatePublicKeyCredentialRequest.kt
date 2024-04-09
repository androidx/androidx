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

import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.PublicKeyCredential.Companion.BUNDLE_KEY_SUBTYPE
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.internal.RequestValidationHelper
import org.json.JSONObject

/**
 * A request to register a passkey from the user's public key credential provider.
 *
 * @property requestJson the request in JSON format in the [standard webauthn web json](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
 * @property clientDataHash a clientDataHash value to sign over in place of assembling and hashing
 * clientDataJSON during the signature request; only meaningful when [origin] is set
 * @param requestJson the request in JSON format in the [standard webauthn web json](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
 * @param clientDataHash a clientDataHash value to sign over in place of assembling and hashing
 * clientDataJSON during the signature request; only meaningful when [origin] is set
 * @param preferImmediatelyAvailableCredentials true if you prefer the operation to return
 * immediately when there is no available passkey registration offering instead of falling back to
 * discovering remote options, and false (default) otherwise
 * @param origin the origin of a different application if the request is being made on behalf of
 * that application (Note: for API level >=34, setting a non-null value for this parameter will
 * throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
 */
class CreatePublicKeyCredentialRequest private constructor(
    val requestJson: String,
    val clientDataHash: ByteArray?,
    isAutoSelectAllowed: Boolean,
    preferImmediatelyAvailableCredentials: Boolean,
    displayInfo: DisplayInfo,
    origin: String? = null,
    credentialData: Bundle = toCredentialDataBundle(requestJson, clientDataHash),
    candidateQueryData: Bundle = toCandidateDataBundle(requestJson, clientDataHash),
) : CreateCredentialRequest(
    type = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    credentialData = credentialData,
    // The whole request data should be passed during the query phase.
    candidateQueryData = candidateQueryData,
    isSystemProviderRequired = false,
    isAutoSelectAllowed = isAutoSelectAllowed,
    displayInfo,
    origin,
    preferImmediatelyAvailableCredentials
) {

    /**
     * Constructs a [CreatePublicKeyCredentialRequest] to register a passkey from the user's public
     * key credential provider.
     *
     * @param requestJson the privileged request in JSON format in the [standard webauthn web json](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
     * @param clientDataHash a hash that is used to verify the relying party identity
     * @param preferImmediatelyAvailableCredentials true if you prefer the operation to return
     * immediately when there is no available passkey registration offering instead of falling back to
     * discovering remote options, and false (default) otherwise
     * @param origin the origin of a different application if the request is being made on behalf of
     * that application (Note: for API level >=34, setting a non-null value for this parameter will
     * throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
     * @param isAutoSelectAllowed whether a create option will be automatically chosen if it is
     * the only one available to the user (note that there is a chance that the crendeiatl provider
     * does not support auto-select even if you turn this bit on)
     * @throws NullPointerException If [requestJson] is null
     * @throws IllegalArgumentException If [requestJson] is empty, or if it is not a valid JSON,
     * or if it doesn't have a valid `user.name` defined according to the
     * [webauthn spec]
     * (https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
     */
    @JvmOverloads constructor(
        requestJson: String,
        clientDataHash: ByteArray? = null,
        preferImmediatelyAvailableCredentials: Boolean = false,
        origin: String? = null,
        isAutoSelectAllowed: Boolean = false,
    ) : this(
        requestJson = requestJson,
        clientDataHash = clientDataHash,
        preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
        displayInfo = getRequestDisplayInfo(requestJson),
        origin = origin,
        isAutoSelectAllowed = isAutoSelectAllowed,
    )

    /**
     * Constructs a [CreatePublicKeyCredentialRequest] to register a passkey from the user's public
     * key credential provider.
     *
     * @param requestJson the privileged request in JSON format in the [standard webauthn web
     * json](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
     * @param clientDataHash a hash that is used to verify the relying party identity
     * @param preferImmediatelyAvailableCredentials true if you prefer the operation to return
     * immediately when there is no available passkey registration offering instead of falling back to
     * discovering remote options, and false (preferably) otherwise
     * @param origin the origin of a different application if the request is being made on behalf of
     * that application (Note: for API level >=34, setting a non-null value for this parameter will
     * throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
     * @param preferDefaultProvider the preferred default provider component name to prioritize in
     * the selection UI flows (Note: tour app must have the permission
     * android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this, or it
     * would not take effect; also this bit may not take effect for Android API level 33 and below,
     * depending on the pre-34 provider(s) you have chosen)
     * @param isAutoSelectAllowed whether a create option will be automatically chosen if it is
     * the only one available to the user (note that there is a chance that the credential provider
     * does not support auto-select even if you turn this bit on)
     * @throws NullPointerException If [requestJson] is null
     * @throws IllegalArgumentException If [requestJson] is empty, or if it doesn't have a valid
     * `user.name` defined according to the [webauthn
     * spec](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
     */
    constructor(
        requestJson: String,
        clientDataHash: ByteArray?,
        preferImmediatelyAvailableCredentials: Boolean,
        origin: String?,
        preferDefaultProvider: String?,
        isAutoSelectAllowed: Boolean,
    ) : this(
        requestJson = requestJson,
        clientDataHash = clientDataHash,
        preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
        displayInfo = getRequestDisplayInfo(requestJson, preferDefaultProvider),
        origin = origin,
        isAutoSelectAllowed = isAutoSelectAllowed,
    )

    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "requestJson must not be empty, and must be a valid JSON" }
    }

    internal companion object {
        internal const val BUNDLE_KEY_CLIENT_DATA_HASH =
            "androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH"
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        internal const val BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST =
            "androidx.credentials.BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST"

        @JvmStatic
        internal fun getRequestDisplayInfo(
            requestJson: String,
            defaultProvider: String? = null,
        ): DisplayInfo {
            return try {
                val json = JSONObject(requestJson)
                val user = json.getJSONObject("user")
                val userName = user.getString("name")
                val displayName: String? =
                    if (user.isNull("displayName")) null else user.getString("displayName")
                DisplayInfo(
                    userId = userName,
                    userDisplayName = displayName,
                    credentialTypeIcon = null,
                    preferDefaultProvider = defaultProvider,
                )
            } catch (e: Exception) {
                throw IllegalArgumentException("user.name must be defined in requestJson")
            }
        }

        @JvmStatic
        internal fun toCredentialDataBundle(
            requestJson: String,
            clientDataHash: ByteArray? = null,
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(
                BUNDLE_KEY_SUBTYPE,
                BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST
            )
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            bundle.putByteArray(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHash)
            return bundle
        }

        @JvmStatic
        internal fun toCandidateDataBundle(
            requestJson: String,
            clientDataHash: ByteArray?,
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(
                BUNDLE_KEY_SUBTYPE,
                BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST
            )
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            bundle.putByteArray(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHash)
            return bundle
        }

        @JvmStatic
        @RequiresApi(23)
        internal fun createFrom(data: Bundle, origin: String?, candidateQueryData: Bundle):
            CreatePublicKeyCredentialRequest {
            try {
                val requestJson = data.getString(BUNDLE_KEY_REQUEST_JSON)!!
                val clientDataHash = data.getByteArray(BUNDLE_KEY_CLIENT_DATA_HASH)
                val preferImmediatelyAvailableCredentials =
                    data.getBoolean(BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS, false)
                val displayInfo = try {
                    DisplayInfo.parseFromCredentialDataBundle(data)
                } catch (e: IllegalArgumentException) {
                    getRequestDisplayInfo(requestJson)
                }
                val isAutoSelectAllowed =
                    data.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false)
                return CreatePublicKeyCredentialRequest(
                    requestJson = requestJson,
                    clientDataHash = clientDataHash,
                    preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
                    displayInfo = displayInfo,
                    origin = origin,
                    isAutoSelectAllowed = isAutoSelectAllowed,
                    credentialData = data,
                    candidateQueryData = candidateQueryData,
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}

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

package androidx.credentials.playservices.controllers.CreatePublicKeyCredential

import android.util.Base64
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.domerrors.AbortError
import androidx.credentials.exceptions.domerrors.ConstraintError
import androidx.credentials.exceptions.domerrors.DataError
import androidx.credentials.exceptions.domerrors.EncodingError
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.NetworkError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.domerrors.NotReadableError
import androidx.credentials.exceptions.domerrors.NotSupportedError
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.domerrors.TimeoutError
import androidx.credentials.exceptions.domerrors.UnknownError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensions
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.COSEAlgorithmIdentifier
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.FidoAppIdExtension
import com.google.android.gms.fido.fido2.api.common.GoogleThirdPartyPaymentExtension
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import com.google.android.gms.fido.fido2.api.common.UserVerificationMethodExtension
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A utility class to handle logic for the begin sign in controller.
 */
internal class PublicKeyCredentialControllerUtility {

    companion object {

        private val JSON_KEY_CLIENT_DATA = "clientDataJSON"
        private val JSON_KEY_ATTESTATION_OBJ = "attestationObject"
        private val JSON_KEY_AUTH_DATA = "authenticationData"
        private val JSON_KEY_SIGNATURE = "signature"
        private val JSON_KEY_USER_HANDLE = "userHandle"
        private val JSON_KEY_RESPONSE = "response"
        private val JSON_KEY_ID = "id"
        private val JSON_KEY_RAW_ID = "rawId"
        private val JSON_KEY_TYPE = "type"
        private val JSON_KEY_RPID = "rpId"
        private val JSON_KEY_CHALLENGE = "challenge"
        private val JSON_KEY_APPID = "appid"
        private val JSON_KEY_THIRD_PARTY_PAYMENT = "thirdPartyPayment"
        private val JSON_KEY_AUTH_SELECTION = "authenticatorSelection"
        private val JSON_KEY_REQUIRE_RES_KEY = "requireResidentKey"
        private val JSON_KEY_RES_KEY = "residentKey"
        private val JSON_KEY_AUTH_ATTACHMENT = "authenticatorAttachment"
        private val JSON_KEY_TIMEOUT = "timeout"
        private val JSON_KEY_EXCLUDE_CREDENTIALS = "excludeCredentials"
        private val JSON_KEY_TRANSPORTS = "transports"
        private val JSON_KEY_RP = "rp"
        private val JSON_KEY_NAME = "name"
        private val JSON_KEY_ICON = "icon"
        private val JSON_KEY_ALG = "alg"
        private val JSON_KEY_USER = "user"
        private val JSON_KEY_DISPLAY_NAME = "displayName"
        private val JSON_KEY_USER_VERIFICATION_METHOD = "userVerificationMethod"
        private val JSON_KEY_KEY_PROTECTION_TYPE = "keyProtectionType"
        private val JSON_KEY_MATCHER_PROTECTION_TYPE = "matcherProtectionType"
        private val JSON_KEY_EXTENSTIONS = "extensions"
        private val JSON_KEY_ATTESTATION = "attestation"
        private val JSON_KEY_PUB_KEY_CRED_PARAMS = "pubKeyCredParams"

        /**
         * This function converts a request json to a PublicKeyCredentialCreationOptions, where
         * there should be a direct mapping from the input string to this data type. See
         * [here](https://w3c.github.io/webauthn/#sctn-parseCreationOptionsFromJSON) for more
         * details. This occurs in the registration, or create, flow for public key credentials.
         *
         * @param request a credential manager data type that holds a requestJson that is expected
         * to parse completely into PublicKeyCredentialCreationOptions
         * @throws JSONException If required data is not present in the requestJson
         */
        @JvmStatic
        fun convert(request: CreatePublicKeyCredentialRequest): PublicKeyCredentialCreationOptions {
            val requestJson = request.requestJson
            val json = JSONObject(requestJson)
            val builder = PublicKeyCredentialCreationOptions.Builder()

            parseRequiredChallengeAndUser(json, builder)
            parseRequiredRpAndParams(json, builder)

            parseOptionalWithRequiredDefaultsAttestationAndExcludeCredentials(json, builder)

            parseOptionalTimeout(json, builder)
            parseOptionalAuthenticatorSelection(json, builder)
            parseOptionalExtensions(json, builder)

            return builder.build()
        }

        /**
         * Converts the response from fido back to json so it can be passed into CredentialManager.
         */
        fun toCreatePasskeyResponseJson(cred: PublicKeyCredential): String {
            val json = JSONObject()
            val authenticatorResponse = cred.response
            // TODO(b/262924507) : Look for FIDO changes in conditional mediation available
            if (authenticatorResponse is AuthenticatorAttestationResponse) {
                val responseJson = JSONObject()
                responseJson.put(
                    JSON_KEY_CLIENT_DATA,
                    b64Encode(authenticatorResponse.clientDataJSON))
                responseJson.put(
                    JSON_KEY_ATTESTATION_OBJ,
                    b64Encode(authenticatorResponse.attestationObject))
                val transportArray = convertToProperNamingScheme(authenticatorResponse)
                val transports = JSONArray(transportArray)

                responseJson.put(JSON_KEY_TRANSPORTS, transports)
                json.put(JSON_KEY_RESPONSE, responseJson)
            } else {
                Log.e(TAG, "Authenticator response expected registration response but " +
                    "got: ${authenticatorResponse.javaClass.name}")
            }

            addOptionalAuthenticatorAttachmentAndExtensions(cred, json)

            json.put(JSON_KEY_ID, cred.id)
            json.put(JSON_KEY_RAW_ID, b64Encode(cred.rawId))
            json.put(JSON_KEY_TYPE, cred.type)
            return json.toString()
        }

        private fun convertToProperNamingScheme(
            authenticatorResponse: AuthenticatorAttestationResponse
        ): Array<out String> {
            val transportArray = authenticatorResponse.transports
            var ix = 0
            for (transport in transportArray) {
                if (transport == "cable") {
                    transportArray[ix] = "hybrid"
                }
                ix += 1
            }
            return transportArray
        }

        private fun addOptionalAuthenticatorAttachmentAndExtensions(
            cred: PublicKeyCredential,
            json: JSONObject
        ) {
            val authenticatorAttachment = cred.authenticatorAttachment
            val clientExtensionResults = cred.clientExtensionResults

            if (authenticatorAttachment != null) {
                json.put(JSON_KEY_AUTH_ATTACHMENT, authenticatorAttachment)
            }

            if (clientExtensionResults != null) {
                try {
                    val uvmEntries = clientExtensionResults.uvmEntries
                    val uvmEntriesList = uvmEntries?.uvmEntryList
                    if (uvmEntriesList != null) {
                        val uvmEntriesJSON = JSONArray()
                        for (entry in uvmEntriesList) {
                            val uvmEntryJSON = JSONObject()
                            uvmEntryJSON.put(JSON_KEY_USER_VERIFICATION_METHOD,
                                entry.userVerificationMethod)
                            uvmEntryJSON.put(JSON_KEY_KEY_PROTECTION_TYPE, entry.keyProtectionType)
                            uvmEntryJSON.put(
                                JSON_KEY_MATCHER_PROTECTION_TYPE, entry.matcherProtectionType)
                            uvmEntriesJSON.put(uvmEntryJSON)
                        }
                        json.put("uvm", uvmEntriesJSON)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "ClientExtensionResults faced possible implementation " +
                        "inconsistency in uvmEntries - $t")
                }
            }
        }

        fun toAssertPasskeyResponse(cred: SignInCredential): String {
            val json = JSONObject()
            val publicKeyCred = cred.publicKeyCredential

            when (val authenticatorResponse = publicKeyCred?.response!!) {
                is AuthenticatorErrorResponse -> {
                    throw beginSignInPublicKeyCredentialResponseContainsError(
                        authenticatorResponse)
                }
                is AuthenticatorAssertionResponse -> {
                    beginSignInAssertionResponse(authenticatorResponse, json, publicKeyCred)
                }
                else -> {
                Log.e(
                    TAG,
                    "AuthenticatorResponse expected assertion response but " +
                        "got: ${authenticatorResponse.javaClass.name}")
                }
            }
            return json.toString()
        }

        private fun beginSignInAssertionResponse(
            authenticatorResponse: AuthenticatorAssertionResponse,
            json: JSONObject,
            publicKeyCred: PublicKeyCredential
        ) {
            val responseJson = JSONObject()
            responseJson.put(
                JSON_KEY_CLIENT_DATA,
                b64Encode(authenticatorResponse.clientDataJSON)
            )
            responseJson.put(
                JSON_KEY_AUTH_DATA,
                b64Encode(authenticatorResponse.authenticatorData)
            )
            responseJson.put(
                JSON_KEY_SIGNATURE,
                b64Encode(authenticatorResponse.signature)
            )
            authenticatorResponse.userHandle?.let {
                responseJson.put(
                    JSON_KEY_USER_HANDLE, b64Encode(authenticatorResponse.userHandle!!)
                )
            }
            // TODO(b/262924507) : attestation object missing in fido impl
            json.put(JSON_KEY_RESPONSE, responseJson)
            json.put(JSON_KEY_ID, publicKeyCred.id)
            json.put(JSON_KEY_RAW_ID, b64Encode(publicKeyCred.rawId))
            json.put(JSON_KEY_TYPE, publicKeyCred.type)
        }

        /**
         * Converts from the Credential Manager public key credential option to the Play Auth
         * Module passkey json option.
         *
         * @return the current auth module passkey request
         */
        fun convertToPlayAuthPasskeyJsonRequest(option: GetPublicKeyCredentialOption):
            BeginSignInRequest.PasskeyJsonRequestOptions {
            return BeginSignInRequest.PasskeyJsonRequestOptions.Builder()
                .setSupported(true)
                .setRequestJson(option.requestJson)
                .build()
        }

        /**
         * Converts from the Credential Manager public key credential option to the Play Auth
         * Module passkey option, used in a backwards compatible flow for the auth dependency.
         *
         * @return the backwards compatible auth module passkey request
         */
        @Deprecated("Upgrade GMS version so 'convertToPlayAuthPasskeyJsoNRequest' is used")
        @Suppress("deprecation")
        fun convertToPlayAuthPasskeyRequest(option: GetPublicKeyCredentialOption):
            BeginSignInRequest.PasskeysRequestOptions {
            val json = JSONObject(option.requestJson)
            val rpId = json.optString(JSON_KEY_RPID, "")
            if (rpId.isEmpty()) {
                throw JSONException("GetPublicKeyCredentialOption - rpId not specified in the " +
                    "request or is unexpectedly empty")
            }
            val challenge = getChallenge(json)
            return BeginSignInRequest.PasskeysRequestOptions.Builder()
                .setSupported(true)
                .setRpId(rpId)
                .setChallenge(challenge)
                .build()
        }

        private fun getChallenge(json: JSONObject): ByteArray {
            val challengeB64 = json.optString(JSON_KEY_CHALLENGE, "")
            if (challengeB64.isEmpty()) {
                throw JSONException("Challenge not found in request or is unexpectedly empty")
            }
            return b64Decode(challengeB64)
        }

        /**
         * Indicates if an error was propagated from the underlying Fido API.
         *
         * @param cred the public key credential response object from fido
         *
         * @return an exception if it exists, else null indicating no exception
         */
        fun publicKeyCredentialResponseContainsError(
            cred: PublicKeyCredential
        ): CreateCredentialException? {
            val authenticatorResponse: AuthenticatorResponse = cred.response
            if (authenticatorResponse is AuthenticatorErrorResponse) {
                val code = authenticatorResponse.errorCode
                var exceptionError = orderedErrorCodeToExceptions[code]
                var msg = authenticatorResponse.errorMessage
                val exception: CreateCredentialException
                if (exceptionError == null) {
                    exception = CreatePublicKeyCredentialDomException(
                        UnknownError(), "unknown fido gms exception - $msg"
                    )
                } else {
                    // This fix is quite fragile because it relies on that the fido module
                    // does not change its error message, but is the only viable solution
                    // because there's no other differentiator.
                    if (code == ErrorCode.CONSTRAINT_ERR &&
                        msg?.contains("Unable to get sync account") == true
                    ) {
                        exception = CreateCredentialCancellationException(
                            "Passkey registration was cancelled by the user.")
                    } else {
                        exception = CreatePublicKeyCredentialDomException(exceptionError, msg)
                    }
                }
                return exception
            }
            return null
        }

        // Helper method for the begin sign in flow to identify an authenticator error response
        private fun beginSignInPublicKeyCredentialResponseContainsError(
            authenticatorResponse: AuthenticatorErrorResponse
        ): GetCredentialException {
            val code = authenticatorResponse.errorCode
            var exceptionError = orderedErrorCodeToExceptions[code]
            var msg = authenticatorResponse.errorMessage
            val exception: GetCredentialException
            if (exceptionError == null) {
                exception = GetPublicKeyCredentialDomException(
                    UnknownError(), "unknown fido gms exception - $msg"
                )
            } else {
                // This fix is quite fragile because it relies on that the fido module
                // does not change its error message, but is the only viable solution
                // because there's no other differentiator.
                if (code == ErrorCode.CONSTRAINT_ERR &&
                    msg?.contains("Unable to get sync account") == true
                ) {
                    exception = GetCredentialCancellationException(
                        "Passkey retrieval was cancelled by the user.")
                } else {
                    exception = GetPublicKeyCredentialDomException(exceptionError, msg)
                }
            }
            return exception
        }

        internal fun parseOptionalExtensions(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            if (json.has(JSON_KEY_EXTENSTIONS)) {
                val extensions = json.getJSONObject(JSON_KEY_EXTENSTIONS)
                val extensionBuilder = AuthenticationExtensions.Builder()
                val appIdExtension = extensions.optString(JSON_KEY_APPID, "")
                if (appIdExtension.isNotEmpty()) {
                    extensionBuilder.setFido2Extension(FidoAppIdExtension(appIdExtension))
                }
                val thirdPartyPaymentExtension = extensions.optBoolean(
                    JSON_KEY_THIRD_PARTY_PAYMENT, false)
                if (thirdPartyPaymentExtension) {
                    extensionBuilder.setGoogleThirdPartyPaymentExtension(
                        GoogleThirdPartyPaymentExtension(true)
                    )
                }
                val uvmStatus = extensions.optBoolean("uvm", false)
                if (uvmStatus) {
                    extensionBuilder.setUserVerificationMethodExtension(
                        UserVerificationMethodExtension(true)
                    )
                }
                // TODO("Ensure JSON keys are correctly named")
                builder.setAuthenticationExtensions(extensionBuilder.build())
            }
        }

        internal fun parseOptionalAuthenticatorSelection(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            if (json.has(JSON_KEY_AUTH_SELECTION)) {
                val authenticatorSelection = json.getJSONObject(
                    JSON_KEY_AUTH_SELECTION
                )
                val authSelectionBuilder = AuthenticatorSelectionCriteria.Builder()
                val requireResidentKey = authenticatorSelection.optBoolean(
                    JSON_KEY_REQUIRE_RES_KEY, false)
                val residentKey = authenticatorSelection
                    .optString(JSON_KEY_RES_KEY, "")
                var residentKeyRequirement: ResidentKeyRequirement? = null
                if (residentKey.isNotEmpty()) {
                    residentKeyRequirement = ResidentKeyRequirement.fromString(residentKey)
                }
                authSelectionBuilder
                    .setRequireResidentKey(requireResidentKey)
                    .setResidentKeyRequirement(residentKeyRequirement)
                val authenticatorAttachmentString = authenticatorSelection
                    .optString(JSON_KEY_AUTH_ATTACHMENT, "")
                if (authenticatorAttachmentString.isNotEmpty()) {
                    authSelectionBuilder.setAttachment(
                        Attachment.fromString(
                            authenticatorAttachmentString
                        )
                    )
                }
                // TODO(b/262924507) : Fido implementation lacks userVerification in current impl
                builder.setAuthenticatorSelection(
                    authSelectionBuilder.build()
                )
            }
        }

        internal fun parseOptionalTimeout(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            if (json.has(JSON_KEY_TIMEOUT)) {
                val timeout = json.getLong(JSON_KEY_TIMEOUT).toDouble() / 1000
                builder.setTimeoutSeconds(timeout)
            }
        }

        internal fun parseOptionalWithRequiredDefaultsAttestationAndExcludeCredentials(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            val excludeCredentialsList: MutableList<PublicKeyCredentialDescriptor> = ArrayList()
            if (json.has(JSON_KEY_EXCLUDE_CREDENTIALS)) {
                val pubKeyDescriptorJSONs = json.getJSONArray(JSON_KEY_EXCLUDE_CREDENTIALS)
                for (i in 0 until pubKeyDescriptorJSONs.length()) {
                    val descriptorJSON = pubKeyDescriptorJSONs.getJSONObject(i)
                    val descriptorId = b64Decode(descriptorJSON.getString(JSON_KEY_ID))
                    val descriptorType = descriptorJSON.getString(JSON_KEY_TYPE)
                    if (descriptorId.isEmpty() || descriptorType.isEmpty()) {
                        throw JSONException("PublicKeyCredentialDescriptor id or type value not " +
                            "found or unexpectedly empty")
                    }
                    var transports: MutableList<Transport>? = null
                    if (descriptorJSON.has(JSON_KEY_TRANSPORTS)) {
                        transports = ArrayList()
                        val descriptorTransports = descriptorJSON.getJSONArray(
                            JSON_KEY_TRANSPORTS
                        )
                        for (j in 0 until descriptorTransports.length()) {
                            try {
                                transports.add(Transport.fromString(
                                    descriptorTransports.getString(j)))
                            } catch (e: Transport.UnsupportedTransportException) {
                                throw CreatePublicKeyCredentialDomException(EncodingError(),
                                    e.message)
                            }
                        }
                    }
                    excludeCredentialsList.add(
                        PublicKeyCredentialDescriptor(
                            descriptorType,
                            descriptorId, transports
                        )
                    ) // TODO(b/262924507) : Ensure spec changes (i.e. int algorithm) in current
                    // fido impl stays that way - edit if fido modifies
                }
            }
            builder.setExcludeList(excludeCredentialsList)

            var attestationString = json.optString(JSON_KEY_ATTESTATION, "none")
            if (attestationString.isEmpty()) {
                attestationString = "none"
            }
            builder.setAttestationConveyancePreference(
                AttestationConveyancePreference.fromString(attestationString)
            )
        }

        internal fun parseRequiredRpAndParams(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            val rp = json.getJSONObject(JSON_KEY_RP)
            val rpId = rp.getString(JSON_KEY_ID)
            val rpName = rp.optString(JSON_KEY_NAME, "")
            // TODO(b/262924507) : Fido and spec differ; always keep re-checking if aligns
            var rpIcon: String? = rp.optString(JSON_KEY_ICON, "")
            if (rpIcon!!.isEmpty()) {
                rpIcon = null
            }
            if (rpName.isEmpty() || rpId.isEmpty()) {
                throw JSONException("PublicKeyCredentialCreationOptions rp ID or rp name are " +
                    "missing or unexpectedly empty")
            }
            builder.setRp(
                PublicKeyCredentialRpEntity(
                    rpId,
                    rpName,
                    rpIcon
                )
            )

            val pubKeyCredParams = json.getJSONArray(JSON_KEY_PUB_KEY_CRED_PARAMS)
            val paramsList: MutableList<PublicKeyCredentialParameters> = ArrayList()
            for (i in 0 until pubKeyCredParams.length()) {
                val param = pubKeyCredParams.getJSONObject(i)
                val paramAlg = param.getLong(JSON_KEY_ALG).toInt()
                val typeParam = param.optString(JSON_KEY_TYPE, "")
                if (typeParam.isEmpty()) {
                    throw JSONException("PublicKeyCredentialCreationOptions " +
                        "PublicKeyCredentialParameter type missing or unexpectedly empty")
                }
                if (checkAlgSupported(paramAlg)) {
                    paramsList.add(
                        PublicKeyCredentialParameters(typeParam, paramAlg))
                }
            }
            builder.setParameters(paramsList)
        }

        internal fun parseRequiredChallengeAndUser(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            val challenge = getChallenge(json)
            builder.setChallenge(challenge)

            val user = json.getJSONObject(JSON_KEY_USER)
            val userId = b64Decode(user.getString(JSON_KEY_ID))
            val userName = user.getString(JSON_KEY_NAME)
            val displayName = user.getString(JSON_KEY_DISPLAY_NAME)
            val userIcon = user.optString(JSON_KEY_ICON, "")
            if (displayName.isEmpty() || userId.isEmpty() || userName.isEmpty()) {
                throw JSONException("PublicKeyCredentialCreationOptions UserEntity missing one " +
                    "or more of displayName, userId or userName, or they are unexpectedly empty")
            }
            builder.setUser(
                PublicKeyCredentialUserEntity(
                    userId,
                    userName,
                    userIcon,
                    displayName
                )
            )
        }

        /**
         * Decode specific to public key credential encoded strings, or any string
         * that requires NO_PADDING, NO_WRAP and URL_SAFE flags for base 64 decoding.
         *
         * @param str the string the decode into a bytearray
         */
        fun b64Decode(str: String): ByteArray {
            return Base64.decode(str, FLAGS)
        }

        /**
         * Encode specific to public key credential decoded strings, or any string
         * that requires NO_PADDING, NO_WRAP and URL_SAFE flags for base 64 encoding.
         *
         * @param data the bytearray to encode into a string
         */
        fun b64Encode(data: ByteArray): String {
            return Base64.encodeToString(data, FLAGS)
        }

        /**
         * Some values are not supported in the webauthn spec - this catches those values
         * and returns false - otherwise it returns true.
         *
         * @param alg the int code of the cryptography algorithm used in the webauthn flow
         */
        fun checkAlgSupported(alg: Int): Boolean {
            try {
                COSEAlgorithmIdentifier.fromCoseValue(alg)
                return true
            } catch (_: Throwable) {
            }
            return false
        }

        private const val FLAGS = Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        private const val TAG = "PublicKeyUtility"
        internal val orderedErrorCodeToExceptions = linkedMapOf(ErrorCode.UNKNOWN_ERR to
            UnknownError(),
            ErrorCode.ABORT_ERR to AbortError(),
            ErrorCode.ATTESTATION_NOT_PRIVATE_ERR to NotReadableError(),
            ErrorCode.CONSTRAINT_ERR to ConstraintError(),
            ErrorCode.DATA_ERR to DataError(),
            ErrorCode.INVALID_STATE_ERR to InvalidStateError(),
            ErrorCode.ENCODING_ERR to EncodingError(),
            ErrorCode.NETWORK_ERR to NetworkError(),
            ErrorCode.NOT_ALLOWED_ERR to NotAllowedError(),
            ErrorCode.NOT_SUPPORTED_ERR to NotSupportedError(),
            ErrorCode.SECURITY_ERR to SecurityError(),
            ErrorCode.TIMEOUT_ERR to TimeoutError()
        )
    }
}

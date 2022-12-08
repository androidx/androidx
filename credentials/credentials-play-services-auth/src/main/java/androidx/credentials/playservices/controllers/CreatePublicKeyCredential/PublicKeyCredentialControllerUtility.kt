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
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialAbortException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialConstraintException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDataException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialInvalidStateException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialNetworkException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialNotAllowedException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialNotReadableException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialNotSupportedException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialSecurityException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialTimeoutException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialUnknownException
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensions
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.COSEAlgorithmIdentifier
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import java.util.concurrent.Executor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A utility class to handle logic for the begin sign in controller.
 *
 * @hide
 */
class PublicKeyCredentialControllerUtility {

    companion object {

        // TODO("Make string constants for keys to the json")

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

        fun toCreatePasskeyResponseJson(cred: PublicKeyCredential): String {
            val json = JSONObject()
            val authenticatorResponse = cred.response
            val authenticatorAttachment = cred.authenticatorAttachment
            val clientExtensionResults = cred.clientExtensionResults
            Log.i(TAG, "clientExtensionResults: ${
                clientExtensionResults?.uvmEntries?.uvmEntryList}")
            // TODO("Extension types have hidden values, update once gms fido updates")
            // TODO("Ask why it is missing conditional mediation available")
            if (authenticatorResponse is AuthenticatorAttestationResponse) {
                val responseJson = JSONObject()
                responseJson.put(
                    "clientDataJSON",
                    b64Encode(authenticatorResponse.clientDataJSON))
                responseJson.put(
                    "attestationObject",
                    b64Encode(authenticatorResponse.attestationObject))
                val transports = JSONArray(listOf(authenticatorResponse.transports))
                responseJson.put("transports", transports)
                json.put("response", responseJson)
            } else {
                Log.e(
                    TAG,
                    "Expected registration response but got: " +
                        authenticatorResponse.javaClass.name)
            }

            if (authenticatorAttachment != null) {
                json.put("authenticatorAttachment", authenticatorAttachment)
            }

            json.put("id", cred.id)
            json.put("rawId", b64Encode(cred.rawId))
            json.put("type", cred.type)
            // TODO: add ExtensionsClientOUtputsJSON conversion
            return json.toString()
        }

        fun toAssertPasskeyResponse(cred: SignInCredential): String {
            val json = JSONObject()
            val publicKeyCred = cred.publicKeyCredential
            val authenticatorResponse = publicKeyCred?.response!!
            Log.i(TAG, authenticatorResponse.clientDataJSON.toString())

            if (authenticatorResponse is AuthenticatorAssertionResponse) {
                val responseJson = JSONObject()
                responseJson.put(
                    "clientDataJSON",
                    Base64.encodeToString(authenticatorResponse.clientDataJSON, FLAGS))
                responseJson.put(
                    "authenticatorData",
                    Base64.encodeToString(authenticatorResponse.authenticatorData, FLAGS))
                responseJson.put(
                    "signature",
                    Base64.encodeToString(authenticatorResponse.signature, FLAGS))
                json.put("response", responseJson)
            } else {
                Log.e(
                    TAG,
                    "Expected assertion response but got: " + authenticatorResponse
                        .javaClass.name)
            }
            json.put("id", publicKeyCred.id)
            json.put("rawId", Base64.encodeToString(publicKeyCred.rawId, FLAGS))
            json.put("type", publicKeyCred.type)
            return json.toString()
        }

        @Suppress("DocumentExceptions")
        fun convertToPlayAuthPasskeyRequest(request: GetPublicKeyCredentialOption):
            BeginSignInRequest.PasskeysRequestOptions {
            // TODO : Make sure this is in compliance with w3
            Log.i(TAG, "Parsing to play auth (get request side)")
            val json = JSONObject(request.requestJson)
            if (json.has("rpId")) {
                val rpId = json.getString("rpId")
                if (json.has("challenge")) {
                    val challenge =
                        Base64.decode(json.getString("challenge"), FLAGS)
                    return BeginSignInRequest.PasskeysRequestOptions.Builder()
                        .setSupported(true)
                        .setRpId(rpId)
                        .setChallenge(challenge)
                        .build()
                } else {
                    Log.i(TAG, "Challenge not found in request for : $rpId")
                }
            } else {
                Log.i(TAG, "Rp Id not found in request")
            }
            throw UnsupportedOperationException("rpId not specified in the request")
        }

        /**
         * Indicates if an error was propagated from the underlying Fido API.
         *
         * @param callback the callback invoked when the request succeeds or fails
         * @param executor the callback will take place on this executor
         * @param cred the public key credential response object from fido
         *
         * @return true if there is an error, false otherwise
         */
        fun reportErrorIfExists(
            callback: CredentialManagerCallback<CreateCredentialResponse,
                CreateCredentialException>,
            executor: Executor,
            cred: PublicKeyCredential
        ): Boolean {
            val authenticatorResponse = cred.response
            if (authenticatorResponse is AuthenticatorErrorResponse) {
                val code = authenticatorResponse.errorCode
                var exception = orderedErrorCodeToExceptions[code]
                if (exception == null) {
                    exception = CreatePublicKeyCredentialUnknownException("unknown fido gms " +
                        "exception")
                }
                executor.execute { callback.onError(
                    exception
                ) }
                return true
            }
            return false
        }

        internal fun parseOptionalExtensions(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            if (json.has("extensions")) {
                builder.setAuthenticationExtensions(AuthenticationExtensions.Builder().build())
                // TODO("Parse this for required cases")
            }
        }

        internal fun parseOptionalAuthenticatorSelection(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            if (json.has("authenticatorSelection")) {
                val authenticatorSelection = json.getJSONObject(
                    "authenticatorSelection"
                )
                val authSelectionBuilder = AuthenticatorSelectionCriteria.Builder()
                val requireResidentKey = authenticatorSelection.optBoolean(
                    "requireResidentKey", false)
                val residentKey = authenticatorSelection
                    .optString("residentKey", "")
                var residentKeyRequirement: ResidentKeyRequirement? = null
                if (residentKey.isNotEmpty()) {
                    residentKeyRequirement = ResidentKeyRequirement.fromString(residentKey)
                }
                authSelectionBuilder
                    .setRequireResidentKey(requireResidentKey)
                    .setResidentKeyRequirement(residentKeyRequirement)
                val authenticatorAttachmentString = authenticatorSelection
                    .optString("authenticatorAttachment", "")
                if (authenticatorAttachmentString.isNotEmpty()) {
                    authSelectionBuilder.setAttachment(
                        Attachment.fromString(
                            authenticatorAttachmentString
                        )
                    )
                }
                // TODO("Note userVerification is not settable in current impl")
                builder.setAuthenticatorSelection(
                    authSelectionBuilder.build()
                )
            }
        }

        internal fun parseOptionalTimeout(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            if (json.has("timeout")) {
                val timeout = json.getLong("timeout").toDouble() / 1000
                builder.setTimeoutSeconds(timeout)
            }
        }

        internal fun parseOptionalWithRequiredDefaultsAttestationAndExcludeCredentials(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            val excludeCredentialsList: MutableList<PublicKeyCredentialDescriptor> = ArrayList()
            if (json.has("excludeCredentials")) {
                val pubKeyDescriptorJSONs = json.getJSONArray("excludeCredentials")
                for (i in 0 until pubKeyDescriptorJSONs.length()) {
                    val descriptorJSON = pubKeyDescriptorJSONs.getJSONObject(i)
                    val descriptorId = b64Decode(descriptorJSON.getString("id"))
                    var transports: MutableList<Transport>? = null
                    if (descriptorJSON.has("transports")) {
                        transports = ArrayList()
                        val descriptorTransports = descriptorJSON.getJSONArray(
                            "transports"
                        )
                        for (j in 0 until descriptorTransports.length()) {
                            transports.add(Transport.fromString(descriptorTransports.getString(j)))
                        }
                    }
                    excludeCredentialsList.add(
                        PublicKeyCredentialDescriptor(
                            descriptorJSON.getString("type"),
                            descriptorId, transports
                        )
                    ) // TODO("Confirm allowed mismatch with the spec such as the int algorithm")
                }
            }
            builder.setExcludeList(excludeCredentialsList)

            var attestationString = "none"
            if (json.has("attestation")) {
                attestationString = json.getString("attestation")
            }
            builder.setAttestationConveyancePreference(
                AttestationConveyancePreference.fromString(attestationString)
            )
        }

        internal fun parseRequiredRpAndParams(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            val rp = json.getJSONObject("rp")
            val rpId = rp.getString("id")
            val rpName = rp.optString("name", "")
            // TODO("Decided things not in the spec but in fido impl are used")
            // TODO("Come back to this if that is ever updated")
            var rpIcon: String? = rp.optString("icon", "")
            if (rpIcon!!.isEmpty()) {
                rpIcon = null
            }
            builder.setRp(
                PublicKeyCredentialRpEntity(
                    rpId,
                    rpName,
                    rpIcon
                )
            )

            val pubKeyCredParams = json.getJSONArray("pubKeyCredParams")
            val paramsList: MutableList<PublicKeyCredentialParameters> = ArrayList()
            for (i in 0 until pubKeyCredParams.length()) {
                val param = pubKeyCredParams.getJSONObject(i)
                val paramAlg = param.getLong("alg").toInt()
                if (checkAlgSupported(paramAlg)) {
                    paramsList.add(
                        PublicKeyCredentialParameters(param.getString("type"), paramAlg))
                }
            }
            builder.setParameters(paramsList)
        }

        internal fun parseRequiredChallengeAndUser(
            json: JSONObject,
            builder: PublicKeyCredentialCreationOptions.Builder
        ) {
            val challenge = b64Decode(json.getString("challenge"))
            builder.setChallenge(challenge)

            val user = json.getJSONObject("user")
            val userId = b64Decode(user.getString("id"))
            val userName = user.getString("name")
            val displayName = user.getString("displayName")
            val userIcon = user.optString("icon", "")
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
        private val TAG = PublicKeyCredentialControllerUtility::class.java.name
        internal val orderedErrorCodeToExceptions = linkedMapOf(ErrorCode.UNKNOWN_ERR to
        CreatePublicKeyCredentialUnknownException("returned unknown transient failure"),
        ErrorCode.ABORT_ERR to CreatePublicKeyCredentialAbortException("indicates the " +
            "operation was aborted"),
        ErrorCode.CONSTRAINT_ERR to CreatePublicKeyCredentialConstraintException(
            "indicates a constraint was not satisfied due to some mutation operation"),
        ErrorCode.ATTESTATION_NOT_PRIVATE_ERR to
            CreatePublicKeyCredentialNotReadableException("indicates the " +
                "authenticator violates privacy requirements"),
            ErrorCode.CONSTRAINT_ERR to CreatePublicKeyCredentialConstraintException(
                "indicates a mutation operation failed due to unsatisfied constraint"),
            ErrorCode.DATA_ERR to CreatePublicKeyCredentialDataException("indicates " +
                "data is inadequate"),
            ErrorCode.ENCODING_ERR to CreatePublicKeyCredentialInvalidStateException(
                "indicates object is in an invalid state"),
            ErrorCode.NETWORK_ERR to CreatePublicKeyCredentialNetworkException(
                "indicates a network error occurred"),
            ErrorCode.NOT_ALLOWED_ERR to CreatePublicKeyCredentialNotAllowedException(
                "indicates the request is not allowed in the current context - usually user " +
                "denied permission."),
            ErrorCode.NOT_SUPPORTED_ERR to CreatePublicKeyCredentialNotSupportedException(
                "indicates the operation is not supported"),
            ErrorCode.SECURITY_ERR to CreatePublicKeyCredentialSecurityException(
                "indicates the operation is insecure"),
            ErrorCode.TIMEOUT_ERR to CreatePublicKeyCredentialTimeoutException(
                "indicates the operation timed out")
        )
    }
}
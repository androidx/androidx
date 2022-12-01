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
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import java.util.concurrent.Executor
import org.json.JSONArray
import org.json.JSONObject

/**
 * A utility class to handle logic for the begin sign in controller.
 *
 * @hide
 */
class PublicKeyCredentialControllerUtility {

    companion object {
        @JvmStatic
        fun convert(request: CreatePublicKeyCredentialRequest): PublicKeyCredentialCreationOptions {
            val requestJson = request.requestJson
            val json = JSONObject(requestJson)
            val builder: PublicKeyCredentialCreationOptions.Builder =
                PublicKeyCredentialCreationOptions.Builder()

            if (json.has("challenge")) {
                Log.d(TAG, "Set challenge")
                val challenge: ByteArray =
                    Base64.decode(json.getString("challenge"), Base64.URL_SAFE)
                builder.setChallenge(challenge)
            }

            if (json.has("user")) {
                Log.d(TAG, "Set user")
                val user: JSONObject = json.getJSONObject("user")
                val id: String = user.getString("id")
                val name: String = user.getString("name")
                val displayName: String = user.getString("displayName")
                builder.setUser(
                    PublicKeyCredentialUserEntity(
                        id.toByteArray(),
                        name,
                        "",
                        displayName
                    )
                )
            }

            if (json.has("rp")) {
                Log.d(TAG, "Set rp")
                val rp: JSONObject = json.getJSONObject("rp")
                val id: String = rp.getString("id")
                val name: String = rp.getString("name")
                builder.setRp(
                    PublicKeyCredentialRpEntity(
                        id,
                        name,
                        null
                    )
                )
            }

            if (json.has("timeout")) {
                Log.d(TAG, "Set timeout")
                val timeout: Double = json.getDouble("timeout") / 1000
                builder.setTimeoutSeconds(timeout)
            }

            if (json.has("pubKeyCredParams")) {
                Log.d(TAG, "Set pubKeyCredParams")
                val pubKeyCredParams: JSONArray = json.getJSONArray("pubKeyCredParams")
                val paramsList: MutableList<PublicKeyCredentialParameters> = ArrayList()
                for (i in 0 until pubKeyCredParams.length()) {
                    val param = pubKeyCredParams.getJSONObject(i)
                    paramsList.add(
                        PublicKeyCredentialParameters(
                            param.getString("type"), param.getInt("alg"))
                    )
                }
                builder.setParameters(paramsList)
            }

            if (json.has("authenticatorSelection")) {
                Log.d("PasskeyRequestConverter", "Set authenticatorSelection")
                val authenticatorSelection: JSONObject = json.getJSONObject(
                    "authenticatorSelection")
                val requireResidentKey = authenticatorSelection.getBoolean(
                    "requireResidentKey")
                val residentKey = authenticatorSelection.getString("residentKey")
                builder.setAuthenticatorSelection(
                    AuthenticatorSelectionCriteria.Builder()
                        .setRequireResidentKey(requireResidentKey)
                        .setResidentKeyRequirement(
                            ResidentKeyRequirement.fromString(residentKey))
                        .build()
                )
            }

            builder.setExcludeList(ArrayList())

            return builder.build()
        }

        fun toCreatePasskeyResponseJson(cred: PublicKeyCredential): String {
            val json = JSONObject()

            val authenticatorResponse: AuthenticatorResponse = cred.response
            if (authenticatorResponse is AuthenticatorAttestationResponse) {
                val responseJson = JSONObject()
                responseJson.put(
                    "clientDataJSON",
                    Base64.encodeToString(authenticatorResponse.clientDataJSON, Base64.NO_WRAP))
                responseJson.put(
                    "attestationObject",
                    Base64.encodeToString(authenticatorResponse.attestationObject, Base64.NO_WRAP))
                val transports = JSONArray(listOf(authenticatorResponse.transports))
                responseJson.put("transports", transports)
                json.put("response", responseJson)
            } else {
                Log.e(
                    TAG,
                    "Expected registration response but got: " +
                        authenticatorResponse.javaClass.name)
            }

            if (cred.authenticatorAttachment != null) {
                json.put("authenticatorAttachment", String(cred.rawId))
            }

            json.put("id", cred.id)
            json.put("rawId", Base64.encodeToString(cred.rawId, Base64.NO_WRAP))
            json.put("type", cred.type)
            // TODO: add ExtensionsClientOUtputsJSON conversion
            return json.toString()
        }

        fun toAssertPasskeyResponse(cred: SignInCredential): String {
            val json = JSONObject()
            val publicKeyCred = cred.publicKeyCredential
            val authenticatorResponse: AuthenticatorResponse = publicKeyCred?.response!!

            if (authenticatorResponse is AuthenticatorAssertionResponse) {
                val responseJson = JSONObject()
                responseJson.put(
                    "clientDataJSON",
                    Base64.encodeToString(authenticatorResponse.clientDataJSON, Base64.NO_WRAP))
                responseJson.put(
                    "assertionObject",
                    Base64.encodeToString(authenticatorResponse.authenticatorData, Base64.NO_WRAP))
                responseJson.put(
                    "signature",
                    Base64.encodeToString(authenticatorResponse.signature, Base64.NO_WRAP))
                json.put("response", responseJson)
            } else {
                Log.e(
                    TAG,
                    "Expected assertion response but got: " + authenticatorResponse.javaClass.name)
            }
            json.put("id", publicKeyCred.id)
            json.put("rawId", Base64.encodeToString(publicKeyCred.rawId, Base64.NO_WRAP))
            json.put("type", publicKeyCred.type)
            return json.toString()
        }

        @Suppress("DocumentExceptions")
        fun convertToPlayAuthPasskeyRequest(request: GetPublicKeyCredentialOption):
            BeginSignInRequest.PasskeysRequestOptions {
            // TODO : Make sure this is in compliance with w3
            val json = JSONObject(request.requestJson)
            if (json.has("rpId")) {
                val rpId: String = json.getString("rpId")
                Log.i(TAG, "Rp Id : $rpId")
                if (json.has("challenge")) {
                    val challenge: ByteArray =
                        Base64.decode(json.getString("challenge"), Base64.URL_SAFE)
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
        fun publicKeyCredentialResponseContainsError(
            callback: CredentialManagerCallback<CreateCredentialResponse,
                CreateCredentialException>,
            executor: Executor,
            cred: PublicKeyCredential
        ): Boolean {
            val authenticatorResponse: AuthenticatorResponse = cred.response
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
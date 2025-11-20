/*
 * Copyright 2025 The Android Open Source Project
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

import android.Manifest.permission.CREDENTIAL_MANAGER_SET_ORIGIN
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.credentials.exceptions.publickeycredential.SignalCredentialSecurityException
import androidx.credentials.internal.isValidBase64Url
import org.json.JSONObject

/**
 * A request to signal the complete list of public key credentials ids for a given user.
 *
 * @param requestJson the request in JSON format. The format of the JSON should follow the
 *   [WebAuthn Spec](https://w3c.github.io/webauthn/#sctn-signalAllAcceptedCredentials). Throws
 *   IllegalArgumentException if the json does not have the required keys according to the spec, or
 *   if base64url decoding fails for the user id or credential id.
 * @param origin the origin of a different application if the request is being made on behalf of
 *   that application, to be used only by browsers or privileged apps recognized by the target
 *   credential provider (Note: if a non-browser/non-privileged app sets an origin, it will be
 *   rejected across all API levels, and for API level >=34, the calling party must also have the
 *   android.permission.CREDENTIAL_MANAGER_SET_ORIGIN permission otherwise a SecurityException will
 *   be thrown)
 *     @throws IllegalArgumentException if request json validation fails
 *     @throws SignalCredentialSecurityException if origin is set without having
 *       android.permission.CREDENTIAl_MANAGER_SET_ORIGIN
 */
class SignalAllAcceptedCredentialIdsRequest
internal constructor(requestJson: String, requestData: Bundle, origin: String? = null) :
    SignalCredentialStateRequest(
        SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE,
        requestJson,
        requestData,
        origin,
    ) {

    init {
        require(isValidRequestJson(requestJson)) {
            "Structural/type validation failed for JSON: '${requestJson}'"
        }
    }

    /**
     * Constructs a request to signal the complete list of public key credentials ids for a given
     * user.
     *
     * @param requestJson the request in JSON format. The format of the JSON should follow the
     *   [WebAuthn Spec](https://w3c.github.io/webauthn/#sctn-signalAllAcceptedCredentials). Throws
     *   IllegalArgumentException if the json does not have the required keys according to the spec,
     *   or if base64url decoding fails for the user id or credential id.
     */
    constructor(requestJson: String) : this(requestJson, null)

    /**
     * Constructs a request to signal the complete list of public key credentials ids for a given
     * user.
     *
     * @param requestJson the request in JSON format. The format of the JSON should follow the
     *   [WebAuthn Spec](https://w3c.github.io/webauthn/#sctn-signalAllAcceptedCredentials). Throws
     *   IllegalArgumentException if the json does not have the required keys according to the spec,
     *   or if base64url decoding fails for the user id or credential id.
     * @param origin the origin of a different application if the request is being made on behalf of
     *   that application, to be used only by browsers or privileged apps recognized by the target
     *   credential provider (Note: if a non-browser/non-privileged app sets an origin, it will be
     *   rejected across all API levels, and for API level >=34, the calling party must also have
     *   the android.permission.CREDENTIAL_MANAGER_SET_ORIGIN permission otherwise a
     *   SecurityException will be thrown)
     * @throws IllegalArgumentException if request json validation fails
     * @throws SignalCredentialSecurityException if origin is set without having
     *   android.permission.CREDENTIAl_MANAGER_SET_ORIGIN
     */
    @RequiresPermission(CREDENTIAL_MANAGER_SET_ORIGIN, conditional = true)
    constructor(
        requestJson: String,
        origin: String?,
    ) : this(requestJson, toRequestData(requestJson), origin)

    internal companion object {
        internal const val SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE"

        private const val TAG = "SignalAcceptedIdsReq"
        private const val RP_ID_KEY = "rpId"
        private const val USER_ID_KEY = "userId"
        private const val ACCEPTED_CREDENTIAL_IDS_KEY = "allAcceptedCredentialIds"

        private val REQUIRED_KEYS =
            listOf<String>(RP_ID_KEY, USER_ID_KEY, ACCEPTED_CREDENTIAL_IDS_KEY)

        fun toRequestData(requestJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(SIGNAL_REQUEST_JSON_KEY, requestJson)
            return bundle
        }

        /** Utility function to verify if the request Json is valid. */
        fun isValidRequestJson(requestJson: String): Boolean {
            try {
                val jsonObject = JSONObject(requestJson)
                for (key in REQUIRED_KEYS) {
                    if (!jsonObject.has(key)) {
                        Log.e(TAG, "Request json is missing required key $key")
                        return false
                    }
                }

                if (!isValidBase64Url(jsonObject.getString(USER_ID_KEY))) {
                    return false
                }
                val jsonArray = jsonObject.getJSONArray(ACCEPTED_CREDENTIAL_IDS_KEY)
                for (i in 0 until jsonArray.length()) {
                    val element = jsonArray.get(i)
                    if (element is String) {
                        if (!isValidBase64Url(element)) {
                            return false
                        } else {
                            continue
                        }
                    } else {
                        return false
                    }
                }
            } catch (e: Exception) {
                return false
            }
            return true
        }
    }
}

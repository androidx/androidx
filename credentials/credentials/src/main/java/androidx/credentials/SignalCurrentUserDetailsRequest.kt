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

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.credentials.internal.isValidBase64Url
import org.json.JSONObject

/**
 * A request to signal the user's current name and display name.
 *
 * @param requestJson the request in JSON format. The format of the JSON should follow the
 *   [WebAuthn Spec](https://w3c.github.io/webauthn/#sctn-signalCurrentUserDetails). Throws
 *   SignalCredentialStateException if base64Url decoding fails for the user id.
 * @param origin the origin of a different application if the request is being made on behalf of
 *   that application (Note: for API level >=34, setting a non-null value for this parameter will
 *   throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SignalCurrentUserDetailsRequest(requestJson: String, origin: String? = null) :
    SignalCredentialStateRequest(
        SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE,
        requestJson,
        origin,
    ) {
    init {
        require(isValidRequestJson(requestJson)) {
            "Structural/type validation failed for JSON: '${requestJson}'"
        }
    }

    internal companion object {
        internal const val SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE"
        private const val TAG = "SignalUserDetailsReq"
        private const val RP_ID_KEY = "rpId"
        private const val USER_ID_KEY = "userId"
        private const val NAME_KEY = "name"
        private const val DISPLAY_NAME_KEY = "displayName"
        private val REQUIRED_KEYS =
            listOf<String>(RP_ID_KEY, USER_ID_KEY, NAME_KEY, DISPLAY_NAME_KEY)

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
                    Log.e(TAG, "User Id is not in base64 url format")
                    return false
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Structural/type validation failed for JSON: '${requestJson}'. Error: ${e.message}",
                )
            }
            return true
        }
    }
}

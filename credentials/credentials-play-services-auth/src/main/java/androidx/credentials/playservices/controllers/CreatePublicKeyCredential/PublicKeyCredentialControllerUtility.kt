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
import androidx.credentials.GetPublicKeyCredentialOption
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import org.json.JSONObject

/**
 * A utility class to handle logic for the create public key credential controller.
 *
 * @hide
 */
class PublicKeyCredentialControllerUtility {

    companion object {
        @JvmStatic
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

        private val TAG = PublicKeyCredentialControllerUtility::class.java.name
    }
}
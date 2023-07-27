/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.webauthn

import android.util.Log
import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PublicKeyCredentialCreationOptions(requestJson: String) {
  val json: JSONObject

  val rp: PublicKeyCredentialRpEntity
  val user: PublicKeyCredentialUserEntity
  val challenge: ByteArray
  val pubKeyCredParams: List<PublicKeyCredentialParameters>

  var timeout: Long
  var excludeCredentials: List<PublicKeyCredentialDescriptor>
  var authenticatorSelection: AuthenticatorSelectionCriteria
  var attestation: String

  init {
    json = JSONObject(requestJson)
    val challengeString = json.getString("challenge")
    challenge = WebAuthnUtils.b64Decode(challengeString)
    val rpJson = json.getJSONObject("rp")
    rp = PublicKeyCredentialRpEntity(rpJson.getString("name"), rpJson.getString("id"))
    val rpUser = json.getJSONObject("user")
    val userId = WebAuthnUtils.b64Decode(rpUser.getString("id"))
    user =
      PublicKeyCredentialUserEntity(
        rpUser.getString("name"),
        userId,
        rpUser.getString("displayName")
      )
    val pubKeyCredParamsJson = json.getJSONArray("pubKeyCredParams")
    val pubKeyCredParamsTmp: MutableList<PublicKeyCredentialParameters> = mutableListOf()
    for (i in 0 until pubKeyCredParamsJson.length()) {
      val e = pubKeyCredParamsJson.getJSONObject(i)
      pubKeyCredParamsTmp.add(PublicKeyCredentialParameters(e.getString("type"), e.getLong("alg")))
    }
    pubKeyCredParams = pubKeyCredParamsTmp.toList()

    timeout = json.optLong("timeout", 0)
    // TODO: Fix excludeCredentials and authenticatorSelection
    excludeCredentials = emptyList()
    authenticatorSelection = AuthenticatorSelectionCriteria("platform", "required")
    attestation = json.optString("attestation", "none")

    Log.i("WebAuthn", "Challenge $challenge()")
    Log.i("WebAuthn", "rp $rp")
    Log.i("WebAuthn", "user $user")
    Log.i("WebAuthn", "pubKeyCredParams $pubKeyCredParams")
    Log.i("WebAuthn", "timeout $timeout")
    Log.i("WebAuthn", "excludeCredentials $excludeCredentials")
    Log.i("WebAuthn", "authenticatorSelection $authenticatorSelection")
    Log.i("WebAuthn", "attestation $attestation")
  }
}

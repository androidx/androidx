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

import androidx.annotation.RestrictTo
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
class AuthenticatorAttestationResponse(
  private val requestOptions: PublicKeyCredentialCreationOptions,
  private val credentialId: ByteArray,
  private val credentialPublicKey: ByteArray,
  private val origin: String,
  private val up: Boolean,
  private val uv: Boolean,
  private val be: Boolean,
  private val bs: Boolean,
  private val packageName: String? = null,
  private val clientDataHash: ByteArray? = null,
) : AuthenticatorResponse {
  override var clientJson = JSONObject()
  var attestationObject: ByteArray

  init {
    clientJson.put("type", "webauthn.create")
    clientJson.put("challenge", WebAuthnUtils.b64Encode(requestOptions.challenge))
    clientJson.put("origin", origin)
    if (packageName != null) {
      clientJson.put("androidPackageName", packageName)
    }

    attestationObject = defaultAttestationObject()
  }

  private fun authData(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    val rpHash = md.digest(requestOptions.rp.id.toByteArray())
    var flags: Int = 0
    if (up) {
      flags = flags or 0x01
    }
    if (uv) {
      flags = flags or 0x04
    }
    if (be) {
      flags = flags or 0x08
    }
    if (bs) {
      flags = flags or 0x10
    }
    flags = flags or 0x40

    val aaguid = ByteArray(16) { 0 }
    val credIdLen = byteArrayOf((credentialId.size shr 8).toByte(), credentialId.size.toByte())

    val ret =
      rpHash +
        byteArrayOf(flags.toByte()) +
        byteArrayOf(0, 0, 0, 0) +
        aaguid +
        credIdLen +
        credentialId +
        credentialPublicKey

    return ret
  }

  internal fun defaultAttestationObject(): ByteArray {
    val ao = mutableMapOf<String, Any>()
    ao.put("fmt", "none")
    ao.put("attStmt", emptyMap<Any, Any>())
    ao.put("authData", authData())
    return Cbor().encode(ao)
  }

  override fun json(): JSONObject {
    // See AuthenticatorAttestationResponseJSON at
    // https://w3c.github.io/webauthn/#ref-for-dom-publickeycredential-tojson

    val clientData = clientJson.toString().toByteArray()
    val response = JSONObject()
    if (clientDataHash == null) {
      response.put("clientDataJSON", WebAuthnUtils.b64Encode(clientData))
    }
    response.put("attestationObject", WebAuthnUtils.b64Encode(attestationObject))
    response.put("transports", JSONArray(listOf("internal", "hybrid")))

    return response
  }
}

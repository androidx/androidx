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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AuthenticatorAttestationResponseTest {

  @Test
  fun constructor() {
    val rawId = byteArrayOf(1)
    val rawPublicKey = byteArrayOf(2)
    val rawJson =
      """
             {
                 "challenge": "AQ",
                 "rp": {
                     "name": "rp name",
                     "id": "rp id"
                 },
                 "user": {
                     "id": "user id",
                     "name": "user name",
                     "displayName": "user display name"
                 },
                 "pubKeyCredParams": [
                     {
                         "type": "pubkey type",
                         "alg": 10
                     }
                 ],
                 "timeout": 1,
                 "attestation": "enabled"
             }
          """
    var options = PublicKeyCredentialCreationOptions(rawJson)
    var response =
      AuthenticatorAttestationResponse(
        options,
        rawId,
        rawPublicKey,
        "origin",
        false,
        true,
        false,
        true
      )
    var json = response.json()
    var attestationObject = response.defaultAttestationObject()

    assertThat(json.getString("attestationObject"))
      .isEqualTo(WebAuthnUtils.b64Encode(attestationObject))
    assertThat(json.getString("transports")).isEqualTo("[\"internal\",\"hybrid\"]")
  }
}

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
class PublicKeyCredentialCreationOptionsTest {

  @Test
  fun constructor() {
    val rawId = byteArrayOf(1)

    val json =
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
    var options = PublicKeyCredentialCreationOptions(json)
    assertThat(options.challenge).isEqualTo(rawId)
    assertThat(options.attestation).isEqualTo("enabled")
    assertThat(options.timeout).isEqualTo(1)
    assertThat(options.user.name).isEqualTo("user name")
    assertThat(options.user.displayName).isEqualTo("user display name")
    assertThat(options.rp.name).isEqualTo("rp name")
    assertThat(options.rp.id).isEqualTo("rp id")
    assertThat(options.pubKeyCredParams.get(0).type).isEqualTo("pubkey type")
    assertThat(options.pubKeyCredParams.get(0).alg).isEqualTo(10)
  }

  @Test
  fun constructor_withoutAttestation() {
    val rawId = byteArrayOf(1)

    val json =
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
                "timeout": 1
            }
         """
    var options = PublicKeyCredentialCreationOptions(json)
    assertThat(options.challenge).isEqualTo(rawId)
    assertThat(options.attestation).isEqualTo("none")
    assertThat(options.timeout).isEqualTo(1)
    assertThat(options.user.name).isEqualTo("user name")
    assertThat(options.user.displayName).isEqualTo("user display name")
    assertThat(options.rp.name).isEqualTo("rp name")
    assertThat(options.rp.id).isEqualTo("rp id")
    assertThat(options.pubKeyCredParams.get(0).type).isEqualTo("pubkey type")
    assertThat(options.pubKeyCredParams.get(0).alg).isEqualTo(10)
  }
}

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
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FidoPublicKeyCredentialTest {

  class TestAuthenticatorResponse() : AuthenticatorResponse {
    override var clientJson = JSONObject()

    override fun json(): JSONObject {
      val response = JSONObject()
      response.put("test", "response")
      return response
    }
  }

  @Test
  fun constructor() {
    val rawId = byteArrayOf(1)
    val encodedId = WebAuthnUtils.b64Encode(rawId)

    val cred = FidoPublicKeyCredential(rawId, TestAuthenticatorResponse(), "attachment")
    val output = JSONObject(cred.json())
    assertThat(output.getString("id")).isEqualTo(encodedId)
    assertThat(output.getString("rawId")).isEqualTo(encodedId)
    assertThat(output.getString("type")).isEqualTo("public-key")
    assertThat(output.getString("authenticatorAttachment")).isEqualTo("attachment")
    assertThat(output.getString("response")).isEqualTo("{\"test\":\"response\"}")
  }
}

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
class PublicKeyCredentialRequestOptionsTest {

  @Test
  fun constructor() {
    val rawId = byteArrayOf(1)

    val json =
      """
             {
                 "challenge": "AQ",
                 "rpId": "rp id",
                 "timeout": 10,
                 "userVerification": "enabled"
             }
          """
    var options = PublicKeyCredentialRequestOptions(json)
    assertThat(options.challenge).isEqualTo(rawId)
    assertThat(options.rpId).isEqualTo("rp id")
    assertThat(options.timeout).isEqualTo(10)
    assertThat(options.userVerification).isEqualTo("enabled")
  }

  @Test
  fun constructor_optionalTimeout() {
    val rawId = byteArrayOf(1)

    val json =
      """
             {
                 "challenge": "AQ",
                 "rpId": "rp id",
                 "userVerification": "enabled"
             }
          """
    var options = PublicKeyCredentialRequestOptions(json)
    assertThat(options.challenge).isEqualTo(rawId)
    assertThat(options.rpId).isEqualTo("rp id")
    assertThat(options.timeout).isEqualTo(0)
    assertThat(options.userVerification).isEqualTo("enabled")
  }

  @Test
  fun constructor_optionalRpId() {
    val rawId = byteArrayOf(1)

    val json =
      """
             {
                 "challenge": "AQ",
                 "timeout": 10,
                 "userVerification": "enabled"
             }
          """
    var options = PublicKeyCredentialRequestOptions(json)
    assertThat(options.challenge).isEqualTo(rawId)
    assertThat(options.rpId).isEqualTo("")
    assertThat(options.timeout).isEqualTo(10)
    assertThat(options.userVerification).isEqualTo("enabled")
  }

  @Test
  fun constructor_optionalUserVerification() {
    val rawId = byteArrayOf(1)

    val json =
      """
             {
                 "challenge": "AQ",
                 "rpId": "rp id",
                 "timeout": 10
             }
          """
    var options = PublicKeyCredentialRequestOptions(json)
    assertThat(options.challenge).isEqualTo(rawId)
    assertThat(options.rpId).isEqualTo("rp id")
    assertThat(options.timeout).isEqualTo(10)
    assertThat(options.userVerification).isEqualTo("preferred")
  }
}

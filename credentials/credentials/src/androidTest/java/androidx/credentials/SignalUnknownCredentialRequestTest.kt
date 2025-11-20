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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignalUnknownCredentialRequestTest {
    @Test
    fun isValidRequestJson_unknownCredential_success() {
        val requestJson = "{\"rpId\":\"example.com\",\"credentialId\":\"dXNlcklkMTIz\"}"
        assertThat(SignalUnknownCredentialRequest.isValidRequestJson(requestJson)).isTrue()
    }

    @Test
    fun isValidSignalRequest_unknownCredential_invalidBase64Url_failure() {
        val requestJson = "{\"rpId\":\"example.com\",\"credentialId\":\"d+\"}"
        assertThat(SignalUnknownCredentialRequest.isValidRequestJson(requestJson)).isFalse()
    }

    @Test
    fun isValidSignalRequest_unknownCredential_missingKey_failure() {
        val requestJson = "{\"rpd\":\"example.com\",\"credentialId\":\"Zm9mFy\"}"
        assertThat(SignalUnknownCredentialRequest.isValidRequestJson(requestJson)).isFalse()
    }

    @Test
    fun isValidSignalRequest_unknownCredential_hasAdditionalKey_success() {
        val requestJson =
            "{\"rpId\":\"example.com\",\"credentialId\":\"dXNlcklkMTIz\", \"userId\":\"user\"}"
        assertThat(SignalUnknownCredentialRequest.isValidRequestJson(requestJson)).isTrue()
    }
}

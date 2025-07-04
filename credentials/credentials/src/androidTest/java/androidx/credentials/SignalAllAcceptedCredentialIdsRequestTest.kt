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

class SignalAllAcceptedCredentialIdsRequestTest {
    @Test
    fun isValidRequestJson_allAcceptedCredentials_success() {
        val requestJson =
            "{\"rpId\":\"example.com\",\"userId\":\"dXNlcklkMTIz\",\"allAcceptedCredentialIds\":[\"Y3JlZDE\",\"Y3JlZDIi\",\"Y3JlZDM\"]}"
        assertThat(SignalAllAcceptedCredentialIdsRequest.isValidRequestJson(requestJson)).isTrue()
    }

    @Test
    fun isValidRequestJson_allAcceptedCredentials_invalidBase64Url_failure() {
        val requestJson =
            "{\"rpId\":\"example.com\",\"userId\":\"d+\",\"allAcceptedCredentialIds\":[\"YlZDE\",\"Y\",\"Y3JlZDM\"]}"
        assertThat(SignalAllAcceptedCredentialIdsRequest.isValidRequestJson(requestJson)).isFalse()
    }

    @Test
    fun isValidRequestJson_allAcceptedCredentials_missingKey_failure() {
        val requestJson =
            "{\"rpd\":\"example.com\",\"userId\":\"dXNlcklkMTIz\",\"allAcceptedCredentialIds\":[\"Y3JlZDE\",\"Y3JlZDIi\",\"Y3JlZDM\"]}"
        assertThat(SignalAllAcceptedCredentialIdsRequest.isValidRequestJson(requestJson)).isFalse()
    }

    @Test
    fun isValidRequestJson_allAcceptedCredentials_additionalKey_success() {
        val requestJson =
            "{\"rpId\":\"example.com\",\"userId\":\"dXNlcklkMTIz\",\"allAcceptedCredentialIds\":[\"Y3JlZDE\",\"Y3JlZDIi\",\"Y3JlZDM\"], \"extra\":\"extra\"}"
        assertThat(SignalAllAcceptedCredentialIdsRequest.isValidRequestJson(requestJson)).isTrue()
    }
}

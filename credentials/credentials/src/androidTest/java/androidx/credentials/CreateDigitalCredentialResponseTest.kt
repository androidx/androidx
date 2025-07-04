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

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalDigitalCredentialApi::class)
class CreateDigitalCredentialResponseTest {

    @Test
    fun constructor_emptyJson_throws() {
        Assert.assertThrows(
            "responseJson must not be empty, and must be a valid JSON",
            IllegalArgumentException::class.java,
        ) {
            CreateDigitalCredentialResponse(responseJson = "")
        }
    }

    @Test
    fun constructor_invalidJson_throws() {
        Assert.assertThrows(
            "responseJson must not be empty, and must be a valid JSON",
            IllegalArgumentException::class.java,
        ) {
            CreateDigitalCredentialResponse(responseJson = "ewpfoj`3oje")
        }
    }

    @Test
    fun constructor_valid() {
        val json = "{key: value}"

        val response = CreateDigitalCredentialResponse(json)

        assertThat(response.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
        assertThat(response.responseJson).isEqualTo(json)
        assertThat(response.data.getString("androidx.credentials.BUNDLE_KEY_RESPONSE_JSON"))
            .isEqualTo(json)
    }

    @Test
    fun createFrom() {
        val json = "{key: value}"
        val bundle =
            Bundle().apply { putString("androidx.credentials.BUNDLE_KEY_RESPONSE_JSON", json) }

        val response = CreateDigitalCredentialResponse.createFrom(bundle)

        assertThat(response.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
        assertThat(response.responseJson).isEqualTo(json)
        assertThat(response.data.getString("androidx.credentials.BUNDLE_KEY_RESPONSE_JSON"))
            .isEqualTo(json)
    }
}

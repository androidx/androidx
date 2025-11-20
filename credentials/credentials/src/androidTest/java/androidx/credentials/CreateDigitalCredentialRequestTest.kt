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
import androidx.credentials.internal.FrameworkClassParsingException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalDigitalCredentialApi::class)
class CreateDigitalCredentialRequestTest {

    @Test
    fun createFrom_emptyJson_throws() {
        Assert.assertThrows(
            "requestJson must not be empty, and must be a valid JSON",
            IllegalArgumentException::class.java,
        ) {
            CreateDigitalCredentialRequest.createFrom(
                Bundle().apply {
                    putString(CreateDigitalCredentialRequest.BUNDLE_KEY_REQUEST_JSON, "")
                },
                "",
                Bundle(),
            )
        }
    }

    @Test
    fun createFrom_invalidJson_throws() {
        Assert.assertThrows(
            "requestJson must not be empty, and must be a valid JSON",
            IllegalArgumentException::class.java,
        ) {
            CreateDigitalCredentialRequest.createFrom(
                Bundle().apply {
                    putString(
                        CreateDigitalCredentialRequest.BUNDLE_KEY_REQUEST_JSON,
                        "dsfliuh4akdsjhbf",
                    )
                },
                "",
                Bundle(),
            )
        }
    }

    @Test
    fun createFrom_valid() {
        val json = "{key: value}"
        val origin = "origin"
        val request =
            CreateDigitalCredentialRequest.createFrom(
                Bundle().apply {
                    putString(CreateDigitalCredentialRequest.BUNDLE_KEY_REQUEST_JSON, json)
                },
                origin,
                Bundle(),
            )

        assertThat(request.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
        assertThat(request.requestJson).isEqualTo(json)
        assertThat(
                request.credentialData.getString(
                    CreateDigitalCredentialRequest.BUNDLE_KEY_REQUEST_JSON
                )
            )
            .isEqualTo(json)
        assertThat(request.displayInfo.userId)
            .isEqualTo(CreateDigitalCredentialRequest.UNUSED_USER_ID)
        assertThat(request.isSystemProviderRequired).isFalse()
        assertThat(request.isAutoSelectAllowed).isFalse()
        assertThat(request.origin).isEqualTo(origin)
        assertThat(request.preferImmediatelyAvailableCredentials).isFalse()
    }

    @Test
    fun createFrom_missingRequestJson_throwsFrameworkClassParsingException() {
        Assert.assertThrows(FrameworkClassParsingException::class.java) {
            CreateDigitalCredentialRequest.createFrom(Bundle(), "origin", Bundle())
        }
    }

    @Test
    fun createFrom_withCandidateQueryData() {
        val json = "{key: value}"
        val origin = "origin"
        val candidateQueryData = Bundle().apply { putBoolean("test_key", true) }
        val request =
            CreateDigitalCredentialRequest.createFrom(
                Bundle().apply {
                    putString(CreateDigitalCredentialRequest.BUNDLE_KEY_REQUEST_JSON, json)
                },
                origin,
                candidateQueryData,
            )

        assertThat(request.candidateQueryData.getBoolean("test_key")).isTrue()
    }

    @Test
    fun constructor_developer_emptyJson_throws() {
        Assert.assertThrows(
            "requestJson must not be empty, and must be a valid JSON",
            IllegalArgumentException::class.java,
        ) {
            CreateDigitalCredentialRequest(requestJson = "", origin = "origin")
        }
    }

    @Test
    fun constructor_developer_invalidJson_throws() {
        Assert.assertThrows(
            "requestJson must not be empty, and must be a valid JSON",
            IllegalArgumentException::class.java,
        ) {
            CreateDigitalCredentialRequest(requestJson = "not a json", origin = "origin")
        }
    }

    @Test
    fun constructor_developer_valid() {
        val json = "{key: value}"
        val origin = "developer_origin"
        val request = CreateDigitalCredentialRequest(requestJson = json, origin = origin)

        assertThat(request.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
        assertThat(request.requestJson).isEqualTo(json)
        assertThat(
                request.credentialData.getString(
                    CreateDigitalCredentialRequest.BUNDLE_KEY_REQUEST_JSON
                )
            )
            .isEqualTo(json)
        assertThat(request.displayInfo.userId)
            .isEqualTo(CreateDigitalCredentialRequest.UNUSED_USER_ID)
        assertThat(request.isSystemProviderRequired).isFalse()
        assertThat(request.isAutoSelectAllowed).isFalse()
        assertThat(request.origin).isEqualTo(origin)
        assertThat(request.preferImmediatelyAvailableCredentials).isFalse()
    }

    @Test
    fun constructor_developer_nullOrigin_valid() {
        val json = "{key: value}"
        val request = CreateDigitalCredentialRequest(requestJson = json, origin = null)
        assertThat(request.origin).isNull()
    }
}

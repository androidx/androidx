/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_ALLOW_HYBRID
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_REQUEST_JSON
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePublicKeyCredentialRequestTest {

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) { CreatePublicKeyCredentialRequest("") }
    }

    @Test
    fun constructor_success() {
        CreatePublicKeyCredentialRequest(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        )
    }

    @Test
    fun constructor_setsAllowHybridToTrueByDefault() {
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            "JSON"
        )
        val allowHybridActual = createPublicKeyCredentialRequest.allowHybrid
        assertThat(allowHybridActual).isTrue()
    }

    @Test
    fun constructor_setsAllowHybridToFalse() {
        val allowHybridExpected = false
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            "testJson",
            allowHybridExpected
        )
        val allowHybridActual = createPublicKeyCredentialRequest.allowHybrid
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val createPublicKeyCredentialReq = CreatePublicKeyCredentialRequest(testJsonExpected)
        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val allowHybridExpected = false
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            CreatePublicKeyCredentialRequest
                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST
        )
        expectedData.putString(
            BUNDLE_KEY_REQUEST_JSON, requestJsonExpected
        )
        expectedData.putBoolean(
            BUNDLE_KEY_ALLOW_HYBRID, allowHybridExpected
        )

        val request = CreatePublicKeyCredentialRequest(
            requestJsonExpected,
            allowHybridExpected
        )

        assertThat(request.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(request.credentialData, expectedData)).isTrue()
        assertThat(equals(request.candidateQueryData, expectedData)).isTrue()
        assertThat(request.requireSystemProvider).isFalse()
    }

    @Test
    fun frameworkConversion_success() {
        val request = CreatePublicKeyCredentialRequest("json", true)

        val convertedRequest = createFrom(
            request.type, request.credentialData,
            request.candidateQueryData, request.requireSystemProvider
        )

        assertThat(convertedRequest).isInstanceOf(
            CreatePublicKeyCredentialRequest::class.java
        )
        val convertedSubclassRequest = convertedRequest as CreatePublicKeyCredentialRequest
        assertThat(convertedSubclassRequest.requestJson).isEqualTo(request.requestJson)
        assertThat(convertedSubclassRequest.allowHybrid).isEqualTo(request.allowHybrid)
    }
}
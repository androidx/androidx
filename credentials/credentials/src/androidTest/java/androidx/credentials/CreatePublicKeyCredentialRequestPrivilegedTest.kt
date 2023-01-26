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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Combines with [CreatePublicKeyCredentialRequestPrivilegedFailureInputsTest] for full tests.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePublicKeyCredentialRequestPrivilegedTest {

    @Test
    fun constructor_success() {
        CreatePublicKeyCredentialRequestPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RelyingParty", "ClientDataHash"
        )
    }

    @Test
    fun constructor_setsAllowHybridToTrueByDefault() {
        val createPublicKeyCredentialRequestPrivileged = CreatePublicKeyCredentialRequestPrivileged(
            "JSON", "RelyingParty", "HASH"
        )
        val allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid
        assertThat(allowHybridActual).isTrue()
    }

    @Test
    fun constructor_setsAllowHybridToFalse() {
        val allowHybridExpected = false
        val createPublicKeyCredentialRequestPrivileged = CreatePublicKeyCredentialRequestPrivileged(
            "testJson",
            "RelyingParty", "Hash", allowHybridExpected
        )
        val allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected)
    }

    @Test
    fun builder_build_defaultAllowHybrid_true() {
        val defaultPrivilegedRequest = CreatePublicKeyCredentialRequestPrivileged.Builder(
            "{\"Data\":5}",
            "RelyingParty", "HASH"
        ).build()
        assertThat(defaultPrivilegedRequest.allowHybrid).isTrue()
    }

    @Test
    fun builder_build_nonDefaultAllowHybrid_false() {
        val allowHybridExpected = false
        val createPublicKeyCredentialRequestPrivileged = CreatePublicKeyCredentialRequestPrivileged
            .Builder(
                "testJson",
                "RelyingParty", "Hash"
            ).setAllowHybrid(allowHybridExpected).build()
        val allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val createPublicKeyCredentialReqPriv =
            CreatePublicKeyCredentialRequestPrivileged(testJsonExpected, "RelyingParty",
                "HASH")
        val testJsonActual = createPublicKeyCredentialReqPriv.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_relyingParty_success() {
        val testRelyingPartyExpected = "RelyingParty"
        val createPublicKeyCredentialRequestPrivileged =
            CreatePublicKeyCredentialRequestPrivileged(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                testRelyingPartyExpected, "X342%4dfd7&"
            )
        val testRelyingPartyActual = createPublicKeyCredentialRequestPrivileged.relyingParty
        assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected)
    }

    @Test
    fun getter_clientDataHash_success() {
        val clientDataHashExpected = "X342%4dfd7&"
        val createPublicKeyCredentialRequestPrivileged =
            CreatePublicKeyCredentialRequestPrivileged(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                "RelyingParty", clientDataHashExpected
            )
        val clientDataHashActual = createPublicKeyCredentialRequestPrivileged.clientDataHash
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val relyingPartyExpected = "RelyingParty"
        val clientDataHashExpected = "X342%4dfd7&"
        val allowHybridExpected = false
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            CreatePublicKeyCredentialRequestPrivileged
                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIVILEGED
        )
        expectedData.putString(
            CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected
        )
        expectedData.putString(CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_RELYING_PARTY,
            relyingPartyExpected)
        expectedData.putString(
            CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHashExpected
        )
        expectedData.putBoolean(
            CreatePublicKeyCredentialRequest.BUNDLE_KEY_ALLOW_HYBRID,
            allowHybridExpected
        )

        val request = CreatePublicKeyCredentialRequestPrivileged(
            requestJsonExpected,
            relyingPartyExpected,
            clientDataHashExpected,
            allowHybridExpected
        )

        assertThat(request.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(request.credentialData, expectedData)).isTrue()
        assertThat(equals(request.candidateQueryData, expectedData)).isTrue()
        assertThat(request.requireSystemProvider).isFalse()
    }

    @Test
    fun frameworkConversion_success() {
        val request = CreatePublicKeyCredentialRequestPrivileged(
            "json", "rp", "clientDataHash", true
        )

        val convertedRequest = createFrom(
            request.type, request.credentialData,
            request.candidateQueryData, request.requireSystemProvider
        )

        assertThat(convertedRequest).isInstanceOf(
            CreatePublicKeyCredentialRequestPrivileged::class.java
        )
        val convertedSubclassRequest =
            convertedRequest as CreatePublicKeyCredentialRequestPrivileged
        assertThat(convertedSubclassRequest.requestJson).isEqualTo(request.requestJson)
        assertThat(convertedSubclassRequest.relyingParty).isEqualTo(request.relyingParty)
        assertThat(convertedSubclassRequest.clientDataHash)
            .isEqualTo(request.clientDataHash)
        assertThat(convertedSubclassRequest.allowHybrid).isEqualTo(request.allowHybrid)
    }
}
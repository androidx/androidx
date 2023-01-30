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
import androidx.credentials.CredentialOption.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Combines with [GetPublicKeyCredentialOptionPrivilegedFailureInputsTest] for full tests.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class GetPublicKeyCredentialOptionPrivilegedTest {

    @Test
    fun constructor_success() {
        GetPublicKeyCredentialOptionPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RelyingParty", "ClientDataHash"
        )
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsToFalseByDefault() {
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged(
            "JSON", "RelyingParty", "HASH"
        )
        val preferImmediatelyAvailableCredentialsActual =
            getPublicKeyCredentialOptionPrivileged.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual).isFalse()
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsTrue() {
        val preferImmediatelyAvailableCredentialsExpected = true
        val getPublicKeyCredentialOptPriv = GetPublicKeyCredentialOptionPrivileged(
            "JSON",
            "RelyingParty",
            "HASH",
            preferImmediatelyAvailableCredentialsExpected
        )
        val preferImmediatelyAvailableCredentialsActual =
            getPublicKeyCredentialOptPriv.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual).isEqualTo(
            preferImmediatelyAvailableCredentialsExpected
        )
    }

    @Test
    fun builder_build_nonDefaultPreferImmediatelyAvailableCredentials_true() {
        val preferImmediatelyAvailableCredentialsExpected = true
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged
            .Builder(
                "testJson",
                "RelyingParty", "Hash",
            )
            .setPreferImmediatelyAvailableCredentials(preferImmediatelyAvailableCredentialsExpected)
            .build()
        val preferImmediatelyAvailableCredentialsActual =
            getPublicKeyCredentialOptionPrivileged.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual).isEqualTo(
            preferImmediatelyAvailableCredentialsExpected
        )
    }

    @Test
    fun builder_build_defaultPreferImmediatelyAvailableCredentials_false() {
        val defaultPrivilegedRequest = GetPublicKeyCredentialOptionPrivileged.Builder(
            "{\"Data\":5}",
            "RelyingParty", "HASH"
        ).build()
        assertThat(defaultPrivilegedRequest.preferImmediatelyAvailableCredentials).isFalse()
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val getPublicKeyCredentialOptionPrivileged =
            GetPublicKeyCredentialOptionPrivileged(
                testJsonExpected, "RelyingParty",
                "HASH"
            )
        val testJsonActual = getPublicKeyCredentialOptionPrivileged.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_relyingParty_success() {
        val testRelyingPartyExpected = "RelyingParty"
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            testRelyingPartyExpected, "X342%4dfd7&"
        )
        val testRelyingPartyActual = getPublicKeyCredentialOptionPrivileged.relyingParty
        assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected)
    }

    @Test
    fun getter_clientDataHash_success() {
        val clientDataHashExpected = "X342%4dfd7&"
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RelyingParty", clientDataHashExpected
        )
        val clientDataHashActual = getPublicKeyCredentialOptionPrivileged.clientDataHash
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val relyingPartyExpected = "RelyingParty"
        val clientDataHashExpected = "X342%4dfd7&"
        val preferImmediatelyAvailableCredentialsExpected = false
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOptionPrivileged
                .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION_PRIVILEGED
        )
        expectedData.putString(
            GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected
        )
        expectedData.putString(
            GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_RELYING_PARTY,
            relyingPartyExpected
        )
        expectedData.putString(
            GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHashExpected
        )
        expectedData.putBoolean(
            GetPublicKeyCredentialOptionPrivileged
                .BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            preferImmediatelyAvailableCredentialsExpected
        )

        val option = GetPublicKeyCredentialOptionPrivileged(
            requestJsonExpected, relyingPartyExpected, clientDataHashExpected,
            preferImmediatelyAvailableCredentialsExpected
        )

        assertThat(option.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(option.requestData, expectedData)).isTrue()
        assertThat(equals(option.candidateQueryData, expectedData)).isTrue()
        assertThat(option.isSystemProviderRequired).isFalse()
    }

    @Test
    fun frameworkConversion_success() {
        val option = GetPublicKeyCredentialOptionPrivileged("json", "rp", "clientDataHash", true)

        val convertedOption = createFrom(
            option.type,
            option.requestData,
            option.candidateQueryData,
            option.isSystemProviderRequired
        )

        assertThat(convertedOption).isInstanceOf(
            GetPublicKeyCredentialOptionPrivileged::class.java
        )
        val convertedSubclassOption = convertedOption as GetPublicKeyCredentialOptionPrivileged
        assertThat(convertedSubclassOption.requestJson).isEqualTo(option.requestJson)
        assertThat(convertedSubclassOption.preferImmediatelyAvailableCredentials)
            .isEqualTo(option.preferImmediatelyAvailableCredentials)
        assertThat(convertedSubclassOption.clientDataHash)
            .isEqualTo(option.clientDataHash)
        assertThat(convertedSubclassOption.relyingParty).isEqualTo(option.relyingParty)
    }
}
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
package androidx.credentials.provider

import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOptionPrivileged
import androidx.credentials.PublicKeyCredential
import androidx.credentials.equals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(34)
class BeginGetPublicKeyCredentialOptionPrivilegedTest {
    companion object {
        private const val BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY"
        private const val BUNDLE_ID = "id"
    }
    @Test
    fun constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        BeginGetPublicKeyCredentialOptionPrivileged(
            Bundle(),
            BUNDLE_ID,
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RelyingParty",
            "ClientDataHash"
        )
    }

    @Test
    fun getter_requestJson_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val getPublicKeyCredentialOptionPrivileged = BeginGetPublicKeyCredentialOptionPrivileged(
            Bundle(),
            BUNDLE_ID,
            testJsonExpected,
            "RelyingParty",
            "HASH"
        )

        val testJsonActual = getPublicKeyCredentialOptionPrivileged.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_relyingParty_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val testRelyingPartyExpected = "RelyingParty"

        val getPublicKeyCredentialOptionPrivileged = BeginGetPublicKeyCredentialOptionPrivileged(
            Bundle(),
            BUNDLE_ID,
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            testRelyingPartyExpected,
            "X342%4dfd7&"
        )

        val testRelyingPartyActual = getPublicKeyCredentialOptionPrivileged.relyingParty
        assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected)
    }

    @Test
    fun getter_clientDataHash_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val clientDataHashExpected = "X342%4dfd7&"

        val getPublicKeyCredentialOptionPrivileged = BeginGetPublicKeyCredentialOptionPrivileged(
            Bundle(),
            BUNDLE_ID,
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RelyingParty",
            clientDataHashExpected
        )

        val clientDataHashActual = getPublicKeyCredentialOptionPrivileged.clientDataHash
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val relyingPartyExpected = "RelyingParty"
        val clientDataHashExpected = "X342%4dfd7&"
        val expectedData = Bundle()
        val preferImmediatelyAvailableCredentialsExpected = false
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
            CreatePublicKeyCredentialRequest.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            preferImmediatelyAvailableCredentialsExpected
        )

        val option = BeginGetPublicKeyCredentialOptionPrivileged(
            expectedData,
            BUNDLE_ID,
            requestJsonExpected, relyingPartyExpected, clientDataHashExpected
        )

        expectedData.putString(BUNDLE_ID_KEY, BUNDLE_ID)
        assertThat(option.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(option.candidateQueryData, expectedData)).isTrue()
    }
    // TODO ("Add framework conversion, createFrom tests")
}
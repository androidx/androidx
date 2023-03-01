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

import android.content.pm.SigningInfo
import android.os.Bundle
import android.service.credentials.CallingAppInfo
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged
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
class BeginCreatePublicKeyCredentialRequestPrivilegedTest {
    @Test
    fun constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        BeginCreatePublicKeyCredentialRequestPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "relyingParty", "ClientDataHash",
            CallingAppInfo(
                "sample_calling_package",
                SigningInfo()
            )
        )
    }

    @Test
    fun getter_requestJson_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val createPublicKeyCredentialReqPriv = BeginCreatePublicKeyCredentialRequestPrivileged(
            testJsonExpected,
            "relyingParty", "HASH",
            CallingAppInfo("sample_package_name", SigningInfo())
        )

        val testJsonActual = createPublicKeyCredentialReqPriv.json
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_relyingParty_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val testRelyingPartyExpected = "relyingParty"

        val createPublicKeyCredentialRequestPrivileged =
            BeginCreatePublicKeyCredentialRequestPrivileged(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                testRelyingPartyExpected, "X342%4dfd7&",
                CallingAppInfo("sample_package_name", SigningInfo()))

        val testRelyingPartyActual = createPublicKeyCredentialRequestPrivileged
            .relyingParty
        assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected)
    }

    @Test
    fun getter_clientDataHash_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val clientDataHashExpected = "X342%4dfd7&"

        val createPublicKeyCredentialRequestPrivileged =
            BeginCreatePublicKeyCredentialRequestPrivileged(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                "relyingParty", clientDataHashExpected,
                CallingAppInfo("sample_package_name", SigningInfo())
            )

        val clientDataHashActual = createPublicKeyCredentialRequestPrivileged.clientDataHash
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val relyingPartyExpected = "relyingParty"
        val clientDataHashExpected = "X342%4dfd7&"
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            CreatePublicKeyCredentialRequestPrivileged
                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIV
        )
        expectedData.putString(
            CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected
        )
        expectedData.putString(
            CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_RELYING_PARTY,
            relyingPartyExpected
        )
        expectedData.putString(
            CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHashExpected
        )

        val request = BeginCreatePublicKeyCredentialRequestPrivileged(
            requestJsonExpected, relyingPartyExpected, clientDataHashExpected,
            CallingAppInfo("sample_package_name", SigningInfo())
        )

        assertThat(request.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(request.data, expectedData)).isTrue()
    }
    // TODO ("Add framework conversion, createFrom & preferImmediatelyAvailable tests")
}
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

package androidx.credentials.provider;

import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_RELYING_PARTY;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_REQUEST_JSON;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.core.os.BuildCompat;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.GetPublicKeyCredentialOptionPrivileged;
import androidx.credentials.GetPublicKeyCredentialOptionPrivilegedFailureInputsJavaTest;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Combines with {@link GetPublicKeyCredentialOptionPrivilegedFailureInputsJavaTest}
 * for full tests.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginGetPublicKeyCredentialOptionPrivilegedJavaTest {
    private static final String BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY";
    private static final String BUNDLE_ID = "id";

    @Test
    public void constructor_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginGetPublicKeyCredentialOptionPrivileged(
                    new Bundle(),
                    BUNDLE_ID,
                    "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                    "RelyingParty",
                    "ClientDataHash");
        }
    }

    @Test
    public void getter_requestJson_success() {
        if (BuildCompat.isAtLeastU()) {
            String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

            BeginGetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                    new BeginGetPublicKeyCredentialOptionPrivileged(
                            new Bundle(),
                            "id",
                            testJsonExpected,
                            "RelyingParty",
                            "HASH");

            String testJsonActual = getPublicKeyCredentialOptionPrivileged.getRequestJson();
            assertThat(testJsonActual).isEqualTo(testJsonExpected);
        }
    }

    @Test
    public void getter_relyingParty_success() {
        if (BuildCompat.isAtLeastU()) {
            String testRelyingPartyExpected = "RelyingParty";

            BeginGetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                    new BeginGetPublicKeyCredentialOptionPrivileged(
                            new Bundle(),
                            BUNDLE_ID,
                            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                            testRelyingPartyExpected,
                            "X342%4dfd7&");

            String testRelyingPartyActual =
                    getPublicKeyCredentialOptionPrivileged.getRelyingParty();
            assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected);
        }
    }

    @Test
    public void getter_clientDataHash_success() {
        if (BuildCompat.isAtLeastU()) {
            String clientDataHashExpected = "X342%4dfd7&";
            BeginGetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                    new BeginGetPublicKeyCredentialOptionPrivileged(
                            new Bundle(),
                            BUNDLE_ID,
                            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                            "RelyingParty",
                            clientDataHashExpected);

            String clientDataHashActual =
                    getPublicKeyCredentialOptionPrivileged.getClientDataHash();
            assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected);
        }
    }

    @Test
    public void getter_frameworkProperties_success() {
        if (BuildCompat.isAtLeastU()) {
            String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
            String relyingPartyExpected = "RelyingParty";
            String clientDataHashExpected = "X342%4dfd7&";
            Boolean preferImmediatelyAvailableCredentialsExpected = false;
            Bundle expectedData = new Bundle();
            expectedData.putString(
                    PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                    GetPublicKeyCredentialOptionPrivileged
                            .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION_PRIVILEGED);
            expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
            expectedData.putString(BUNDLE_KEY_RELYING_PARTY, relyingPartyExpected);
            expectedData.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHashExpected);
            expectedData.putBoolean(
                    CreatePublicKeyCredentialRequest
                            .BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                    preferImmediatelyAvailableCredentialsExpected);

            BeginGetPublicKeyCredentialOptionPrivileged option =
                    new BeginGetPublicKeyCredentialOptionPrivileged(
                            expectedData,
                            BUNDLE_ID,
                            requestJsonExpected, relyingPartyExpected, clientDataHashExpected);

            expectedData.putString(BUNDLE_ID_KEY, BUNDLE_ID);
            assertThat(option.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
            assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), expectedData)).isTrue();
        }
    }
    // TODO ("Add framework conversion, createFrom tests")
}

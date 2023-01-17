/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_RELYING_PARTY;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_REQUEST_JSON;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.SigningInfo;
import android.os.Bundle;
import android.service.credentials.CallingAppInfo;

import androidx.core.os.BuildCompat;
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged;
import androidx.credentials.CreatePublicKeyCredentialRequestPrivilegedFailureInputsJavaTest;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Combines with {@link CreatePublicKeyCredentialRequestPrivilegedFailureInputsJavaTest}
 * for full tests.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginCreatePublicKeyCredentialRequestPrivilegedJavaTest {
    @Test
    public void constructor_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginCreatePublicKeyCredentialRequestPrivileged(
                    "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                    "relyingParty", "ClientDataHash",
                    new CallingAppInfo("sample_calling_package",
                            new SigningInfo()));
        }
    }

    @Test
    public void getter_requestJson_success() {
        if (BuildCompat.isAtLeastU()) {
            String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

            BeginCreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialReqPriv =
                    new BeginCreatePublicKeyCredentialRequestPrivileged(
                    testJsonExpected,
                    "relyingParty", "HASH",
                    new CallingAppInfo("sample_package_name", new SigningInfo()));

            String testJsonActual = createPublicKeyCredentialReqPriv.getJson();
            assertThat(testJsonActual).isEqualTo(testJsonExpected);
        }
    }

    @Test
    public void getter_relyingParty_success() {
        if (BuildCompat.isAtLeastU()) {
            String testRelyingPartyExpected = "relyingParty";
            BeginCreatePublicKeyCredentialRequestPrivileged
                    createPublicKeyCredentialRequestPrivileged =
                    new BeginCreatePublicKeyCredentialRequestPrivileged(
                            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                            testRelyingPartyExpected, "X342%4dfd7&",
                            new CallingAppInfo("sample_package_name",
                                    new SigningInfo()));

            String testRelyingPartyActual = createPublicKeyCredentialRequestPrivileged
                    .getRelyingParty();
            assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected);
        }
    }

    @Test
    public void getter_clientDataHash_success() {
        if (BuildCompat.isAtLeastU()) {
            String clientDataHashExpected = "X342%4dfd7&";
            BeginCreatePublicKeyCredentialRequestPrivileged
                    createPublicKeyCredentialRequestPrivileged =
                    new BeginCreatePublicKeyCredentialRequestPrivileged(
                            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                            "relyingParty", clientDataHashExpected,
                            new CallingAppInfo("sample_package_name",
                                    new SigningInfo()));

            String clientDataHashActual =
                    createPublicKeyCredentialRequestPrivileged.getClientDataHash();
            assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected);
        }
    }

    @Test
    public void getter_frameworkProperties_success() {
        if (BuildCompat.isAtLeastU()) {
            String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
            String relyingPartyExpected = "relyingParty";
            String clientDataHashExpected = "X342%4dfd7&";
            Bundle expectedData = new Bundle();
            expectedData.putString(
                    PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                    CreatePublicKeyCredentialRequestPrivileged
                            .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIV);
            expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
            expectedData.putString(BUNDLE_KEY_RELYING_PARTY, relyingPartyExpected);
            expectedData.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHashExpected);

            BeginCreatePublicKeyCredentialRequestPrivileged request =
                    new BeginCreatePublicKeyCredentialRequestPrivileged(
                            requestJsonExpected, relyingPartyExpected, clientDataHashExpected,
                            new CallingAppInfo(
                                    "sample_package_name", new SigningInfo()));

            assertThat(request.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
            assertThat(TestUtilsKt.equals(request.getData(), expectedData)).isTrue();
        }
    }

    // TODO ("Add framework conversion, createFrom & preferImmediatelyAvailable tests")
}

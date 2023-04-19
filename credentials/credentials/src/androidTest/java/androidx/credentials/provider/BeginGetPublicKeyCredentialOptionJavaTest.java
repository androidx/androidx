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

import static androidx.credentials.GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.core.os.BuildCompat;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginGetPublicKeyCredentialOptionJavaTest {
    private static final String BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY";
    private static final String BUNDLE_ID = "id";
    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        if (BuildCompat.isAtLeastU()) {
            assertThrows("Expected empty Json to throw error",
                    IllegalArgumentException.class,
                    () -> new BeginGetPublicKeyCredentialOption(
                            new Bundle(), "", "")
            );
        }
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        if (BuildCompat.isAtLeastU()) {
            assertThrows("Expected null Json to throw NPE",
                    NullPointerException.class,
                    () -> new BeginGetPublicKeyCredentialOption(
                            new Bundle(), BUNDLE_ID, null)
            );
        }
    }

    @Test
    public void constructor_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginGetPublicKeyCredentialOption(
                    new Bundle(), BUNDLE_ID,
                    "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}");
        }
    }

    @Test
    public void constructorWithClientDataHash_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginGetPublicKeyCredentialOption(
                    new Bundle(), BUNDLE_ID,
                    "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                    "client_data_hash".getBytes());
        }
    }

    @Test
    public void getter_requestJson_success() {
        if (BuildCompat.isAtLeastU()) {
            String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

            BeginGetPublicKeyCredentialOption getPublicKeyCredentialOpt =
                    new BeginGetPublicKeyCredentialOption(
                            new Bundle(), BUNDLE_ID, testJsonExpected);

            String testJsonActual = getPublicKeyCredentialOpt.getRequestJson();
            assertThat(testJsonActual).isEqualTo(testJsonExpected);
        }
    }

    @Test
    public void getter_clientDataHash_success() {
        if (BuildCompat.isAtLeastU()) {
            byte[] testClientDataHashExpected = "client_data_hash".getBytes();

            BeginGetPublicKeyCredentialOption beginGetPublicKeyCredentialOpt =
                    new BeginGetPublicKeyCredentialOption(
                            new Bundle(), BUNDLE_ID, "test_json",
                            testClientDataHashExpected);

            byte[] testClientDataHashActual = beginGetPublicKeyCredentialOpt.getClientDataHash();
            assertThat(testClientDataHashActual).isEqualTo(testClientDataHashExpected);
        }
    }

    @Test
    public void getter_frameworkProperties_success() {
        if (BuildCompat.isAtLeastU()) {
            String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
            byte[] clientDataHash = "client_data_hash".getBytes();
            Bundle expectedData = new Bundle();
            expectedData.putString(
                    PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                    GetPublicKeyCredentialOption
                            .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION);
            expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
            expectedData.putByteArray(GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
                    clientDataHash);

            BeginGetPublicKeyCredentialOption option = new BeginGetPublicKeyCredentialOption(
                    expectedData, BUNDLE_ID, requestJsonExpected, clientDataHash);

            expectedData.putString(BUNDLE_ID_KEY, BUNDLE_ID);
            assertThat(option.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
            assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), expectedData)).isTrue();
        }
    }
    // TODO ("Add framework conversion, createFrom tests")
}

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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.pm.SigningInfo;
import android.os.Bundle;
import android.service.credentials.CallingAppInfo;

import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginCreatePublicKeyCredentialRequestJavaTest {
    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        if (BuildCompat.isAtLeastU()) {
            assertThrows("Expected empty Json to throw error",
                    IllegalArgumentException.class,
                    () -> new BeginCreatePublicKeyCredentialRequest(
                            "",
                            new CallingAppInfo(
                                    "sample_package_name", new SigningInfo()),
                            new Bundle()
                    )
            );
        }
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        if (BuildCompat.isAtLeastU()) {
            assertThrows("Expected null Json to throw NPE",
                    NullPointerException.class,
                    () -> new BeginCreatePublicKeyCredentialRequest(
                            null,
                            new CallingAppInfo("sample_package_name",
                                    new SigningInfo()),
                            new Bundle()
                    )
            );
        }
    }

    @Test
    public void constructor_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginCreatePublicKeyCredentialRequest(
                    "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                    new CallingAppInfo(
                            "sample_package_name", new SigningInfo()
                    ),
                    new Bundle()
            );
        }
    }

    @Test
    public void constructorWithClientDataHash_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginCreatePublicKeyCredentialRequest(
                    "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                    new CallingAppInfo(
                            "sample_package_name", new SigningInfo()
                    ),
                    new Bundle(),
                    "client_data_hash".getBytes()
            );
        }
    }

    @Test
    public void getter_requestJson_success() {
        if (BuildCompat.isAtLeastU()) {
            String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

            BeginCreatePublicKeyCredentialRequest
                    createPublicKeyCredentialReq = new BeginCreatePublicKeyCredentialRequest(
                    testJsonExpected,
                    new CallingAppInfo(
                            "sample_package_name", new SigningInfo()),
                    new Bundle()
            );

            String testJsonActual = createPublicKeyCredentialReq.getRequestJson();
            assertThat(testJsonActual).isEqualTo(testJsonExpected);
            assertThat(createPublicKeyCredentialReq.getClientDataHash()).isNull();

        }
    }

    @Test
    public void getter_clientDataHash_success() {
        if (BuildCompat.isAtLeastU()) {
            String testClientDataHashExpected = "client_data_hash";
            BeginCreatePublicKeyCredentialRequest createPublicKeyCredentialReq =
                    new BeginCreatePublicKeyCredentialRequest(
                            "json",
                            new CallingAppInfo("sample_package_name",
                                    new SigningInfo()),
                            new Bundle(),
                            testClientDataHashExpected.getBytes());

            assertThat(createPublicKeyCredentialReq.getClientDataHash())
                    .isEqualTo(testClientDataHashExpected);
        }
    }
    // TODO ("Add framework conversion, createFrom & preferImmediatelyAvailable tests")
}

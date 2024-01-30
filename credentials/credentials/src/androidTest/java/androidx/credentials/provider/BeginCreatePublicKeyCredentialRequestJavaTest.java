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

import androidx.credentials.internal.FrameworkClassParsingException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
@SmallTest
public class BeginCreatePublicKeyCredentialRequestJavaTest {

    private static final String BUNDLE_KEY_CLIENT_DATA_HASH =
            "androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH";
    private static final String BUNDLE_KEY_REQUEST_JSON =
            "androidx.credentials.BUNDLE_KEY_REQUEST_JSON";

    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows(
                "Expected empty Json to throw error",
                IllegalArgumentException.class,
                () ->
                        new BeginCreatePublicKeyCredentialRequest(
                                "",
                                new CallingAppInfo("sample_package_name", new SigningInfo()),
                                new Bundle()));
    }

    @Test
    public void constructor_invalidJson_throwsIllegalArgumentException() {
        assertThrows(
                "Expected invalid Json to throw error",
                IllegalArgumentException.class,
                () ->
                        new BeginCreatePublicKeyCredentialRequest(
                                "invalid",
                                new CallingAppInfo("sample_package_name", new SigningInfo()),
                                new Bundle()));
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows(
                "Expected null Json to throw NPE",
                NullPointerException.class,
                () ->
                        new BeginCreatePublicKeyCredentialRequest(
                                null,
                                new CallingAppInfo("sample_package_name", new SigningInfo()),
                                new Bundle()));
    }

    @Test
    public void constructor_success() {
        new BeginCreatePublicKeyCredentialRequest(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                new CallingAppInfo("sample_package_name", new SigningInfo()),
                new Bundle());
    }

    @Test
    public void constructorWithClientDataHash_success() {
        new BeginCreatePublicKeyCredentialRequest(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                new CallingAppInfo("sample_package_name", new SigningInfo()),
                new Bundle(),
                "client_data_hash".getBytes());
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

        BeginCreatePublicKeyCredentialRequest createPublicKeyCredentialReq =
                new BeginCreatePublicKeyCredentialRequest(
                        testJsonExpected,
                        new CallingAppInfo("sample_package_name", new SigningInfo()),
                        new Bundle());

        String testJsonActual = createPublicKeyCredentialReq.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
        assertThat(createPublicKeyCredentialReq.getClientDataHash()).isNull();
    }

    @Test
    public void getter_clientDataHash_success() {
        String testClientDataHashExpected = "client_data_hash";
        BeginCreatePublicKeyCredentialRequest createPublicKeyCredentialReq =
                new BeginCreatePublicKeyCredentialRequest(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        new CallingAppInfo("sample_package_name", new SigningInfo()),
                        new Bundle(),
                        testClientDataHashExpected.getBytes());

        assertThat(createPublicKeyCredentialReq.getClientDataHash())
                .isEqualTo(testClientDataHashExpected.getBytes());
    }

    @Test
    public void constructor_success_createFrom() {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_REQUEST_JSON, "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}");
        bundle.putByteArray(BUNDLE_KEY_CLIENT_DATA_HASH, new byte[0]);

        BeginCreatePublicKeyCredentialRequest.createForTest(
                bundle, new CallingAppInfo("sample_package_name", new SigningInfo()));
    }

    @Test
    public void constructor_error_createFrom() {
        assertThrows(
                "Expected create from to throw error",
                FrameworkClassParsingException.class,
                () ->
                        BeginCreatePublicKeyCredentialRequest.createForTest(
                                new Bundle(),
                                new CallingAppInfo("sample_package_name", new SigningInfo())));
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void conversion() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

        BeginCreatePublicKeyCredentialRequest req =
                new BeginCreatePublicKeyCredentialRequest(
                        testJsonExpected,
                        new CallingAppInfo("sample_package_name", new SigningInfo()),
                        new Bundle());

        Bundle bundle = BeginCreateCredentialRequest.asBundle(req);
        assertThat(bundle).isNotNull();

        BeginCreateCredentialRequest converted = BeginCreateCredentialRequest.fromBundle(bundle);
        assertThat(req.getType()).isEqualTo(converted.getType());
    }
}

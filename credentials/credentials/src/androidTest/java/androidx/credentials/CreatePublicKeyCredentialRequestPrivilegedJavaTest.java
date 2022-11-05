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

package androidx.credentials;

import static androidx.credentials.CreatePublicKeyCredentialBaseRequest.BUNDLE_KEY_REQUEST_JSON;
import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_ALLOW_HYBRID;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_RP;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

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
public class CreatePublicKeyCredentialRequestPrivilegedJavaTest {

    @Test
    public void constructor_success() {
        new CreatePublicKeyCredentialRequestPrivileged(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                "RP", "ClientDataHash");
    }

    @Test
    public void constructor_setsAllowHybridToTrueByDefault() {
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "JSON", "RP", "HASH");
        boolean allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid();
        assertThat(allowHybridActual).isTrue();
    }

    @Test
    public void constructor_setsAllowHybridToFalse() {
        boolean allowHybridExpected = false;
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged("JSON", "RP", "HASH",
                        allowHybridExpected);
        boolean allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid();
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void builder_build_defaultAllowHybrid_true() {
        CreatePublicKeyCredentialRequestPrivileged defaultPrivilegedRequest = new
                CreatePublicKeyCredentialRequestPrivileged.Builder("{\"Data\":5}",
                "RP", "HASH").build();
        assertThat(defaultPrivilegedRequest.allowHybrid()).isTrue();
    }

    @Test
    public void builder_build_nonDefaultAllowHybrid_false() {
        boolean allowHybridExpected = false;
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged.Builder("JSON", "RP", "HASH")
                        .setAllowHybrid(allowHybridExpected).build();
        boolean allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid();
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialReqPriv =
                new CreatePublicKeyCredentialRequestPrivileged(testJsonExpected, "RP", "HASH");
        String testJsonActual = createPublicKeyCredentialReqPriv.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_rp_success() {
        String testRpExpected = "RP";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        testRpExpected, "X342%4dfd7&");
        String testRpActual = createPublicKeyCredentialRequestPrivileged.getRp();
        assertThat(testRpActual).isEqualTo(testRpExpected);
    }

    @Test
    public void getter_clientDataHash_success() {
        String clientDataHashExpected = "X342%4dfd7&";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                         "RP", clientDataHashExpected);
        String clientDataHashActual =
                createPublicKeyCredentialRequestPrivileged.getClientDataHash();
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected);
    }

    @Test
    public void getter_frameworkProperties_success() {
        String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        String rpExpected = "RP";
        String clientDataHashExpected = "X342%4dfd7&";
        boolean allowHybridExpected = false;
        Bundle expectedData = new Bundle();
        expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putString(BUNDLE_KEY_RP, rpExpected);
        expectedData.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHashExpected);
        expectedData.putBoolean(BUNDLE_KEY_ALLOW_HYBRID, allowHybridExpected);

        CreatePublicKeyCredentialRequestPrivileged request =
                new CreatePublicKeyCredentialRequestPrivileged(
                        requestJsonExpected, rpExpected, clientDataHashExpected,
                        allowHybridExpected);

        assertThat(request.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(request.getData(), expectedData)).isTrue();
        assertThat(request.getRequireSystemProvider()).isFalse();
    }
}

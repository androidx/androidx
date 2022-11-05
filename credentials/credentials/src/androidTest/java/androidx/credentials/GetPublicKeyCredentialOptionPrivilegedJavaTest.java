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

import static androidx.credentials.GetPublicKeyCredentialBaseOption.BUNDLE_KEY_REQUEST_JSON;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_ALLOW_HYBRID;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_RP;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

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
public class GetPublicKeyCredentialOptionPrivilegedJavaTest {

    @Test
    public void constructor_success() {
        new GetPublicKeyCredentialOptionPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        "RP", "ClientDataHash");
    }

    @Test
    public void constructor_setsAllowHybridToTrueByDefault() {
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(
                        "JSON", "RP", "HASH");
        boolean allowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid();
        assertThat(allowHybridActual).isTrue();
    }

    @Test
    public void constructor_setsAllowHybridToFalse() {
        boolean allowHybridExpected = false;
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged("testJson",
                        "RP", "Hash", allowHybridExpected);
        boolean getAllowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid();
        assertThat(getAllowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void builder_build_defaultAllowHybrid_success() {
        GetPublicKeyCredentialOptionPrivileged defaultPrivilegedRequest = new
                GetPublicKeyCredentialOptionPrivileged.Builder("{\"Data\":5}",
                "RP", "HASH").build();
        assertThat(defaultPrivilegedRequest.allowHybrid()).isTrue();
    }

    @Test
    public void builder_build_nonDefaultAllowHybrid_success() {
        boolean allowHybridExpected = false;
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged.Builder("testJson",
                        "RP", "Hash").setAllowHybrid(allowHybridExpected).build();
        boolean getAllowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid();
        assertThat(getAllowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(testJsonExpected, "RP", "HASH");
        String testJsonActual = getPublicKeyCredentialOptionPrivileged.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_rp_success() {
        String testRpExpected = "RP";
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        testRpExpected, "X342%4dfd7&");
        String testRpActual = getPublicKeyCredentialOptionPrivileged.getRp();
        assertThat(testRpActual).isEqualTo(testRpExpected);
    }

    @Test
    public void getter_clientDataHash_success() {
        String clientDataHashExpected = "X342%4dfd7&";
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        "RP", clientDataHashExpected);
        String clientDataHashActual = getPublicKeyCredentialOptionPrivileged.getClientDataHash();
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

        GetPublicKeyCredentialOptionPrivileged option =
                new GetPublicKeyCredentialOptionPrivileged(
                        requestJsonExpected, rpExpected, clientDataHashExpected,
                        allowHybridExpected);

        assertThat(option.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(option.getData(), expectedData)).isTrue();
        assertThat(option.getRequireSystemProvider()).isFalse();
    }
}

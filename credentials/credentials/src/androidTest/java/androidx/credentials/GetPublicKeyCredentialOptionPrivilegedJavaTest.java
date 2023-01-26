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

import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_ALLOW_HYBRID;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_RELYING_PARTY;
import static androidx.credentials.GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_REQUEST_JSON;

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
                        "RelyingParty", "ClientDataHash");
    }

    @Test
    public void constructor_setsAllowHybridToTrueByDefault() {
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(
                        "JSON", "RelyingParty", "HASH");
        boolean allowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid();
        assertThat(allowHybridActual).isTrue();
    }

    @Test
    public void constructor_setsAllowHybridToFalse() {
        boolean allowHybridExpected = false;
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged("testJson",
                        "RelyingParty", "Hash", allowHybridExpected);
        boolean getAllowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid();
        assertThat(getAllowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void builder_build_defaultAllowHybrid_success() {
        GetPublicKeyCredentialOptionPrivileged defaultPrivilegedRequest = new
                GetPublicKeyCredentialOptionPrivileged.Builder("{\"Data\":5}",
                "RelyingParty", "HASH").build();
        assertThat(defaultPrivilegedRequest.allowHybrid()).isTrue();
    }

    @Test
    public void builder_build_nonDefaultAllowHybrid_success() {
        boolean allowHybridExpected = false;
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged.Builder("testJson",
                        "RelyingParty", "Hash")
                        .setAllowHybrid(allowHybridExpected).build();
        boolean getAllowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid();
        assertThat(getAllowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(testJsonExpected,
                        "RelyingParty", "HASH");
        String testJsonActual = getPublicKeyCredentialOptionPrivileged.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_relyingParty_success() {
        String testRelyingPartyExpected = "RelyingParty";
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        testRelyingPartyExpected, "X342%4dfd7&");
        String testRelyingPartyActual = getPublicKeyCredentialOptionPrivileged.getRelyingParty();
        assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected);
    }

    @Test
    public void getter_clientDataHash_success() {
        String clientDataHashExpected = "X342%4dfd7&";
        GetPublicKeyCredentialOptionPrivileged getPublicKeyCredentialOptionPrivileged =
                new GetPublicKeyCredentialOptionPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        "RelyingParty", clientDataHashExpected);
        String clientDataHashActual = getPublicKeyCredentialOptionPrivileged.getClientDataHash();
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected);
    }

    @Test
    public void getter_frameworkProperties_success() {
        String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        String relyingPartyExpected = "RelyingParty";
        String clientDataHashExpected = "X342%4dfd7&";
        boolean allowHybridExpected = false;
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                GetPublicKeyCredentialOptionPrivileged
                        .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION_PRIVILEGED);
        expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putString(BUNDLE_KEY_RELYING_PARTY, relyingPartyExpected);
        expectedData.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHashExpected);
        expectedData.putBoolean(BUNDLE_KEY_ALLOW_HYBRID, allowHybridExpected);

        GetPublicKeyCredentialOptionPrivileged option =
                new GetPublicKeyCredentialOptionPrivileged(
                        requestJsonExpected, relyingPartyExpected, clientDataHashExpected,
                        allowHybridExpected);

        assertThat(option.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(option.getRequestData(), expectedData)).isTrue();
        assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), expectedData)).isTrue();
        assertThat(option.getRequireSystemProvider()).isFalse();
    }

    @Test
    public void frameworkConversion_success() {
        GetPublicKeyCredentialOptionPrivileged option =
                new GetPublicKeyCredentialOptionPrivileged("json", "rp", "clientDataHash", true);

        GetCredentialOption convertedOption = GetCredentialOption.createFrom(
                option.getType(), option.getRequestData(),
                option.getCandidateQueryData(), option.getRequireSystemProvider());

        assertThat(convertedOption).isInstanceOf(GetPublicKeyCredentialOptionPrivileged.class);
        GetPublicKeyCredentialOptionPrivileged convertedSubclassOption =
                (GetPublicKeyCredentialOptionPrivileged) convertedOption;
        assertThat(convertedSubclassOption.getRequestJson()).isEqualTo(option.getRequestJson());
        assertThat(convertedSubclassOption.allowHybrid()).isEqualTo(option.allowHybrid());
        assertThat(convertedSubclassOption.getClientDataHash())
                .isEqualTo(option.getClientDataHash());
        assertThat(convertedSubclassOption.getRelyingParty()).isEqualTo(option.getRelyingParty());
    }
}

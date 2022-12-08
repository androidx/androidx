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

import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_ALLOW_HYBRID;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_RELYING_PARTY;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_REQUEST_JSON;

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
                "relyingParty", "ClientDataHash");
    }

    @Test
    public void constructor_setsAllowHybridToTrueByDefault() {
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "JSON", "relyingParty", "HASH");
        boolean allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid();
        assertThat(allowHybridActual).isTrue();
    }

    @Test
    public void constructor_setsAllowHybridToFalse() {
        boolean allowHybridExpected = false;
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged("JSON",
                        "relyingParty",
                        "HASH",
                        allowHybridExpected);
        boolean allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid();
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void builder_build_defaultAllowHybrid_true() {
        CreatePublicKeyCredentialRequestPrivileged defaultPrivilegedRequest = new
                CreatePublicKeyCredentialRequestPrivileged.Builder("{\"Data\":5}",
                "relyingParty", "HASH").build();
        assertThat(defaultPrivilegedRequest.allowHybrid()).isTrue();
    }

    @Test
    public void builder_build_nonDefaultAllowHybrid_false() {
        boolean allowHybridExpected = false;
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged.Builder("JSON",
                        "relyingParty", "HASH")
                        .setAllowHybrid(allowHybridExpected).build();
        boolean allowHybridActual = createPublicKeyCredentialRequestPrivileged.allowHybrid();
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialReqPriv =
                new CreatePublicKeyCredentialRequestPrivileged(testJsonExpected,
                        "relyingParty", "HASH");
        String testJsonActual = createPublicKeyCredentialReqPriv.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_relyingParty_success() {
        String testrelyingPartyExpected = "relyingParty";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                        testrelyingPartyExpected, "X342%4dfd7&");
        String testrelyingPartyActual = createPublicKeyCredentialRequestPrivileged
                .getRelyingParty();
        assertThat(testrelyingPartyActual).isEqualTo(testrelyingPartyExpected);
    }

    @Test
    public void getter_clientDataHash_success() {
        String clientDataHashExpected = "X342%4dfd7&";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                         "relyingParty", clientDataHashExpected);
        String clientDataHashActual =
                createPublicKeyCredentialRequestPrivileged.getClientDataHash();
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected);
    }

    @Test
    public void getter_frameworkProperties_success() {
        String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        String relyingPartyExpected = "relyingParty";
        String clientDataHashExpected = "X342%4dfd7&";
        boolean allowHybridExpected = false;
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                CreatePublicKeyCredentialRequestPrivileged
                        .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIVILEGED);
        expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putString(BUNDLE_KEY_RELYING_PARTY, relyingPartyExpected);
        expectedData.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHashExpected);
        expectedData.putBoolean(BUNDLE_KEY_ALLOW_HYBRID, allowHybridExpected);

        CreatePublicKeyCredentialRequestPrivileged request =
                new CreatePublicKeyCredentialRequestPrivileged(
                        requestJsonExpected, relyingPartyExpected, clientDataHashExpected,
                        allowHybridExpected);

        assertThat(request.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(request.getCredentialData(), expectedData)).isTrue();
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(), expectedData)).isTrue();
        assertThat(request.getRequireSystemProvider()).isFalse();
    }

    @Test
    public void frameworkConversion_success() {
        CreatePublicKeyCredentialRequestPrivileged request =
                new CreatePublicKeyCredentialRequestPrivileged(
                        "json", "rp", "clientDataHash", true);

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), request.getCredentialData(),
                request.getCandidateQueryData(), request.getRequireSystemProvider()
        );

        assertThat(convertedRequest).isInstanceOf(CreatePublicKeyCredentialRequestPrivileged.class);
        CreatePublicKeyCredentialRequestPrivileged convertedSubclassRequest =
                (CreatePublicKeyCredentialRequestPrivileged) convertedRequest;
        assertThat(convertedSubclassRequest.getRequestJson()).isEqualTo(request.getRequestJson());
        assertThat(convertedSubclassRequest.getRelyingParty()).isEqualTo(request.getRelyingParty());
        assertThat(convertedSubclassRequest.getClientDataHash())
                .isEqualTo(request.getClientDataHash());
        assertThat(convertedSubclassRequest.allowHybrid()).isEqualTo(request.allowHybrid());
    }
}

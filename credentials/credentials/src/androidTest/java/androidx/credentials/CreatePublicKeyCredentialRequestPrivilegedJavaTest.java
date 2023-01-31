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

import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_RELYING_PARTY;
import static androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.BUNDLE_KEY_REQUEST_JSON;
import static androidx.credentials.internal.FrameworkImplHelper.getFinalCreateCredentialData;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Combines with {@link CreatePublicKeyCredentialRequestPrivilegedFailureInputsJavaTest}
 * for full tests.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreatePublicKeyCredentialRequestPrivilegedJavaTest {
    private static final String TEST_USERNAME = "test-user-name@gmail.com";
    private static final String TEST_USER_DISPLAYNAME = "Test User";
    private static final String TEST_REQUEST_JSON = String.format("{\"rp\":{\"name\":true,"
                    + "\"id\":\"app-id\"},\"user\":{\"name\":\"%s\",\"id\":\"id-value\","
                    + "\"displayName\":\"%s\",\"icon\":true}, \"challenge\":true,"
                    + "\"pubKeyCredParams\":true,\"excludeCredentials\":true,"
                    + "\"attestation\":true}", TEST_USERNAME,
            TEST_USER_DISPLAYNAME);
    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Test
    public void constructor_success() {
        new CreatePublicKeyCredentialRequestPrivileged(
                "{\"user\":{\"name\":{\"lol\":\"Value\"}}}",
                "relyingParty", "ClientDataHash");
    }

    @Test
    public void constructor_setPreferImmediatelyAvailableCredentialsToFalseByDefault() {
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        TEST_REQUEST_JSON, "relyingParty", "HASH");
        boolean preferImmediatelyAvailableCredentialsActual =
                createPublicKeyCredentialRequestPrivileged.preferImmediatelyAvailableCredentials();
        assertThat(preferImmediatelyAvailableCredentialsActual).isFalse();
    }

    @Test
    public void constructor_setPreferImmediatelyAvailableCredentialsToTrue() {
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(TEST_REQUEST_JSON,
                        "relyingParty",
                        "HASH",
                        preferImmediatelyAvailableCredentialsExpected);
        boolean preferImmediatelyAvailableCredentialsActual =
                createPublicKeyCredentialRequestPrivileged.preferImmediatelyAvailableCredentials();
        assertThat(preferImmediatelyAvailableCredentialsActual).isEqualTo(
                preferImmediatelyAvailableCredentialsExpected);
    }

    @Test
    public void builder_build_defaultPreferImmediatelyAvailableCredentials_false() {
        CreatePublicKeyCredentialRequestPrivileged defaultPrivilegedRequest = new
                CreatePublicKeyCredentialRequestPrivileged.Builder(TEST_REQUEST_JSON,
                "relyingParty", "HASH").build();
        assertThat(defaultPrivilegedRequest.preferImmediatelyAvailableCredentials()).isFalse();
    }

    @Test
    public void builder_build_nonDefaultPreferImmediatelyAvailableCredentials_true() {
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged.Builder(TEST_REQUEST_JSON,
                        "relyingParty", "HASH")
                        .setPreferImmediatelyAvailableCredentials(
                                preferImmediatelyAvailableCredentialsExpected).build();
        boolean preferImmediatelyAvailableCredentialsActual =
                createPublicKeyCredentialRequestPrivileged.preferImmediatelyAvailableCredentials();
        assertThat(preferImmediatelyAvailableCredentialsActual).isEqualTo(
                preferImmediatelyAvailableCredentialsExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"user\":{\"name\":{\"lol\":\"Value\"}}}";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialReqPriv =
                new CreatePublicKeyCredentialRequestPrivileged(testJsonExpected,
                        "relyingParty", "HASH");
        String testJsonActual = createPublicKeyCredentialReqPriv.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_relyingParty_success() {
        String testRelyingPartyExpected = "relyingParty";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        TEST_REQUEST_JSON, testRelyingPartyExpected, "X342%4dfd7&");
        String testRelyingPartyActual = createPublicKeyCredentialRequestPrivileged
                .getRelyingParty();
        assertThat(testRelyingPartyActual).isEqualTo(testRelyingPartyExpected);
    }

    @Test
    public void getter_clientDataHash_success() {
        String clientDataHashExpected = "X342%4dfd7&";
        CreatePublicKeyCredentialRequestPrivileged createPublicKeyCredentialRequestPrivileged =
                new CreatePublicKeyCredentialRequestPrivileged(
                        TEST_REQUEST_JSON, "relyingParty", clientDataHashExpected);
        String clientDataHashActual =
                createPublicKeyCredentialRequestPrivileged.getClientDataHash();
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected);
    }

    @SdkSuppress(minSdkVersion = 28)
    @SuppressWarnings("deprecation") // bundle.get(key)
    @Test
    public void getter_frameworkProperties_success() {
        String requestJsonExpected = TEST_REQUEST_JSON;
        String relyingPartyExpected = "relyingParty";
        String clientDataHashExpected = "X342%4dfd7&";
        boolean preferImmediatelyAvailableCredentialsExpected = false;
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                CreatePublicKeyCredentialRequestPrivileged
                        .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIV);
        expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putString(BUNDLE_KEY_RELYING_PARTY, relyingPartyExpected);
        expectedData.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHashExpected);
        expectedData.putBoolean(
                BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                preferImmediatelyAvailableCredentialsExpected);

        CreatePublicKeyCredentialRequestPrivileged request =
                new CreatePublicKeyCredentialRequestPrivileged(
                        requestJsonExpected, relyingPartyExpected, clientDataHashExpected,
                        preferImmediatelyAvailableCredentialsExpected);

        assertThat(request.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(), expectedData)).isTrue();
        assertThat(request.isSystemProviderRequired()).isFalse();
        Bundle credentialData = getFinalCreateCredentialData(
                request, mContext);
        assertThat(credentialData.keySet())
                .hasSize(expectedData.size() + /* added request info */ 1);
        for (String key : expectedData.keySet()) {
            assertThat(credentialData.get(key)).isEqualTo(credentialData.get(key));
        }
        Bundle displayInfoBundle =
                credentialData.getBundle(
                        CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO);
        assertThat(displayInfoBundle.keySet()).hasSize(3);
        assertThat(displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_ID)).isEqualTo(TEST_USERNAME);
        assertThat(displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_DISPLAY_NAME)).isEqualTo(
                TEST_USER_DISPLAYNAME);
        assertThat(((Icon) (displayInfoBundle.getParcelable(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON))).getResId()
        ).isEqualTo(R.drawable.ic_passkey);
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void frameworkConversion_success() {
        CreatePublicKeyCredentialRequestPrivileged request =
                new CreatePublicKeyCredentialRequestPrivileged(
                        TEST_REQUEST_JSON, "rp", "clientDataHash", true);

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), getFinalCreateCredentialData(
                        request, mContext),
                request.getCandidateQueryData(), request.isSystemProviderRequired()
        );

        assertThat(convertedRequest).isInstanceOf(CreatePublicKeyCredentialRequestPrivileged.class);
        CreatePublicKeyCredentialRequestPrivileged convertedSubclassRequest =
                (CreatePublicKeyCredentialRequestPrivileged) convertedRequest;
        assertThat(convertedSubclassRequest.getRequestJson()).isEqualTo(request.getRequestJson());
        assertThat(convertedSubclassRequest.getRelyingParty()).isEqualTo(request.getRelyingParty());
        assertThat(convertedSubclassRequest.getClientDataHash())
                .isEqualTo(request.getClientDataHash());
        assertThat(convertedSubclassRequest.preferImmediatelyAvailableCredentials()).isEqualTo(
                request.preferImmediatelyAvailableCredentials());
        CreateCredentialRequest.DisplayInfo displayInfo =
                convertedRequest.getDisplayInfo$credentials_debug();
        assertThat(displayInfo.getUserDisplayName()).isEqualTo(TEST_USER_DISPLAYNAME);
        assertThat(displayInfo.getUserId()).isEqualTo(TEST_USERNAME);
        assertThat(displayInfo.getCredentialTypeIcon().getResId())
                .isEqualTo(R.drawable.ic_passkey);
    }
}

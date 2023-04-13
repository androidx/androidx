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

import static androidx.credentials.CreateCredentialRequest.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS;
import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED;
import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_REQUEST_JSON;
import static androidx.credentials.internal.FrameworkImplHelper.getFinalCreateCredentialData;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreatePublicKeyCredentialRequestJavaTest {
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
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new CreatePublicKeyCredentialRequest("")
        );
    }

    @Test
    public void constructor_jsonMissingUserName_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreatePublicKeyCredentialRequest("json")
        );
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows("Expected null Json to throw NPE",
                NullPointerException.class,
                () -> new CreatePublicKeyCredentialRequest(null)
        );
    }

    @Test
    public void constructor_success() {
        new CreatePublicKeyCredentialRequest(
                "{\"user\":{\"name\":{\"lol\":\"Value\"}}}");
    }

    @Test
    public void constructor_defaultProviderVariant() {
        String clientDataHashExpected = "hash";
        String originExpected = "origin";
        Boolean preferImmediatelyAvailableCredentialsExpected = true;
        String defaultProviderExpected = "com.test/com.test.TestProviderComponent";

        CreatePublicKeyCredentialRequest request = new CreatePublicKeyCredentialRequest(
                TEST_REQUEST_JSON, clientDataHashExpected,
                preferImmediatelyAvailableCredentialsExpected, originExpected,
                defaultProviderExpected);

        assertThat(request.getDisplayInfo().getPreferDefaultProvider())
                .isEqualTo(defaultProviderExpected);
        assertThat(request.getClientDataHash()).isEqualTo(clientDataHashExpected);
        assertThat(request.getOrigin()).isEqualTo(originExpected);
        assertThat(request.getRequestJson()).isEqualTo(TEST_REQUEST_JSON);
        assertThat(request.preferImmediatelyAvailableCredentials())
                .isEqualTo(preferImmediatelyAvailableCredentialsExpected);
    }

    @Test
    public void constructor_setsPreferImmediatelyAvailableCredentialsToFalseByDefault() {
        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest =
                new CreatePublicKeyCredentialRequest(TEST_REQUEST_JSON);
        boolean preferImmediatelyAvailableCredentialsActual =
                createPublicKeyCredentialRequest.preferImmediatelyAvailableCredentials();
        assertThat(preferImmediatelyAvailableCredentialsActual).isFalse();
    }

    @Test
    public void constructor_setPreferImmediatelyAvailableCredentialsToTrue() {
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        String clientDataHash = "hash";
        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest =
                new CreatePublicKeyCredentialRequest(TEST_REQUEST_JSON,
                        clientDataHash,
                        preferImmediatelyAvailableCredentialsExpected);
        boolean preferImmediatelyAvailableCredentialsActual =
                createPublicKeyCredentialRequest.preferImmediatelyAvailableCredentials();
        assertThat(preferImmediatelyAvailableCredentialsActual).isEqualTo(
                preferImmediatelyAvailableCredentialsExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"user\":{\"name\":{\"lol\":\"Value\"}}}";
        CreatePublicKeyCredentialRequest createPublicKeyCredentialReq =
                new CreatePublicKeyCredentialRequest(testJsonExpected);

        String testJsonActual = createPublicKeyCredentialReq.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @SdkSuppress(minSdkVersion = 28)
    @SuppressWarnings("deprecation") // bundle.get(key)
    @Test
    public void getter_frameworkProperties_success() {
        String requestJsonExpected = TEST_REQUEST_JSON;
        String clientDataHash = "hash";
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        boolean autoSelectExpected = false;
        Bundle expectedCandidateQueryData = new Bundle();
        expectedCandidateQueryData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                CreatePublicKeyCredentialRequest
                        .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST);
        expectedCandidateQueryData.putString(
                BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedCandidateQueryData.putString(
                CreatePublicKeyCredentialRequest.BUNDLE_KEY_CLIENT_DATA_HASH,
                clientDataHash);
        expectedCandidateQueryData.putBoolean(
                BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
                autoSelectExpected);
        Bundle expectedCredentialData = expectedCandidateQueryData.deepCopy();
        expectedCredentialData.putBoolean(
                BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                preferImmediatelyAvailableCredentialsExpected);

        CreatePublicKeyCredentialRequest request = new CreatePublicKeyCredentialRequest(
                requestJsonExpected, clientDataHash, preferImmediatelyAvailableCredentialsExpected);

        assertThat(request.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(), expectedCandidateQueryData))
                .isTrue();
        assertThat(request.isSystemProviderRequired()).isFalse();
        Bundle credentialData = getFinalCreateCredentialData(
                request, mContext);
        assertThat(credentialData.keySet())
                .hasSize(expectedCredentialData.size() + /* added request info */ 1);
        for (String key : expectedCredentialData.keySet()) {
            assertThat(credentialData.get(key)).isEqualTo(expectedCredentialData.get(key));
        }
        Bundle displayInfoBundle =
                credentialData.getBundle(
                        CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO);
        assertThat(displayInfoBundle.keySet()).hasSize(3);
        assertThat(displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_ID)).isEqualTo(TEST_USERNAME);
        assertThat(displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_DISPLAY_NAME
        )).isEqualTo(TEST_USER_DISPLAYNAME);
        assertThat(((Icon) (displayInfoBundle.getParcelable(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON))).getResId()
        ).isEqualTo(R.drawable.ic_passkey);
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void frameworkConversion_success() {
        String clientDataHashExpected = "hash";
        String originExpected = "origin";
        Boolean preferImmediatelyAvailableCredentialsExpected = true;
        CreatePublicKeyCredentialRequest request = new CreatePublicKeyCredentialRequest(
                TEST_REQUEST_JSON, clientDataHashExpected,
                preferImmediatelyAvailableCredentialsExpected, originExpected);

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), getFinalCreateCredentialData(
                        request, mContext),
                request.getCandidateQueryData(), request.isSystemProviderRequired(),
                request.getOrigin()
        );

        assertThat(convertedRequest).isInstanceOf(CreatePublicKeyCredentialRequest.class);
        CreatePublicKeyCredentialRequest convertedSubclassRequest =
                (CreatePublicKeyCredentialRequest) convertedRequest;
        assertThat(convertedSubclassRequest.getRequestJson()).isEqualTo(request.getRequestJson());
        assertThat(convertedSubclassRequest.getOrigin()).isEqualTo(originExpected);
        assertThat(convertedSubclassRequest.getClientDataHash()).isEqualTo(clientDataHashExpected);
        assertThat(convertedSubclassRequest.preferImmediatelyAvailableCredentials())
                .isEqualTo(preferImmediatelyAvailableCredentialsExpected);
        CreateCredentialRequest.DisplayInfo displayInfo =
                convertedRequest.getDisplayInfo();
        assertThat(displayInfo.getUserDisplayName()).isEqualTo(TEST_USER_DISPLAYNAME);
        assertThat(displayInfo.getUserId()).isEqualTo(TEST_USERNAME);
        assertThat(displayInfo.getCredentialTypeIcon().getResId())
                .isEqualTo(R.drawable.ic_passkey);
    }
}

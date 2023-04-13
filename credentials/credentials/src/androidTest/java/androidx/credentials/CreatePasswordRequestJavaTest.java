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
public class CreatePasswordRequestJavaTest {
    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Test
    public void constructor_nullId_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreatePasswordRequest(null, "pwd")
        );
    }

    @Test
    public void constructor_nullPassword_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreatePasswordRequest("id", null)
        );
    }

    @Test
    public void constructor_withDefaults() {
        String idExpected = "id";
        String passwordExpected = "password";

        CreatePasswordRequest request = new CreatePasswordRequest(idExpected, passwordExpected);

        assertThat(request.getDisplayInfo().getPreferDefaultProvider()).isNull();
        assertThat(request.preferImmediatelyAvailableCredentials()).isFalse();
        assertThat(request.getOrigin()).isNull();
        assertThat(request.getId()).isEqualTo(idExpected);
        assertThat(request.getPassword()).isEqualTo(passwordExpected);
    }

    @Test
    public void constructor_withoutDefaults() {
        String idExpected = "id";
        String passwordExpected = "password";
        String originExpected = "origin";
        boolean preferImmediatelyAvailableCredentialsExpected = true;

        CreatePasswordRequest request = new CreatePasswordRequest(idExpected, passwordExpected,
                originExpected, preferImmediatelyAvailableCredentialsExpected);

        assertThat(request.preferImmediatelyAvailableCredentials())
                .isEqualTo(preferImmediatelyAvailableCredentialsExpected);
        assertThat(request.getDisplayInfo().getPreferDefaultProvider()).isNull();
        assertThat(request.getOrigin()).isEqualTo(originExpected);
        assertThat(request.getId()).isEqualTo(idExpected);
        assertThat(request.getPassword()).isEqualTo(passwordExpected);
    }

    @Test
    public void constructor_emptyPassword_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreatePasswordRequest("id", "")
        );
    }

    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    @Test
    public void constructor_defaultProviderVariant() {
        String idExpected = "id";
        String passwordExpected = "pwd";
        String originExpected = "origin";
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        String defaultProviderExpected = "com.test/com.test.TestProviderComponent";

        CreatePasswordRequest request = new CreatePasswordRequest(
                idExpected, passwordExpected, originExpected, defaultProviderExpected,
                preferImmediatelyAvailableCredentialsExpected);

        assertThat(request.getDisplayInfo().getPreferDefaultProvider())
                .isEqualTo(defaultProviderExpected);
        assertThat(request.preferImmediatelyAvailableCredentials())
                .isEqualTo(preferImmediatelyAvailableCredentialsExpected);
        assertThat(request.getOrigin()).isEqualTo(originExpected);
        assertThat(request.getId()).isEqualTo(idExpected);
        assertThat(request.getPassword()).isEqualTo(passwordExpected);
    }

    @Test
    public void getter_id() {
        String idExpected = "id";
        CreatePasswordRequest request = new CreatePasswordRequest(idExpected, "password");
        assertThat(request.getId()).isEqualTo(idExpected);
    }

    @Test
    public void getter_password() {
        String passwordExpected = "pwd";
        CreatePasswordRequest request = new CreatePasswordRequest("id", passwordExpected);
        assertThat(request.getPassword()).isEqualTo(passwordExpected);
    }

    @SdkSuppress(minSdkVersion = 28)
    @SuppressWarnings("deprecation") // bundle.get(key)
    @Test
    public void getter_frameworkProperties() {
        String idExpected = "id";
        String passwordExpected = "pwd";
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        Bundle expectedCredentialData = new Bundle();
        boolean expectedAutoSelect = false;
        expectedCredentialData.putString(CreatePasswordRequest.BUNDLE_KEY_ID, idExpected);
        expectedCredentialData.putString(CreatePasswordRequest.BUNDLE_KEY_PASSWORD,
                passwordExpected);
        expectedCredentialData.putBoolean(CreatePasswordRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
                expectedAutoSelect);
        expectedCredentialData.putBoolean(
                BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                preferImmediatelyAvailableCredentialsExpected);
        Bundle expectedCandidateData = new Bundle();
        expectedCandidateData.putBoolean(CreatePasswordRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
                expectedAutoSelect);

        CreatePasswordRequest request = new CreatePasswordRequest(idExpected, passwordExpected,
                /*origin=*/ null, preferImmediatelyAvailableCredentialsExpected);

        assertThat(request.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        CreateCredentialRequest.DisplayInfo displayInfo =
                request.getDisplayInfo();
        assertThat(displayInfo.getUserDisplayName()).isNull();
        assertThat(displayInfo.getUserId()).isEqualTo(idExpected);
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(), expectedCandidateData))
                .isTrue();
        assertThat(request.isSystemProviderRequired()).isFalse();
        Bundle credentialData =
                getFinalCreateCredentialData(
                        request, mContext);
        assertThat(credentialData.keySet())
                .hasSize(expectedCredentialData.size() + /* added request info */ 1);
        for (String key : expectedCredentialData.keySet()) {
            assertThat(credentialData.get(key)).isEqualTo(expectedCredentialData.get(key));
        }
        Bundle displayInfoBundle =
                credentialData.getBundle(
                        CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO);
        assertThat(displayInfoBundle.keySet()).hasSize(2);
        assertThat(displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_ID)).isEqualTo(idExpected);
        assertThat(((Icon) (displayInfoBundle.getParcelable(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON))).getResId()
        ).isEqualTo(R.drawable.ic_password);
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void frameworkConversion_success() {
        String idExpected = "id";
        String passwordExpected = "pwd";
        boolean preferImmediatelyAvailableCredentialsExpected = true;
        String originExpected = "origin";
        String defaultProviderExpected = "com.test/com.test.TestProviderComponent";
        CreatePasswordRequest request = new CreatePasswordRequest(
                idExpected, passwordExpected, originExpected, defaultProviderExpected,
                preferImmediatelyAvailableCredentialsExpected);

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), getFinalCreateCredentialData(
                        request, mContext),
                request.getCandidateQueryData(), request.isSystemProviderRequired(),
                request.getOrigin()
        );

        assertThat(convertedRequest).isInstanceOf(CreatePasswordRequest.class);
        CreatePasswordRequest convertedCreatePasswordRequest =
                (CreatePasswordRequest) convertedRequest;
        assertThat(convertedCreatePasswordRequest.getPassword()).isEqualTo(passwordExpected);
        assertThat(convertedCreatePasswordRequest.getId()).isEqualTo(idExpected);
        assertThat(convertedCreatePasswordRequest.preferImmediatelyAvailableCredentials())
                .isEqualTo(preferImmediatelyAvailableCredentialsExpected);
        assertThat(convertedCreatePasswordRequest.getOrigin()).isEqualTo(originExpected);
        CreateCredentialRequest.DisplayInfo displayInfo =
                convertedCreatePasswordRequest.getDisplayInfo();
        assertThat(displayInfo.getUserDisplayName()).isNull();
        assertThat(displayInfo.getUserId()).isEqualTo(idExpected);
        assertThat(displayInfo.getCredentialTypeIcon().getResId())
                .isEqualTo(R.drawable.ic_password);
        assertThat(displayInfo.getPreferDefaultProvider()).isEqualTo(defaultProviderExpected);
    }
}

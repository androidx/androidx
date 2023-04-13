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

import static androidx.credentials.CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED;
import static androidx.credentials.CreateCredentialRequest.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.test.filters.SdkSuppress;

import org.junit.Test;

public class CreateCustomCredentialRequestJavaTest {
    @Test
    public void constructor_nullType_throws() {
        assertThrows("Expected null type to throw NPE",
                NullPointerException.class,
                () -> new CreateCustomCredentialRequest(null, new Bundle(), new Bundle(),
                        false,
                        new CreateCredentialRequest.DisplayInfo("userId"),
                        false)
        );
    }

    @Test
    public void constructor_nullCredentialData_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreateCustomCredentialRequest("T", null, new Bundle(), false,
                        new CreateCredentialRequest.DisplayInfo("userId"), true)
        );
    }

    @Test
    public void constructor_nullCandidateQueryData_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreateCustomCredentialRequest("T", new Bundle(), null, false,
                        new CreateCredentialRequest.DisplayInfo("userId"),
                        true)
        );
    }

    @Test
    public void constructor_emptyType_throws() {
        assertThrows("Expected empty type to throw IAE",
                IllegalArgumentException.class,
                () -> new CreateCustomCredentialRequest("", new Bundle(), new Bundle(), false,
                        new CreateCredentialRequest.DisplayInfo("userId"),
                        false)
        );
    }

    @Test
    public void constructor_nonEmptyTypeNonNullBundle_success() {
        new CreateCustomCredentialRequest("T", new Bundle(), new Bundle(), false,
                new CreateCredentialRequest.DisplayInfo("userId"), true);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void getter() {
        String expectedType = "TYPE";
        boolean expectedAutoSelectAllowed = true;
        boolean expectedPreferImmediatelyAvailableCredentials = true;
        Bundle inputCredentialDataBundle = new Bundle();
        inputCredentialDataBundle.putString("Test", "Test");
        Bundle expectedCredentialDataBundle = inputCredentialDataBundle.deepCopy();
        expectedCredentialDataBundle.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
                expectedAutoSelectAllowed);
        expectedCredentialDataBundle.putBoolean(BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                expectedPreferImmediatelyAvailableCredentials);
        Bundle inputCandidateQueryDataBundle = new Bundle();
        inputCandidateQueryDataBundle.putBoolean("key", true);
        Bundle expectedCandidateQueryDataBundle = inputCandidateQueryDataBundle.deepCopy();
        expectedCandidateQueryDataBundle.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
                expectedAutoSelectAllowed);
        CreateCredentialRequest.DisplayInfo expectedDisplayInfo =
                new CreateCredentialRequest.DisplayInfo("userId");
        boolean expectedSystemProvider = true;
        String expectedOrigin = "Origin";

        CreateCustomCredentialRequest request = new CreateCustomCredentialRequest(expectedType,
                inputCredentialDataBundle,
                inputCandidateQueryDataBundle,
                expectedSystemProvider,
                expectedDisplayInfo,
                expectedAutoSelectAllowed,
                expectedOrigin,
                expectedPreferImmediatelyAvailableCredentials);

        assertThat(request.getType()).isEqualTo(expectedType);
        assertThat(TestUtilsKt.equals(request.getCredentialData(), expectedCredentialDataBundle))
                .isTrue();
        assertThat(TestUtilsKt.equals(request.getCustomRequestData(), expectedCredentialDataBundle))
                .isTrue();
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(),
                expectedCandidateQueryDataBundle)).isTrue();
        assertThat(TestUtilsKt.equals(request.getCustomRequestCandidateQueryData(),
                expectedCandidateQueryDataBundle)).isTrue();
        assertThat(request.isSystemProviderRequired()).isEqualTo(expectedSystemProvider);
        assertThat(request.isAutoSelectAllowed()).isEqualTo(expectedAutoSelectAllowed);
        assertThat(request.preferImmediatelyAvailableCredentials()).isEqualTo(
                expectedPreferImmediatelyAvailableCredentials);
        assertThat(request.getDisplayInfo()).isEqualTo(expectedDisplayInfo);
        assertThat(request.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(request.getCustomRequestOrigin()).isEqualTo(expectedOrigin);
    }

    @Test
    public void constructionWithNullRequestDisplayInfo_throws() {
        assertThrows(
                NullPointerException.class, () -> new CreateCustomCredentialRequest("type",
                        new Bundle(), new Bundle(), false,
                        /* requestDisplayInfo= */null, false));
    }


    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void frameworkConversion_success() {
        String expectedType = "TYPE";
        Bundle expectedCredentialDataBundle = new Bundle();
        expectedCredentialDataBundle.putString("Test", "Test");
        Bundle expectedCandidateQueryDataBundle = new Bundle();
        expectedCandidateQueryDataBundle.putBoolean("key", true);
        CreateCredentialRequest.DisplayInfo expectedDisplayInfo =
                new CreateCredentialRequest.DisplayInfo("userId");
        boolean expectedSystemProvider = true;
        boolean expectedAutoSelectAllowed = true;
        boolean expectedPreferImmediatelyAvailableCredentials = true;
        String expectedOrigin = "Origin";
        CreateCustomCredentialRequest request = new CreateCustomCredentialRequest(expectedType,
                expectedCredentialDataBundle,
                expectedCandidateQueryDataBundle,
                expectedSystemProvider,
                expectedDisplayInfo,
                expectedAutoSelectAllowed,
                expectedOrigin,
                expectedPreferImmediatelyAvailableCredentials);
        Bundle finalCredentialData = request.getCredentialData();
        finalCredentialData.putBundle(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO,
                expectedDisplayInfo.toBundle()
        );

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), request.getCredentialData(), request.getCandidateQueryData(),
                request.isSystemProviderRequired(), request.getOrigin());

        assertThat(convertedRequest).isInstanceOf(CreateCustomCredentialRequest.class);
        CreateCustomCredentialRequest actualRequest =
                (CreateCustomCredentialRequest) convertedRequest;
        assertThat(actualRequest.getType()).isEqualTo(expectedType);
        assertThat(actualRequest.getCustomRequestType()).isEqualTo(expectedType);
        assertThat(TestUtilsKt.equals(actualRequest.getCredentialData(),
                expectedCredentialDataBundle))
                .isTrue();
        assertThat(TestUtilsKt.equals(actualRequest.getCustomRequestData(),
                expectedCredentialDataBundle))
                .isTrue();
        assertThat(TestUtilsKt.equals(actualRequest.getCandidateQueryData(),
                expectedCandidateQueryDataBundle)).isTrue();
        assertThat(TestUtilsKt.equals(actualRequest.getCustomRequestCandidateQueryData(),
                expectedCandidateQueryDataBundle)).isTrue();
        assertThat(actualRequest.isSystemProviderRequired()).isEqualTo(expectedSystemProvider);
        assertThat(actualRequest.isAutoSelectAllowed()).isEqualTo(expectedAutoSelectAllowed);
        assertThat(actualRequest.getDisplayInfo().getUserId())
                .isEqualTo(expectedDisplayInfo.getUserId());
        assertThat(actualRequest.getDisplayInfo().getUserDisplayName())
                .isEqualTo(expectedDisplayInfo.getUserDisplayName());
        assertThat(actualRequest.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(actualRequest.getCustomRequestOrigin()).isEqualTo(expectedOrigin);
        assertThat(actualRequest.preferImmediatelyAvailableCredentials()).isEqualTo(
                expectedPreferImmediatelyAvailableCredentials);
    }
}

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

import static androidx.credentials.CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED;
import static androidx.credentials.CredentialOption.BUNDLE_KEY_TYPE_PRIORITY_VALUE;
import static androidx.credentials.GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class GetPublicKeyCredentialOptionJavaTest {
    private static final String TEST_REQUEST_JSON = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
    private static final @PriorityHints int EXPECTED_PASSKEY_PRIORITY =
            PriorityHints.PRIORITY_PASSKEY_OR_SIMILAR;


    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new GetPublicKeyCredentialOption("")
        );
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows("Expected null Json to throw NPE",
                NullPointerException.class,
                () -> new GetPublicKeyCredentialOption(null)
        );
    }

    @Test
    public void constructor_success() {
        new GetPublicKeyCredentialOption(TEST_REQUEST_JSON);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        GetPublicKeyCredentialOption getPublicKeyCredentialOpt =
                new GetPublicKeyCredentialOption(testJsonExpected);
        String testJsonActual = getPublicKeyCredentialOpt.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_defaultPriorityHint_success() {
        GetPublicKeyCredentialOption getPublicKeyCredentialOption = new
                GetPublicKeyCredentialOption(TEST_REQUEST_JSON);

        assertThat(getPublicKeyCredentialOption.getTypePriorityHint())
                .isEqualTo(EXPECTED_PASSKEY_PRIORITY);
    }

    @Test
    public void getter_frameworkProperties_success() {
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        boolean expectedIsAutoSelect = true;
        byte[] clientDataHash = "hash".getBytes();
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION);
        expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putByteArray(GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
                clientDataHash);
        expectedData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, expectedIsAutoSelect);
        expectedData.putInt(BUNDLE_KEY_TYPE_PRIORITY_VALUE, EXPECTED_PASSKEY_PRIORITY);

        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(
                requestJsonExpected, clientDataHash, expectedAllowedProviders);

        assertThat(option.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(option.getRequestData(), expectedData)).isTrue();
        assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), expectedData)).isTrue();
        assertThat(option.isSystemProviderRequired()).isFalse();
        assertThat(option.getAllowedProviders())
                .containsAtLeastElementsIn(expectedAllowedProviders);
        assertThat(option.getTypePriorityHint()).isEqualTo(EXPECTED_PASSKEY_PRIORITY);
    }

    @Test
    public void frameworkConversion_success() {
        byte[] clientDataHash = "hash".getBytes();
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(
                TEST_REQUEST_JSON, clientDataHash, expectedAllowedProviders);
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        Bundle requestData = option.getRequestData();
        String customRequestDataKey = "customRequestDataKey";
        String customRequestDataValue = "customRequestDataValue";
        requestData.putString(customRequestDataKey, customRequestDataValue);
        Bundle candidateQueryData = option.getCandidateQueryData();
        String customCandidateQueryDataKey = "customRequestDataKey";
        Boolean customCandidateQueryDataValue = true;
        candidateQueryData.putBoolean(customCandidateQueryDataKey, customCandidateQueryDataValue);

        CredentialOption convertedOption = CredentialOption.createFrom(
                option.getType(), requestData, candidateQueryData,
                option.isSystemProviderRequired(), option.getAllowedProviders());

        assertThat(convertedOption).isInstanceOf(GetPublicKeyCredentialOption.class);
        GetPublicKeyCredentialOption convertedSubclassOption =
                (GetPublicKeyCredentialOption) convertedOption;
        assertThat(convertedSubclassOption.getRequestJson()).isEqualTo(option.getRequestJson());
        assertThat(convertedSubclassOption.getAllowedProviders())
                .containsExactlyElementsIn(expectedAllowedProviders);
        assertThat(convertedOption.getRequestData().getString(customRequestDataKey))
                .isEqualTo(customRequestDataValue);
        assertThat(convertedOption.getCandidateQueryData().getBoolean(customCandidateQueryDataKey))
                .isEqualTo(customCandidateQueryDataValue);
        assertThat(option.getTypePriorityHint()).isEqualTo(EXPECTED_PASSKEY_PRIORITY);
    }
}

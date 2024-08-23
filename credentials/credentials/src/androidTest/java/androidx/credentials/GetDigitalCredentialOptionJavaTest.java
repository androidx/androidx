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

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@OptIn(markerClass = ExperimentalDigitalCredentialApi.class)
public class GetDigitalCredentialOptionJavaTest {
    private static final String TEST_REQUEST_JSON =
            "{\"protocol\":{\"preview\":{\"test\":\"val\"}}}";

    private static final int EXPECTED_PRIORITY =
            CredentialOption.PRIORITY_PASSKEY_OR_SIMILAR;

    @Test
    public void constructorAndGetter() {
        GetDigitalCredentialOption option = new GetDigitalCredentialOption(TEST_REQUEST_JSON);

        assertThat(option.getRequestJson()).isEqualTo(TEST_REQUEST_JSON);
        assertThat(option.getAllowedProviders()).isEmpty();
        assertThat(option.isSystemProviderRequired()).isFalse();
        assertThat(option.isAutoSelectAllowed()).isFalse();
        assertThat(option.getType()).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL);
        assertThat(option.getTypePriorityHint()).isEqualTo(EXPECTED_PRIORITY);
    }

    @Test
    public void frameworkConversion_success() {
        GetDigitalCredentialOption option = new GetDigitalCredentialOption(TEST_REQUEST_JSON);
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        Bundle requestData = option.getRequestData();
        String customRequestDataKey = "customRequestDataKey";
        String customRequestDataValue = "customRequestDataValue";
        requestData.putString(customRequestDataKey, customRequestDataValue);
        Bundle candidateQueryData = option.getCandidateQueryData();
        String customCandidateQueryDataKey = "customRequestDataKey";
        boolean customCandidateQueryDataValue = true;
        candidateQueryData.putBoolean(customCandidateQueryDataKey, customCandidateQueryDataValue);

        CredentialOption convertedOption = CredentialOption.createFrom(
                option.getType(), requestData, candidateQueryData,
                option.isSystemProviderRequired(), option.getAllowedProviders());

        assertThat(convertedOption).isInstanceOf(GetDigitalCredentialOption.class);
        GetDigitalCredentialOption actualOption = (GetDigitalCredentialOption) convertedOption;
        assertThat(actualOption.isAutoSelectAllowed()).isFalse();
        assertThat(actualOption.getAllowedProviders()).isEmpty();
        assertThat(actualOption.getRequestJson()).isEqualTo(TEST_REQUEST_JSON);
        assertThat(convertedOption.getRequestData().getString(customRequestDataKey))
                .isEqualTo(customRequestDataValue);
        assertThat(convertedOption.getCandidateQueryData().getBoolean(customCandidateQueryDataKey))
                .isEqualTo(customCandidateQueryDataValue);
        assertThat(convertedOption.getTypePriorityHint()).isEqualTo(EXPECTED_PRIORITY);
    }
}

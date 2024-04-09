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
public class GetPasswordOptionJavaTest {

    private static final @PriorityHints int EXPECTED_PASSWORD_PRIORITY =
            PriorityHints.PRIORITY_PASSWORD_OR_SIMILAR;

    @Test
    public void emptyConstructor_success() {
        GetPasswordOption option = new GetPasswordOption();

        assertThat(option.isAutoSelectAllowed()).isFalse();
        assertThat(option.getAllowedUserIds()).isEmpty();
        assertThat(option.getAllowedProviders()).isEmpty();
        assertThat(option.getTypePriorityHint()).isEqualTo(EXPECTED_PASSWORD_PRIORITY);
    }

    @Test
    public void construction_setOptionalValues_success() {
        boolean expectedIsAutoSelectAllowed = true;
        Set<String> expectedAllowedUserIds = ImmutableSet.of("id1", "id2", "id3");
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );

        GetPasswordOption option = new GetPasswordOption(
                expectedAllowedUserIds, expectedIsAutoSelectAllowed,
                expectedAllowedProviders);

        assertThat(option.isAutoSelectAllowed()).isEqualTo(expectedIsAutoSelectAllowed);
        assertThat(option.getAllowedUserIds()).containsExactlyElementsIn(expectedAllowedUserIds);
        assertThat(option.getAllowedProviders())
                .containsExactlyElementsIn(expectedAllowedProviders);
    }

    @Test
    public void getter_defaultPriorityHint_success() {
        GetPasswordOption option = new GetPasswordOption();

        assertThat(option.getTypePriorityHint()).isEqualTo(EXPECTED_PASSWORD_PRIORITY);
    }

    @Test
    public void getter_frameworkProperties() {
        Set<String> expectedAllowedUserIds = ImmutableSet.of("id1", "id2", "id3");
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        boolean expectedIsAutoSelectAllowed = true;
        int expectedPriorityCategoryValue = EXPECTED_PASSWORD_PRIORITY;

        GetPasswordOption option = new GetPasswordOption(expectedAllowedUserIds,
                expectedIsAutoSelectAllowed, expectedAllowedProviders);

        assertThat(option.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertThat(option.getRequestData().getBoolean(
                CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)).isTrue();
        assertThat(option.getRequestData().getStringArrayList(
                GetPasswordOption.BUNDLE_KEY_ALLOWED_USER_IDS))
                .containsExactlyElementsIn(expectedAllowedUserIds);
        assertThat(option.getCandidateQueryData().getBoolean(
                CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)).isTrue();
        assertThat(option.getCandidateQueryData().getStringArrayList(
                GetPasswordOption.BUNDLE_KEY_ALLOWED_USER_IDS))
                .containsExactlyElementsIn(expectedAllowedUserIds);
        assertThat(option.getRequestData().getInt(CredentialOption.BUNDLE_KEY_TYPE_PRIORITY_VALUE))
                .isEqualTo(expectedPriorityCategoryValue);
        assertThat(option.getCandidateQueryData().getInt(CredentialOption
                .BUNDLE_KEY_TYPE_PRIORITY_VALUE))
                .isEqualTo(expectedPriorityCategoryValue);
        assertThat(option.isSystemProviderRequired()).isFalse();
        assertThat(option.getAllowedProviders())
                .containsExactlyElementsIn(expectedAllowedProviders);
        assertThat(option.getTypePriorityHint()).isEqualTo(
                EXPECTED_PASSWORD_PRIORITY);
    }

    @Test
    public void frameworkConversion_success() {
        boolean expectedIsAutoSelectAllowed = true;
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        Set<String> expectedAllowedUserIds = ImmutableSet.of("id1", "id2", "id3");
        GetPasswordOption option = new GetPasswordOption(expectedAllowedUserIds,
                expectedIsAutoSelectAllowed, expectedAllowedProviders);
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

        assertThat(convertedOption).isInstanceOf(GetPasswordOption.class);
        GetPasswordOption getPasswordOption = (GetPasswordOption) convertedOption;
        assertThat(getPasswordOption.isAutoSelectAllowed()).isEqualTo(expectedIsAutoSelectAllowed);
        assertThat(getPasswordOption.getAllowedProviders())
                .containsExactlyElementsIn(expectedAllowedProviders);
        assertThat(getPasswordOption.getAllowedUserIds())
                .containsExactlyElementsIn(expectedAllowedUserIds);
        assertThat(convertedOption.getRequestData().getString(customRequestDataKey))
                .isEqualTo(customRequestDataValue);
        assertThat(convertedOption.getCandidateQueryData().getBoolean(customCandidateQueryDataKey))
                .isEqualTo(customCandidateQueryDataValue);
        assertThat(convertedOption.getTypePriorityHint()).isEqualTo(
                EXPECTED_PASSWORD_PRIORITY);
    }
}

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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GetPasswordOptionJavaTest {
    @Test
    public void emptyConstructor_success() {
        GetPasswordOption option = new GetPasswordOption();

        assertThat(option.isAutoSelectAllowed()).isFalse();
        assertThat(option.getAllowedUserIds()).isEmpty();
        assertThat(option.getAllowedProviders()).isEmpty();
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
    public void getter_frameworkProperties() {
        Set<String> expectedAllowedUserIds = ImmutableSet.of("id1", "id2", "id3");
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        boolean expectedIsAutoSelectAllowed = true;

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
        assertThat(option.isSystemProviderRequired()).isFalse();
        assertThat(option.getAllowedProviders())
                .containsExactlyElementsIn(expectedAllowedProviders);
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

        CredentialOption convertedOption = CredentialOption.createFrom(
                option.getType(), option.getRequestData(), option.getCandidateQueryData(),
                option.isSystemProviderRequired(),
                option.getAllowedProviders());

        assertThat(convertedOption).isInstanceOf(GetPasswordOption.class);
        GetPasswordOption getPasswordOption = (GetPasswordOption) convertedOption;
        assertThat(getPasswordOption.isAutoSelectAllowed()).isEqualTo(expectedIsAutoSelectAllowed);
        assertThat(getPasswordOption.getAllowedProviders())
                .containsExactlyElementsIn(expectedAllowedProviders);
        assertThat(option.getAllowedUserIds()).containsExactlyElementsIn(expectedAllowedUserIds);
    }
}

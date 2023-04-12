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

package androidx.credentials

import android.content.ComponentName
import androidx.credentials.CredentialOption.Companion.createFrom
import androidx.credentials.GetPasswordOption.Companion.BUNDLE_KEY_ALLOWED_USER_IDS
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetPasswordOptionTest {
    @Test
    fun emptyConstructor_success() {
        val option = GetPasswordOption()

        assertThat(option.isAutoSelectAllowed).isFalse()
        assertThat(option.allowedProviders).isEmpty()
        assertThat(option.allowedUserIds).isEmpty()
    }

    @Test
    fun construction_setOptionalValues_success() {
        val expectedIsAutoSelectAllowed = true
        val expectedAllowedProviders: Set<ComponentName> = setOf(
            ComponentName("pkg", "cls"),
            ComponentName("pkg2", "cls2")
        )
        val expectedAllowedUserIds: Set<String> = setOf("id1", "id2", "id3")

        val option = GetPasswordOption(
            allowedUserIds = expectedAllowedUserIds,
            isAutoSelectAllowed = expectedIsAutoSelectAllowed,
            allowedProviders = expectedAllowedProviders,
        )

        assertThat(option.isAutoSelectAllowed).isEqualTo(expectedIsAutoSelectAllowed)
        assertThat(option.allowedProviders)
            .containsExactlyElementsIn(expectedAllowedProviders)
        assertThat(option.allowedUserIds)
            .containsExactlyElementsIn(expectedAllowedUserIds)
    }

    @Test
    fun getter_frameworkProperties() {
        val expectedAllowedUserIds: Set<String> = setOf("id1", "id2", "id3")
        val expectedAllowedProviders: Set<ComponentName> = setOf(
            ComponentName("pkg", "cls"),
            ComponentName("pkg2", "cls2")
        )
        val expectedIsAutoSelectAllowed = true

        val option = GetPasswordOption(
            allowedUserIds = expectedAllowedUserIds,
            isAutoSelectAllowed = expectedIsAutoSelectAllowed,
            allowedProviders = expectedAllowedProviders,
        )

        assertThat(option.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertThat(option.requestData.getBoolean(
            CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)).isTrue()
        assertThat(option.requestData.getStringArrayList(
            BUNDLE_KEY_ALLOWED_USER_IDS)).containsExactlyElementsIn(expectedAllowedUserIds)
        assertThat(option.candidateQueryData.getBoolean(
            CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)).isTrue()
        assertThat(option.candidateQueryData.getStringArrayList(
            BUNDLE_KEY_ALLOWED_USER_IDS)).containsExactlyElementsIn(expectedAllowedUserIds)
        assertThat(option.isSystemProviderRequired).isFalse()
        assertThat(option.allowedProviders)
            .containsExactlyElementsIn(expectedAllowedProviders)
    }

    @Test
    fun frameworkConversion_success() {
        val expectedAllowedUserIds: Set<String> = setOf("id1", "id2", "id3")
        val expectedAutoSelectAllowed = true
        val expectedAllowedProviders: Set<ComponentName> = setOf(
            ComponentName("pkg", "cls"),
            ComponentName("pkg2", "cls2")
        )
        val option = GetPasswordOption(
            allowedUserIds = expectedAllowedUserIds,
            isAutoSelectAllowed = expectedAutoSelectAllowed,
            allowedProviders = expectedAllowedProviders,
        )

        val convertedOption = createFrom(
            option.type,
            option.requestData,
            option.candidateQueryData,
            option.isSystemProviderRequired,
            option.allowedProviders
        )

        assertThat(convertedOption).isInstanceOf(GetPasswordOption::class.java)
        assertThat(convertedOption.isAutoSelectAllowed).isEqualTo(expectedAutoSelectAllowed)
        assertThat(convertedOption.allowedProviders)
            .containsExactlyElementsIn(expectedAllowedProviders)
        assertThat((convertedOption as GetPasswordOption).allowedUserIds)
            .containsExactlyElementsIn(expectedAllowedUserIds)
    }
}
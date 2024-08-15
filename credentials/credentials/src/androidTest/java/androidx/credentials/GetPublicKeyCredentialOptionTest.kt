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
import android.os.Bundle
import androidx.credentials.CredentialOption.Companion.BUNDLE_KEY_TYPE_PRIORITY_VALUE
import androidx.credentials.CredentialOption.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetPublicKeyCredentialOptionTest {

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            GetPublicKeyCredentialOption("")
        }
    }

    @Test
    fun constructor_success() {
        GetPublicKeyCredentialOption(TEST_REQUEST_JSON)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(testJsonExpected)
        val testJsonActual = getPublicKeyCredentialOption.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_defaultPriorityHint_success() {
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(TEST_REQUEST_JSON)

        assertThat(getPublicKeyCredentialOption.typePriorityHint)
            .isEqualTo(EXPECTED_PASSKEY_PRIORITY)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val expectedAutoSelectAllowed = true
        val expectedAllowedProviders: Set<ComponentName> =
            setOf(ComponentName("pkg", "cls"), ComponentName("pkg2", "cls2"))
        val clientDataHash = "hash".toByteArray()
        val expectedData = Bundle()
        val expectedPriorityInt = EXPECTED_PASSKEY_PRIORITY
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION
        )
        expectedData.putString(
            GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected
        )
        expectedData.putInt(BUNDLE_KEY_TYPE_PRIORITY_VALUE, expectedPriorityInt)
        expectedData.putByteArray(
            GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash
        )
        expectedData.putBoolean(
            CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelectAllowed
        )

        val option =
            GetPublicKeyCredentialOption(
                requestJsonExpected,
                clientDataHash,
                expectedAllowedProviders
            )

        assertThat(option.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(option.requestData, expectedData)).isTrue()
        assertThat(equals(option.candidateQueryData, expectedData)).isTrue()
        assertThat(option.isSystemProviderRequired).isFalse()
        assertThat(option.isAutoSelectAllowed).isTrue()
        assertThat(option.allowedProviders).containsAtLeastElementsIn(expectedAllowedProviders)
        assertThat(option.typePriorityHint).isEqualTo(EXPECTED_PASSKEY_PRIORITY)
    }

    @Test
    fun frameworkConversion_success() {
        val clientDataHash = "hash".toByteArray()
        val expectedAllowedProviders: Set<ComponentName> =
            ImmutableSet.of(ComponentName("pkg", "cls"), ComponentName("pkg2", "cls2"))
        val option =
            GetPublicKeyCredentialOption(
                TEST_REQUEST_JSON,
                clientDataHash,
                expectedAllowedProviders
            )
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        val requestData = option.requestData
        val customRequestDataKey = "customRequestDataKey"
        val customRequestDataValue = "customRequestDataValue"
        requestData.putString(customRequestDataKey, customRequestDataValue)
        val candidateQueryData = option.candidateQueryData
        val customCandidateQueryDataKey = "customRequestDataKey"
        val customCandidateQueryDataValue = true
        candidateQueryData.putBoolean(customCandidateQueryDataKey, customCandidateQueryDataValue)

        val convertedOption =
            createFrom(
                option.type,
                requestData,
                candidateQueryData,
                option.isSystemProviderRequired,
                option.allowedProviders
            )

        assertThat(convertedOption).isInstanceOf(GetPublicKeyCredentialOption::class.java)
        val convertedSubclassOption = convertedOption as GetPublicKeyCredentialOption
        assertThat(convertedSubclassOption.requestJson).isEqualTo(option.requestJson)
        assertThat(convertedSubclassOption.allowedProviders)
            .containsExactlyElementsIn(expectedAllowedProviders)
        assertThat(convertedOption.requestData.getString(customRequestDataKey))
            .isEqualTo(customRequestDataValue)
        assertThat(convertedOption.candidateQueryData.getBoolean(customCandidateQueryDataKey))
            .isEqualTo(customCandidateQueryDataValue)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun frameworkConversion_frameworkClass_success() {
        val clientDataHash = "hash".toByteArray()
        val expectedAllowedProviders: Set<ComponentName> =
            ImmutableSet.of(ComponentName("pkg", "cls"), ComponentName("pkg2", "cls2"))
        val option =
            GetPublicKeyCredentialOption(
                TEST_REQUEST_JSON,
                clientDataHash,
                expectedAllowedProviders
            )
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        val requestData = option.requestData
        val customRequestDataKey = "customRequestDataKey"
        val customRequestDataValue = "customRequestDataValue"
        requestData.putString(customRequestDataKey, customRequestDataValue)
        val candidateQueryData = option.candidateQueryData
        val customCandidateQueryDataKey = "customRequestDataKey"
        val customCandidateQueryDataValue = true
        candidateQueryData.putBoolean(customCandidateQueryDataKey, customCandidateQueryDataValue)

        val convertedOption =
            createFrom(
                android.credentials.CredentialOption.Builder(
                        option.type,
                        requestData,
                        candidateQueryData
                    )
                    .setAllowedProviders(option.allowedProviders)
                    .setIsSystemProviderRequired(option.isSystemProviderRequired)
                    .build()
            )

        assertEquals(convertedOption, option)
    }

    companion object Constant {
        private const val TEST_REQUEST_JSON = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        const val EXPECTED_PASSKEY_PRIORITY = CredentialOption.PRIORITY_PASSKEY_OR_SIMILAR
    }
}

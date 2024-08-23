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

import androidx.credentials.CredentialOption.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalDigitalCredentialApi::class)
class GetDigitalCredentialOptionTest {
    @Test
    fun constructorAndGetter() {
        val option = GetDigitalCredentialOption(TEST_REQUEST_JSON)

        assertThat(option.requestJson).isEqualTo(TEST_REQUEST_JSON)
        assertThat(option.allowedProviders).isEmpty()
        assertThat(option.isSystemProviderRequired).isFalse()
        assertThat(option.isAutoSelectAllowed).isFalse()
        assertThat(option.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
        assertThat(option.typePriorityHint).isEqualTo(EXPECTED_PRIORITY)
    }

    @Test
    fun frameworkConversion_success() {
        val option = GetDigitalCredentialOption(TEST_REQUEST_JSON)
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

        assertThat(convertedOption).isInstanceOf(GetDigitalCredentialOption::class.java)
        val actualOption = convertedOption as GetDigitalCredentialOption
        assertThat(actualOption.isAutoSelectAllowed).isFalse()
        assertThat(actualOption.allowedProviders).isEmpty()
        assertThat(actualOption.requestJson).isEqualTo(TEST_REQUEST_JSON)
        assertThat(convertedOption.requestData.getString(customRequestDataKey))
            .isEqualTo(customRequestDataValue)
        assertThat(convertedOption.candidateQueryData.getBoolean(customCandidateQueryDataKey))
            .isEqualTo(customCandidateQueryDataValue)
        assertThat(convertedOption.typePriorityHint).isEqualTo(EXPECTED_PRIORITY)
    }

    companion object {
        private const val TEST_REQUEST_JSON = "{\"protocol\":{\"preview\":{\"test\":\"val\"}}}"

        private const val EXPECTED_PRIORITY = CredentialOption.PRIORITY_PASSKEY_OR_SIMILAR
    }
}

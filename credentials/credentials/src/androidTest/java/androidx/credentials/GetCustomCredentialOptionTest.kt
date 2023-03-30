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

import android.os.Bundle
import androidx.credentials.CredentialOption.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetCustomCredentialOptionTest {

    @Test
    fun constructor_emptyType_throws() {
        Assert.assertThrows(
            "Expected empty type to throw IAE",
            IllegalArgumentException::class.java
        ) {
            GetCustomCredentialOption(
                "",
                Bundle(),
                Bundle(),
                false,
                false
            )
        }
    }

    @Test
    fun constructor_nonEmptyTypeNonNullBundle_success() {
        GetCustomCredentialOption("T", Bundle(), Bundle(), true, true)
    }

    @Test
    fun getter_frameworkProperties() {
        val expectedType = "TYPE"
        val expectedBundle = Bundle()
        expectedBundle.putString("Test", "Test")
        val expectedCandidateQueryDataBundle = Bundle()
        expectedCandidateQueryDataBundle.putBoolean("key", true)
        val expectedAutoSelectAllowed = true
        val expectedSystemProvider = true

        val option = GetCustomCredentialOption(
            expectedType,
            expectedBundle,
            expectedCandidateQueryDataBundle,
            expectedSystemProvider,
            expectedAutoSelectAllowed
        )

        assertThat(option.type).isEqualTo(expectedType)
        assertThat(option.getCustomRequestType()).isEqualTo(expectedType)
        assertThat(equals(option.requestData, expectedBundle)).isTrue()
        assertThat(equals(option.getCustomRequestData(), expectedBundle)).isTrue()
        assertThat(
            equals(
                option.candidateQueryData,
                expectedCandidateQueryDataBundle
            )
        ).isTrue()
        assertThat(
            equals(
                option.getCustomRequestCandidateQueryData(),
                expectedCandidateQueryDataBundle
            )
        ).isTrue()
        assertThat(option.isAutoSelectAllowed).isEqualTo(expectedAutoSelectAllowed)
        assertThat(option.isSystemProviderRequired).isEqualTo(expectedSystemProvider)
    }

    @Test
    fun frameworkConversion_success() {
        val expectedType = "TYPE"
        val expectedBundle = Bundle()
        expectedBundle.putString("Test", "Test")
        val expectedCandidateQueryDataBundle = Bundle()
        expectedCandidateQueryDataBundle.putBoolean("key", true)
        val expectedSystemProvider = true
        val expectedAutoSelectAllowed = false
        val option = GetCustomCredentialOption(
            expectedType,
            expectedBundle,
            expectedCandidateQueryDataBundle,
            expectedSystemProvider,
            expectedAutoSelectAllowed
        )

        val convertedOption = createFrom(
            option.type, option.requestData, option.candidateQueryData,
            option.isSystemProviderRequired
        )

        assertThat(convertedOption).isInstanceOf(GetCustomCredentialOption::class.java)
        val actualOption = convertedOption as GetCustomCredentialOption
        assertThat(actualOption.getCustomRequestType()).isEqualTo(expectedType)
        assertThat(actualOption.type).isEqualTo(expectedType)
        assertThat(equals(actualOption.requestData, expectedBundle)).isTrue()
        assertThat(equals(actualOption.getCustomRequestData(), expectedBundle))
            .isTrue()
        assertThat(
            equals(
                actualOption.candidateQueryData,
                expectedCandidateQueryDataBundle
            )
        ).isTrue()
        assertThat(
            equals(
                actualOption.getCustomRequestCandidateQueryData(),
                expectedCandidateQueryDataBundle
            )
        ).isTrue()
        assertThat(actualOption.isAutoSelectAllowed).isEqualTo(expectedAutoSelectAllowed)
        assertThat(actualOption.isSystemProviderRequired).isEqualTo(expectedSystemProvider)
    }
}
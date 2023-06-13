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
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.credentials.CreateCredentialRequest.DisplayInfo
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CreateCustomCredentialRequestTest {
    @Test
    fun constructor_emptyType_throws() {
        assertThrows(
            "Expected empty type to throw IAE",
            IllegalArgumentException::class.java
        ) {
            CreateCustomCredentialRequest(
                "", Bundle(), Bundle(), false,
                DisplayInfo("userId"), false
            )
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun getter() {
        val expectedType = "TYPE"
        val expectedAutoSelectAllowed = true
        val expectedPreferImmediatelyAvailableCredentials = true
        val inputCredentialDataBundle = Bundle()
        inputCredentialDataBundle.putString("Test", "Test")
        val expectedCredentialDataBundle = inputCredentialDataBundle.deepCopy()
        expectedCredentialDataBundle.putBoolean(
            CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelectAllowed
        )
        expectedCredentialDataBundle.putBoolean(
            CreateCredentialRequest.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            expectedPreferImmediatelyAvailableCredentials
        )
        val inputCandidateQueryDataBundle = Bundle()
        inputCandidateQueryDataBundle.putBoolean("key", true)
        val expectedCandidateQueryDataBundle = inputCandidateQueryDataBundle.deepCopy()
        expectedCandidateQueryDataBundle.putBoolean(
            CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelectAllowed
        )
        val expectedDisplayInfo = DisplayInfo("userId")
        val expectedSystemProvider = true
        val expectedOrigin = "Origin"

        val request = CreateCustomCredentialRequest(
            expectedType,
            inputCredentialDataBundle,
            inputCandidateQueryDataBundle,
            expectedSystemProvider,
            expectedDisplayInfo,
            expectedAutoSelectAllowed,
            expectedOrigin,
            expectedPreferImmediatelyAvailableCredentials
        )

        assertThat(request.type).isEqualTo(expectedType)
        assertThat(equals(request.credentialData, expectedCredentialDataBundle))
            .isTrue()
        assertThat(
            equals(
                request.candidateQueryData,
                expectedCandidateQueryDataBundle
            )
        ).isTrue()
        assertThat(request.isSystemProviderRequired).isEqualTo(expectedSystemProvider)
        assertThat(request.isAutoSelectAllowed).isEqualTo(expectedAutoSelectAllowed)
        assertThat(request.preferImmediatelyAvailableCredentials).isEqualTo(
            expectedPreferImmediatelyAvailableCredentials
        )
        assertThat(request.displayInfo).isEqualTo(expectedDisplayInfo)
        assertThat(request.origin).isEqualTo(expectedOrigin)
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun frameworkConversion_success() {
        val expectedType = "TYPE"
        val expectedCredentialDataBundle = Bundle()
        expectedCredentialDataBundle.putString("Test", "Test")
        val expectedCandidateQueryDataBundle = Bundle()
        expectedCandidateQueryDataBundle.putBoolean("key", true)
        val expectedDisplayInfo = DisplayInfo("userId")
        val expectedSystemProvider = true
        val expectedAutoSelectAllowed = true
        val expectedPreferImmediatelyAvailableCredentials = true
        val expectedOrigin = "Origin"
        val request = CreateCustomCredentialRequest(
            expectedType,
            expectedCredentialDataBundle,
            expectedCandidateQueryDataBundle,
            expectedSystemProvider,
            expectedDisplayInfo,
            expectedAutoSelectAllowed,
            expectedOrigin,
            expectedPreferImmediatelyAvailableCredentials,
        )
        val finalCredentialData = request.credentialData
        finalCredentialData.putBundle(
            DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO,
            expectedDisplayInfo.toBundle()
        )

        val convertedRequest = createFrom(
            request.type, request.credentialData, request.candidateQueryData,
            request.isSystemProviderRequired, request.origin
        )!!

        assertThat(convertedRequest).isInstanceOf(CreateCustomCredentialRequest::class.java)
        val actualRequest = convertedRequest as CreateCustomCredentialRequest
        assertThat(actualRequest.type).isEqualTo(expectedType)
        assertThat(
            equals(
                actualRequest.credentialData,
                expectedCredentialDataBundle
            )
        ).isTrue()
        assertThat(
            equals(
                actualRequest.candidateQueryData,
                expectedCandidateQueryDataBundle
            )
        ).isTrue()
        assertThat(actualRequest.isSystemProviderRequired).isEqualTo(expectedSystemProvider)
        assertThat(actualRequest.isAutoSelectAllowed).isEqualTo(expectedAutoSelectAllowed)
        assertThat(actualRequest.displayInfo.userId)
            .isEqualTo(expectedDisplayInfo.userId)
        assertThat(actualRequest.displayInfo.userDisplayName)
            .isEqualTo(expectedDisplayInfo.userDisplayName)
        assertThat(actualRequest.origin).isEqualTo(expectedOrigin)
        assertThat(actualRequest.origin).isEqualTo(expectedOrigin)
        assertThat(actualRequest.preferImmediatelyAvailableCredentials).isEqualTo(
            expectedPreferImmediatelyAvailableCredentials
        )
    }
}
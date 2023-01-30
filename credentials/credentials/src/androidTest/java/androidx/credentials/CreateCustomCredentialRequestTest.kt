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
import androidx.credentials.CreateCredentialRequest.DisplayInfo
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
                DisplayInfo("userId")
            )
        }
    }

    @Test
    fun getter() {
        val expectedType = "TYPE"
        val expectedCredentialDataBundle = Bundle()
        expectedCredentialDataBundle.putString("Test", "Test")
        val expectedCandidateQueryDataBundle = Bundle()
        expectedCandidateQueryDataBundle.putBoolean("key", true)
        val expectedDisplayInfo = DisplayInfo("userId")
        val expectedSystemProvider = true

        val request = CreateCustomCredentialRequest(
            expectedType,
            expectedCredentialDataBundle,
            expectedCandidateQueryDataBundle,
            expectedSystemProvider,
            expectedDisplayInfo
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
        assertThat(request.displayInfo).isEqualTo(expectedDisplayInfo)
    }
}
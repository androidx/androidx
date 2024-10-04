/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider

import android.content.ComponentName
import android.content.pm.SigningInfo
import android.os.Bundle
import androidx.credentials.CredentialOption.Companion.createFrom
import androidx.credentials.assertEquals
import androidx.credentials.getTestCallingAppInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderGetCredentialRequestTest {

    @Test
    fun constructor_success() {
        ProviderGetCredentialRequest(
            listOf(createFrom("type", Bundle(), Bundle(), true, emptySet())),
            getTestCallingAppInfo(null)
        )
    }

    @Test
    fun constructor_createFrom_success() {
        ProviderGetCredentialRequest.createFrom(
            listOf(createFrom("type", Bundle(), Bundle(), true, emptySet())),
            getTestCallingAppInfo("origin"),
            null,
            null
        )
    }

    @Test
    fun getter_credentialOptions() {
        val expectedType = "BoeingCred"
        val expectedQueryKey = "PilotName"
        val expectedQueryValue = "PilotPassword"
        val expectedCandidateQueryData = Bundle()
        expectedCandidateQueryData.putString(expectedQueryKey, expectedQueryValue)
        val expectedRequestKey = "PlaneKey"
        val expectedRequestValue = "PlaneInfo"
        val expectedRequestData = Bundle()
        expectedRequestData.putString(expectedRequestKey, expectedRequestValue)
        val expectedRequireSystemProvider = true
        val expectedAllowedProviders: Set<ComponentName> =
            setOf(ComponentName("pkg", "cls"), ComponentName("pkg2", "cls2"))

        val providerGetCredentialRequest =
            ProviderGetCredentialRequest(
                listOf(
                    createFrom(
                        expectedType,
                        expectedRequestData,
                        expectedCandidateQueryData,
                        expectedRequireSystemProvider,
                        expectedAllowedProviders
                    )
                ),
                getTestCallingAppInfo(null)
            )
        val actualCredentialOptionsList = providerGetCredentialRequest.credentialOptions
        assertThat(actualCredentialOptionsList.size).isEqualTo(1)
        val actualType = actualCredentialOptionsList[0].type
        val actualRequestValue =
            actualCredentialOptionsList[0].requestData.getString(expectedRequestKey)
        val actualQueryValue =
            actualCredentialOptionsList[0].candidateQueryData.getString(expectedQueryKey)
        val actualRequireSystemProvider = actualCredentialOptionsList[0].isSystemProviderRequired

        assertThat(actualType).isEqualTo(expectedType)
        assertThat(actualRequestValue).isEqualTo(expectedRequestValue)
        assertThat(actualQueryValue).isEqualTo(expectedQueryValue)
        assertThat(actualRequireSystemProvider).isEqualTo(expectedRequireSystemProvider)
        assertThat(actualCredentialOptionsList[0].allowedProviders)
            .containsAtLeastElementsIn(expectedAllowedProviders)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun getter_signingInfo() {
        val expectedPackageName = "cool.security.package"

        val providerGetCredentialRequest =
            ProviderGetCredentialRequest(
                listOf(createFrom("type", Bundle(), Bundle(), true, emptySet())),
                CallingAppInfo(expectedPackageName, SigningInfo())
            )
        val actualPackageName = providerGetCredentialRequest.callingAppInfo.packageName

        assertThat(actualPackageName).isEqualTo(expectedPackageName)
    }

    @Test
    fun bundleConversion_success() {
        val request =
            ProviderGetCredentialRequest(
                listOf(createFrom("type", Bundle(), Bundle(), true, emptySet())),
                getTestCallingAppInfo("test-origin")
            )

        val actualRequest =
            ProviderGetCredentialRequest.fromBundle(ProviderGetCredentialRequest.asBundle(request))

        assertEquals(request, actualRequest)
    }

    @Test
    fun bundleConversion_emptyBundle_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderGetCredentialRequest.fromBundle(Bundle())
        }
    }
}

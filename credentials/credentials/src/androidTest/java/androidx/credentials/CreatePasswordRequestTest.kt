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

import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.credentials.internal.FrameworkImplHelper.Companion.getFinalCreateCredentialData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePasswordRequestTest {

    private val mContext = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun constructor_emptyPassword_throws() {
        assertThrows<IllegalArgumentException> {
            CreatePasswordRequest("id", "")
        }
    }

    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    @Test
    fun constructor_defaultProvider() {
        val defaultProvider = "com.test/com.test.TestProviderComponent"

        val request = CreatePasswordRequest(
            id = "id", password = "password", origin = null, preferDefaultProvider = defaultProvider
        )

        assertThat(request.displayInfo.preferDefaultProvider).isEqualTo(defaultProvider)
    }

    @Test
    fun getter_id() {
        val idExpected = "id"
        val request = CreatePasswordRequest(idExpected, "password")
        assertThat(request.id).isEqualTo(idExpected)
    }

    @Test
    fun getter_password() {
        val passwordExpected = "pwd"
        val request = CreatePasswordRequest("id", passwordExpected)
        assertThat(request.password).isEqualTo(passwordExpected)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Suppress("DEPRECATION") // bundle.get(key)
    @Test
    fun getter_frameworkProperties() {
        val idExpected = "id"
        val passwordExpected = "pwd"
        val expectedCredentialData = Bundle()
        val expectedAutoSelect = false
        expectedCredentialData.putString(CreatePasswordRequest.BUNDLE_KEY_ID, idExpected)
        expectedCredentialData.putString(
            CreatePasswordRequest.BUNDLE_KEY_PASSWORD,
            passwordExpected
        )
        expectedCredentialData.putBoolean(CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelect)
        val expectedCandidateData = Bundle()
        expectedCandidateData.putBoolean(
            CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelect
        )

        val request = CreatePasswordRequest(idExpected, passwordExpected)

        assertThat(request.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertThat(equals(request.candidateQueryData, expectedCandidateData)).isTrue()
        assertThat(request.isSystemProviderRequired).isFalse()
        assertThat(request.displayInfo.userDisplayName).isNull()
        assertThat(request.displayInfo.userId).isEqualTo(idExpected)
        val credentialData = getFinalCreateCredentialData(
            request, mContext
        )
        assertThat(credentialData.keySet())
            .hasSize(expectedCredentialData.size() + /* added request info */ 1)
        for (key in expectedCredentialData.keySet()) {
            assertThat(expectedCredentialData.get(key)).isEqualTo(credentialData.get(key))
        }
        val displayInfoBundle =
            credentialData.getBundle(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO)!!
        assertThat(displayInfoBundle.keySet()).hasSize(2)
        assertThat(displayInfoBundle.getString(
            CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_ID)).isEqualTo(idExpected)
        assertThat((displayInfoBundle.getParcelable(
            CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON) as Icon?)!!.resId
        ).isEqualTo(R.drawable.ic_password)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun frameworkConversion_success() {
        val idExpected = "id"
        val request = CreatePasswordRequest(idExpected, "password")
        val origin = "origin"

        val convertedRequest = createFrom(
            request.type, getFinalCreateCredentialData(
                request, mContext
            ),
            request.candidateQueryData, request.isSystemProviderRequired,
            origin
        )

        assertThat(convertedRequest).isInstanceOf(
            CreatePasswordRequest::class.java
        )
        val convertedCreatePasswordRequest = convertedRequest as CreatePasswordRequest
        assertThat(convertedCreatePasswordRequest.password).isEqualTo(request.password)
        assertThat(convertedCreatePasswordRequest.id).isEqualTo(request.id)
        assertThat(convertedCreatePasswordRequest.displayInfo.userDisplayName).isNull()
        assertThat(convertedCreatePasswordRequest.displayInfo.userId).isEqualTo(idExpected)
        assertThat(convertedCreatePasswordRequest.displayInfo.credentialTypeIcon?.resId)
            .isEqualTo(R.drawable.ic_password)
        assertThat(convertedCreatePasswordRequest.origin).isEqualTo(origin)
    }
}
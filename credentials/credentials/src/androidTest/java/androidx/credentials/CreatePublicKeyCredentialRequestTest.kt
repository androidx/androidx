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
import android.os.Parcelable
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_REQUEST_JSON
import androidx.credentials.internal.FrameworkImplHelper.Companion.getFinalCreateCredentialData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePublicKeyCredentialRequestTest {
    private val mContext = InstrumentationRegistry.getInstrumentation().context
    companion object Constant {
        private const val TEST_USERNAME = "test-user-name@gmail.com"
        private const val TEST_USER_DISPLAYNAME = "Test User"
        private const val TEST_REQUEST_JSON = "{\"rp\":{\"name\":true,\"id\":\"app-id\"}," +
            "\"user\":{\"name\":\"$TEST_USERNAME\",\"id\":\"id-value\",\"displayName" +
            "\":\"$TEST_USER_DISPLAYNAME\",\"icon\":true}, \"challenge\":true," +
            "\"pubKeyCredParams\":true,\"excludeCredentials\":true," + "\"attestation\":true}"
    }

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) { CreatePublicKeyCredentialRequest("") }
    }

    @Test
    fun constructor_jsonMissingUserName_throwsIllegalArgumentException() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { CreatePublicKeyCredentialRequest("json") }
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsToFalseByDefault() {
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            TEST_REQUEST_JSON
        )
        val preferImmediatelyAvailableCredentialsActual =
            createPublicKeyCredentialRequest.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual).isFalse()
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsToTrue() {
        val preferImmediatelyAvailableCredentialsExpected = true
        val origin = "origin"
        val clientDataHash = "hash"
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            TEST_REQUEST_JSON,
            clientDataHash,
            preferImmediatelyAvailableCredentialsExpected,
            origin
        )
        val preferImmediatelyAvailableCredentialsActual =
            createPublicKeyCredentialRequest.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual)
            .isEqualTo(preferImmediatelyAvailableCredentialsExpected)
        assertThat(createPublicKeyCredentialRequest.origin).isEqualTo(origin)
    }

    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    @Test
    fun constructor_defaultProvider() {
        val defaultProvider = "com.test/com.test.TestProviderComponent"

        val request = CreatePublicKeyCredentialRequest(
            requestJson = "{\"user\":{\"name\":{\"lol\":\"Value\"}}}",
            clientDataHash = null,
            preferImmediatelyAvailableCredentials = false,
            origin = null,
            preferDefaultProvider = defaultProvider
        )

        assertThat(request.displayInfo.preferDefaultProvider).isEqualTo(defaultProvider)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"user\":{\"name\":{\"lol\":\"Value\"}}}"
        val createPublicKeyCredentialReq = CreatePublicKeyCredentialRequest(testJsonExpected)
        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Suppress("DEPRECATION") // bundle.get(key)
    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = TEST_REQUEST_JSON
        val preferImmediatelyAvailableCredentialsExpected = false
        val autoSelectExpected = false
        val origin = "origin"
        val clientDataHash = "hash"
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            CreatePublicKeyCredentialRequest
                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST
        )
        expectedData.putString(
            BUNDLE_KEY_REQUEST_JSON, requestJsonExpected
        )
        expectedData.putString(CreatePublicKeyCredentialRequest.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash)
        expectedData.putBoolean(
            BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            preferImmediatelyAvailableCredentialsExpected
        )
        expectedData.putBoolean(
            CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            autoSelectExpected
        )

        val request = CreatePublicKeyCredentialRequest(
            requestJsonExpected,
            clientDataHash,
            preferImmediatelyAvailableCredentialsExpected,
            origin
        )

        assertThat(request.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(request.candidateQueryData, expectedData)).isTrue()
        assertThat(request.isSystemProviderRequired).isFalse()
        assertThat(request.origin).isEqualTo(origin)
        val credentialData = getFinalCreateCredentialData(
            request, mContext
        )
        assertThat(credentialData.keySet())
            .hasSize(expectedData.size() + /* added request info */1)
        for (key in expectedData.keySet()) {
            assertThat(credentialData[key]).isEqualTo(credentialData[key])
        }
        val displayInfoBundle = credentialData.getBundle(
            CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO
        )!!
        assertThat(displayInfoBundle.keySet()).hasSize(3)
        assertThat(
            displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_ID
            )
        ).isEqualTo(TEST_USERNAME)
        assertThat(
            displayInfoBundle.getString(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_DISPLAY_NAME
            )
        ).isEqualTo(TEST_USER_DISPLAYNAME)
        assertThat(
            (displayInfoBundle.getParcelable<Parcelable>(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON
            ) as Icon?)!!.resId
        ).isEqualTo(R.drawable.ic_passkey)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun frameworkConversion_success() {
        val origin = "origin"
        val clientDataHash = "hash"
        val request = CreatePublicKeyCredentialRequest(TEST_REQUEST_JSON, clientDataHash,
            true, origin)

        val convertedRequest = createFrom(
            request.type, getFinalCreateCredentialData(
                request, mContext
            ),
            request.candidateQueryData, request.isSystemProviderRequired,
            request.origin
        )

        assertThat(convertedRequest).isInstanceOf(
            CreatePublicKeyCredentialRequest::class.java
        )
        assertThat(convertedRequest?.origin).isEqualTo(origin)
        val convertedSubclassRequest = convertedRequest as CreatePublicKeyCredentialRequest
        assertThat(convertedSubclassRequest.requestJson).isEqualTo(request.requestJson)
        assertThat(convertedSubclassRequest.preferImmediatelyAvailableCredentials)
            .isEqualTo(request.preferImmediatelyAvailableCredentials)
        val displayInfo = convertedRequest.displayInfo
        assertThat(displayInfo.userDisplayName).isEqualTo(TEST_USER_DISPLAYNAME)
        assertThat(displayInfo.userId).isEqualTo(TEST_USERNAME)
        assertThat(displayInfo.credentialTypeIcon?.resId)
            .isEqualTo(R.drawable.ic_passkey)
    }
}
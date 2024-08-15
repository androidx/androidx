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
import androidx.credentials.CreateCredentialRequest.Companion.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_REQUEST_JSON
import androidx.credentials.internal.getFinalCreateCredentialData
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
        private const val TEST_REQUEST_JSON =
            "{\"rp\":{\"name\":true,\"id\":\"app-id\"}," +
                "\"user\":{\"name\":\"$TEST_USERNAME\",\"id\":\"id-value\",\"displayName" +
                "\":\"$TEST_USER_DISPLAYNAME\",\"icon\":true}, \"challenge\":true," +
                "\"pubKeyCredParams\":true,\"excludeCredentials\":true," +
                "\"attestation\":true}"
    }

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error", IllegalArgumentException::class.java) {
            CreatePublicKeyCredentialRequest("")
        }
    }

    @Test
    fun constructor_invalidJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error", IllegalArgumentException::class.java) {
            CreatePublicKeyCredentialRequest("invalid")
        }
    }

    @Test
    fun constructor_jsonMissingUserName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            CreatePublicKeyCredentialRequest("{\"hey\":{\"hi\":{\"hello\":\"hii\"}}}")
        }
    }

    @Test
    fun constructor_setsAutoSelectToFalseByDefault() {
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(TEST_REQUEST_JSON)

        assertThat(createPublicKeyCredentialRequest.isAutoSelectAllowed).isFalse()
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsToFalseByDefault() {
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(TEST_REQUEST_JSON)
        val preferImmediatelyAvailableCredentialsActual =
            createPublicKeyCredentialRequest.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual).isFalse()
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsToTrue() {
        val preferImmediatelyAvailableCredentialsExpected = true
        val origin = "origin"
        val clientDataHash = "hash".toByteArray()

        val createPublicKeyCredentialRequest =
            CreatePublicKeyCredentialRequest(
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

    @Test
    fun constructor_defaultProviderVariant() {
        val clientDataHashExpected = "hash".toByteArray()
        val originExpected = "origin"
        val preferImmediatelyAvailableCredentialsExpected = true
        val defaultProviderExpected = "com.test/com.test.TestProviderComponent"
        val isAutoSelectAllowedExpected = true

        val request =
            CreatePublicKeyCredentialRequest(
                TEST_REQUEST_JSON,
                clientDataHashExpected,
                preferImmediatelyAvailableCredentialsExpected,
                originExpected,
                defaultProviderExpected,
                isAutoSelectAllowedExpected,
            )

        assertThat(request.displayInfo.preferDefaultProvider).isEqualTo(defaultProviderExpected)
        assertThat(request.clientDataHash).isEqualTo(clientDataHashExpected)
        assertThat(request.origin).isEqualTo(originExpected)
        assertThat(request.requestJson).isEqualTo(TEST_REQUEST_JSON)
        assertThat(request.preferImmediatelyAvailableCredentials)
            .isEqualTo(preferImmediatelyAvailableCredentialsExpected)
        assertThat(request.isAutoSelectAllowed).isEqualTo(isAutoSelectAllowedExpected)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"user\":{\"name\":{\"lol\":\"Value\"}}}"
        val createPublicKeyCredentialReq = CreatePublicKeyCredentialRequest(testJsonExpected)
        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Suppress("DEPRECATION") // bundle.get(key)
    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = TEST_REQUEST_JSON
        val clientDataHash = "hash".toByteArray()
        val preferImmediatelyAvailableCredentialsExpected = true
        val autoSelectExpected = true
        val expectedCandidateQueryData = Bundle()
        expectedCandidateQueryData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            CreatePublicKeyCredentialRequest
                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST
        )
        expectedCandidateQueryData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected)
        expectedCandidateQueryData.putByteArray(
            CreatePublicKeyCredentialRequest.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash
        )
        expectedCandidateQueryData.putBoolean(
            CreateCredentialRequest.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            autoSelectExpected
        )
        val expectedCredentialData = expectedCandidateQueryData.deepCopy()
        expectedCredentialData.putBoolean(
            BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            preferImmediatelyAvailableCredentialsExpected
        )

        val request =
            CreatePublicKeyCredentialRequest(
                requestJsonExpected,
                clientDataHash,
                preferImmediatelyAvailableCredentialsExpected,
                origin = null,
                autoSelectExpected
            )

        assertThat(request.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(request.candidateQueryData, expectedCandidateQueryData)).isTrue()
        assertThat(request.isSystemProviderRequired).isFalse()
        val credentialData = getFinalCreateCredentialData(request, mContext)
        assertThat(credentialData.keySet())
            .hasSize(expectedCredentialData.size() + /* added request info */ 1)
        for (key in expectedCredentialData.keySet()) {
            assertThat(credentialData[key]).isEqualTo(expectedCredentialData[key])
        }
        val displayInfoBundle =
            credentialData.getBundle(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO
            )
        assertThat(displayInfoBundle!!.keySet()).hasSize(3)
        assertThat(
                displayInfoBundle.getString(CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_ID)
            )
            .isEqualTo(TEST_USERNAME)
        assertThat(
                displayInfoBundle.getString(
                    CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_USER_DISPLAY_NAME
                )
            )
            .isEqualTo(TEST_USER_DISPLAYNAME)
        assertThat(
                (displayInfoBundle.getParcelable<Parcelable>(
                        CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON
                    ) as Icon?)!!
                    .resId
            )
            .isEqualTo(R.drawable.ic_passkey)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun frameworkConversion_success() {
        val clientDataHashExpected = "hash".toByteArray()
        val originExpected = "origin"
        val preferImmediatelyAvailableCredentialsExpected = true
        val isAutoSelectAllowedExpected = true
        val request =
            CreatePublicKeyCredentialRequest(
                TEST_REQUEST_JSON,
                clientDataHashExpected,
                preferImmediatelyAvailableCredentialsExpected,
                originExpected,
                isAutoSelectAllowedExpected
            )
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        val credentialData = getFinalCreateCredentialData(request, mContext)
        val customRequestDataKey = "customRequestDataKey"
        val customRequestDataValue = "customRequestDataValue"
        credentialData.putString(customRequestDataKey, customRequestDataValue)
        val candidateQueryData = request.candidateQueryData
        val customCandidateQueryDataKey = "customRequestDataKey"
        val customCandidateQueryDataValue = true
        candidateQueryData.putBoolean(customCandidateQueryDataKey, customCandidateQueryDataValue)

        val convertedRequest =
            createFrom(
                request.type,
                credentialData,
                candidateQueryData,
                request.isSystemProviderRequired,
                request.origin
            )

        assertThat(convertedRequest).isInstanceOf(CreatePublicKeyCredentialRequest::class.java)
        val convertedSubclassRequest = convertedRequest as CreatePublicKeyCredentialRequest
        assertThat(convertedSubclassRequest.requestJson).isEqualTo(request.requestJson)
        assertThat(convertedSubclassRequest.origin).isEqualTo(originExpected)
        assertThat(convertedSubclassRequest.clientDataHash).isEqualTo(clientDataHashExpected)
        assertThat(convertedSubclassRequest.preferImmediatelyAvailableCredentials)
            .isEqualTo(preferImmediatelyAvailableCredentialsExpected)
        assertThat(convertedSubclassRequest.isAutoSelectAllowed)
            .isEqualTo(isAutoSelectAllowedExpected)
        val displayInfo = convertedSubclassRequest.displayInfo
        assertThat(displayInfo.userDisplayName).isEqualTo(TEST_USER_DISPLAYNAME)
        assertThat(displayInfo.userId).isEqualTo(TEST_USERNAME)
        assertThat(displayInfo.credentialTypeIcon!!.resId).isEqualTo(R.drawable.ic_passkey)
        assertThat(convertedRequest.credentialData.getString(customRequestDataKey))
            .isEqualTo(customRequestDataValue)
        assertThat(convertedRequest.candidateQueryData.getBoolean(customCandidateQueryDataKey))
            .isEqualTo(customCandidateQueryDataValue)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun frameworkConversion_frameworkClass_success() {
        val clientDataHashExpected = "hash".toByteArray()
        val originExpected = "origin"
        val preferImmediatelyAvailableCredentialsExpected = true
        val isAutoSelectAllowedExpected = true
        val request =
            CreatePublicKeyCredentialRequest(
                TEST_REQUEST_JSON,
                clientDataHashExpected,
                preferImmediatelyAvailableCredentialsExpected,
                originExpected,
                isAutoSelectAllowedExpected
            )
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        val credentialData = getFinalCreateCredentialData(request, mContext)
        val customRequestDataKey = "customRequestDataKey"
        val customRequestDataValue = "customRequestDataValue"
        credentialData.putString(customRequestDataKey, customRequestDataValue)
        val candidateQueryData = request.candidateQueryData
        val customCandidateQueryDataKey = "customRequestDataKey"
        val customCandidateQueryDataValue = true
        candidateQueryData.putBoolean(customCandidateQueryDataKey, customCandidateQueryDataValue)

        val convertedRequest =
            createFrom(
                android.credentials.CreateCredentialRequest.Builder(
                        request.type,
                        credentialData,
                        candidateQueryData
                    )
                    .setOrigin(originExpected)
                    .setIsSystemProviderRequired(request.isSystemProviderRequired)
                    .build()
            )

        assertThat(convertedRequest).isInstanceOf(CreatePublicKeyCredentialRequest::class.java)
        val convertedSubclassRequest = convertedRequest as CreatePublicKeyCredentialRequest
        assertThat(convertedSubclassRequest.requestJson).isEqualTo(request.requestJson)
        assertThat(convertedSubclassRequest.origin).isEqualTo(originExpected)
        assertThat(convertedSubclassRequest.clientDataHash).isEqualTo(clientDataHashExpected)
        assertThat(convertedSubclassRequest.preferImmediatelyAvailableCredentials)
            .isEqualTo(preferImmediatelyAvailableCredentialsExpected)
        assertThat(convertedSubclassRequest.isAutoSelectAllowed)
            .isEqualTo(isAutoSelectAllowedExpected)
        val displayInfo = convertedSubclassRequest.displayInfo
        assertThat(displayInfo.userDisplayName).isEqualTo(TEST_USER_DISPLAYNAME)
        assertThat(displayInfo.userId).isEqualTo(TEST_USERNAME)
        assertThat(displayInfo.credentialTypeIcon!!.resId).isEqualTo(R.drawable.ic_passkey)
        assertThat(convertedRequest.credentialData.getString(customRequestDataKey))
            .isEqualTo(customRequestDataValue)
        assertThat(convertedRequest.candidateQueryData.getBoolean(customCandidateQueryDataKey))
            .isEqualTo(customCandidateQueryDataValue)
    }
}

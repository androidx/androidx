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
class GetPublicKeyCredentialOptionTest {

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) { GetPublicKeyCredentialOption("") }
    }

    @Test
    fun constructor_success() {
        GetPublicKeyCredentialOption(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        )
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsToFalseByDefault() {
        val getPublicKeyCredentialOpt = GetPublicKeyCredentialOption(
            "JSON"
        )
        val preferImmediatelyAvailableCredentialsActual =
            getPublicKeyCredentialOpt.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual).isFalse()
    }

    @Test
    fun constructor_setPreferImmediatelyAvailableCredentialsTrue() {
        val preferImmediatelyAvailableCredentialsExpected = true
        val clientDataHash = "hash"
        val getPublicKeyCredentialOpt = GetPublicKeyCredentialOption(
            "JSON", clientDataHash, preferImmediatelyAvailableCredentialsExpected
        )
        val preferImmediatelyAvailableCredentialsActual =
            getPublicKeyCredentialOpt.preferImmediatelyAvailableCredentials
        assertThat(preferImmediatelyAvailableCredentialsActual)
            .isEqualTo(preferImmediatelyAvailableCredentialsExpected)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val createPublicKeyCredentialReq = GetPublicKeyCredentialOption(testJsonExpected)
        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val preferImmediatelyAvailableCredentialsExpected = false
        val expectedAutoSelectAllowed = true
        val clientDataHash = "hash"
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION
        )
        expectedData.putString(
            GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected
        )
        expectedData.putString(GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash)
        expectedData.putBoolean(
            GetPublicKeyCredentialOption.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            preferImmediatelyAvailableCredentialsExpected
        )
        expectedData.putBoolean(
            CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelectAllowed
        )

        val option = GetPublicKeyCredentialOption(
            requestJsonExpected, clientDataHash, preferImmediatelyAvailableCredentialsExpected
        )

        assertThat(option.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(option.requestData, expectedData)).isTrue()
        expectedData.remove(CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)
        assertThat(equals(option.candidateQueryData, expectedData)).isTrue()
        assertThat(option.isSystemProviderRequired).isFalse()
        assertThat(option.isAutoSelectAllowed).isTrue()
    }

    @Test
    fun frameworkConversion_success() {
        val clientDataHash = "hash"
        val option = GetPublicKeyCredentialOption("json", clientDataHash, true)

        val convertedOption = createFrom(
            option.type,
            option.requestData,
            option.candidateQueryData,
            option.isSystemProviderRequired
        )

        assertThat(convertedOption).isInstanceOf(
            GetPublicKeyCredentialOption::class.java
        )
        val convertedSubclassOption = convertedOption as GetPublicKeyCredentialOption
        assertThat(convertedSubclassOption.requestJson).isEqualTo(option.requestJson)
        assertThat(convertedSubclassOption.preferImmediatelyAvailableCredentials)
            .isEqualTo(option.preferImmediatelyAvailableCredentials)
    }
}
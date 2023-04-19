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
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val createPublicKeyCredentialReq = GetPublicKeyCredentialOption(testJsonExpected)
        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val expectedAutoSelectAllowed = true
        val expectedAllowedProviders: Set<ComponentName> = setOf(
            ComponentName("pkg", "cls"),
            ComponentName("pkg2", "cls2")
        )
        val clientDataHash = "hash".toByteArray()
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION
        )
        expectedData.putString(
            GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected
        )
        expectedData.putByteArray(GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash)
        expectedData.putBoolean(
            CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            expectedAutoSelectAllowed
        )

        val option = GetPublicKeyCredentialOption(
            requestJsonExpected, clientDataHash, expectedAllowedProviders
        )

        assertThat(option.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(option.requestData, expectedData)).isTrue()
        assertThat(equals(option.candidateQueryData, expectedData)).isTrue()
        assertThat(option.isSystemProviderRequired).isFalse()
        assertThat(option.isAutoSelectAllowed).isTrue()
        assertThat(option.allowedProviders).containsAtLeastElementsIn(expectedAllowedProviders)
    }

    @Test
    fun frameworkConversion_success() {
        val clientDataHash = "hash".toByteArray()
        val expectedAllowedProviders: Set<ComponentName> = setOf(
            ComponentName("pkg", "cls"),
            ComponentName("pkg2", "cls2")
        )
        val option = GetPublicKeyCredentialOption(
            "json", clientDataHash, expectedAllowedProviders)

        val convertedOption = createFrom(
            option.type,
            option.requestData,
            option.candidateQueryData,
            option.isSystemProviderRequired,
            option.allowedProviders,
        )

        assertThat(convertedOption).isInstanceOf(
            GetPublicKeyCredentialOption::class.java
        )
        val convertedSubclassOption = convertedOption as GetPublicKeyCredentialOption
        assertThat(convertedSubclassOption.requestJson).isEqualTo(option.requestJson)
        assertThat(convertedOption.allowedProviders)
            .containsAtLeastElementsIn(expectedAllowedProviders)
    }
}
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
import androidx.credentials.Credential.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Note that "PublicKeyCredential" and "Passkey" are used interchangeably.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class PublicKeyCredentialTest {

    @Test
    fun typeConstant() {
        assertThat(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
            .isEqualTo("androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL")
    }

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) { PublicKeyCredential("") }
    }

    @Test
    fun constructor_success() {
        PublicKeyCredential(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        )
    }

    @Test
    fun getter_authJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val publicKeyCredential = PublicKeyCredential(testJsonExpected)
        val testJsonActual = publicKeyCredential.authenticationResponseJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties() {
        val jsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON, jsonExpected
        )

        val publicKeyCredential = PublicKeyCredential(jsonExpected)

        assertThat(publicKeyCredential.type).isEqualTo(
            PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL
        )
        assertThat(equals(publicKeyCredential.data, expectedData)).isTrue()
    }

    @Test
    fun frameworkConversion_success() {
        val credential = PublicKeyCredential("json")

        val convertedCredential = createFrom(
            credential.type, credential.data
        )

        assertThat(convertedCredential).isInstanceOf(PublicKeyCredential::class.java)
        val convertedSubclassCredential = convertedCredential as PublicKeyCredential
        assertThat(convertedSubclassCredential.authenticationResponseJson)
            .isEqualTo(credential.authenticationResponseJson)
    }

    @Test
    fun staticProperty_hasCorrectTypeConstantValue() {
        val typeExpected = "androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"
        val typeActual = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL
        assertThat(typeActual).isEqualTo(typeExpected)
    }
}
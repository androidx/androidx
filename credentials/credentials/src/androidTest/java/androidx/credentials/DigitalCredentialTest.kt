/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.credentials.Credential.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DigitalCredentialTest {
    @Test
    fun typeConstant() {
        assertThat(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
            .isEqualTo("androidx.credentials.TYPE_DIGITAL_CREDENTIAL")
    }

    @Test
    fun constructor_emptyCredentialJson_throws() {
        assertThrows(IllegalArgumentException::class.java) { DigitalCredential("") }
    }

    @Test
    fun constructor_invalidCredentialJsonFormat_throws() {
        assertThrows(IllegalArgumentException::class.java) { DigitalCredential("hello") }
    }

    @Test
    fun constructorAndGetter() {
        val credential = DigitalCredential(TEST_CREDENTIAL_JSON)
        assertThat(credential.credentialJson).isEqualTo(TEST_CREDENTIAL_JSON)
    }

    @Test
    fun frameworkConversion_success() {
        val credential = DigitalCredential(TEST_CREDENTIAL_JSON)
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        val data = credential.data
        val customDataKey = "customRequestDataKey"
        val customDataValue: CharSequence = "customRequestDataValue"
        data.putCharSequence(customDataKey, customDataValue)

        val convertedCredential = createFrom(credential.type, data)

        assertThat(convertedCredential).isInstanceOf(DigitalCredential::class.java)
        val convertedSubclassCredential = convertedCredential as DigitalCredential
        assertThat(convertedSubclassCredential.credentialJson).isEqualTo(credential.credentialJson)
        assertThat(convertedCredential.data.getCharSequence(customDataKey))
            .isEqualTo(customDataValue)
    }

    companion object {
        private const val TEST_CREDENTIAL_JSON = "{\"protocol\":{\"preview\":{\"test\":\"val\"}}}"
    }
}

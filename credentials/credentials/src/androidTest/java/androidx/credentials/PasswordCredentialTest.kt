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
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PasswordCredentialTest {

    @Test
    fun typeConstant() {
        assertThat(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
            .isEqualTo("android.credentials.TYPE_PASSWORD_CREDENTIAL")
    }

    @Test
    fun constructor_emptyPassword_throws() {
        assertThrows<IllegalArgumentException> {
            PasswordCredential("id", "")
        }
    }

    @Test
    fun getter_id() {
        val idExpected = "id"
        val credential = PasswordCredential(idExpected, "password")
        assertThat(credential.id).isEqualTo(idExpected)
    }

    @Test
    fun getter_password() {
        val passwordExpected = "pwd"
        val credential = PasswordCredential("id", passwordExpected)
        assertThat(credential.password).isEqualTo(passwordExpected)
    }

    @Test
    fun getter_frameworkProperties() {
        val idExpected = "id"
        val passwordExpected = "pwd"
        val expectedData = Bundle()
        expectedData.putString(PasswordCredential.BUNDLE_KEY_ID, idExpected)
        expectedData.putString(PasswordCredential.BUNDLE_KEY_PASSWORD, passwordExpected)

        val credential = PasswordCredential(idExpected, passwordExpected)

        assertThat(credential.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertThat(equals(credential.data, expectedData)).isTrue()
    }

    @Test
    fun frameworkConversion_success() {
        val credential = PasswordCredential("id", "password")

        val convertedCredential = createFrom(
            credential.type, credential.data
        )

        assertThat(convertedCredential).isInstanceOf(PasswordCredential::class.java)
        val convertedSubclassCredential = convertedCredential as PasswordCredential
        assertThat(convertedSubclassCredential.password).isEqualTo(credential.password)
        assertThat(convertedSubclassCredential.id).isEqualTo(credential.id)
    }
}
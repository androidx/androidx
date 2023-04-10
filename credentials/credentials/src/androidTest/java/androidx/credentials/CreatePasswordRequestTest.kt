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
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePasswordRequestTest {
    @Test
    fun constructor_emptyPassword_throws() {
        assertThrows<IllegalArgumentException> {
            CreatePasswordRequest("id", "")
        }
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

    @Test
    fun getter_frameworkProperties() {
        val idExpected = "id"
        val passwordExpected = "pwd"
        val expectedData = Bundle()
        expectedData.putString(CreatePasswordRequest.BUNDLE_KEY_ID, idExpected)
        expectedData.putString(CreatePasswordRequest.BUNDLE_KEY_PASSWORD, passwordExpected)

        val request = CreatePasswordRequest(idExpected, passwordExpected)

        assertThat(request.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertThat(equals(request.credentialData, expectedData)).isTrue()
        assertThat(equals(request.candidateQueryData, Bundle.EMPTY)).isTrue()
        assertThat(request.requireSystemProvider).isFalse()
    }

    @Test
    fun frameworkConversion_success() {
        val request = CreatePasswordRequest("id", "password")

        val convertedRequest = createFrom(
            request.type, request.credentialData,
            request.candidateQueryData, request.requireSystemProvider
        )

        assertThat(convertedRequest).isInstanceOf(
            CreatePasswordRequest::class.java
        )
        val convertedCreatePasswordRequest = convertedRequest as CreatePasswordRequest
        assertThat(convertedCreatePasswordRequest.password).isEqualTo(request.password)
        assertThat(convertedCreatePasswordRequest.id).isEqualTo(request.id)
    }
}
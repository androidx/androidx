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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreateRestoreCredentialResponseTest {

    companion object Constant {
        private const val TEST_RESPONSE_JSON = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
    }

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            CreateRestoreCredentialResponse("")
        }
    }

    @Test
    fun constructor_invalidJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            CreateRestoreCredentialResponse("invalid")
        }
    }

    @Test
    fun constructor_success() {
        CreateRestoreCredentialResponse(TEST_RESPONSE_JSON)
    }

    @Test
    fun getter_registrationResponseJson_success() {
        val testJsonExpected = "{\"input\":5}"
        val createRestoreCredentialResponse = CreateRestoreCredentialResponse(testJsonExpected)
        val testJsonActual = createRestoreCredentialResponse.responseJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val registrationResponseJsonExpected = "{\"input\":5}"
        val expectedData = Bundle()
        expectedData.putString(
            CreateRestoreCredentialResponse.BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_RESPONSE,
            registrationResponseJsonExpected
        )

        val response = CreateRestoreCredentialResponse(registrationResponseJsonExpected)

        assertThat(response.type).isEqualTo(RestoreCredential.TYPE_RESTORE_CREDENTIAL)
        assertThat(equals(response.data, expectedData)).isTrue()
    }
}

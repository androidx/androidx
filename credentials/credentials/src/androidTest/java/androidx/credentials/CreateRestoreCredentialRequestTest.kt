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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreateRestoreCredentialRequestTest {
    companion object Constant {
        private const val TEST_USERNAME = "test-user-natcme@gmail.com"
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
            CreateRestoreCredentialRequest("")
        }
    }

    @Test
    fun constructor_invalidJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error", IllegalArgumentException::class.java) {
            CreateRestoreCredentialRequest("invalid")
        }
    }

    @Test
    fun constructor_jsonMissingUserName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            CreateRestoreCredentialRequest("{\"hey\":{\"hi\":{\"hello\":\"hii\"}}}")
        }
    }

    @Test
    fun constructor_setsIsCloudBackupEnabledByDefault() {
        val createRestoreCredentialRequest = CreateRestoreCredentialRequest(TEST_REQUEST_JSON)

        assertThat(createRestoreCredentialRequest.isCloudBackupEnabled).isTrue()
    }

    @Test
    fun constructor_setsIsCloudBackupEnabledToFalse() {
        val createRestoreCredentialRequest =
            CreateRestoreCredentialRequest(TEST_REQUEST_JSON, /* isCloudBackupEnabled= */ false)

        assertThat(createRestoreCredentialRequest.isCloudBackupEnabled).isFalse()
    }

    @Test
    fun getter_requestJson_success() {
        val createRestoreCredentialRequest = CreateRestoreCredentialRequest(TEST_REQUEST_JSON)

        assertThat(createRestoreCredentialRequest.requestJson).isEqualTo(TEST_REQUEST_JSON)
    }
}

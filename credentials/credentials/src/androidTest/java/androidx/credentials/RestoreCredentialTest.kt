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

@RunWith(AndroidJUnit4::class)
@SmallTest
class RestoreCredentialTest {

    companion object Constant {
        private const val TEST_JSON = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
    }

    @Test
    fun typeConstant() {
        assertThat(RestoreCredential.TYPE_RESTORE_CREDENTIAL)
            .isEqualTo("androidx.credentials.TYPE_RESTORE_CREDENTIAL")
    }

    @Test
    fun createFrom_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) {
            val bundle = Bundle()
            bundle.putString("androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE", "")
            RestoreCredential.createFrom(bundle)
        }
    }

    @Test
    fun createFrom_invalidJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected invalid Json to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) {
            val bundle = Bundle()
            bundle.putString(
                "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
                "invalid"
            )
            RestoreCredential.createFrom(bundle)
        }
    }

    @Test
    fun createFrom_success() {
        val bundle = Bundle()
        bundle.putString(
            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
            TEST_JSON
        )
        RestoreCredential.createFrom(bundle)
    }

    @Test
    fun getter_authJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val bundle = Bundle()
        bundle.putString(
            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
            testJsonExpected
        )
        val restoreCredential = RestoreCredential.createFrom(bundle)
        val testJsonActual = restoreCredential.authenticationResponseJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }
}

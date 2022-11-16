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

package androidx.credentials.exceptions.createpublickeycredential

import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDataException
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CreatePublicKeyCredentialDataExceptionTest {
    @Test(expected = CreatePublicKeyCredentialDataException::class)
    fun construct_inputNonEmpty_success() {
        throw CreatePublicKeyCredentialDataException(
            "msg"
        )
    }

    @Test(expected = CreatePublicKeyCredentialDataException::class)
    fun construct_errorMessageNull_success() {
        throw CreatePublicKeyCredentialDataException(null)
    }

    @Test
    fun getter_success() {
        val expectedMessage = "msg"
        val exception = CreatePublicKeyCredentialDataException(expectedMessage)
        val expectedType =
            CreatePublicKeyCredentialDataException.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DATA_EXCEPTION
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.message).isEqualTo(expectedMessage)
    }
}
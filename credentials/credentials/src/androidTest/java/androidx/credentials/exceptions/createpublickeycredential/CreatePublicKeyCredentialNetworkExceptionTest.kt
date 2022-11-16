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

import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialNetworkException
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CreatePublicKeyCredentialNetworkExceptionTest {
    @Test(expected = CreatePublicKeyCredentialNetworkException::class)
    fun construct_inputNonEmpty_success() {
        throw CreatePublicKeyCredentialNetworkException(
            "msg"
        )
    }

    @Test(expected = CreatePublicKeyCredentialNetworkException::class)
    fun construct_errorMessageNull_success() {
        throw CreatePublicKeyCredentialNetworkException(null)
    }

    @Test
    fun getter_success() {
        val expectedMessage = "msg"
        val exception = CreatePublicKeyCredentialNetworkException(expectedMessage)
        val expectedType =
            CreatePublicKeyCredentialNetworkException
                .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NETWORK_EXCEPTION
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.message).isEqualTo(expectedMessage)
    }
}

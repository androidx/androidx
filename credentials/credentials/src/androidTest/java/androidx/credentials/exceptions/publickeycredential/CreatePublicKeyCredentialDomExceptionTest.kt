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

package androidx.credentials.exceptions.publickeycredential

import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.domerrors.AbortError
import androidx.credentials.exceptions.domerrors.EncodingError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialException.Companion.createFrom
import androidx.credentials.exceptions.publickeycredential.DomExceptionUtils.Companion.SEPARATOR
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CreatePublicKeyCredentialDomExceptionTest {
    @Test(expected = CreatePublicKeyCredentialDomException::class)
    fun construct_inputNonEmpty_success() {
        throw CreatePublicKeyCredentialDomException(
            AbortError(), "msg"
        )
    }

    @Test
    fun getter_success() {
        val expectedMessage = "msg"
        val expectedDomError = EncodingError()
        val expectedType =
            CreatePublicKeyCredentialDomException.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION +
                SEPARATOR + expectedDomError.type

        val exception = CreatePublicKeyCredentialDomException(expectedDomError, expectedMessage)

        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.errorMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun frameworkToJetpackConversion_success() {
        val expectedMessage = "msg"
        val expectedDomError = EncodingError()
        val expectedType = CreatePublicKeyCredentialDomException
            .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION + SEPARATOR + expectedDomError.type

        val exception = CreatePublicKeyCredentialException.createFrom(expectedType,
            expectedMessage)

        assertThat(exception).isInstanceOf(
            CreatePublicKeyCredentialDomException::class.java
        )
        assertThat((exception as CreatePublicKeyCredentialDomException).domError)
            .isInstanceOf(EncodingError::class.java)
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.errorMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun frameworkToJetpackConversion_failure_createsCustomException() {
        val expectedMessage = "CustomMessage"
        val expectedType =
            CreatePublicKeyCredentialDomException.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION +
                "/CustomType"

        val exception = createFrom(expectedType, expectedMessage)

        assertThat(exception).isInstanceOf(
            CreateCredentialCustomException::class.java
        )
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.message).isEqualTo(expectedMessage)
    }
}

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

import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.domerrors.DomError
import androidx.credentials.exceptions.domerrors.EncodingError
import androidx.credentials.exceptions.publickeycredential.DomExceptionUtils.Companion.SEPARATOR
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialException.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetPublicKeyCredentialExceptionTest {

    @Test(expected = GetPublicKeyCredentialException::class)
    fun construct_inputsNonEmpty_success() {
        throw GetPublicKeyCredentialException("type", "msg")
    }

    @Test(expected = GetPublicKeyCredentialException::class)
    fun construct_errorMessageNull_success() {
        throw GetPublicKeyCredentialException("type", null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun construct_typeEmpty_throws() {
        throw GetPublicKeyCredentialException("", "msg")
    }

    @Test
    fun getter_success() {
        val expectedType = "type"
        val expectedMessage = "message"

        val exception = GetPublicKeyCredentialException(expectedType, expectedMessage)

        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.errorMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun frameworkToJetpackConversion_success() {
        val expectedMessage = "msg"
        val expectedDomError: DomError = EncodingError()
        val expectedType =
            GetPublicKeyCredentialDomException.TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION +
                SEPARATOR + expectedDomError.type

        val exception = createFrom(expectedType, expectedMessage)

        assertThat(exception).isInstanceOf(
            GetPublicKeyCredentialDomException::class.java
        )
        assertThat((exception as GetPublicKeyCredentialDomException).domError)
            .isInstanceOf(EncodingError::class.java)
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.message).isEqualTo(expectedMessage)
    }

    @Test
    fun frameworkToJetpackConversion_failure_createsCustomException() {
        val expectedMessage = "CustomMessage"
        val expectedType = "CustomType"

        val exception = createFrom(expectedType, expectedMessage)

        assertThat(exception).isInstanceOf(
            GetCredentialCustomException::class.java
        )
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.message).isEqualTo(expectedMessage)
    }
}

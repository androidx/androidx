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

package androidx.credentials.exceptions

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreateCredentialCustomExceptionTest {
    @Test(expected = CreateCredentialCustomException::class)
    fun construct_inputsNonEmpty_success() {
        throw CreateCredentialCustomException("type", "msg")
    }

    @Test(expected = CreateCredentialCustomException::class)
    fun construct_errorMessageNull_success() {
        throw CreateCredentialCustomException("type", null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun construct_typeEmpty_throws() {
        throw CreateCredentialCustomException("", "msg")
    }

    @Test
    fun getter_success() {
        val expectedType = "type"
        val expectedMessage = "message"
        val exception = CreateCredentialCustomException(expectedType, expectedMessage)
        assertThat(exception.type).isEqualTo(expectedType)
        assertThat(exception.errorMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun bundleConversion_withMessage_success() {
        val expectedType = "type"
        val expectedMessage = "message"
        val exception = CreateCredentialCustomException(expectedType, expectedMessage)

        val actual =
            CreateCredentialException.fromBundle(CreateCredentialException.asBundle(exception))

        assertThat(actual).isInstanceOf(CreateCredentialCustomException::class.java)
        assertThat(actual.type).isEqualTo(expectedType)
        assertThat(actual.errorMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun bundleConversion_withoutMessage_success() {
        val expectedType = "type"
        val expectedMessage = null
        val exception = CreateCredentialCustomException(expectedType, expectedMessage)

        val actual =
            CreateCredentialException.fromBundle(CreateCredentialException.asBundle(exception))

        assertThat(actual).isInstanceOf(CreateCredentialCustomException::class.java)
        assertThat(actual.type).isEqualTo(expectedType)
        assertThat(actual.errorMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun bundleConversion_emptyBundle_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            CreateCredentialException.fromBundle(Bundle())
        }
    }
}

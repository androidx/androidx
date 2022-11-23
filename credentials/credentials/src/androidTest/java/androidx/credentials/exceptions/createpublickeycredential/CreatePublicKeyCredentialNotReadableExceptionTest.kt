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

import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialNotReadableException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePublicKeyCredentialNotReadableExceptionTest {
    @Test(expected = CreatePublicKeyCredentialNotReadableException::class)
    fun construct_inputNonEmpty_success() {
        throw CreatePublicKeyCredentialNotReadableException("msg")
    }

    @Test(expected = CreatePublicKeyCredentialNotReadableException::class)
    fun construct_errorMessageNull_success() {
        throw CreatePublicKeyCredentialNotReadableException(null)
    }

    @Test
    fun getter_type_success() {
        val exception = CreatePublicKeyCredentialNotReadableException("msg")
        val expectedType =
            CreatePublicKeyCredentialNotReadableException
                .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NOT_READABLE_EXCEPTION
        Truth.assertThat(exception.type).isEqualTo(expectedType)
    }
}
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

package androidx.credentials.exceptions.createpublickeycredential;

import static com.google.common.truth.Truth.assertThat;

import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialEncodingException;

import org.junit.Test;

public class CreatePublicKeyCredentialEncodingExceptionJavaTest {
    @Test(expected = CreatePublicKeyCredentialEncodingException.class)
    public void construct_inputNonEmpty_success() throws
            CreatePublicKeyCredentialEncodingException {
        throw new CreatePublicKeyCredentialEncodingException(
                "msg");
    }

    @Test(expected = CreatePublicKeyCredentialEncodingException.class)
    public void construct_errorMessageNull_success() throws
            CreatePublicKeyCredentialEncodingException {
        throw new CreatePublicKeyCredentialEncodingException(null);
    }

    @Test
    public void getter_success() {
        String expectedMessage = "msg";
        CreatePublicKeyCredentialEncodingException exception = new
                CreatePublicKeyCredentialEncodingException(expectedMessage);
        String expectedType =
                CreatePublicKeyCredentialEncodingException
                        .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_ENCODING_EXCEPTION;
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}

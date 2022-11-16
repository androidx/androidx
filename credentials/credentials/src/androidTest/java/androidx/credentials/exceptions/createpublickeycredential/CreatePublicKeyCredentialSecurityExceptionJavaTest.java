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

import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialSecurityException;

import org.junit.Test;

public class CreatePublicKeyCredentialSecurityExceptionJavaTest {
    @Test(expected = CreatePublicKeyCredentialSecurityException.class)
    public void construct_inputNonEmpty_success() throws
            CreatePublicKeyCredentialSecurityException {
        throw new CreatePublicKeyCredentialSecurityException(
                "msg");
    }

    @Test(expected = CreatePublicKeyCredentialSecurityException.class)
    public void construct_errorMessageNull_success() throws
            CreatePublicKeyCredentialSecurityException {
        throw new CreatePublicKeyCredentialSecurityException(null);
    }

    @Test
    public void getter_success() {
        String expectedMessage = "msg";
        CreatePublicKeyCredentialSecurityException exception = new
                CreatePublicKeyCredentialSecurityException(expectedMessage);
        String expectedType =
                CreatePublicKeyCredentialSecurityException
                        .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_SECURITY_EXCEPTION;
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}

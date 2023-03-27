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

import androidx.credentials.exceptions.domerrors.AbortError;
import androidx.credentials.exceptions.domerrors.DomError;
import androidx.credentials.exceptions.domerrors.EncodingError;
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException;

import org.junit.Test;

public class CreatePublicKeyCredentialDomExceptionJavaTest {

    @Test(expected = CreatePublicKeyCredentialDomException.class)
    public void construct_inputNonEmpty_success() throws
            CreatePublicKeyCredentialDomException {
        throw new CreatePublicKeyCredentialDomException(
                new AbortError(), "msg");
    }

    @Test(expected = CreatePublicKeyCredentialDomException.class)
    public void construct_errorMessageNull_success() throws
            CreatePublicKeyCredentialDomException {
        throw new CreatePublicKeyCredentialDomException(new
                AbortError(), null);
    }

    @Test(expected = NullPointerException.class)
    public void construct_errorNull_failure() throws CreatePublicKeyCredentialDomException {
        throw new CreatePublicKeyCredentialDomException(null, "msg");
    }

    @Test
    public void getter_success() {
        String expectedMessage = "msg";
        DomError expectedDomError = new EncodingError();
        String expectedType =
                CreatePublicKeyCredentialDomException
                        .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION
                        + expectedDomError.getType();

        CreatePublicKeyCredentialDomException exception = new
                CreatePublicKeyCredentialDomException(expectedDomError, expectedMessage);

        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getErrorMessage()).isEqualTo(expectedMessage);
    }
}

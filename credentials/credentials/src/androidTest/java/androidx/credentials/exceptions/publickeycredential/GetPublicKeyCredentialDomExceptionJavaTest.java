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

package androidx.credentials.exceptions.publickeycredential;

import static androidx.credentials.exceptions.publickeycredential.DomExceptionUtils.SEPARATOR;

import static com.google.common.truth.Truth.assertThat;

import androidx.credentials.exceptions.GetCredentialCustomException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.domerrors.AbortError;
import androidx.credentials.exceptions.domerrors.DomError;
import androidx.credentials.exceptions.domerrors.EncodingError;

import org.junit.Test;

public class GetPublicKeyCredentialDomExceptionJavaTest {

    @Test(expected = GetPublicKeyCredentialDomException.class)
    public void construct_inputNonEmpty_success() throws
            GetPublicKeyCredentialDomException {
        throw new GetPublicKeyCredentialDomException(
                new AbortError(), "msg");
    }

    @Test(expected = GetPublicKeyCredentialDomException.class)
    public void construct_errorMessageNull_success() throws
            GetPublicKeyCredentialDomException {
        throw new GetPublicKeyCredentialDomException(new
                AbortError(), null);
    }

    @Test(expected = NullPointerException.class)
    public void construct_errorNull_failure() throws GetPublicKeyCredentialDomException {
        throw new GetPublicKeyCredentialDomException(null, "msg");
    }

    @Test
    public void getter_success() {
        String expectedMessage = "msg";
        DomError expectedDomError = new EncodingError();
        String expectedType =
                GetPublicKeyCredentialDomException
                        .TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION + SEPARATOR
                        + expectedDomError.getType();

        GetPublicKeyCredentialDomException exception = new
                GetPublicKeyCredentialDomException(expectedDomError, expectedMessage);

        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getErrorMessage()).isEqualTo(expectedMessage);
    }

    @Test
    public void frameworkToJetpackConversion_success() {
        String expectedMessage = "msg";
        DomError expectedDomError = new EncodingError();
        String expectedType = GetPublicKeyCredentialDomException
                .TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION + SEPARATOR
                + expectedDomError.getType();

        GetCredentialException exception = GetPublicKeyCredentialException
                .createFrom(expectedType, expectedMessage);

        assertThat(exception).isInstanceOf(GetPublicKeyCredentialDomException.class);
        assertThat(((GetPublicKeyCredentialDomException) exception).getDomError())
                .isInstanceOf(EncodingError.class);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    public void frameworkToJetpackConversion_failure_createsCustomException() {
        String expectedMessage = "CustomMessage";
        String expectedType =
                GetPublicKeyCredentialDomException
                        .TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION + "/CustomType";

        GetCredentialException exception = GetPublicKeyCredentialException
                .createFrom(expectedType, expectedMessage);

        assertThat(exception).isInstanceOf(GetCredentialCustomException.class);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}

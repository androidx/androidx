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
import androidx.credentials.exceptions.domerrors.DomError;
import androidx.credentials.exceptions.domerrors.EncodingError;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GetPublicKeyCredentialExceptionJavaTest {

    @Test(expected = GetPublicKeyCredentialException.class)
    public void construct_inputsNonEmpty_success() throws GetPublicKeyCredentialException {
        throw new GetPublicKeyCredentialException("type", "msg");
    }

    @Test(expected = GetPublicKeyCredentialException.class)
    public void construct_errorMessageNull_success() throws GetPublicKeyCredentialException {
        throw new GetPublicKeyCredentialException("type", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void construct_typeEmpty_throws() throws GetPublicKeyCredentialException {
        throw new GetPublicKeyCredentialException("", "msg");
    }

    @Test(expected = NullPointerException.class)
    public void construct_typeNull_throws() throws GetPublicKeyCredentialException {
        throw new GetPublicKeyCredentialException(null, "msg");
    }

    @Test
    public void getter_success() {
        String expectedType = "type";
        String expectedMessage = "message";
        GetPublicKeyCredentialException exception = new
                GetPublicKeyCredentialException(expectedType , expectedMessage);
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
        String expectedType = "CustomType";

        GetCredentialException exception = GetPublicKeyCredentialException
                .createFrom(expectedType, expectedMessage);

        assertThat(exception).isInstanceOf(GetCredentialCustomException.class);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}

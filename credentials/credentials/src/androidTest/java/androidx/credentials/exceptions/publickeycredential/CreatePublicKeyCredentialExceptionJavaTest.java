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

import androidx.credentials.exceptions.CreateCredentialCustomException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.domerrors.DataCloneError;
import androidx.credentials.exceptions.domerrors.DomError;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreatePublicKeyCredentialExceptionJavaTest {

    @Test(expected = CreatePublicKeyCredentialException.class)
    public void construct_inputsNonEmpty_success() throws CreatePublicKeyCredentialException {
        throw new CreatePublicKeyCredentialException("type", "msg");
    }

    @Test(expected = CreatePublicKeyCredentialException.class)
    public void construct_errorMessageNull_success() throws CreatePublicKeyCredentialException {
        throw new CreatePublicKeyCredentialException("type", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void construct_typeEmpty_throws() throws CreatePublicKeyCredentialException {
        throw new CreatePublicKeyCredentialException("", "msg");
    }

    @Test(expected = NullPointerException.class)
    public void construct_typeNull_throws() throws CreatePublicKeyCredentialException {
        throw new CreatePublicKeyCredentialException(null, "msg");
    }

    @Test
    public void getter_success() {
        String expectedType = "type";
        String expectedMessage = "message";
        CreatePublicKeyCredentialException exception = new
                CreatePublicKeyCredentialException(expectedType , expectedMessage);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getErrorMessage()).isEqualTo(expectedMessage);
    }

    @Test
    public void frameworkToJetpackConversion_success() {
        String expectedMessage = "msg";
        DomError expectedDomError = new DataCloneError();
        String expectedType = CreatePublicKeyCredentialDomException
                .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION + SEPARATOR
                + expectedDomError.getType();

        CreateCredentialException exception = CreatePublicKeyCredentialException
                .createFrom(expectedType, expectedMessage);

        assertThat(exception).isInstanceOf(CreatePublicKeyCredentialDomException.class);
        assertThat(((CreatePublicKeyCredentialDomException) exception).getDomError())
                .isInstanceOf(DataCloneError.class);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    public void frameworkToJetpackConversion_failure_createsCustomException() {
        String expectedMessage = "CustomMessage";
        String expectedType = "CustomType";

        CreateCredentialException exception = CreatePublicKeyCredentialException
                .createFrom(expectedType, expectedMessage);

        assertThat(exception.getClass()).isEqualTo(CreateCredentialCustomException.class);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}

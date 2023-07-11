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

package androidx.credentials.exceptions;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateCredentialCustomExceptionJavaTest {
    @Test(expected = CreateCredentialCustomException.class)
    public void construct_inputsNonEmpty_success() throws CreateCredentialCustomException {
        throw new CreateCredentialCustomException("type", "msg");
    }

    @Test(expected = CreateCredentialCustomException.class)
    public void construct_errorMessageNull_success() throws CreateCredentialCustomException {
        throw new CreateCredentialCustomException("type", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void construct_typeEmpty_throws() throws CreateCredentialCustomException {
        throw new CreateCredentialCustomException("", "msg");
    }

    @Test(expected = NullPointerException.class)
    public void construct_typeNull_throws() throws CreateCredentialCustomException {
        throw new CreateCredentialCustomException(null, "msg");
    }

    @Test
    public void getter_success() {
        String expectedType = "type";
        String expectedMessage = "message";
        CreateCredentialCustomException exception = new
                CreateCredentialCustomException(expectedType , expectedMessage);
        assertThat(exception.getType()).isEqualTo(expectedType);
        assertThat(exception.getErrorMessage()).isEqualTo(expectedMessage);
    }
}

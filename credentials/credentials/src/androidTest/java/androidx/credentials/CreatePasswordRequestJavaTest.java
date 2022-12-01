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

package androidx.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreatePasswordRequestJavaTest {
    @Test
    public void constructor_nullId_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreatePasswordRequest(null, "pwd")
        );
    }

    @Test
    public void constructor_nullPassword_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreatePasswordRequest("id", null)
        );
    }

    @Test
    public void constructor_emptyPassword_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreatePasswordRequest("id", "")
        );
    }

    @Test
    public void getter_id() {
        String idExpected = "id";
        CreatePasswordRequest request = new CreatePasswordRequest(idExpected, "password");
        assertThat(request.getId()).isEqualTo(idExpected);
    }

    @Test
    public void getter_password() {
        String passwordExpected = "pwd";
        CreatePasswordRequest request = new CreatePasswordRequest("id", passwordExpected);
        assertThat(request.getPassword()).isEqualTo(passwordExpected);
    }

    @Test
    public void getter_frameworkProperties() {
        String idExpected = "id";
        String passwordExpected = "pwd";
        Bundle expectedData = new Bundle();
        expectedData.putString(CreatePasswordRequest.BUNDLE_KEY_ID, idExpected);
        expectedData.putString(CreatePasswordRequest.BUNDLE_KEY_PASSWORD, passwordExpected);

        CreatePasswordRequest request = new CreatePasswordRequest(idExpected, passwordExpected);

        assertThat(request.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertThat(TestUtilsKt.equals(request.getCredentialData(), expectedData)).isTrue();
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(), Bundle.EMPTY)).isTrue();
        assertThat(request.getRequireSystemProvider()).isFalse();
    }

    @Test
    public void frameworkConversion_success() {
        CreatePasswordRequest request = new CreatePasswordRequest("id", "password");

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), request.getCredentialData(),
                request.getCandidateQueryData(), request.getRequireSystemProvider()
        );

        assertThat(convertedRequest).isInstanceOf(CreatePasswordRequest.class);
        CreatePasswordRequest convertedCreatePasswordRequest =
                (CreatePasswordRequest) convertedRequest;
        assertThat(convertedCreatePasswordRequest.getPassword()).isEqualTo(request.getPassword());
        assertThat(convertedCreatePasswordRequest.getId()).isEqualTo(request.getId());
    }
}

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
public class PasswordCredentialJavaTest {

    @Test
    public void typeConstant() {
        assertThat(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
                .isEqualTo("android.credentials.TYPE_PASSWORD_CREDENTIAL");
    }

    @Test
    public void constructor_nullId_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new PasswordCredential(null, "pwd")
        );
    }

    @Test
    public void constructor_nullPassword_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new PasswordCredential("id", null)
        );
    }

    @Test
    public void constructor_emptyPassword_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordCredential("id", "")
        );
    }

    @Test
    public void getter_id() {
        String idExpected = "id";
        PasswordCredential credential = new PasswordCredential(idExpected, "password");
        assertThat(credential.getId()).isEqualTo(idExpected);
    }

    @Test
    public void getter_password() {
        String passwordExpected = "pwd";
        PasswordCredential credential = new PasswordCredential("id", passwordExpected);
        assertThat(credential.getPassword()).isEqualTo(passwordExpected);
    }

    @Test
    public void getter_frameworkProperties() {
        String idExpected = "id";
        String passwordExpected = "pwd";
        Bundle expectedData = new Bundle();
        expectedData.putString(PasswordCredential.BUNDLE_KEY_ID, idExpected);
        expectedData.putString(PasswordCredential.BUNDLE_KEY_PASSWORD, passwordExpected);

        PasswordCredential credential = new PasswordCredential(idExpected, passwordExpected);

        assertThat(credential.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertThat(TestUtilsKt.equals(credential.getData(), expectedData)).isTrue();
    }

    @Test
    public void frameworkConversion_success() {
        PasswordCredential credential = new PasswordCredential("id", "password");

        Credential convertedCredential = Credential.createFrom(
                credential.getType(), credential.getData());

        assertThat(convertedCredential).isInstanceOf(PasswordCredential.class);
        PasswordCredential convertedSubclassCredential = (PasswordCredential) convertedCredential;
        assertThat(convertedSubclassCredential.getPassword()).isEqualTo(credential.getPassword());
        assertThat(convertedSubclassCredential.getId()).isEqualTo(credential.getId());
    }
}

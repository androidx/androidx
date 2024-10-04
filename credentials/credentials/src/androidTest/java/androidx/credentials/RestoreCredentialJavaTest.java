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
public class RestoreCredentialJavaTest {
    private static final String TEST_JSON = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";

    @Test
    public void typeConstant() {
        assertThat(RestoreCredential.TYPE_RESTORE_CREDENTIAL)
                .isEqualTo("androidx.credentials.TYPE_RESTORE_CREDENTIAL");
    }

    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(
                            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
                            ""
                    );
                    Credential.createFrom(RestoreCredential.TYPE_RESTORE_CREDENTIAL, bundle);
                }
        );
    }

    @Test
    public void constructor_success() {
        Bundle bundle = new Bundle();
        bundle.putString(
                "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
                TEST_JSON
        );
        Credential.createFrom(RestoreCredential.TYPE_RESTORE_CREDENTIAL, bundle);
    }

    @Test
    public void getter_authJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        Bundle bundle = new Bundle();
        bundle.putString(
                "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
                testJsonExpected
        );
        RestoreCredential credential = (RestoreCredential) Credential
                .createFrom(RestoreCredential.TYPE_RESTORE_CREDENTIAL, bundle);
        String testJsonActual = credential.getAuthenticationResponseJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }
}

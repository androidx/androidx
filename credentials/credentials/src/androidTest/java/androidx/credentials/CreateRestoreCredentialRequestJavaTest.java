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

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateRestoreCredentialRequestJavaTest {
    private static final String TEST_USERNAME = "test-user-name@gmail.com";
    private static final String TEST_USER_DISPLAYNAME = "Test User";
    private static final String TEST_REQUEST_JSON = String.format("{\"rp\":{\"name\":true,"
                    + "\"id\":\"app-id\"},\"user\":{\"name\":\"%s\",\"id\":\"id-value\","
                    + "\"displayName\":\"%s\",\"icon\":true}, \"challenge\":true,"
                    + "\"pubKeyCredParams\":true,\"excludeCredentials\":true,"
                    + "\"attestation\":true}", TEST_USERNAME,
            TEST_USER_DISPLAYNAME);

    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new CreateRestoreCredentialRequest("")
        );
    }

    @Test
    public void constructor_invalidJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new CreateRestoreCredentialRequest("invalid")
        );
    }

    @Test
    public void constructor_jsonMissingUserName_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateRestoreCredentialRequest(
                        "{\"key\":{\"value\":{\"lol\":\"Value\"}}}"
                )
        );
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows("Expected null Json to throw NPE",
                NullPointerException.class,
                () -> new CreateRestoreCredentialRequest(null)
        );
    }

    @Test
    public void constructor_success() {
        new CreateRestoreCredentialRequest(TEST_REQUEST_JSON);
    }

    @Test
    public void constructor_setsIsCloudBackupEnabledByDefault() {
        CreateRestoreCredentialRequest createRestoreCredentialRequest =
                new CreateRestoreCredentialRequest(TEST_REQUEST_JSON);

        assertThat(createRestoreCredentialRequest.isCloudBackupEnabled()).isTrue();
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = TEST_REQUEST_JSON;
        CreateRestoreCredentialRequest createRestoreCredentialRequest =
                new CreateRestoreCredentialRequest(testJsonExpected);

        String testJsonActual = createRestoreCredentialRequest.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }
}

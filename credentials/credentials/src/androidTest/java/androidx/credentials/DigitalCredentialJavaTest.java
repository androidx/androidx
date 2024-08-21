/*
 * Copyright 2024 The Android Open Source Project
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
public class DigitalCredentialJavaTest {
    private static final String TEST_CREDENTIAL_JSON =
            "{\"protocol\":{\"preview\":{\"test\":\"val\"}}}";
    @Test
    public void typeConstant() {
        assertThat(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
                .isEqualTo("androidx.credentials.TYPE_DIGITAL_CREDENTIAL");
    }

    @Test
    public void constructor_emptyCredentialJson_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DigitalCredential("")
        );
    }


    @Test
    public void constructor_invalidCredentialJsonFormat_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DigitalCredential("hello")
        );
    }

    @Test
    public void constructorAndGetter() {
        DigitalCredential credential = new DigitalCredential(TEST_CREDENTIAL_JSON);
        assertThat(credential.getCredentialJson()).isEqualTo(TEST_CREDENTIAL_JSON);
    }

    @Test
    public void frameworkConversion_success() {
        DigitalCredential credential = new DigitalCredential(TEST_CREDENTIAL_JSON);
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        Bundle data = credential.getData();
        String customDataKey = "customRequestDataKey";
        CharSequence customDataValue = "customRequestDataValue";
        data.putCharSequence(customDataKey, customDataValue);

        Credential convertedCredential = Credential.createFrom(
                credential.getType(), data);

        assertThat(convertedCredential).isInstanceOf(DigitalCredential.class);
        DigitalCredential convertedSubclassCredential = (DigitalCredential) convertedCredential;
        assertThat(convertedSubclassCredential.getCredentialJson())
                .isEqualTo(credential.getCredentialJson());
        assertThat(convertedCredential.getData().getCharSequence(customDataKey))
                .isEqualTo(customDataValue);
    }
}

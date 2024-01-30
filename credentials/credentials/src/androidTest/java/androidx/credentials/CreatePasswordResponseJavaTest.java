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

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreatePasswordResponseJavaTest {
    @Test
    public void getter_frameworkProperties() {
        CreatePasswordResponse response = new CreatePasswordResponse();

        assertThat(response.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertThat(TestUtilsKt.equals(response.getData(), Bundle.EMPTY)).isTrue();
    }

    @Test
    public void frameworkConversion_success() {
        CreatePasswordResponse response = new CreatePasswordResponse();
        // Add additional data to the request data and candidate query data to make sure
        // they persist after the conversion
        Bundle data = response.getData();
        String customDataKey = "customRequestDataKey";
        CharSequence customDataValue = "customRequestDataValue";
        data.putCharSequence(customDataKey, customDataValue);

        CreateCredentialResponse convertedResponse =
                CreateCredentialResponse.createFrom(response.getType(), data);

        assertThat(convertedResponse).isInstanceOf(CreatePasswordResponse.class);
        assertThat(convertedResponse.getData().getCharSequence(customDataKey))
                .isEqualTo(customDataValue);
    }
}

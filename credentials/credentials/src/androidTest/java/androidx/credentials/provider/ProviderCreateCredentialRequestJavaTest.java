/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider;


import static org.junit.Assert.assertThrows;

import android.content.pm.SigningInfo;

import androidx.credentials.CreatePasswordRequest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ProviderCreateCredentialRequestJavaTest {

    @Test
    public void constructor_success() {
        CreatePasswordRequest request = new CreatePasswordRequest("id", "password");

        new ProviderCreateCredentialRequest(request, new CallingAppInfo("name", new SigningInfo()));
    }

    @Test
    public void constructor_nullInputs_throws() {
        assertThrows(
                "Expected null list to throw NPE",
                NullPointerException.class,
                () -> new ProviderCreateCredentialRequest(null, null));
    }
}

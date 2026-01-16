/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.registry.provider;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.credentials.CreatePasswordRequest;
import androidx.credentials.provider.ProviderCreateCredentialRequest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ProviderCreateCredentialRequestJavaTest {
    @Test
    public void getSelectedEntryId_success() {
        ProviderCreateCredentialRequest request = new ProviderCreateCredentialRequest(
                new CreatePasswordRequest("id", "password"),
                TestUtilsKt.getTestCallingAppInfo(null)
        );
        Bundle requestBundle = ProviderCreateCredentialRequest.asBundle(request);
        requestBundle.putString(ProviderGetCredentialRequest.EXTRA_CREDENTIAL_ID, "id");

        ProviderCreateCredentialRequest actual =
                ProviderCreateCredentialRequest.fromBundle(requestBundle);

        assertThat(
                androidx.credentials.registry.provider.ProviderCreateCredentialRequest
                        .getSelectedEntryId(actual)).isEqualTo("id");
    }

    @Test
    public void getSelectedEntryId_doesNotExist_returnsNull() {
        ProviderCreateCredentialRequest request = new ProviderCreateCredentialRequest(
                new CreatePasswordRequest("id", "password"),
                TestUtilsKt.getTestCallingAppInfo(null)
        );

        assertThat(
                androidx.credentials.registry.provider.ProviderCreateCredentialRequest
                        .getSelectedEntryId(request)).isNull();
    }
}

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

package androidx.credentials.registry.provider;

import static androidx.credentials.registry.provider.ProviderGetCredentialRequest.EXTRA_CREDENTIAL_ID;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.credentials.GetPasswordOption;
import androidx.credentials.provider.ProviderGetCredentialRequest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ProviderGetCredentialRequestJavaTest {
    @Test
    public void selectedEntryId_success() {
        ProviderGetCredentialRequest request = new ProviderGetCredentialRequest(
                Collections.singletonList(new GetPasswordOption()),
                TestUtilsKt.getTestCallingAppInfo(null)
        );
        Bundle requestBundle = ProviderGetCredentialRequest.asBundle(request);
        requestBundle.putString(EXTRA_CREDENTIAL_ID, "id");

        ProviderGetCredentialRequest actual =
                ProviderGetCredentialRequest.fromBundle(requestBundle);

        assertThat(androidx.credentials.registry.provider.ProviderGetCredentialRequest
                .getSelectedEntryId(actual)).isEqualTo("id");
    }

    @Test
    public void selectedEntryId_doesNotExist_returnsNull() {
        ProviderGetCredentialRequest request = new ProviderGetCredentialRequest(
                Collections.singletonList(new GetPasswordOption()),
                TestUtilsKt.getTestCallingAppInfo(null)
        );

        assertThat(androidx.credentials.registry.provider.ProviderGetCredentialRequest
                .getSelectedEntryId(request)).isNull();
    }
}

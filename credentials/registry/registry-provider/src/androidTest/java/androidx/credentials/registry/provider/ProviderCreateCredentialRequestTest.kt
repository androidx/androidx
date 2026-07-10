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

package androidx.credentials.registry.provider

import androidx.credentials.CreatePasswordRequest
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderCreateCredentialRequestTest {
    @Test
    fun selectedEntryId_success() {
        val request =
            ProviderCreateCredentialRequest(
                CreatePasswordRequest("id", "password"),
                getTestCallingAppInfo(null),
            )
        val requestBundle = ProviderCreateCredentialRequest.asBundle(request)
        requestBundle.putString(EXTRA_CREDENTIAL_ID, "id")

        val actual = ProviderCreateCredentialRequest.fromBundle(requestBundle)

        assertThat(actual.selectedEntryId).isEqualTo("id")
    }

    @Test
    fun selectedEntryId_doesNotExist_returnsNull() {
        val request =
            ProviderCreateCredentialRequest(
                CreatePasswordRequest("id", "password"),
                getTestCallingAppInfo(null),
            )

        assertThat(request.selectedEntryId).isNull()
    }
}

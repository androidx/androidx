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

package androidx.credentials.provider

import android.os.Bundle
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.assertEquals
import androidx.credentials.getTestCallingAppInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderCreateCredentialRequestTest {

    @Test
    fun constructor_success() {
        val request = CreatePasswordRequest("id", "password")

        ProviderCreateCredentialRequest(request, getTestCallingAppInfo("origin"))
    }

    @Test
    fun bundleConversion_success() {
        val request =
            ProviderCreateCredentialRequest(
                CreatePasswordRequest("id", "password", "origin"),
                getTestCallingAppInfo("origin"),
            )

        val actualRequest =
            ProviderCreateCredentialRequest.fromBundle(
                ProviderCreateCredentialRequest.asBundle(request)
            )

        assertEquals(ApplicationProvider.getApplicationContext(), request, actualRequest)
    }

    @Test
    fun bundleConversion_emptyBundle_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderCreateCredentialRequest.fromBundle(Bundle())
        }
    }
}

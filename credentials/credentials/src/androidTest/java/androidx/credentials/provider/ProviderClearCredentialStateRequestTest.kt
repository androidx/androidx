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
import androidx.credentials.assertEquals
import androidx.credentials.equals
import androidx.credentials.getTestCallingAppInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderClearCredentialStateRequestTest {

    @Test
    fun testConstructor_success() {
        val callingAppInfo = getTestCallingAppInfo("origin")

        val request = ProviderClearCredentialStateRequest(callingAppInfo)

        assertThat(equals(callingAppInfo, request.callingAppInfo)).isTrue()
    }

    @Test
    fun bundleConversion_success() {
        val callingAppInfo = getTestCallingAppInfo("origin")
        val request = ProviderClearCredentialStateRequest(callingAppInfo)

        val actualRequest =
            ProviderClearCredentialStateRequest.fromBundle(
                ProviderClearCredentialStateRequest.asBundle(request)
            )

        assertEquals(request, actualRequest)
    }

    @Test
    fun bundleConversion_emptyBundle_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderClearCredentialStateRequest.fromBundle(Bundle())
        }
    }
}

/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import android.content.Intent
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UiDelegationFulfillmentServiceTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private class TestFulfillmentService(context: Context) : UiDelegationFulfillmentService() {
        init {
            attachBaseContext(context)
        }

        var lastRequest: ProviderGetCredentialRequest? = null
        var lastCallback: OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException>? =
            null

        override fun onGetCredentialRequest(
            request: ProviderGetCredentialRequest,
            callback: OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException>,
        ) {
            lastRequest = request
            lastCallback = callback
        }
    }

    @Test
    fun onBind_nullIntent_returnsNull() {
        val service = TestFulfillmentService(context)
        assertThat(service.onBind(null)).isNull()
    }

    @Test
    fun onBind_emptyIntent_returnsNull() {
        val service = TestFulfillmentService(context)
        val intent = Intent("androidx.credentials.action.GET_CREDENTIAL_SERVICE")
        assertThat(service.onBind(intent)).isNull()
    }

    @Test
    fun onGetCredentialRequest_parametersReceived() {
        val service = TestFulfillmentService(context)
        val request =
            ProviderGetCredentialRequest(emptyList(), getTestCallingAppInfo("https://example.com"))
        val callback =
            object : OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {}

                override fun onError(error: GetCredentialException) {}
            }

        service.onGetCredentialRequest(request, callback)

        assertThat(service.lastRequest).isSameInstanceAs(request)
        assertThat(service.lastCallback).isSameInstanceAs(callback)
    }
}

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

import android.content.pm.SigningInfo
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34)
@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialProviderServiceTest {

    private val LOG_TAG = "CredentialProviderServiceTest"

    @Test
    fun test_createRequest() {
        var service = CredentialProviderServiceTestImpl()
        service.isTestMode = true

        var request = android.service.credentials.BeginCreateCredentialRequest("test", Bundle())
        val outcome = OutcomeReceiver<android.service.credentials.BeginCreateCredentialResponse,
            android.credentials.CreateCredentialException> {
            fun onResult(response: android.service.credentials.BeginCreateCredentialResponse) {
                Log.i(LOG_TAG, "create request: " + response.toString())
            }

            fun onError(error: android.credentials.CreateCredentialException) {
                Log.e(LOG_TAG, "create request error", error)
            }
        }

        // Call the service.
        assertThat(service.lastCreateRequest).isNull()
        service.onBeginCreateCredential(request, CancellationSignal(), outcome)
        assertThat(service.lastCreateRequest).isNotNull()
    }

    @Test
    fun test_getRequest() {
        var service = CredentialProviderServiceTestImpl()
        service.isTestMode = true

        var option = android.service.credentials.BeginGetCredentialOption("id", "type", Bundle())
        var request = android.service.credentials.BeginGetCredentialRequest.Builder()
            .setBeginGetCredentialOptions(listOf(option)).build()
        val outcome = OutcomeReceiver<
            android.service.credentials.BeginGetCredentialResponse,
            android.credentials.GetCredentialException> {
            fun onResult(response: android.service.credentials.BeginGetCredentialResponse) {
                Log.i(LOG_TAG, "get request: " + response.toString())
            }

            fun onError(error: android.credentials.GetCredentialException) {
                Log.e(LOG_TAG, "get request error", error)
            }
        }

        // Call the service.
        assertThat(service.lastGetRequest).isNull()
        service.onBeginGetCredential(request, CancellationSignal(), outcome)
        assertThat(service.lastGetRequest).isNotNull()
    }

    @Test
    fun test_clearRequest() {
        var service = CredentialProviderServiceTestImpl()
        service.isTestMode = true

        var request = android.service.credentials.ClearCredentialStateRequest(
            android.service.credentials.CallingAppInfo("name", SigningInfo()), Bundle())
        val outcome = OutcomeReceiver<Void,
            android.credentials.ClearCredentialStateException> {
            fun onResult(response: Void) {
                Log.i(LOG_TAG, "clear request: " + response.toString())
            }

            fun onError(error: android.credentials.ClearCredentialStateException) {
                Log.e(LOG_TAG, "clear request error", error)
            }
        }

        // Call the service.
        assertThat(service.lastClearRequest).isNull()
        service.onClearCredentialState(request, CancellationSignal(), outcome)
        assertThat(service.lastClearRequest).isNotNull()
    }
}

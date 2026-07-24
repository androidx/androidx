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

package androidx.credentials.registry.provider.playservices

import android.content.Context
import android.os.Bundle
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.UiDelegationFulfillmentService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.identitycredentials.CallingAppInfoParcelable as GmsCallingAppInfoParcelable
import com.google.android.gms.identitycredentials.CredentialOption as GmsCredentialOption
import com.google.android.gms.identitycredentials.GetCredentialRequest as GmsGetCredentialRequest
import com.google.android.gms.identitycredentials.GetCredentialResponse as GmsGetCredentialResponse
import com.google.android.gms.identitycredentials.provider.IDelegatedCredentialService
import com.google.android.gms.identitycredentials.provider.IGetCredentialCallbacks
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalDigitalCredentialApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class UiDelegationFulfillmentProviderPlayServicesTest {

    private val provider = UiDelegationFulfillmentProviderPlayServices()

    private fun createFakeService(): UiDelegationFulfillmentService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return object : UiDelegationFulfillmentService() {
            init {
                attachBaseContext(context)
            }

            override fun onGetCredentialRequest(
                request: ProviderGetCredentialRequest,
                callback: OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException>,
            ) {}
        }
    }

    @Test
    fun getStubImplementation_returnsNonNullBinder() {
        val fakeService = createFakeService()
        val binder = provider.getStubImplementation(fakeService)
        assertThat(binder).isNotNull()
    }

    @Test
    fun onGetCredentialRequest_nonGooglePlayServicesCaller_failsWithConfigurationException() {
        val fakeService = createFakeService()
        val binder = provider.getStubImplementation(fakeService)!!
        val stub = IDelegatedCredentialService.Stub.asInterface(binder)

        val gmsOption =
            GmsCredentialOption(
                "androidx.credentials.TYPE_DIGITAL_CREDENTIAL",
                Bundle.EMPTY,
                Bundle.EMPTY,
                "",
                "",
                "",
            )
        val gmsRequest =
            GmsGetCredentialRequest(
                listOf(gmsOption),
                Bundle.EMPTY,
                "https://origin.example.com",
                android.os.ResultReceiver(null),
            )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callingAppInfo =
            GmsCallingAppInfoParcelable(
                context.packageName,
                emptyList(),
                "https://origin.example.com",
            )

        val latch = CountDownLatch(1)
        var receivedErrorType: String? = null
        var receivedErrorMessage: String? = null

        val callback =
            object : IGetCredentialCallbacks.Stub() {
                override fun onSuccess(response: GmsGetCredentialResponse) {
                    latch.countDown()
                }

                override fun onFailure(errorType: String, message: String) {
                    receivedErrorType = errorType
                    receivedErrorMessage = message
                    latch.countDown()
                }
            }

        stub.onGetCredentialRequest(gmsRequest, callingAppInfo, callback)

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(receivedErrorType)
            .isEqualTo(
                GetCredentialProviderConfigurationException("Caller is not Google Play Services")
                    .type
            )
        assertThat(receivedErrorMessage).isEqualTo("Caller is not Google Play Services")
    }

    @Test
    fun onGetCredentialRequest_serviceGarbageCollected_failsWithUnknownException() {
        var fakeService: UiDelegationFulfillmentService? = createFakeService()
        val binder = provider.getStubImplementation(fakeService!!)!!
        val stub = IDelegatedCredentialService.Stub.asInterface(binder)

        fakeService = null
        Runtime.getRuntime().gc()
        System.gc()

        val gmsOption =
            GmsCredentialOption(
                "androidx.credentials.TYPE_DIGITAL_CREDENTIAL",
                Bundle.EMPTY,
                Bundle.EMPTY,
                "",
                "",
                "",
            )
        val gmsRequest =
            GmsGetCredentialRequest(
                listOf(gmsOption),
                Bundle.EMPTY,
                "https://origin.example.com",
                android.os.ResultReceiver(null),
            )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callingAppInfo =
            GmsCallingAppInfoParcelable(
                context.packageName,
                emptyList(),
                "https://origin.example.com",
            )

        val latch = CountDownLatch(1)
        var receivedErrorType: String? = null
        var receivedErrorMessage: String? = null

        val callback =
            object : IGetCredentialCallbacks.Stub() {
                override fun onSuccess(response: GmsGetCredentialResponse) {
                    latch.countDown()
                }

                override fun onFailure(errorType: String, message: String) {
                    receivedErrorType = errorType
                    receivedErrorMessage = message
                    latch.countDown()
                }
            }

        stub.onGetCredentialRequest(gmsRequest, callingAppInfo, callback)

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(receivedErrorType)
            .isEqualTo(
                GetCredentialUnknownException("Service instance is no longer available").type
            )
        assertThat(receivedErrorMessage).isEqualTo("Service instance is no longer available")
    }
}

/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people.playservices

import androidx.people.internal.PeopleBackendFactory
import androidx.people.internal.PeopleBackendResolver
import androidx.people.internal.UnavailablePeopleBackend
import androidx.people.playservices.FakePeopleProviderCompatClient.Companion.apiException
import androidx.people.playservices.FakePeopleProviderCompatClient.Companion.connectionApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedResponse
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PlayServicesPeopleBackendFactoryTest {

    private val context = RuntimeEnvironment.getApplication()
    private val providerClient = FakePeopleProviderCompatClient()

    private fun factory(googlePlayServicesAvailability: Int = ConnectionResult.SUCCESS) =
        PlayServicesPeopleBackendFactory(
            checkGooglePlayServicesAvailability = { googlePlayServicesAvailability },
            createProviderClient = { providerClient },
            createDirectoryClient = { FakePeopleDirectoryCompatClient() },
            coroutineDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun create_whenBackendTypeIsGmsCore_returnsPlayServicesBackend() = runTest {
        providerClient.supportedBackendType = IsPeopleProviderSupportedResponse.BACKEND_GMS_CORE

        val backend = factory().create(context)

        assertThat(backend).isInstanceOf(PlayServicesPeopleBackend::class.java)
        assertThat(backend!!.isAvailable).isTrue()
    }

    @Test
    fun create_whenBackendTypeIsAospPlatform_defersToThePlatform() = runTest {
        providerClient.supportedBackendType =
            IsPeopleProviderSupportedResponse.BACKEND_AOSP_PLATFORM

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenBackendTypeIsUnavailable_returnsUnavailableBackend() = runTest {
        providerClient.supportedBackendType = IsPeopleProviderSupportedResponse.BACKEND_UNAVAILABLE

        val backend = factory().create(context)

        assertThat(backend).isSameInstanceAs(UnavailablePeopleBackend)
    }

    @Test
    fun create_whenBackendTypeIsUnknown_returnsUnavailableBackend() = runTest {
        providerClient.supportedBackendType = IsPeopleProviderSupportedResponse.BACKEND_UNKNOWN

        val backend = factory().create(context)

        assertThat(backend).isSameInstanceAs(UnavailablePeopleBackend)
    }

    @Test
    fun create_whenBackendTypeIsUnrecognized_returnsUnavailableBackend() = runTest {
        providerClient.supportedBackendType = 100

        val backend = factory().create(context)

        assertThat(backend).isSameInstanceAs(UnavailablePeopleBackend)
    }

    @Test
    fun create_whenGooglePlayServicesIsMissingOrTooOld_defersToThePlatformWithoutCallingIt() =
        runTest {
            for (availability in
                listOf(
                    ConnectionResult.SERVICE_MISSING,
                    ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
                    ConnectionResult.SERVICE_DISABLED,
                    ConnectionResult.SERVICE_INVALID,
                )) {
                val backend = factory(googlePlayServicesAvailability = availability).create(context)

                assertThat(backend).isNull()
                assertThat(providerClient.supportCheckCount).isEqualTo(0)
            }
        }

    @Test
    fun create_whenGooglePlayServicesIsUpdating_throwsWithoutCallingIt() = runTest {
        val result = runCatching {
            factory(googlePlayServicesAvailability = ConnectionResult.SERVICE_UPDATING)
                .create(context)
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(providerClient.supportCheckCount).isEqualTo(0)
    }

    @Test
    fun create_whenGooglePlayServicesPredatesThePeopleProvider_defersToThePlatform() = runTest {
        // How the client library reports a Google Play services without the People Provider
        // feature.
        providerClient.supportCheckFailure =
            UnsupportedApiCallException(Feature("people_provider_compat_api", 1L))

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenGooglePlayServicesLacksThePeopleProviderApi_defersToThePlatform() = runTest {
        for (errorCode in
            listOf(
                ConnectionResult.SERVICE_MISSING,
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
                ConnectionResult.SERVICE_DISABLED,
                ConnectionResult.SERVICE_INVALID,
                ConnectionResult.API_UNAVAILABLE,
            )) {
            providerClient.supportCheckFailure = connectionApiException(errorCode)

            val backend = factory().create(context)

            assertThat(backend).isNull()
        }
    }

    @Test
    fun create_whenConnectionFailsTransientlyWithApiNotConnected_throws() = runTest {
        for (failure in
            listOf(
                connectionApiException(ConnectionResult.INTERNAL_ERROR),
                connectionApiException(ConnectionResult.NETWORK_ERROR),
                connectionApiException(ConnectionResult.TIMEOUT),
                connectionApiException(ConnectionResult.INTERRUPTED),
                connectionApiException(ConnectionResult.SERVICE_UPDATING),
                connectionApiException(ConnectionResult.CANCELED),
                connectionApiException(ConnectionResult.API_DISABLED),
                connectionApiException(ConnectionResult.API_DISABLED_FOR_CONNECTION),
                apiException(CommonStatusCodes.API_NOT_CONNECTED),
            )) {
            providerClient.supportCheckFailure = failure

            val result = runCatching { factory().create(context) }

            assertThat(result.exceptionOrNull()).isSameInstanceAs(failure)
        }
    }

    @Test
    fun create_whenTheSupportCheckFailsTransiently_throws() = runTest {
        val failure = apiException(CommonStatusCodes.NETWORK_ERROR)
        providerClient.supportCheckFailure = failure

        val result = runCatching { factory().create(context) }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(failure)
    }

    @Test
    fun create_whenTheSupportCheckFailsUnexpectedly_throws() = runTest {
        val failure = IllegalStateException("Binder died")
        providerClient.supportCheckFailure = failure

        val result = runCatching { factory().create(context) }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(failure)
    }

    @Test
    fun create_whenCancelled_propagatesTheCancellation() = runTest {
        val cancellation = CancellationException("Cancelled")
        providerClient.supportCheckFailure = cancellation

        val result = runCatching { factory().create(context) }

        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun factory_canBeLoadedByTheNameAndConstructorThatPeopleUses() {
        // Mirrors PeopleBackendResolver, which cannot reference this artifact at compile time.
        val factory =
            Class.forName("androidx.people.playservices.PlayServicesPeopleBackendFactory")
                .getDeclaredConstructor()
                .newInstance()

        assertThat(factory).isInstanceOf(PeopleBackendFactory::class.java)
    }

    @Test
    fun peopleBackendResolver_withoutGooglePlayServicesOrThePlatform_isUnavailable() = runTest {
        // Robolectric has no Google Play services and runs below Android 37.2, so this artifact's
        // factory defers to the platform People Provider, which is also absent.
        val backend = PeopleBackendResolver.resolve(context)

        assertThat(backend.isAvailable).isFalse()
    }
}

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
import androidx.people.playservices.FakePeopleProviderCompatClient.Companion.apiException
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

    private fun factory(googlePlayServicesAvailable: Boolean = true) =
        PlayServicesPeopleBackendFactory(
            isGooglePlayServicesAvailable = { googlePlayServicesAvailable },
            createProviderClient = { providerClient },
            createDirectoryClient = { FakePeopleDirectoryCompatClient() },
            coroutineDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun create_whenGooglePlayServicesChoosesItself_returnsItsBackend() = runTest {
        providerClient.supportedBackendType = IsPeopleProviderSupportedResponse.BACKEND_GMS_CORE

        val backend = factory().create(context)

        assertThat(backend).isInstanceOf(PlayServicesPeopleBackend::class.java)
        assertThat(backend!!.isAvailable).isTrue()
    }

    @Test
    fun create_whenGooglePlayServicesChoosesThePlatform_returnsNull() = runTest {
        providerClient.supportedBackendType =
            IsPeopleProviderSupportedResponse.BACKEND_AOSP_PLATFORM

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenGooglePlayServicesReportsNoBackend_returnsNull() = runTest {
        providerClient.supportedBackendType = IsPeopleProviderSupportedResponse.BACKEND_UNAVAILABLE

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenTheBackendIsUnknown_returnsNull() = runTest {
        providerClient.supportedBackendType = IsPeopleProviderSupportedResponse.BACKEND_UNKNOWN

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenTheBackendTypeIsNewerThanThisArtifact_returnsNull() = runTest {
        providerClient.supportedBackendType = 100

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenGooglePlayServicesIsMissingOrTooOld_returnsNullWithoutCallingIt() = runTest {
        val backend = factory(googlePlayServicesAvailable = false).create(context)

        assertThat(backend).isNull()
        assertThat(providerClient.supportCheckCount).isEqualTo(0)
    }

    @Test
    fun create_whenGooglePlayServicesPredatesThePeopleProvider_returnsNull() = runTest {
        // How the client library reports a Google Play services without the People Provider
        // feature.
        providerClient.supportCheckFailure =
            UnsupportedApiCallException(Feature("people_provider_compat_api", 1L))

        val backend = factory().create(context)

        assertThat(backend).isNull()
    }

    @Test
    fun create_whenGooglePlayServicesLacksThePeopleProviderApi_returnsNull() = runTest {
        providerClient.supportCheckFailure = apiException(CommonStatusCodes.API_NOT_CONNECTED)

        val backend = factory().create(context)

        assertThat(backend).isNull()
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
    fun peopleBackendResolver_withoutGooglePlayServices_isUnavailable() = runTest {
        // Robolectric runs below Android 37.2 and has no Google Play services, so the resolver asks
        // this artifact's factory, which reports Google Play services as missing.
        val backend = PeopleBackendResolver.resolve(context)

        assertThat(backend.isAvailable).isFalse()
    }
}

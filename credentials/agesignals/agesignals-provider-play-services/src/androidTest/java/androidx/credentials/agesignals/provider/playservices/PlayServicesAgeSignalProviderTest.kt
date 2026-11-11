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

package androidx.credentials.agesignals.provider.playservices

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.AgeSignalResultHandler
import androidx.credentials.agesignals.GetAgeRangeRequest
import androidx.credentials.agesignals.GetAgeRangeResponse
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.credentials.agesignals.exceptions.GetAgeRangeInterruptedException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnavailableException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.android.gms.identitycredentials.ClearCreationOptionsRequest
import com.google.android.gms.identitycredentials.ClearCreationOptionsResponse
import com.google.android.gms.identitycredentials.ClearCredentialStateRequest
import com.google.android.gms.identitycredentials.ClearCredentialStateResponse
import com.google.android.gms.identitycredentials.ClearExportRequest
import com.google.android.gms.identitycredentials.ClearExportResponse
import com.google.android.gms.identitycredentials.ClearRegistryRequest
import com.google.android.gms.identitycredentials.ClearRegistryResponse
import com.google.android.gms.identitycredentials.CreateCredentialHandle
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.GetAgeRangeRequest as GmsGetAgeRangeRequest
import com.google.android.gms.identitycredentials.GetCredentialRequest
import com.google.android.gms.identitycredentials.IdentityCredentialClient
import com.google.android.gms.identitycredentials.ImportCredentialsRequest
import com.google.android.gms.identitycredentials.PendingGetAgeRangeHandle
import com.google.android.gms.identitycredentials.PendingGetCredentialHandle
import com.google.android.gms.identitycredentials.PendingImportCredentialsHandle
import com.google.android.gms.identitycredentials.RegisterCreationOptionsRequest
import com.google.android.gms.identitycredentials.RegisterCreationOptionsResponse
import com.google.android.gms.identitycredentials.RegisterExportRequest
import com.google.android.gms.identitycredentials.RegisterExportResponse
import com.google.android.gms.identitycredentials.RegistrationRequest
import com.google.android.gms.identitycredentials.RegistrationResponse
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateResponse
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlayServicesAgeSignalProviderTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var provider: PlayServicesAgeSignalProvider
    private lateinit var fakeGoogleApiAvailability: FakeGoogleApiAvailability
    private lateinit var fakeClient: FakeIdentityCredentialClient

    private class FakeGoogleApiAvailability : GoogleApiAvailability() {
        var availabilityResult: Int = ConnectionResult.SUCCESS
        var capturedContext: Context? = null
        var capturedMinApkVersion: Int? = null

        override fun isGooglePlayServicesAvailable(context: Context, minApkVersion: Int): Int {
            capturedContext = context
            capturedMinApkVersion = minApkVersion
            return availabilityResult
        }
    }

    private class FakeIdentityCredentialClient : IdentityCredentialClient {
        var getAgeRangeTask: Task<PendingGetAgeRangeHandle>? = null

        override fun getAgeRange(request: GmsGetAgeRangeRequest): Task<PendingGetAgeRangeHandle> {
            return getAgeRangeTask
                ?: Tasks.forException(
                    ApiException(Status(CommonStatusCodes.INTERNAL_ERROR, "No task configured"))
                )
        }

        override fun getCredential(
            request: GetCredentialRequest
        ): Task<PendingGetCredentialHandle> = TODO()

        override fun registerCredentials(request: RegistrationRequest): Task<RegistrationResponse> =
            TODO()

        override fun registerCreationOptions(
            request: RegisterCreationOptionsRequest
        ): Task<RegisterCreationOptionsResponse> = TODO()

        override fun clearCreationOptions(
            request: ClearCreationOptionsRequest
        ): Task<ClearCreationOptionsResponse> = TODO()

        override fun clearRegistry(request: ClearRegistryRequest): Task<ClearRegistryResponse> =
            TODO()

        override fun importCredentials(
            request: ImportCredentialsRequest
        ): Task<PendingImportCredentialsHandle> = TODO()

        override fun registerExport(request: RegisterExportRequest): Task<RegisterExportResponse> =
            TODO()

        override fun clearExport(request: ClearExportRequest): Task<ClearExportResponse> = TODO()

        override fun clearCredentialState(
            request: ClearCredentialStateRequest
        ): Task<ClearCredentialStateResponse> = TODO()

        override fun signalCredentialState(
            request: SignalCredentialStateRequest
        ): Task<SignalCredentialStateResponse> = TODO()

        override fun createCredential(
            request: CreateCredentialRequest
        ): Task<CreateCredentialHandle> = TODO()
    }

    @Before
    fun setUp() {
        provider = PlayServicesAgeSignalProvider(context)
        fakeGoogleApiAvailability = FakeGoogleApiAvailability()
        provider.googleApiAvailability = fakeGoogleApiAvailability
        fakeClient = FakeIdentityCredentialClient()
        provider.credentialClient = fakeClient
    }

    @Test
    fun isAvailable_whenGooglePlayServicesSuccess_returnsTrue() {
        fakeGoogleApiAvailability.availabilityResult = ConnectionResult.SUCCESS

        assertThat(provider.isAvailable()).isTrue()
        assertThat(fakeGoogleApiAvailability.capturedMinApkVersion)
            .isEqualTo(PlayServicesAgeSignalProvider.MIN_GMS_APK_VERSION)
        assertThat(fakeGoogleApiAvailability.capturedContext)
            .isSameInstanceAs(context.applicationContext)
    }

    @Test
    fun isAvailable_whenGooglePlayServicesNotSuccess_returnsFalse() {
        val failureCodes =
            listOf(
                ConnectionResult.SERVICE_MISSING,
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
                ConnectionResult.SERVICE_DISABLED,
                ConnectionResult.SERVICE_INVALID,
            )
        for (code in failureCodes) {
            fakeGoogleApiAvailability.availabilityResult = code

            assertThat(provider.isAvailable()).isFalse()
        }
    }

    @Test
    fun onGetAgeRange_whenGmsNotAvailable_dispatchesProviderConfigurationException() {
        fakeGoogleApiAvailability.availabilityResult = ConnectionResult.SERVICE_MISSING
        var capturedError: GetAgeRangeException? = null

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {}

                    override fun onError(error: GetAgeRangeException) {
                        capturedError = error
                    }
                },
        )

        assertThat(capturedError)
            .isInstanceOf(GetAgeRangeProviderConfigurationException::class.java)
    }

    @Test
    fun onGetAgeRange_whenCancelledInitially_doesNotDispatch() {
        fakeGoogleApiAvailability.availabilityResult = ConnectionResult.SERVICE_MISSING
        val cancellationSignal = CancellationSignal()
        cancellationSignal.cancel()
        var callbackCalled = false

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = cancellationSignal,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        callbackCalled = true
                    }

                    override fun onError(error: GetAgeRangeException) {
                        callbackCalled = true
                    }
                },
        )

        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun handleSilentResult_withValidResponse_dispatchesOnResult() {
        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val resultBundle = AgeSignalResultHandler.toResponseBundle(expectedResponse)
        var capturedResult: GetAgeRangeResponse? = null
        var capturedError: GetAgeRangeException? = null

        provider.handleSilentResult(
            resultData = resultBundle,
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult = result
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError = error
                    }
                },
        )

        assertThat(capturedResult).isEqualTo(expectedResponse)
        assertThat(capturedError).isNull()
    }

    @Test
    fun handleSilentResult_withValidException_dispatchesOnError() {
        val expectedException = GetAgeRangeUnavailableException("No eligible accounts")
        val resultBundle = AgeSignalResultHandler.toExceptionBundle(expectedException)
        var capturedResult: GetAgeRangeResponse? = null
        var capturedError: GetAgeRangeException? = null

        provider.handleSilentResult(
            resultData = resultBundle,
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult = result
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError = error
                    }
                },
        )

        assertThat(capturedResult).isNull()
        assertThat(capturedError).isInstanceOf(GetAgeRangeUnavailableException::class.java)
        assertThat(capturedError!!.message).isEqualTo("No eligible accounts")
    }

    @Test
    fun handleSilentResult_withMalformedBundle_dispatchesUnknownException() {
        val malformedBundle = Bundle()
        var capturedResult: GetAgeRangeResponse? = null
        var capturedError: GetAgeRangeException? = null

        provider.handleSilentResult(
            resultData = malformedBundle,
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult = result
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError = error
                    }
                },
        )

        assertThat(capturedResult).isNull()
        assertThat(capturedError).isInstanceOf(GetAgeRangeUnknownException::class.java)
    }

    @Test
    fun handleSilentResult_whenCancelled_doesNotDispatch() {
        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 18,
                upperAgeBound = null,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_B,
            )
        val resultBundle = AgeSignalResultHandler.toResponseBundle(expectedResponse)
        val cancellationSignal = CancellationSignal()
        cancellationSignal.cancel()
        var callbackInvoked = false

        provider.handleSilentResult(
            resultData = resultBundle,
            cancellationSignal = cancellationSignal,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        callbackInvoked = true
                    }

                    override fun onError(error: GetAgeRangeException) {
                        callbackInvoked = true
                    }
                },
        )

        assertThat(callbackInvoked).isFalse()
    }

    @Test
    fun onGetAgeRange_silentFulfillment_dispatchesOnResultViaExecutor() {
        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val resultBundle = AgeSignalResultHandler.toResponseBundle(expectedResponse)
        val handle = PendingGetAgeRangeHandle(null, resultBundle)
        fakeClient.getAgeRangeTask = Tasks.forResult(handle)

        val latch = CountDownLatch(1)
        val executorInvocations = AtomicInteger(0)
        val inExecutor = AtomicBoolean(false)
        val callbackRanInExecutor = AtomicBoolean(false)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()
        val customExecutor = Executor { command ->
            executorInvocations.incrementAndGet()
            inExecutor.set(true)
            try {
                command.run()
            } finally {
                inExecutor.set(false)
            }
        }

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = customExecutor,
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        callbackRanInExecutor.set(inExecutor.get())
                        capturedResult.set(result)
                        latch.countDown()
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError.set(error)
                        latch.countDown()
                    }
                },
        )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(executorInvocations.get()).isEqualTo(1)
        assertThat(callbackRanInExecutor.get()).isTrue()
        assertThat(capturedResult.get()).isEqualTo(expectedResponse)
        assertThat(capturedError.get()).isNull()
    }

    @Test
    fun onGetAgeRange_silentException_dispatchesOnErrorViaExecutor() {
        val expectedException = GetAgeRangeUnavailableException("No eligible accounts")
        val handle =
            PendingGetAgeRangeHandle(
                null,
                AgeSignalResultHandler.toExceptionBundle(expectedException),
            )
        fakeClient.getAgeRangeTask = Tasks.forResult(handle)

        val latch = CountDownLatch(1)
        val executorInvocations = AtomicInteger(0)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()
        val customExecutor = Executor { command ->
            executorInvocations.incrementAndGet()
            command.run()
        }

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = customExecutor,
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult.set(result)
                        latch.countDown()
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError.set(error)
                        latch.countDown()
                    }
                },
        )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(executorInvocations.get()).isEqualTo(1)
        assertThat(capturedResult.get()).isNull()
        assertThat(capturedError.get()).isInstanceOf(GetAgeRangeUnavailableException::class.java)
        assertThat(capturedError.get()!!.message).isEqualTo("No eligible accounts")
    }

    @Test
    fun onGetAgeRange_taskFailure_mapsApiExceptionToCallbackViaExecutor() {
        fakeClient.getAgeRangeTask =
            Tasks.forException(
                ApiException(Status(CommonStatusCodes.NETWORK_ERROR, "Network error"))
            )

        val latch = CountDownLatch(1)
        val executorInvocations = AtomicInteger(0)
        val capturedError = AtomicReference<GetAgeRangeException>()
        val customExecutor = Executor { command ->
            executorInvocations.incrementAndGet()
            command.run()
        }

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = customExecutor,
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        latch.countDown()
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError.set(error)
                        latch.countDown()
                    }
                },
        )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(executorInvocations.get()).isEqualTo(1)
        assertThat(capturedError.get()).isInstanceOf(GetAgeRangeInterruptedException::class.java)
    }

    @Test
    fun onGetAgeRange_taskCanceled_dispatchesInterruptedExceptionViaExecutor() {
        fakeClient.getAgeRangeTask = Tasks.forCanceled()

        val latch = CountDownLatch(1)
        val executorInvocations = AtomicInteger(0)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()
        val customExecutor = Executor { command ->
            executorInvocations.incrementAndGet()
            command.run()
        }

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = customExecutor,
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult.set(result)
                        latch.countDown()
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError.set(error)
                        latch.countDown()
                    }
                },
        )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(executorInvocations.get()).isEqualTo(1)
        assertThat(capturedResult.get()).isNull()
        assertThat(capturedError.get()).isInstanceOf(GetAgeRangeInterruptedException::class.java)
    }

    @Test
    fun onGetAgeRange_interactiveHandle_dispatchesNotImplementedUnknownException() {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent("com.example.TEST_ACTION"),
                PendingIntent.FLAG_IMMUTABLE,
            )
        fakeClient.getAgeRangeTask = Tasks.forResult(PendingGetAgeRangeHandle(pendingIntent, null))

        val latch = CountDownLatch(1)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult.set(result)
                        latch.countDown()
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError.set(error)
                        latch.countDown()
                    }
                },
        )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(capturedResult.get()).isNull()
        assertThat(capturedError.get()).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(capturedError.get()!!.message)
            .isEqualTo("Interactive age signal flow is not yet implemented.")
    }

    @Test
    fun onGetAgeRange_nullHandle_dispatchesUnknownException() {
        fakeClient.getAgeRangeTask = Tasks.forResult(null)
        val latch = CountDownLatch(1)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult.set(result)
                        latch.countDown()
                    }

                    override fun onError(error: GetAgeRangeException) {
                        capturedError.set(error)
                        latch.countDown()
                    }
                },
        )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(capturedResult.get()).isNull()
        assertThat(capturedError.get()).isInstanceOf(GetAgeRangeUnknownException::class.java)
    }

    @Test
    fun onGetAgeRange_whenCancelledWhileTaskInFlight_doesNotDispatchOnSuccess() {
        val taskCompletionSource = TaskCompletionSource<PendingGetAgeRangeHandle>()
        fakeClient.getAgeRangeTask = taskCompletionSource.task
        val cancellationSignal = CancellationSignal()
        val executorInvocations = AtomicInteger(0)
        val callbackCalled = AtomicBoolean(false)

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = cancellationSignal,
            executor = {
                executorInvocations.incrementAndGet()
                it.run()
            },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        callbackCalled.set(true)
                    }

                    override fun onError(error: GetAgeRangeException) {
                        callbackCalled.set(true)
                    }
                },
        )

        cancellationSignal.cancel()
        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        taskCompletionSource.setResult(
            PendingGetAgeRangeHandle(
                null,
                AgeSignalResultHandler.toResponseBundle(expectedResponse),
            )
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(executorInvocations.get()).isEqualTo(0)
        assertThat(callbackCalled.get()).isFalse()
    }

    @Test
    fun onGetAgeRange_whenCancelledWhileTaskInFlight_doesNotDispatchOnFailure() {
        val taskCompletionSource = TaskCompletionSource<PendingGetAgeRangeHandle>()
        fakeClient.getAgeRangeTask = taskCompletionSource.task
        val cancellationSignal = CancellationSignal()
        val executorInvocations = AtomicInteger(0)
        val callbackCalled = AtomicBoolean(false)

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = cancellationSignal,
            executor = {
                executorInvocations.incrementAndGet()
                it.run()
            },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        callbackCalled.set(true)
                    }

                    override fun onError(error: GetAgeRangeException) {
                        callbackCalled.set(true)
                    }
                },
        )

        cancellationSignal.cancel()
        taskCompletionSource.setException(
            ApiException(Status(CommonStatusCodes.NETWORK_ERROR, "Network error"))
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(executorInvocations.get()).isEqualTo(0)
        assertThat(callbackCalled.get()).isFalse()
    }

    @Test
    fun onGetAgeRange_whenCancelledWhileTaskInFlight_doesNotDispatchOnTaskCanceled() {
        val cancellationTokenSource = CancellationTokenSource()
        val taskCompletionSource =
            TaskCompletionSource<PendingGetAgeRangeHandle>(cancellationTokenSource.token)
        fakeClient.getAgeRangeTask = taskCompletionSource.task
        val cancellationSignal = CancellationSignal()
        val executorInvocations = AtomicInteger(0)
        val callbackCalled = AtomicBoolean(false)

        provider.onGetAgeRange(
            context = context,
            request = GetAgeRangeRequest(),
            cancellationSignal = cancellationSignal,
            executor = {
                executorInvocations.incrementAndGet()
                it.run()
            },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        callbackCalled.set(true)
                    }

                    override fun onError(error: GetAgeRangeException) {
                        callbackCalled.set(true)
                    }
                },
        )

        cancellationSignal.cancel()
        cancellationTokenSource.cancel()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(executorInvocations.get()).isEqualTo(0)
        assertThat(callbackCalled.get()).isFalse()
    }

    @Test
    fun fromGmsException_apiExceptionCancelled_returnsUnavailableException() {
        val msg = "Operation cancelled"
        val apiException = ApiException(Status(CommonStatusCodes.CANCELED, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeUnavailableException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_apiExceptionNotConnected_returnsUnavailableException() {
        val msg = "API not connected"
        val apiException = ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeUnavailableException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_apiExceptionNetworkError_returnsInterruptedException() {
        val msg = "Network error"
        val apiException = ApiException(Status(CommonStatusCodes.NETWORK_ERROR, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_apiExceptionConnectionSuspended_returnsInterruptedException() {
        val msg = "Connection suspended"
        val apiException =
            ApiException(Status(CommonStatusCodes.CONNECTION_SUSPENDED_DURING_CALL, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_apiExceptionInterrupted_returnsInterruptedException() {
        val msg = "Interrupted while getting age accounts"
        val apiException = ApiException(Status(CommonStatusCodes.INTERRUPTED, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_apiExceptionTimeout_returnsInterruptedException() {
        val msg = "Timed out"
        val apiException = ApiException(Status(CommonStatusCodes.TIMEOUT, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_apiExceptionOther_returnsUnknownException() {
        val msg = "Internal error"
        val apiException = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR, msg))

        val result = provider.fromGmsException(apiException)

        assertThat(result).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(result.message).contains(msg)
    }

    @Test
    fun fromGmsException_unsupportedApiCallException_returnsUnavailableException() {
        val exception = UnsupportedApiCallException(Feature("sample", 1))

        val result = provider.fromGmsException(exception)

        assertThat(result).isInstanceOf(GetAgeRangeUnavailableException::class.java)
    }

    @Test
    fun fromGmsException_otherException_returnsUnknownException() {
        val exception = RuntimeException("Unexpected error")

        val result = provider.fromGmsException(exception)

        assertThat(result).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(result.message).contains("Unexpected error")
    }
}

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

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.content.IntentCompat
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.AgeSignalResultHandler
import androidx.credentials.agesignals.GetAgeRangeRequest
import androidx.credentials.agesignals.GetAgeRangeResponse
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.credentials.agesignals.exceptions.GetAgeRangeInterruptedException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnavailableException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
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
    fun handleActivityResult_withValidResponseExtra_dispatchesOnResult() {
        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 15,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val resultBundle = AgeSignalResultHandler.toResponseBundle(expectedResponse)
        var capturedResult: GetAgeRangeResponse? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_OK,
            resultData = resultBundle,
            cancellationSignal = null,
            executor = { it.run() },
            callback =
                object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                    override fun onResult(result: GetAgeRangeResponse) {
                        capturedResult = result
                    }

                    override fun onError(error: GetAgeRangeException) {}
                },
        )

        assertThat(capturedResult).isEqualTo(expectedResponse)
    }

    @Test
    fun handleActivityResult_withValidExceptionExtra_dispatchesOnError() {
        val expectedException = GetAgeRangeInterruptedException("Interrupted by user")
        val resultBundle = AgeSignalResultHandler.toExceptionBundle(expectedException)
        var capturedError: GetAgeRangeException? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_CANCELED,
            resultData = resultBundle,
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

        assertThat(capturedError).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(capturedError!!.message).contains("Interrupted by user")
    }

    @Test
    fun handleActivityResult_resultCanceledWithoutExtras_dispatchesUnavailableException() {
        var capturedError: GetAgeRangeException? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_CANCELED,
            resultData = Bundle(),
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

        assertThat(capturedError).isInstanceOf(GetAgeRangeUnavailableException::class.java)
    }

    @Test
    fun handleActivityResult_resultCanceledWithResponseExtra_dispatchesUnavailableException() {
        val response =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 15,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val resultBundle = AgeSignalResultHandler.toResponseBundle(response)
        var capturedResult: GetAgeRangeResponse? = null
        var capturedError: GetAgeRangeException? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_CANCELED,
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
    }

    @Test
    fun handleActivityResult_resultOkWithoutExtras_dispatchesUnknownException() {
        var capturedError: GetAgeRangeException? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_OK,
            resultData = Bundle(),
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

        assertThat(capturedError).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(capturedError!!.message).contains("No provider data returned.")
    }

    @Test
    fun handleActivityResult_resultCanceledWithNullData_dispatchesUnavailableException() {
        var capturedError: GetAgeRangeException? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_CANCELED,
            resultData = null,
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

        assertThat(capturedError).isInstanceOf(GetAgeRangeUnavailableException::class.java)
    }

    @Test
    fun handleActivityResult_unrecognizedResultCode_dispatchesUnknownException() {
        var capturedError: GetAgeRangeException? = null

        provider.handleActivityResult(
            resultCode = Activity.RESULT_FIRST_USER,
            resultData = null,
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

        assertThat(capturedError).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(capturedError!!.message)
            .contains("Activity failed with result code: ${Activity.RESULT_FIRST_USER}")
    }

    @Test
    fun handleActivityResult_whenCancelled_doesNotDispatch() {
        val cancellationSignal = CancellationSignal()
        cancellationSignal.cancel()
        var callbackInvoked = false

        provider.handleActivityResult(
            resultCode = Activity.RESULT_CANCELED,
            resultData = Bundle(),
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
    fun createHiddenActivityIntent_returnsCorrectlyConfiguredIntent() {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent("com.example.TEST_ACTION"),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val latch = CountDownLatch(1)
        val receivedResultCode = AtomicInteger(-1)
        val receivedResponse = AtomicReference<GetAgeRangeResponse>()
        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    receivedResultCode.set(resultCode)
                    receivedResponse.set(
                        resultData?.let { AgeSignalResultHandler.retrieveGetAgeRangeResponse(it) }
                    )
                    latch.countDown()
                }
            }

        val intent = provider.createHiddenActivityIntent(context, pendingIntent, receiver)

        assertThat(intent.component?.className).isEqualTo(AgeSignalHiddenActivity::class.java.name)
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .isEqualTo(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        assertThat(
                IntentCompat.getParcelableExtra(
                    intent,
                    AgeSignalHiddenActivity.EXTRA_PENDING_INTENT,
                    PendingIntent::class.java,
                )
            )
            .isEqualTo(pendingIntent)
        val ipcReceiver =
            IntentCompat.getParcelableExtra(
                intent,
                AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER,
                ResultReceiver::class.java,
            )
        assertThat(ipcReceiver).isNotNull()

        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 16,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        ipcReceiver!!.send(
            Activity.RESULT_OK,
            AgeSignalResultHandler.toResponseBundle(expectedResponse),
        )
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(receivedResultCode.get()).isEqualTo(Activity.RESULT_OK)
        assertThat(receivedResponse.get()).isEqualTo(expectedResponse)
    }

    @Test
    fun createHiddenActivityIntent_nonActivityContext_doesNotAddNewTaskFlag() {
        val appContext = context.applicationContext
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent("com.example.TEST_ACTION"),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {}

        val intent = provider.createHiddenActivityIntent(appContext, pendingIntent, receiver)

        assertThat(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(0)
    }

    // On API 24-27 the platform does not enforce FLAG_ACTIVITY_NEW_TASK for non-Activity contexts.
    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun onGetAgeRange_nonActivityContext_interactiveFulfillment_dispatchesFailedToLaunchError() {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent("com.example.TEST_ACTION"),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val handle = PendingGetAgeRangeHandle(pendingIntent, null)
        fakeClient.getAgeRangeTask = Tasks.forResult(handle)

        val latch = CountDownLatch(1)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()

        provider.onGetAgeRange(
            context = context.applicationContext,
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
        assertThat(capturedError.get()!!.message).startsWith("Failed to launch interactive UI:")
    }

    @Test
    fun onGetAgeRange_interactiveFulfillment_dispatchesOnResultViaExecutor() {
        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val consentIntent =
            Intent(context, FakeAgeSignalConsentActivity::class.java).apply {
                putExtra(FakeAgeSignalConsentActivity.EXTRA_RESULT_CODE, Activity.RESULT_OK)
                putExtra(
                    FakeAgeSignalConsentActivity.EXTRA_RESULT_BUNDLE,
                    AgeSignalResultHandler.toResponseBundle(expectedResponse),
                )
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                101,
                consentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        fakeClient.getAgeRangeTask = Tasks.forResult(PendingGetAgeRangeHandle(pendingIntent, null))

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

        ActivityScenario.launch(TestAgeSignalHostActivity::class.java).use { scenario ->
            lateinit var hostActivity: Activity
            scenario.onActivity { hostActivity = it }

            provider.onGetAgeRange(
                context = hostActivity,
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
        }

        assertThat(executorInvocations.get()).isEqualTo(1)
        assertThat(callbackRanInExecutor.get()).isTrue()
        assertThat(capturedResult.get()).isEqualTo(expectedResponse)
        assertThat(capturedError.get()).isNull()
    }

    @Test
    fun onGetAgeRange_interactiveCanceled_dispatchesUnavailableExceptionViaExecutor() {
        val consentIntent =
            Intent(context, FakeAgeSignalConsentActivity::class.java).apply {
                putExtra(FakeAgeSignalConsentActivity.EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                102,
                consentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        fakeClient.getAgeRangeTask = Tasks.forResult(PendingGetAgeRangeHandle(pendingIntent, null))

        val latch = CountDownLatch(1)
        val executorInvocations = AtomicInteger(0)
        val capturedResult = AtomicReference<GetAgeRangeResponse>()
        val capturedError = AtomicReference<GetAgeRangeException>()
        val customExecutor = Executor { command ->
            executorInvocations.incrementAndGet()
            command.run()
        }

        ActivityScenario.launch(TestAgeSignalHostActivity::class.java).use { scenario ->
            lateinit var hostActivity: Activity
            scenario.onActivity { hostActivity = it }

            provider.onGetAgeRange(
                context = hostActivity,
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
        }

        assertThat(executorInvocations.get()).isEqualTo(1)
        assertThat(capturedResult.get()).isNull()
        assertThat(capturedError.get()).isInstanceOf(GetAgeRangeUnavailableException::class.java)
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

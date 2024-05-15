/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.services.client

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import android.os.RemoteException
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.MeasureCapabilities
import androidx.health.services.client.impl.IMeasureApiService
import androidx.health.services.client.impl.IMeasureCallback
import androidx.health.services.client.impl.IpcConstants
import androidx.health.services.client.impl.ServiceBackedMeasureClient
import androidx.health.services.client.impl.internal.IStatusCallback
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.MeasureRegistrationRequest
import androidx.health.services.client.impl.request.MeasureUnregistrationRequest
import androidx.health.services.client.impl.response.MeasureCapabilitiesResponse
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MeasureClientTest {

    private lateinit var callback: FakeCallback
    private lateinit var client: ServiceBackedMeasureClient
    private lateinit var service: FakeServiceStub
    private var cleanup: Boolean = false
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun TestScope.advanceMainLooperIdle() =
        launch { Shadows.shadowOf(Looper.getMainLooper()).idle() }
    private fun CoroutineScope.advanceMainLooperIdle() =
        launch { Shadows.shadowOf(Looper.getMainLooper()).idle() }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        callback = FakeCallback()
        client =
            ServiceBackedMeasureClient(context, ConnectionManager(context, context.mainLooper))
        service = FakeServiceStub()

        val packageName = CLIENT_CONFIGURATION.servicePackageName
        val action = CLIENT_CONFIGURATION.bindAction
        Shadows.shadowOf(context).setComponentNameAndServiceForBindServiceForIntent(
            Intent().setPackage(packageName).setAction(action),
            ComponentName(packageName, CLIENT),
            service
        )
        cleanup = true
    }

    @After
    fun tearDown() {
        if (!cleanup)
            return
        runBlocking {
            launch { client.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback) }
            advanceMainLooperIdle()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun unregisterCallbackReachesServiceSynchronously() = runTest {
        val deferred = async {
            client.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            client.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
            cleanup = false // Already unregistered
        }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(service.unregisterEvents).hasSize(1)
        Truth.assertThat(service.unregisterEvents[0].request.dataType)
            .isEqualTo(DataType.HEART_RATE_BPM)
        Truth.assertThat(service.unregisterEvents[0].request.packageName)
            .isEqualTo("androidx.health.services.client.test")
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun unregisterCallbackSynchronously_callbackNotRegistered_success() = runTest {
        val deferred = async {
            client.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
        }
        advanceMainLooperIdle()

        Truth.assertThat(deferred.await()).isNull()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun capabilitiesReturnsCorrectValueSynchronously() = runTest {
        lateinit var capabilities: MeasureCapabilities
        val deferred = async {
            service.supportedDataTypes = setOf(DataType.HEART_RATE_BPM)
            client.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            capabilities = client.getCapabilities()
        }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(capabilities.supportedDataTypesMeasure).hasSize(1)
        Truth.assertThat(capabilities.supportedDataTypesMeasure)
            .containsExactly(DataType.HEART_RATE_BPM)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun capabilitiesReturnsCorrectValue_cancelSuccessfully() = runTest {
        var isCancellationException = false
        val deferred = async {
            service.supportedDataTypes = setOf(DataType.HEART_RATE_BPM)
            client.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            client.getCapabilities()
        }
        val cancelCoroutine = async { deferred.cancel() }
        try {
            deferred.await()
        } catch (e: CancellationException) {
            isCancellationException = true
        }
        cancelCoroutine.await()

        Truth.assertThat(isCancellationException).isTrue()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun capabilitiesReturnsCorrectValue_catchException() = runTest {
        var isHealthServiceException = false
        val deferred = async {
            service.supportedDataTypes = setOf(DataType.HEART_RATE_BPM)
            client.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            service.setException()
            try {
                client.getCapabilities()
            } catch (e: HealthServicesException) {
                isHealthServiceException = true
            }
        }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(isHealthServiceException).isTrue()
    }

    class FakeCallback : MeasureCallback {
        data class AvailabilityChangeEvent(
            val dataType: DataType<*, *>,
            val availability: Availability
        )

        data class DataReceivedEvent(val data: DataPointContainer)

        val availabilityChangeEvents = mutableListOf<AvailabilityChangeEvent>()
        val dataReceivedEvents = mutableListOf<DataReceivedEvent>()
        var onRegisteredInvocationCount = 0
        var registrationFailureThrowables = mutableListOf<Throwable>()

        override fun onRegistered() {
            onRegisteredInvocationCount++
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            registrationFailureThrowables += throwable
        }

        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            availabilityChangeEvents += AvailabilityChangeEvent(dataType, availability)
        }

        override fun onDataReceived(data: DataPointContainer) {
            dataReceivedEvents += DataReceivedEvent(data)
        }
    }

    class FakeServiceStub : IMeasureApiService.Stub() {
        private var throwExcepotion = false

        class RegisterEvent(
            val request: MeasureRegistrationRequest,
            val callback: IMeasureCallback,
            val statusCallback: IStatusCallback
        )

        class UnregisterEvent(
            val request: MeasureUnregistrationRequest,
            val callback: IMeasureCallback,
            val statusCallback: IStatusCallback
        )

        var statusCallbackAction: (IStatusCallback) -> Unit = { it.onSuccess() }
        var supportedDataTypes = setOf(DataType.HEART_RATE_BPM)

        val registerEvents = mutableListOf<RegisterEvent>()
        val unregisterEvents = mutableListOf<UnregisterEvent>()
        var callingAppHasPermissions = true

        override fun getApiVersion() = 42

        override fun registerCallback(
            request: MeasureRegistrationRequest,
            callback: IMeasureCallback,
            statusCallback: IStatusCallback
        ) {
            if (callingAppHasPermissions) {
                registerEvents += RegisterEvent(request, callback, statusCallback)
                statusCallbackAction.invoke(statusCallback)
            } else {
                statusCallback.onFailure("Missing permissions")
            }
        }

        override fun unregisterCallback(
            request: MeasureUnregistrationRequest,
            callback: IMeasureCallback,
            statusCallback: IStatusCallback
        ) {
            unregisterEvents += UnregisterEvent(request, callback, statusCallback)
            statusCallbackAction.invoke(statusCallback)
        }

        override fun getCapabilities(request: CapabilitiesRequest): MeasureCapabilitiesResponse {
            if (throwExcepotion) {
                throw RemoteException("Remote Exception")
            }
            return MeasureCapabilitiesResponse(MeasureCapabilities(supportedDataTypes))
        }

        fun setException() {
            throwExcepotion = true
        }
    }

    internal companion object {
        internal const val CLIENT = "HealthServicesMeasureClient"
        internal val CLIENT_CONFIGURATION =
            ClientConfiguration(
                CLIENT,
                IpcConstants.SERVICE_PACKAGE_NAME,
                IpcConstants.MEASURE_API_BIND_ACTION
            )
    }
}

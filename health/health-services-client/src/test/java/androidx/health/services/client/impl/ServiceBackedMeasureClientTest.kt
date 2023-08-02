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

package androidx.health.services.client.impl

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataPoints
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.DataTypeAvailability.Companion.AVAILABLE
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.HeartRateAccuracy
import androidx.health.services.client.data.HeartRateAccuracy.SensorStatus.Companion.ACCURACY_HIGH
import androidx.health.services.client.data.MeasureCapabilities
import androidx.health.services.client.impl.event.MeasureCallbackEvent
import androidx.health.services.client.impl.internal.IStatusCallback
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.MeasureRegistrationRequest
import androidx.health.services.client.impl.request.MeasureUnregistrationRequest
import androidx.health.services.client.impl.response.AvailabilityResponse
import androidx.health.services.client.impl.response.DataPointsResponse
import androidx.health.services.client.impl.response.MeasureCapabilitiesResponse
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ServiceBackedMeasureClientTest {

    private val callback = FakeCallback()
    private lateinit var client: ServiceBackedMeasureClient
    private lateinit var fakeService: FakeServiceStub

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        client =
            ServiceBackedMeasureClient(context, ConnectionManager(context, context.mainLooper))
        fakeService = FakeServiceStub()

        val packageName = ServiceBackedMeasureClient.CLIENT_CONFIGURATION.servicePackageName
        val action = ServiceBackedMeasureClient.CLIENT_CONFIGURATION.bindAction
        shadowOf(context).setComponentNameAndServiceForBindServiceForIntent(
            Intent().setPackage(packageName).setAction(action),
            ComponentName(packageName, ServiceBackedMeasureClient.CLIENT),
            fakeService
        )
    }

    @After
    fun tearDown() {
        client.unregisterMeasureCallbackAsync(HEART_RATE_BPM, callback)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun registerCallbackReachesService() {
        client.registerMeasureCallback(HEART_RATE_BPM, callback)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(callback.onRegisteredInvocationCount).isEqualTo(1)
        assertThat(fakeService.registerEvents).hasSize(1)
        assertThat(fakeService.registerEvents[0].request.dataType).isEqualTo(HEART_RATE_BPM)
        assertThat(fakeService.registerEvents[0].request.packageName)
            .isEqualTo("androidx.health.services.client.test")
    }
    @Test
    fun registerCallbackFailureReachesClient() {
        fakeService.statusCallbackAction = { it.onFailure("Measure twice, cut once.") }

        client.registerMeasureCallback(HEART_RATE_BPM, callback)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(callback.registrationFailureThrowables).hasSize(1)
        assertThat(callback.registrationFailureThrowables[0].message)
            .isEqualTo("Measure twice, cut once.")
    }

    @Test
    fun unregisterCallbackReachesService() {
        client.registerMeasureCallback(HEART_RATE_BPM, callback)
        client.unregisterMeasureCallbackAsync(HEART_RATE_BPM, callback)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeService.unregisterEvents).hasSize(1)
        assertThat(fakeService.unregisterEvents[0].request.dataType).isEqualTo(HEART_RATE_BPM)
        assertThat(fakeService.unregisterEvents[0].request.packageName)
            .isEqualTo("androidx.health.services.client.test")
    }

    @Test
    fun dataPointsReachAppCallback() {
        val event = MeasureCallbackEvent.createDataPointsUpdateEvent(
            DataPointsResponse(
                listOf(
                    DataPoints.heartRate(
                        50.0,
                        Duration.ofSeconds(42),
                        HeartRateAccuracy(ACCURACY_HIGH)
                    )
                )
            )
        )
        client.registerMeasureCallback(HEART_RATE_BPM, callback)
        shadowOf(Looper.getMainLooper()).idle()

        fakeService.registerEvents[0].callback.onMeasureCallbackEvent(event)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(callback.dataReceivedEvents).hasSize(1)
        val dataEvent = callback.dataReceivedEvents[0]
        assertThat(dataEvent.data.getData(HEART_RATE_BPM)).hasSize(1)
        val dataPoint = dataEvent.data.getData(HEART_RATE_BPM)[0]
        assertThat(dataPoint.value).isEqualTo(50.0)
        assertThat((dataPoint.accuracy as HeartRateAccuracy).sensorStatus).isEqualTo(ACCURACY_HIGH)
        assertThat(dataPoint.timeDurationFromBoot.seconds).isEqualTo(42)
    }

    @Test
    fun availabilityReachesAppCallback() {
        val event = MeasureCallbackEvent.createAvailabilityUpdateEvent(
            AvailabilityResponse(HEART_RATE_BPM, AVAILABLE)
        )
        client.registerMeasureCallback(HEART_RATE_BPM, callback)
        shadowOf(Looper.getMainLooper()).idle()

        fakeService.registerEvents[0].callback.onMeasureCallbackEvent(event)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(callback.availabilityChangeEvents).hasSize(1)
        val availabilityEvent = callback.availabilityChangeEvents[0]
        assertThat(availabilityEvent.dataType).isEqualTo(HEART_RATE_BPM)
        assertThat(availabilityEvent.availability).isEqualTo(AVAILABLE)
    }

    @Test
    fun capabilitiesReturnsCorrectValue() {
        fakeService.supportedDataTypes = setOf(HEART_RATE_BPM)

        val capabilitiesFuture = client.getCapabilitiesAsync()
        shadowOf(Looper.getMainLooper()).idle()
        val capabilities = capabilitiesFuture.get()

        assertThat(capabilities.supportedDataTypesMeasure).hasSize(1)
        assertThat(capabilities.supportedDataTypesMeasure).containsExactly(HEART_RATE_BPM)
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
        var supportedDataTypes = setOf(HEART_RATE_BPM)

        val registerEvents = mutableListOf<RegisterEvent>()
        val unregisterEvents = mutableListOf<UnregisterEvent>()

        override fun getApiVersion() = 42

        override fun registerCallback(
            request: MeasureRegistrationRequest,
            callback: IMeasureCallback,
            statusCallback: IStatusCallback
        ) {
            registerEvents += RegisterEvent(request, callback, statusCallback)
            statusCallbackAction.invoke(statusCallback)
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
            return MeasureCapabilitiesResponse(MeasureCapabilities(supportedDataTypes))
        }
    }
}

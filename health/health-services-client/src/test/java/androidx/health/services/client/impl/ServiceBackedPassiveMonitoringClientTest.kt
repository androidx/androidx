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
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.ComparisonType.Companion.GREATER_THAN
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType.Companion.CALORIES_DAILY
import androidx.health.services.client.data.DataType.Companion.STEPS_DAILY
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.HealthEvent
import androidx.health.services.client.data.HealthEvent.Type.Companion.FALL_DETECTED
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.PassiveGoal.TriggerFrequency.Companion.ONCE
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState
import androidx.health.services.client.impl.event.PassiveListenerEvent
import androidx.health.services.client.impl.internal.IStatusCallback
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PassiveListenerCallbackRegistrationRequest
import androidx.health.services.client.impl.request.PassiveListenerServiceRegistrationRequest
import androidx.health.services.client.impl.response.HealthEventResponse
import androidx.health.services.client.impl.response.PassiveMonitoringCapabilitiesResponse
import androidx.health.services.client.impl.response.PassiveMonitoringGoalResponse
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ComparisonType.COMPARISON_TYPE_GREATER_THAN
import androidx.health.services.client.proto.DataProto.HealthEvent.HealthEventType.HEALTH_EVENT_TYPE_FALL_DETECTED
import androidx.health.services.client.proto.DataProto.PassiveGoal.TriggerFrequency.TRIGGER_FREQUENCY_ONCE
import androidx.health.services.client.proto.DataProto.UserActivityState.USER_ACTIVITY_STATE_PASSIVE
import androidx.health.services.client.proto.ResponsesProto
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ServiceBackedPassiveMonitoringClientTest {

    private lateinit var client: ServiceBackedPassiveMonitoringClient
    private lateinit var fakeService: FakeServiceStub

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        client = ServiceBackedPassiveMonitoringClient(
            context, ConnectionManager(context, context.mainLooper)
        )
        fakeService = FakeServiceStub()

        val packageName =
            ServiceBackedPassiveMonitoringClient.CLIENT_CONFIGURATION.servicePackageName
        val action = ServiceBackedPassiveMonitoringClient.CLIENT_CONFIGURATION.bindAction
        shadowOf(context).setComponentNameAndServiceForBindServiceForIntent(
            Intent().setPackage(packageName).setAction(action),
            ComponentName(packageName, ServiceBackedPassiveMonitoringClient.CLIENT),
            fakeService
        )
    }

    @After
    fun tearDown() {
        client.clearPassiveListenerCallbackAsync()
        client.clearPassiveListenerServiceAsync()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun registersPassiveListenerService() {
        val config = PassiveListenerConfig(
            dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
            shouldRequestUserActivityState = true,
            dailyGoals = setOf(),
            healthEventTypes = setOf()
        )

        val future = client.setPassiveListenerServiceAsync(FakeListenerService::class.java, config)
        shadowOf(Looper.getMainLooper()).idle()

        // Return value of future.get() is not used, but verifying no exceptions are thrown.
        future.get()
        assertThat(fakeService.registerServiceRequests).hasSize(1)
        val request = fakeService.registerServiceRequests[0]
        assertThat(request.passiveListenerConfig.dataTypes).containsExactly(
            STEPS_DAILY, CALORIES_DAILY
        )
        assertThat(request.passiveListenerConfig.shouldRequestUserActivityState).isTrue()
        assertThat(request.packageName).isEqualTo("androidx.health.services.client.test")
    }

    @Test
    fun registersPassiveListenerCallback() {
        val config = PassiveListenerConfig(
            dataTypes = setOf(STEPS_DAILY),
            shouldRequestUserActivityState = true,
            dailyGoals = setOf(),
            healthEventTypes = setOf()
        )
        val callback = FakeCallback()

        client.setPassiveListenerCallback(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeService.registerCallbackRequests).hasSize(1)
        assertThat(callback.onRegisteredCalls).isEqualTo(1)
        val request = fakeService.registerCallbackRequests[0]
        assertThat(request.passiveListenerConfig.dataTypes).containsExactly(STEPS_DAILY)
        assertThat(request.passiveListenerConfig.shouldRequestUserActivityState).isTrue()
        assertThat(request.packageName).isEqualTo("androidx.health.services.client.test")
    }

    @Test
    fun callbackReceivesDataPointsAndUserActivityInfo() {
        shadowOf(Looper.getMainLooper()).idle() // ?????
        val config = PassiveListenerConfig(
            dataTypes = setOf(STEPS_DAILY),
            shouldRequestUserActivityState = true,
            dailyGoals = setOf(),
            healthEventTypes = setOf()
        )
        val callback = FakeCallback()
        client.setPassiveListenerCallback(config, callback)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(fakeService.registerCallbackRequests).hasSize(1)
        val callbackFromService = fakeService.registeredCallbacks[0]
        val passiveDataPointEvent = PassiveListenerEvent.createPassiveUpdateResponse(
            PassiveMonitoringUpdateResponse(
                ResponsesProto.PassiveMonitoringUpdateResponse.newBuilder().setUpdate(
                    DataProto.PassiveMonitoringUpdate.newBuilder().addDataPoints(
                        DataProto.DataPoint.newBuilder().setDataType(STEPS_DAILY.proto)
                            .setStartDurationFromBootMs(2)
                            .setEndDurationFromBootMs(49)
                            .setValue(DataProto.Value.newBuilder().setLongVal(89)
                            )
                    ).addUserActivityInfoUpdates(
                        DataProto.UserActivityInfo.newBuilder()
                            .setState(USER_ACTIVITY_STATE_PASSIVE)
                    )
                ).build()
            )
        )

        callbackFromService.onPassiveListenerEvent(passiveDataPointEvent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeService.registerCallbackRequests).hasSize(1)
        assertThat(callback.dataPointsReceived).hasSize(1)
        assertThat(callback.dataPointsReceived[0].dataPoints).hasSize(1)
        val stepsDataPoint = callback.dataPointsReceived[0].getData(STEPS_DAILY)[0]
        assertThat(stepsDataPoint.value).isEqualTo(89)
        assertThat(stepsDataPoint.dataType).isEqualTo(STEPS_DAILY)
        assertThat(callback.userActivityInfosReceived).hasSize(1)
        assertThat(callback.userActivityInfosReceived[0].userActivityState).isEqualTo(
            UserActivityState.USER_ACTIVITY_PASSIVE
        )
    }

    @Test
    fun callbackReceivesCompletedGoals() {
        val config = PassiveListenerConfig(
            dataTypes = setOf(STEPS_DAILY),
            shouldRequestUserActivityState = false,
            dailyGoals = setOf(
                PassiveGoal(DataTypeCondition(STEPS_DAILY, 87, GREATER_THAN), ONCE)
            ),
            healthEventTypes = setOf()
        )
        val callback = FakeCallback()
        client.setPassiveListenerCallback(config, callback)
        shadowOf(Looper.getMainLooper()).idle()
        val callbackFromService = fakeService.registeredCallbacks[0]
        val passiveGoalEvent = PassiveListenerEvent.createPassiveGoalResponse(
            PassiveMonitoringGoalResponse(
                ResponsesProto.PassiveMonitoringGoalResponse.newBuilder()
                    .setGoal(DataProto.PassiveGoal.newBuilder()
                        .setTriggerFrequency(TRIGGER_FREQUENCY_ONCE)
                        .setCondition(DataProto.DataTypeCondition.newBuilder()
                            .setDataType(STEPS_DAILY.proto)
                            .setComparisonType(COMPARISON_TYPE_GREATER_THAN)
                            .setThreshold(DataProto.Value.newBuilder().setLongVal(87))
                            .build())
                        .build())
                    .build()
            )
        )

        callbackFromService.onPassiveListenerEvent(passiveGoalEvent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeService.registerCallbackRequests).hasSize(1)
        assertThat(callback.completedGoals).hasSize(1)
        val goal = callback.completedGoals[0]
        assertThat(goal.triggerFrequency).isEqualTo(ONCE)
        assertThat(goal.dataTypeCondition.dataType).isEqualTo(STEPS_DAILY)
        assertThat(goal.dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(goal.dataTypeCondition.threshold).isEqualTo(87)
    }

    @Test
    fun callbackReceivesHealthEvents() {
        val config = PassiveListenerConfig(
            dataTypes = setOf(),
            shouldRequestUserActivityState = false,
            dailyGoals = setOf(),
            healthEventTypes = setOf(FALL_DETECTED)
        )
        val callback = FakeCallback()
        client.setPassiveListenerCallback(config, callback)
        shadowOf(Looper.getMainLooper()).idle()
        val callbackFromService = fakeService.registeredCallbacks[0]
        val passiveHealthEvent = PassiveListenerEvent.createHealthEventResponse(
            HealthEventResponse(
                ResponsesProto.HealthEventResponse.newBuilder().setHealthEvent(
                    DataProto.HealthEvent.newBuilder()
                        .setType(HEALTH_EVENT_TYPE_FALL_DETECTED)
                        .build()
                ).build()
            )
        )

        callbackFromService.onPassiveListenerEvent(passiveHealthEvent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeService.registerCallbackRequests).hasSize(1)
        assertThat(callback.healthEventsReceived).hasSize(1)
        val healthEvent = callback.healthEventsReceived[0]
        assertThat(healthEvent.type).isEqualTo(FALL_DETECTED)
    }

    @Test
    fun callbackReceivesPermissionsLost() {
        val config = PassiveListenerConfig(
            dataTypes = setOf(STEPS_DAILY),
            shouldRequestUserActivityState = false,
            dailyGoals = setOf(
                PassiveGoal(DataTypeCondition(STEPS_DAILY, 87, GREATER_THAN), ONCE)
            ),
            healthEventTypes = setOf()
        )
        val callback = FakeCallback()
        client.setPassiveListenerCallback(config, callback)
        shadowOf(Looper.getMainLooper()).idle()
        val callbackFromService = fakeService.registeredCallbacks[0]
        val passiveGoalEvent = PassiveListenerEvent.createPermissionLostResponse()

        callbackFromService.onPassiveListenerEvent(passiveGoalEvent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(callback.onPermissionLostCalls).isEqualTo(1)
    }

    class FakeListenerService : PassiveListenerService()

    internal class FakeCallback : PassiveListenerCallback {
        var onRegisteredCalls = 0
        val onRegistrationFailedThrowables = mutableListOf<Throwable>()
        val dataPointsReceived = mutableListOf<DataPointContainer>()
        val userActivityInfosReceived = mutableListOf<UserActivityInfo>()
        val completedGoals = mutableListOf<PassiveGoal>()
        val healthEventsReceived = mutableListOf<HealthEvent>()
        var onPermissionLostCalls = 0

        override fun onRegistered() {
            onRegisteredCalls++
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            onRegistrationFailedThrowables += throwable
        }

        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            dataPointsReceived += dataPoints
        }

        override fun onUserActivityInfoReceived(info: UserActivityInfo) {
            userActivityInfosReceived += info
        }

        override fun onGoalCompleted(goal: PassiveGoal) {
            completedGoals += goal
        }

        override fun onHealthEventReceived(event: HealthEvent) {
            healthEventsReceived += event
        }

        override fun onPermissionLost() {
            onPermissionLostCalls++
        }
    }

    internal class FakeServiceStub : IPassiveMonitoringApiService.Stub() {
        @JvmField
        var apiVersion = 42

        var statusCallbackAction: (IStatusCallback?) -> Unit = { it!!.onSuccess() }
        val registerServiceRequests = mutableListOf<PassiveListenerServiceRegistrationRequest>()
        val registerCallbackRequests = mutableListOf<PassiveListenerCallbackRegistrationRequest>()
        val registeredCallbacks = mutableListOf<IPassiveListenerCallback>()
        val unregisterServicePackageNames = mutableListOf<String>()
        val unregisterCallbackPackageNames = mutableListOf<String>()

        override fun getApiVersion() = 42

        override fun getCapabilities(
            request: CapabilitiesRequest?
        ): PassiveMonitoringCapabilitiesResponse {
            throw NotImplementedError()
        }

        override fun flush(request: FlushRequest?, statusCallback: IStatusCallback?) {
            throw NotImplementedError()
        }

        override fun registerPassiveListenerService(
            request: PassiveListenerServiceRegistrationRequest,
            statusCallback: IStatusCallback
        ) {
            registerServiceRequests += request
            statusCallbackAction.invoke(statusCallback)
        }

        override fun registerPassiveListenerCallback(
            request: PassiveListenerCallbackRegistrationRequest,
            callback: IPassiveListenerCallback,
            statusCallback: IStatusCallback
        ) {
            registerCallbackRequests += request
            registeredCallbacks += callback
            statusCallbackAction.invoke(statusCallback)
        }

        override fun unregisterPassiveListenerService(
            packageName: String,
            statusCallback: IStatusCallback
        ) {
            unregisterServicePackageNames += packageName
            statusCallbackAction.invoke(statusCallback)
        }

        override fun unregisterPassiveListenerCallback(
            packageName: String,
            statusCallback: IStatusCallback
        ) {
            unregisterCallbackPackageNames += packageName
            statusCallbackAction.invoke(statusCallback)
        }
    }
}
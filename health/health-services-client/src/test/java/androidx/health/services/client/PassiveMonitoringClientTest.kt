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
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType.Companion.CALORIES_DAILY
import androidx.health.services.client.data.DataType.Companion.CALORIES_TOTAL
import androidx.health.services.client.data.DataType.Companion.DISTANCE
import androidx.health.services.client.data.DataType.Companion.STEPS
import androidx.health.services.client.data.DataType.Companion.STEPS_DAILY
import androidx.health.services.client.data.HealthEvent
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState
import androidx.health.services.client.impl.IPassiveListenerCallback
import androidx.health.services.client.impl.IPassiveMonitoringApiService
import androidx.health.services.client.impl.ServiceBackedPassiveMonitoringClient
import androidx.health.services.client.impl.internal.IStatusCallback
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PassiveListenerCallbackRegistrationRequest
import androidx.health.services.client.impl.request.PassiveListenerServiceRegistrationRequest
import androidx.health.services.client.impl.response.PassiveMonitoringCapabilitiesResponse
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
class PassiveMonitoringClientTest {

    private lateinit var client: PassiveMonitoringClient
    private lateinit var service: FakeServiceStub

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun TestScope.advanceMainLooperIdle() = launch {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun CoroutineScope.advanceMainLooperIdle() = launch {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        client =
            ServiceBackedPassiveMonitoringClient(
                context,
                ConnectionManager(context, context.mainLooper)
            )
        service = FakeServiceStub()

        val packageName =
            ServiceBackedPassiveMonitoringClient.CLIENT_CONFIGURATION.servicePackageName
        val action = ServiceBackedPassiveMonitoringClient.CLIENT_CONFIGURATION.bindAction
        Shadows.shadowOf(context)
            .setComponentNameAndServiceForBindServiceForIntent(
                Intent().setPackage(packageName).setAction(action),
                ComponentName(packageName, ServiceBackedPassiveMonitoringClient.CLIENT),
                service
            )
    }

    @After
    fun tearDown() {
        runBlocking {
            launch { client.clearPassiveListenerCallback() }
            advanceMainLooperIdle()
            launch { client.clearPassiveListenerService() }
            advanceMainLooperIdle()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun registersPassiveListenerServiceSynchronously() = runTest {
        launch {
            val config =
                PassiveListenerConfig(
                    dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
                    shouldUserActivityInfoBeRequested = true,
                    dailyGoals = setOf(),
                    healthEventTypes = setOf()
                )

            client.setPassiveListenerService(FakeListenerService::class.java, config)
            val request = service.registerServiceRequests[0]

            Truth.assertThat(service.registerServiceRequests).hasSize(1)
            Truth.assertThat(request.passiveListenerConfig.dataTypes)
                .containsExactly(STEPS_DAILY, CALORIES_DAILY)
            Truth.assertThat(request.passiveListenerConfig.shouldUserActivityInfoBeRequested)
                .isTrue()
            Truth.assertThat(request.packageName).isEqualTo("androidx.health.services.client.test")
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun registersPassiveListenerServiceSynchronously_throwsSecurityException() = runTest {
        launch {
            val config =
                PassiveListenerConfig(
                    dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
                    shouldUserActivityInfoBeRequested = true,
                    dailyGoals = setOf(),
                    healthEventTypes = setOf()
                )

            var exception: Exception? = null
            service.callingAppHasPermissions = false
            try {
                client.setPassiveListenerService(FakeListenerService::class.java, config)
            } catch (e: SecurityException) {
                exception = e
            }

            Truth.assertThat(exception).isNotNull()
            Truth.assertThat(exception).isInstanceOf(SecurityException::class.java)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun flushSynchronously() = runTest {
        launch {
            val config =
                PassiveListenerConfig(
                    dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
                    shouldUserActivityInfoBeRequested = true,
                    dailyGoals = setOf(),
                    healthEventTypes = setOf()
                )
            val callback = FakeCallback()
            client.setPassiveListenerCallback(config, callback)

            client.flush()

            Truth.assertThat(service.registerFlushRequests).hasSize(1)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCapabilitiesSynchronously() = runTest {
        launch {
            val config =
                PassiveListenerConfig(
                    dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
                    shouldUserActivityInfoBeRequested = true,
                    dailyGoals = setOf(),
                    healthEventTypes = setOf()
                )
            val callback = FakeCallback()
            client.setPassiveListenerCallback(config, callback)

            val passiveMonitoringCapabilities = client.getCapabilities()

            Truth.assertThat(service.registerGetCapabilitiesRequests).hasSize(1)
            Truth.assertThat(passiveMonitoringCapabilities).isNotNull()
            Truth.assertThat(service.getTestCapabilities().toString())
                .isEqualTo(passiveMonitoringCapabilities.toString())
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCapabilitiesSynchronously_cancelled() = runTest {
        val config =
            PassiveListenerConfig(
                dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
                shouldUserActivityInfoBeRequested = true,
                dailyGoals = setOf(),
                healthEventTypes = setOf()
            )
        val callback = FakeCallback()
        client.setPassiveListenerCallback(config, callback)
        var isCancellationException = false

        val deferred = async { client.getCapabilities() }
        val cancellationDeferred = async { deferred.cancel(CancellationException()) }
        try {
            deferred.await()
        } catch (e: CancellationException) {
            isCancellationException = true
        }
        cancellationDeferred.await()

        Truth.assertThat(isCancellationException).isTrue()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCapabilitiesSynchronously_Exception() = runTest {
        val config =
            PassiveListenerConfig(
                dataTypes = setOf(STEPS_DAILY, CALORIES_DAILY),
                shouldUserActivityInfoBeRequested = true,
                dailyGoals = setOf(),
                healthEventTypes = setOf()
            )
        val callback = FakeCallback()
        client.setPassiveListenerCallback(config, callback)
        var isExceptionCaught = false
        val deferred = async {
            service.throwException = true
            try {

                client.getCapabilities()
            } catch (e: HealthServicesException) {
                isExceptionCaught = true
            }
        }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(isExceptionCaught).isTrue()
    }

    class FakeListenerService : PassiveListenerService()

    internal class FakeCallback : PassiveListenerCallback {
        private var onRegisteredCalls = 0
        private val onRegistrationFailedThrowables = mutableListOf<Throwable>()
        private val dataPointsReceived = mutableListOf<DataPointContainer>()
        private val userActivityInfosReceived = mutableListOf<UserActivityInfo>()
        private val completedGoals = mutableListOf<PassiveGoal>()
        private val healthEventsReceived = mutableListOf<HealthEvent>()
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
        @JvmField var apiVersion = 42

        private var statusCallbackAction: (IStatusCallback?) -> Unit = { it!!.onSuccess() }
        val registerServiceRequests = mutableListOf<PassiveListenerServiceRegistrationRequest>()
        private val registerCallbackRequests =
            mutableListOf<PassiveListenerCallbackRegistrationRequest>()
        val registerFlushRequests = mutableListOf<FlushRequest>()
        val registerGetCapabilitiesRequests = mutableListOf<CapabilitiesRequest>()
        private val registeredCallbacks = mutableListOf<IPassiveListenerCallback>()
        private val unregisterServicePackageNames = mutableListOf<String>()
        private val unregisterCallbackPackageNames = mutableListOf<String>()
        var throwException = false
        var callingAppHasPermissions = true

        override fun getApiVersion() = 42

        override fun getCapabilities(
            request: CapabilitiesRequest
        ): PassiveMonitoringCapabilitiesResponse {
            if (throwException) {
                throw RemoteException("Remote Exception")
            }
            registerGetCapabilitiesRequests.add(request)
            val capabilities = getTestCapabilities()
            return PassiveMonitoringCapabilitiesResponse(capabilities)
        }

        override fun flush(request: FlushRequest, statusCallback: IStatusCallback?) {
            registerFlushRequests.add(request)
            statusCallbackAction.invoke(statusCallback)
        }

        override fun registerPassiveListenerService(
            request: PassiveListenerServiceRegistrationRequest,
            statusCallback: IStatusCallback
        ) {
            if (callingAppHasPermissions) {
                registerServiceRequests += request
                statusCallbackAction.invoke(statusCallback)
            } else {
                statusCallback.onFailure("Missing permissions")
            }
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

        fun getTestCapabilities(): PassiveMonitoringCapabilities {
            return PassiveMonitoringCapabilities(
                supportedDataTypesPassiveMonitoring = setOf(STEPS, DISTANCE),
                supportedDataTypesPassiveGoals = setOf(CALORIES_TOTAL),
                supportedHealthEventTypes = setOf(HealthEvent.Type.FALL_DETECTED),
                supportedUserActivityStates = setOf(UserActivityState.USER_ACTIVITY_PASSIVE)
            )
        }
    }
}

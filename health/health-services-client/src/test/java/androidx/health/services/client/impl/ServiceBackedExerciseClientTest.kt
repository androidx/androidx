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
import android.os.Looper.getMainLooper
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM_STATS
import androidx.health.services.client.data.DataTypeAvailability.Companion.ACQUIRING
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.impl.event.ExerciseUpdateListenerEvent
import androidx.health.services.client.impl.internal.IExerciseInfoCallback
import androidx.health.services.client.impl.internal.IStatusCallback
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.AutoPauseAndResumeConfigRequest
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.ExerciseGoalRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PrepareExerciseRequest
import androidx.health.services.client.impl.request.StartExerciseRequest
import androidx.health.services.client.impl.response.AvailabilityResponse
import androidx.health.services.client.impl.response.ExerciseCapabilitiesResponse
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ServiceBackedExerciseClientTest {

    private lateinit var client: ServiceBackedExerciseClient
    private lateinit var fakeService: FakeServiceStub
    private val callback = FakeExerciseUpdateCallback()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        client =
            ServiceBackedExerciseClient(context, ConnectionManager(context, context.mainLooper))
        fakeService = FakeServiceStub()

        val packageName = ServiceBackedExerciseClient.CLIENT_CONFIGURATION.servicePackageName
        val action = ServiceBackedExerciseClient.CLIENT_CONFIGURATION.bindAction
        shadowOf(context).setComponentNameAndServiceForBindServiceForIntent(
            Intent().setPackage(packageName).setAction(action),
            ComponentName(packageName, ServiceBackedExerciseClient.CLIENT),
            fakeService
        )
    }

    @After
    fun tearDown() {
        client.clearUpdateCallbackAsync(callback)
    }

    @Test
    fun registeredCallbackShouldBeInvoked() {
        client.setUpdateCallback(callback)
        shadowOf(getMainLooper()).idle()

        assertThat(callback.onRegisteredCalls).isEqualTo(1)
        assertThat(callback.onRegistrationFailedCalls).isEqualTo(0)
    }

    @Test
    fun registrationFailedCallbackShouldBeInvoked() {
        fakeService.statusCallbackAction = { it!!.onFailure("Terrible failure!") }

        client.setUpdateCallback(callback)
        shadowOf(getMainLooper()).idle()

        assertThat(callback.onRegisteredCalls).isEqualTo(0)
        assertThat(callback.onRegistrationFailedCalls).isEqualTo(1)
        assertThat(callback.registrationFailureThrowables[0].message).isEqualTo("Terrible failure!")
    }

    @Test
    fun dataTypeInAvailabilityCallbackShouldMatchRequested_justSampleType_startExercise() {
        val exerciseConfig = ExerciseConfig(
            ExerciseType.WALKING,
            setOf(HEART_RATE_BPM),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = false
        )
        val availabilityEvent = ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
            AvailabilityResponse(HEART_RATE_BPM, ACQUIRING)
        )
        client.setUpdateCallback(callback)
        client.startExerciseAsync(exerciseConfig)
        shadowOf(getMainLooper()).idle()

        fakeService.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
        shadowOf(getMainLooper()).idle()

        assertThat(callback.availabilities).containsEntry(HEART_RATE_BPM, ACQUIRING)
    }

    @Test
    fun dataTypeInAvailabilityCallbackShouldMatchRequested_justStatsType_startExercise() {
        val exerciseConfig = ExerciseConfig(
            ExerciseType.WALKING,
            setOf(HEART_RATE_BPM_STATS),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = false
        )
        val availabilityEvent = ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
            // Currently the proto form of HEART_RATE_BPM and HEART_RATE_BPM_STATS is identical. The
            // APK doesn't know about _STATS, so pass the sample type to mimic that behavior.
            AvailabilityResponse(HEART_RATE_BPM, ACQUIRING)
        )
        client.setUpdateCallback(callback)
        client.startExerciseAsync(exerciseConfig)
        shadowOf(getMainLooper()).idle()

        fakeService.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
        shadowOf(getMainLooper()).idle()

        assertThat(callback.availabilities).containsEntry(HEART_RATE_BPM_STATS, ACQUIRING)
    }

    @Test
    fun dataTypeInAvailabilityCallbackShouldMatchRequested_statsAndSample_startExercise() {
        val exerciseConfig = ExerciseConfig(
            ExerciseType.WALKING,
            setOf(HEART_RATE_BPM, HEART_RATE_BPM_STATS),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = false
        )
        val availabilityEvent = ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
            // Currently the proto form of HEART_RATE_BPM and HEART_RATE_BPM_STATS is identical. The
            // APK doesn't know about _STATS, so pass the sample type to mimic that behavior.
            AvailabilityResponse(HEART_RATE_BPM, ACQUIRING)
        )
        client.setUpdateCallback(callback)
        client.startExerciseAsync(exerciseConfig)
        shadowOf(getMainLooper()).idle()

        fakeService.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
        shadowOf(getMainLooper()).idle()

        // When both the sample type and stat type are requested, both should be notified
        assertThat(callback.availabilities).containsEntry(HEART_RATE_BPM, ACQUIRING)
        assertThat(callback.availabilities).containsEntry(HEART_RATE_BPM_STATS, ACQUIRING)
    }

    @Test
    fun dataTypeInAvailabilityCallbackShouldMatchRequested_justSampleType_prepare() {
        val warmUpConfig = WarmUpConfig(
            ExerciseType.WALKING,
            setOf(HEART_RATE_BPM),
        )
        val availabilityEvent = ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
            AvailabilityResponse(HEART_RATE_BPM, ACQUIRING)
        )
        client.setUpdateCallback(callback)
        client.prepareExerciseAsync(warmUpConfig)
        shadowOf(getMainLooper()).idle()

        fakeService.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
        shadowOf(getMainLooper()).idle()

        assertThat(callback.availabilities).containsEntry(HEART_RATE_BPM, ACQUIRING)
    }

    class FakeExerciseUpdateCallback : ExerciseUpdateCallback {
        val availabilities = mutableMapOf<DataType<*, *>, Availability>()
        val registrationFailureThrowables = mutableListOf<Throwable>()
        var onRegisteredCalls = 0
        var onRegistrationFailedCalls = 0

        override fun onRegistered() {
            onRegisteredCalls++
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            onRegistrationFailedCalls++
            registrationFailureThrowables.add(throwable)
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {}

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            availabilities[dataType] = availability
        }
    }

    class FakeServiceStub : IExerciseApiService.Stub() {

        var listener: IExerciseUpdateListener? = null
        var statusCallbackAction: (IStatusCallback?) -> Unit = { it!!.onSuccess() }

        override fun getApiVersion(): Int = 12

        override fun prepareExercise(
            prepareExerciseRequest: PrepareExerciseRequest?,
            statusCallback: IStatusCallback?
        ) {
            statusCallbackAction.invoke(statusCallback)
        }

        override fun startExercise(
            startExerciseRequest: StartExerciseRequest?,
            statusCallback: IStatusCallback?
        ) {
            statusCallbackAction.invoke(statusCallback)
        }

        override fun pauseExercise(packageName: String?, statusCallback: IStatusCallback?) {
            throw NotImplementedError()
        }

        override fun resumeExercise(packageName: String?, statusCallback: IStatusCallback?) {
            throw NotImplementedError()
        }

        override fun endExercise(packageName: String?, statusCallback: IStatusCallback?) {
            throw NotImplementedError()
        }

        override fun markLap(packageName: String?, statusCallback: IStatusCallback?) {
            throw NotImplementedError()
        }

        override fun getCurrentExerciseInfo(
            packageName: String?,
            exerciseInfoCallback: IExerciseInfoCallback?
        ) {
            throw NotImplementedError()
        }

        override fun setUpdateListener(
            packageName: String?,
            listener: IExerciseUpdateListener?,
            statusCallback: IStatusCallback?
        ) {
            this.listener = listener
            statusCallbackAction.invoke(statusCallback)
        }

        override fun clearUpdateListener(
            packageName: String?,
            listener: IExerciseUpdateListener?,
            statusCallback: IStatusCallback?
        ) {
            throw NotImplementedError()
        }

        override fun addGoalToActiveExercise(
            request: ExerciseGoalRequest?,
            statusCallback: IStatusCallback?
        ) {
            throw NotImplementedError()
        }

        override fun removeGoalFromActiveExercise(
            request: ExerciseGoalRequest?,
            statusCallback: IStatusCallback?
        ) {
            throw NotImplementedError()
        }

        override fun overrideAutoPauseAndResumeForActiveExercise(
            request: AutoPauseAndResumeConfigRequest?,
            statusCallback: IStatusCallback?
        ) {
            throw NotImplementedError()
        }

        override fun getCapabilities(request: CapabilitiesRequest?): ExerciseCapabilitiesResponse {
            throw NotImplementedError()
        }

        override fun flushExercise(request: FlushRequest?, statusCallback: IStatusCallback?) {
            throw NotImplementedError()
        }
    }
}
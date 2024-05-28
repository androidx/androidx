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
import androidx.health.services.client.data.BatchingMode
import androidx.health.services.client.data.ComparisonType
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.DebouncedDataTypeCondition
import androidx.health.services.client.data.DebouncedGoal
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseTypeCapabilities
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.GolfExerciseTypeConfig
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.impl.IExerciseApiService
import androidx.health.services.client.impl.IExerciseUpdateListener
import androidx.health.services.client.impl.IpcConstants
import androidx.health.services.client.impl.ServiceBackedExerciseClient
import androidx.health.services.client.impl.event.ExerciseUpdateListenerEvent
import androidx.health.services.client.impl.internal.IExerciseInfoCallback
import androidx.health.services.client.impl.internal.IStatusCallback
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.AutoPauseAndResumeConfigRequest
import androidx.health.services.client.impl.request.BatchingModeConfigRequest
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.DebouncedGoalRequest
import androidx.health.services.client.impl.request.ExerciseGoalRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PrepareExerciseRequest
import androidx.health.services.client.impl.request.StartExerciseRequest
import androidx.health.services.client.impl.request.UpdateExerciseTypeConfigRequest
import androidx.health.services.client.impl.response.AvailabilityResponse
import androidx.health.services.client.impl.response.ExerciseCapabilitiesResponse
import androidx.health.services.client.impl.response.ExerciseInfoResponse
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth
import java.util.concurrent.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ExerciseClientTest {

    private lateinit var client: ServiceBackedExerciseClient
    private lateinit var service: FakeServiceStub
    private val callback = FakeExerciseUpdateCallback()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun TestScope.advanceMainLooperIdle() = launch {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        client =
            ServiceBackedExerciseClient(context, ConnectionManager(context, context.mainLooper))
        service = FakeServiceStub()

        val packageName = CLIENT_CONFIGURATION.servicePackageName
        val action = CLIENT_CONFIGURATION.bindAction
        Shadows.shadowOf(context)
            .setComponentNameAndServiceForBindServiceForIntent(
                Intent().setPackage(packageName).setAction(action),
                ComponentName(packageName, CLIENT),
                service
            )
    }

    @After
    fun tearDown() {
        client.clearUpdateCallbackAsync(callback)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun callbackShouldMatchRequested_justSampleType_prepareExerciseSynchronously() = runTest {
        launch {
            val warmUpConfig =
                WarmUpConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                )
            val availabilityEvent =
                ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
                    AvailabilityResponse(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
                )
            client.setUpdateCallback(callback)
            client.prepareExercise(warmUpConfig)

            service.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            Truth.assertThat(callback.availabilities)
                .containsEntry(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun prepareExerciseSynchronously_ThrowsException() = runTest {
        launch {
            val warmUpConfig =
                WarmUpConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                )
            var exception: Exception? = null
            client.setUpdateCallback(callback)
            // Mocking the calling app already has an active exercise in progress
            service.throwException = true

            try {
                client.prepareExercise(warmUpConfig)
            } catch (e: HealthServicesException) {
                exception = e
            }

            Truth.assertThat(exception).isNotNull()
            Truth.assertThat(exception).isInstanceOf(HealthServicesException::class.java)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun prepareExerciseSynchronously_ThrowsSecurityException() = runTest {
        launch {
            val warmUpConfig =
                WarmUpConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                )
            var exception: Exception? = null
            client.setUpdateCallback(callback)
            // Mocking the calling app does not have the required permissions
            service.callingAppHasPermissions = false

            try {
                client.prepareExercise(warmUpConfig)
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
    fun callbackShouldMatchRequested_justSampleType_startExerciseSynchronously() = runTest {
        launch {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            val availabilityEvent =
                ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
                    AvailabilityResponse(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
                )
            client.setUpdateCallback(callback)
            client.startExercise(exerciseConfig)

            service.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            Truth.assertThat(callback.availabilities)
                .containsEntry(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun callbackShouldMatchRequested_justStatsType_startExerciseSynchronously() = runTest {
        launch {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM_STATS),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            val availabilityEvent =
                ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
                    // Currently the proto form of HEART_RATE_BPM and HEART_RATE_BPM_STATS is
                    // identical.
                    // The APK doesn't know about _STATS, so pass the sample type to mimic that
                    // behavior.
                    AvailabilityResponse(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
                )
            client.setUpdateCallback(callback)
            client.startExercise(exerciseConfig)

            service.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            Truth.assertThat(callback.availabilities)
                .containsEntry(DataType.HEART_RATE_BPM_STATS, DataTypeAvailability.ACQUIRING)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun callbackShouldMatchRequested_statsAndSample_startExerciseSynchronously() = runTest {
        launch {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM, DataType.HEART_RATE_BPM_STATS),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            val availabilityEvent =
                ExerciseUpdateListenerEvent.createAvailabilityUpdateEvent(
                    // Currently the proto form of HEART_RATE_BPM and HEART_RATE_BPM_STATS is
                    // identical.
                    // The APK doesn't know about _STATS, so pass the sample type to mimic that
                    // behavior.
                    AvailabilityResponse(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
                )
            client.setUpdateCallback(callback)
            client.startExercise(exerciseConfig)

            service.listener!!.onExerciseUpdateListenerEvent(availabilityEvent)
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // When both the sample type and stat type are requested, both should be notified
            Truth.assertThat(callback.availabilities)
                .containsEntry(DataType.HEART_RATE_BPM, DataTypeAvailability.ACQUIRING)
            Truth.assertThat(callback.availabilities)
                .containsEntry(DataType.HEART_RATE_BPM_STATS, DataTypeAvailability.ACQUIRING)
        }
        advanceMainLooperIdle()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun startExerciseSynchronously_ThrowsSecurityException() = runTest {
        launch {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM, DataType.HEART_RATE_BPM_STATS),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            var exception: Exception? = null
            client.setUpdateCallback(callback)
            // Mocking the calling app does not have the required permissions
            service.callingAppHasPermissions = false

            try {
                client.startExercise(exerciseConfig)
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
    fun callbackShouldMatchRequested_justSampleType_pauseExerciseSynchronously() = runTest {
        val statesList = mutableListOf<TestExerciseStates>()
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        startExercise.await()
        val pauseExercise = async {
            client.pauseExercise()
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        pauseExercise.await()

        Truth.assertThat(statesList)
            .containsExactly(TestExerciseStates.STARTED, TestExerciseStates.PAUSED)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun callbackShouldMatchRequested_justSampleType_resumeExerciseSynchronously() = runTest {
        val statesList = mutableListOf<TestExerciseStates>()
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        startExercise.await()
        val pauseExercise = async {
            client.pauseExercise()
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        pauseExercise.await()
        val resumeExercise = async {
            client.resumeExercise()
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        resumeExercise.await()

        Truth.assertThat(statesList)
            .containsExactly(
                TestExerciseStates.STARTED,
                TestExerciseStates.PAUSED,
                TestExerciseStates.RESUMED
            )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun callbackShouldMatchRequested_justSampleType_endExerciseSynchronously() = runTest {
        val statesList = mutableListOf<TestExerciseStates>()
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        startExercise.await()
        val endExercise = async {
            client.endExercise()
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        endExercise.await()

        Truth.assertThat(statesList)
            .containsExactly(TestExerciseStates.STARTED, TestExerciseStates.ENDED)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun callbackShouldMatchRequested_justSampleType_endPausedExerciseSynchronously() = runTest {
        val statesList = mutableListOf<TestExerciseStates>()
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        startExercise.await()
        val pauseExercise = async {
            client.pauseExercise()
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        pauseExercise.await()
        val endExercise = async {
            client.endExercise()
            statesList += service.testExerciseStates
        }
        advanceMainLooperIdle()
        endExercise.await()

        Truth.assertThat(statesList)
            .containsExactly(
                TestExerciseStates.STARTED,
                TestExerciseStates.PAUSED,
                TestExerciseStates.ENDED
            )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun flushSynchronously() = runTest {
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val flushExercise = async { client.flush() }

        advanceMainLooperIdle()
        flushExercise.await()

        Truth.assertThat(service.registerFlushRequests).hasSize(1)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun markLapSynchronously() = runTest {
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val markLap = async { client.markLap() }

        advanceMainLooperIdle()
        markLap.await()

        Truth.assertThat(service.laps).isEqualTo(1)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCurrentExerciseInfoSynchronously() = runTest {
        lateinit var exerciseInfo: ExerciseInfo
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val currentExerciseInfoDeferred = async { exerciseInfo = client.getCurrentExerciseInfo() }
        advanceMainLooperIdle()
        currentExerciseInfoDeferred.await()

        Truth.assertThat(exerciseInfo.exerciseType).isEqualTo(ExerciseType.WALKING)
        Truth.assertThat(exerciseInfo.exerciseTrackedStatus)
            .isEqualTo(ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCurrentExerciseInfoSynchronously_cancelled() = runTest {
        var isCancellationException = false
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val currentExerciseInfoDeferred = async { client.getCurrentExerciseInfo() }
        val cancelDeferred = async { currentExerciseInfoDeferred.cancel() }
        try {
            currentExerciseInfoDeferred.await()
        } catch (e: CancellationException) {
            isCancellationException = true
        }
        cancelDeferred.await()

        Truth.assertThat(isCancellationException).isTrue()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCurrentExerciseInfoSynchronously_exception() = runTest {
        var isException = false
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val currentExerciseInfoDeferred = async {
            service.throwException = true
            try {
                client.getCurrentExerciseInfo()
            } catch (e: HealthServicesException) {
                isException = true
            }
        }
        advanceMainLooperIdle()
        currentExerciseInfoDeferred.await()

        Truth.assertThat(isException).isTrue()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun clearUpdateCallbackShouldBeInvoked() = runTest {
        val statesList = mutableListOf<Boolean>()

        client.setUpdateCallback(callback)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        statesList += (service.listener == null)
        val deferred = async {
            client.clearUpdateCallback(callback)
            statesList += (service.listener == null)
        }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(statesList).containsExactly(false, true)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun clearUpdateCallback_nothingRegistered_noOp() = runTest {
        val deferred = async { client.clearUpdateCallback(callback) }
        advanceMainLooperIdle()

        Truth.assertThat(deferred.await()).isNull()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun addGoalToActiveExerciseShouldBeInvoked() = runTest {
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false,
                    exerciseGoals =
                        listOf(
                            ExerciseGoal.createOneTimeGoal(
                                DataTypeCondition(
                                    DataType.DISTANCE_TOTAL,
                                    50.0,
                                    ComparisonType.GREATER_THAN
                                )
                            ),
                            ExerciseGoal.createOneTimeGoal(
                                DataTypeCondition(
                                    DataType.DISTANCE_TOTAL,
                                    150.0,
                                    ComparisonType.GREATER_THAN
                                )
                            ),
                        )
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val addGoalDeferred = async {
            val proto =
                ExerciseGoal.createOneTimeGoal(
                        DataTypeCondition(
                            DataType.HEART_RATE_BPM_STATS,
                            145.0,
                            ComparisonType.GREATER_THAN
                        )
                    )
                    .proto
            val goal = ExerciseGoal.fromProto(proto)

            client.addGoalToActiveExercise(goal)
        }
        advanceMainLooperIdle()
        addGoalDeferred.await()

        Truth.assertThat(service.goals).hasSize(3)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun removeGoalFromActiveExerciseShouldBeInvoked() = runTest {
        val goal1 =
            ExerciseGoal.createOneTimeGoal(
                DataTypeCondition(DataType.DISTANCE_TOTAL, 50.0, ComparisonType.GREATER_THAN)
            )
        val goal2 =
            ExerciseGoal.createOneTimeGoal(
                DataTypeCondition(DataType.DISTANCE_TOTAL, 150.0, ComparisonType.GREATER_THAN)
            )
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false,
                    exerciseGoals =
                        listOf(
                            goal1,
                            goal2,
                        )
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val removeGoalDeferred = async { client.removeGoalFromActiveExercise(goal1) }
        advanceMainLooperIdle()
        removeGoalDeferred.await()

        Truth.assertThat(service.goals).hasSize(1)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCapabilitiesSynchronously() = runTest {
        lateinit var passiveMonitoringCapabilities: ExerciseCapabilities
        val deferred = async { passiveMonitoringCapabilities = client.getCapabilities() }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(service.registerGetCapabilitiesRequests).hasSize(1)
        Truth.assertThat(passiveMonitoringCapabilities).isNotNull()
        Truth.assertThat(service.getTestCapabilities().toString())
            .isEqualTo(passiveMonitoringCapabilities.toString())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun getCapabilitiesSynchronously_cancelled() = runTest {
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
        var isExceptionCaught = false
        val deferred = async {
            service.setException()
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun updateExerciseTypeConfigForActiveExerciseSynchronously() = runTest {
        val exerciseConfig = ExerciseConfig.builder(ExerciseType.GOLF).build()
        val exerciseTypeConfig =
            GolfExerciseTypeConfig(
                GolfExerciseTypeConfig.GolfShotTrackingPlaceInfo
                    .GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
            )

        client.setUpdateCallback(callback)
        var deferred = async { client.startExercise(exerciseConfig) }
        advanceMainLooperIdle()
        deferred.await()
        deferred = async { client.updateExerciseTypeConfig(exerciseTypeConfig) }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(service.exerciseConfig?.exerciseTypeConfig).isEqualTo(exerciseTypeConfig)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun overrideBatchingModesForActiveExerciseSynchronously() = runTest {
        val batchingMode = HashSet<BatchingMode>()
        batchingMode.add(BatchingMode.HEART_RATE_5_SECONDS)
        client.setUpdateCallback(callback)

        var deferred = async { client.overrideBatchingModesForActiveExercise(batchingMode) }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(service.batchingModeOverrides?.size).isEqualTo(1)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun clearBatchingModesForActiveExerciseSynchronously() = runTest {
        val batchingMode = HashSet<BatchingMode>()
        batchingMode.add(BatchingMode.HEART_RATE_5_SECONDS)
        val emptyBatchingMode = HashSet<BatchingMode>()
        client.setUpdateCallback(callback)
        var deferred = async {
            // override batching mode with HEART_RATE_5_SECONDS
            client.overrideBatchingModesForActiveExercise(batchingMode)
        }
        advanceMainLooperIdle()
        deferred.await()

        deferred = async {
            // Clear existing batching modes with empty set
            client.overrideBatchingModesForActiveExercise(emptyBatchingMode)
        }
        advanceMainLooperIdle()
        deferred.await()

        Truth.assertThat(service.batchingModeOverrides?.size).isEqualTo(0)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun addDebouncedGoalToActiveExerciseShouldBeInvoked() = runTest {
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false,
                    debouncedGoals =
                        listOf(
                            DebouncedGoal.createSampleDebouncedGoal(
                                DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                                    DataType.HEART_RATE_BPM,
                                    120.0,
                                    ComparisonType.GREATER_THAN,
                                    /* initialDelay= */ 60,
                                    /* durationAtThreshold= */ 5
                                )
                            )
                        )
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val addDebouncedGoalDeferred = async {
            val proto =
                DebouncedGoal.createAggregateDebouncedGoal(
                        DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                            DataType.HEART_RATE_BPM_STATS,
                            120.0,
                            ComparisonType.GREATER_THAN,
                            /* initialDelay= */ 60,
                            /* durationAtThreshold= */ 5
                        )
                    )
                    .proto
            val debouncedGoal = DebouncedGoal.fromProto(proto)

            client.addDebouncedGoalToActiveExercise(debouncedGoal)
        }
        advanceMainLooperIdle()
        addDebouncedGoalDeferred.await()

        Truth.assertThat(service.debouncedGoals).hasSize(2)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun removeDebouncedGoalFromActiveExerciseShouldBeInvoked() = runTest {
        val debouncedGoal1 =
            DebouncedGoal.createAggregateDebouncedGoal(
                DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                    DataType.HEART_RATE_BPM_STATS,
                    120.0,
                    ComparisonType.GREATER_THAN,
                    /* initialDelay= */ 60,
                    /* durationAtThreshold= */ 5
                )
            )
        val debouncedGoal2 =
            DebouncedGoal.createSampleDebouncedGoal(
                DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                    DataType.HEART_RATE_BPM,
                    120.0,
                    ComparisonType.GREATER_THAN,
                    /* initialDelay= */ 60,
                    /* durationAtThreshold= */ 5
                )
            )
        val startExercise = async {
            val exerciseConfig =
                ExerciseConfig(
                    ExerciseType.WALKING,
                    setOf(DataType.HEART_RATE_BPM),
                    isAutoPauseAndResumeEnabled = false,
                    isGpsEnabled = false,
                    debouncedGoals = listOf(debouncedGoal1, debouncedGoal2)
                )
            client.setUpdateCallback(callback)

            client.startExercise(exerciseConfig)
        }
        advanceMainLooperIdle()
        startExercise.await()
        val removeGoalDeferred = async {
            client.removeDebouncedGoalFromActiveExercise(debouncedGoal1)
        }
        advanceMainLooperIdle()
        removeGoalDeferred.await()

        Truth.assertThat(service.debouncedGoals).hasSize(1)
    }

    class FakeExerciseUpdateCallback : ExerciseUpdateCallback {
        val availabilities = mutableMapOf<DataType<*, *>, Availability>()
        val registrationFailureThrowables = mutableListOf<Throwable>()
        var onRegisteredCalls = 0
        var onRegistrationFailedCalls = 0
        var update: ExerciseUpdate? = null

        override fun onRegistered() {
            onRegisteredCalls++
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            onRegistrationFailedCalls++
            registrationFailureThrowables.add(throwable)
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            this@FakeExerciseUpdateCallback.update = update
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            availabilities[dataType] = availability
        }
    }

    class FakeServiceStub : IExerciseApiService.Stub() {

        var listener: IExerciseUpdateListener? = null
        val registerFlushRequests = mutableListOf<FlushRequest>()
        var statusCallbackAction: (IStatusCallback?) -> Unit = { it!!.onSuccess() }
        var testExerciseStates = TestExerciseStates.UNKNOWN
        var laps = 0
        var exerciseConfig: ExerciseConfig? = null

        override fun getApiVersion(): Int = 12

        val goals = mutableListOf<ExerciseGoal<*>>()
        val debouncedGoals = mutableListOf<DebouncedGoal<*>>()
        var throwException = false
        var callingAppHasPermissions = true
        val registerGetCapabilitiesRequests = mutableListOf<CapabilitiesRequest>()
        var batchingModeOverrides: Set<BatchingMode>? = null

        override fun prepareExercise(
            prepareExerciseRequest: PrepareExerciseRequest?,
            statusCallback: IStatusCallback
        ) {
            if (throwException) {
                statusCallback.onFailure("Remote Exception")
            } else if (callingAppHasPermissions) {
                statusCallbackAction.invoke(statusCallback)
            } else {
                statusCallback.onFailure("Missing permissions")
            }
        }

        override fun startExercise(
            startExerciseRequest: StartExerciseRequest?,
            statusCallback: IStatusCallback?
        ) {
            if (callingAppHasPermissions) {
                exerciseConfig = startExerciseRequest?.exerciseConfig
                exerciseConfig?.exerciseGoals?.let { goals.addAll(it) }
                exerciseConfig?.debouncedGoals?.let { debouncedGoals.addAll(it) }
                statusCallbackAction.invoke(statusCallback)
                testExerciseStates = TestExerciseStates.STARTED
            } else {
                statusCallback?.onFailure("Missing permissions")
            }
        }

        override fun pauseExercise(packageName: String?, statusCallback: IStatusCallback?) {
            statusCallbackAction.invoke(statusCallback)
            testExerciseStates = TestExerciseStates.PAUSED
        }

        override fun resumeExercise(packageName: String?, statusCallback: IStatusCallback?) {
            statusCallbackAction.invoke(statusCallback)
            testExerciseStates = TestExerciseStates.RESUMED
        }

        override fun endExercise(packageName: String?, statusCallback: IStatusCallback?) {
            statusCallbackAction.invoke(statusCallback)
            testExerciseStates = TestExerciseStates.ENDED
        }

        override fun markLap(packageName: String?, statusCallback: IStatusCallback?) {
            laps++
            statusCallbackAction.invoke(statusCallback)
        }

        override fun getCurrentExerciseInfo(
            packageName: String?,
            exerciseInfoCallback: IExerciseInfoCallback?
        ) {
            if (throwException) {
                exerciseInfoCallback?.onFailure("Remote Exception")
            }
            if (exerciseConfig == null) {
                exerciseInfoCallback?.onExerciseInfo(
                    ExerciseInfoResponse(
                        ExerciseInfo(ExerciseTrackedStatus.UNKNOWN, ExerciseType.UNKNOWN)
                    )
                )
            } else {
                exerciseInfoCallback?.onExerciseInfo(
                    ExerciseInfoResponse(
                        ExerciseInfo(
                            ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS,
                            exerciseConfig!!.exerciseType
                        )
                    )
                )
            }
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
            if (this.listener == listener) this.listener = null
            statusCallbackAction.invoke(statusCallback)
        }

        override fun addGoalToActiveExercise(
            request: ExerciseGoalRequest?,
            statusCallback: IStatusCallback?
        ) {
            if (request != null) {
                goals.add(request.exerciseGoal)
            }
            statusCallbackAction.invoke(statusCallback)
        }

        override fun removeGoalFromActiveExercise(
            request: ExerciseGoalRequest?,
            statusCallback: IStatusCallback?
        ) {
            if (request != null) {
                goals.remove(request.exerciseGoal)
            }
            statusCallbackAction.invoke(statusCallback)
        }

        override fun addDebouncedGoalToActiveExercise(
            request: DebouncedGoalRequest?,
            statusCallback: IStatusCallback?
        ) {
            if (request != null) {
                debouncedGoals.add(request.debouncedGoal)
            }
            statusCallbackAction.invoke(statusCallback)
        }

        override fun removeDebouncedGoalFromActiveExercise(
            request: DebouncedGoalRequest?,
            statusCallback: IStatusCallback?
        ) {
            if (request != null) {
                debouncedGoals.remove(request.debouncedGoal)
            }
            statusCallbackAction.invoke(statusCallback)
        }

        override fun overrideAutoPauseAndResumeForActiveExercise(
            request: AutoPauseAndResumeConfigRequest?,
            statusCallback: IStatusCallback?
        ) {
            throw NotImplementedError()
        }

        override fun overrideBatchingModesForActiveExercise(
            batchingModeConfigRequest: BatchingModeConfigRequest?,
            statuscallback: IStatusCallback?
        ) {
            batchingModeOverrides = batchingModeConfigRequest?.batchingModeOverrides
            statusCallbackAction.invoke(statuscallback)
        }

        override fun getCapabilities(request: CapabilitiesRequest): ExerciseCapabilitiesResponse {
            if (throwException) {
                throw RemoteException("Remote Exception")
            }
            registerGetCapabilitiesRequests.add(request)
            val capabilities = getTestCapabilities()
            return ExerciseCapabilitiesResponse(capabilities)
        }

        fun getTestCapabilities(): ExerciseCapabilities {
            val exerciseTypeToCapabilitiesMapping =
                ImmutableMap.of(
                    ExerciseType.WALKING,
                    ExerciseTypeCapabilities(
                        /* supportedDataTypes= */ ImmutableSet.of(DataType.STEPS),
                        ImmutableMap.of(
                            DataType.STEPS_TOTAL,
                            ImmutableSet.of(ComparisonType.GREATER_THAN)
                        ),
                        ImmutableMap.of(
                            DataType.STEPS_TOTAL,
                            ImmutableSet.of(ComparisonType.LESS_THAN, ComparisonType.GREATER_THAN)
                        ),
                        /* supportsAutoPauseAndResume= */ false
                    ),
                    ExerciseType.RUNNING,
                    ExerciseTypeCapabilities(
                        ImmutableSet.of(DataType.HEART_RATE_BPM, DataType.SPEED),
                        ImmutableMap.of(
                            DataType.HEART_RATE_BPM_STATS,
                            ImmutableSet.of(ComparisonType.GREATER_THAN, ComparisonType.LESS_THAN),
                            DataType.SPEED_STATS,
                            ImmutableSet.of(ComparisonType.LESS_THAN)
                        ),
                        ImmutableMap.of(
                            DataType.HEART_RATE_BPM_STATS,
                            ImmutableSet.of(ComparisonType.GREATER_THAN_OR_EQUAL),
                            DataType.SPEED_STATS,
                            ImmutableSet.of(ComparisonType.LESS_THAN, ComparisonType.GREATER_THAN)
                        ),
                        /* supportsAutoPauseAndResume= */ true
                    ),
                    ExerciseType.SWIMMING_POOL,
                    ExerciseTypeCapabilities(
                        /* supportedDataTypes= */ ImmutableSet.of(),
                        /* supportedGoals= */ ImmutableMap.of(),
                        /* supportedMilestones= */ ImmutableMap.of(),
                        /* supportsAutoPauseAndResume= */ true
                    )
                )

            return ExerciseCapabilities(exerciseTypeToCapabilitiesMapping)
        }

        override fun flushExercise(request: FlushRequest, statusCallback: IStatusCallback?) {
            registerFlushRequests += request
            statusCallbackAction.invoke(statusCallback)
        }

        override fun updateExerciseTypeConfigForActiveExercise(
            updateExerciseTypeConfigRequest: UpdateExerciseTypeConfigRequest,
            statusCallback: IStatusCallback,
        ) {
            val newExerciseTypeConfig = updateExerciseTypeConfigRequest.exerciseTypeConfig
            val newExerciseConfig =
                ExerciseConfig.builder(exerciseConfig!!.exerciseType)
                    .setExerciseTypeConfig(newExerciseTypeConfig)
                    .build()
            this.exerciseConfig = newExerciseConfig
            statusCallbackAction.invoke(statusCallback)
        }

        fun setException() {
            throwException = true
        }
    }

    enum class TestExerciseStates {
        UNKNOWN,
        PREPARED,
        STARTED,
        PAUSED,
        RESUMED,
        ENDED
    }

    internal companion object {
        internal const val CLIENT = "HealthServicesExerciseClient"
        internal val CLIENT_CONFIGURATION =
            ClientConfiguration(
                CLIENT,
                IpcConstants.SERVICE_PACKAGE_NAME,
                IpcConstants.EXERCISE_API_BIND_ACTION
            )
    }
}

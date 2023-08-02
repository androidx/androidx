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
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.health.services.client.data.ComparisonType
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataPoints
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataType.Companion.STEPS_DAILY
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.HealthEvent
import androidx.health.services.client.data.HealthEvent.Type.Companion.FALL_DETECTED
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.PassiveMonitoringUpdate
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState.Companion.USER_ACTIVITY_EXERCISE
import androidx.health.services.client.data.UserActivityState.Companion.USER_ACTIVITY_PASSIVE
import androidx.health.services.client.impl.IPassiveListenerService
import androidx.health.services.client.impl.event.PassiveListenerEvent
import androidx.health.services.client.impl.response.HealthEventResponse
import androidx.health.services.client.impl.response.PassiveMonitoringGoalResponse
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class PassiveListenerServiceTest {
    private fun Int.duration() = Duration.ofSeconds(this.toLong())
    private fun Int.instant() = Instant.ofEpochMilli(this.toLong())

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var service: FakeService
    private lateinit var stub: IPassiveListenerService

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            stub = IPassiveListenerService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    @Before
    fun setUp() {
        service = FakeService()

        Shadows.shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
        Shadows.shadowOf(context)
            .setComponentNameAndServiceForBindService(
                ComponentName(context, FakeService::class.java),
                service.IPassiveListenerServiceWrapper()
            )
    }

    @Test
    fun receivesDataPoints() {
        context.bindService(
            Intent(context, FakeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        val listenerEvent = PassiveListenerEvent.createPassiveUpdateResponse(
            PassiveMonitoringUpdateResponse(
                PassiveMonitoringUpdate(
                    DataPointContainer(
                        listOf(
                            DataPoints.dailySteps(100, 10.duration(), 20.duration())
                        )
                    ),
                    listOf()
                )
            )
        )

        stub.onPassiveListenerEvent(listenerEvent)

        val dataPoint = service.dataPointsReceived!!.getData(STEPS_DAILY).first()
        assertThat(dataPoint.value).isEqualTo(100)
        assertThat(dataPoint.startDurationFromBoot).isEqualTo(10.duration())
        assertThat(dataPoint.endDurationFromBoot).isEqualTo(20.duration())
    }

    @Test
    fun receivesUserActivityState() {
        context.bindService(
            Intent(context, FakeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        val listenerEvent = PassiveListenerEvent.createPassiveUpdateResponse(
            PassiveMonitoringUpdateResponse(
                PassiveMonitoringUpdate(
                    DataPointContainer(listOf()),
                    listOf(UserActivityInfo(USER_ACTIVITY_PASSIVE, null, 42.instant()))
                )
            )
        )

        stub.onPassiveListenerEvent(listenerEvent)

        assertThat(service.userActivityReceived!!.userActivityState)
            .isEqualTo(USER_ACTIVITY_PASSIVE)
        assertThat(service.userActivityReceived!!.exerciseInfo).isNull()
        assertThat(service.userActivityReceived!!.stateChangeTime).isEqualTo(42.instant())
    }

    @Test
    fun receivesUserActivityStateWithExerciseInfo() {
        context.bindService(
            Intent(context, FakeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        val listenerEvent = PassiveListenerEvent.createPassiveUpdateResponse(
            PassiveMonitoringUpdateResponse(
                PassiveMonitoringUpdate(
                    DataPointContainer(listOf()),
                    listOf(
                        UserActivityInfo(
                            USER_ACTIVITY_EXERCISE,
                            ExerciseInfo(
                                ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS,
                                ExerciseType.RUNNING
                            ),
                            42.instant()
                        )
                    )
                )
            )
        )

        stub.onPassiveListenerEvent(listenerEvent)

        val activityInfo = service.userActivityReceived!!
        assertThat(activityInfo.userActivityState).isEqualTo(USER_ACTIVITY_EXERCISE)
        assertThat(activityInfo.stateChangeTime).isEqualTo(42.instant())
        assertThat(activityInfo.exerciseInfo!!.exerciseType).isEqualTo(ExerciseType.RUNNING)
        assertThat(activityInfo.exerciseInfo!!.exerciseTrackedStatus)
            .isEqualTo(ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS)
    }

    @Test
    fun receivesGoalCompleted() {
        context.bindService(
            Intent(context, FakeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        val listenerEvent = PassiveListenerEvent.createPassiveGoalResponse(
            PassiveMonitoringGoalResponse(
                PassiveGoal(
                    DataTypeCondition(
                        STEPS_DAILY,
                        100,
                        ComparisonType.GREATER_THAN
                    )
                )
            )
        )

        stub.onPassiveListenerEvent(listenerEvent)

        assertThat(service.goalReceived!!.dataTypeCondition.dataType).isEqualTo(STEPS_DAILY)
        assertThat(service.goalReceived!!.dataTypeCondition.threshold).isEqualTo(100)
        assertThat(service.goalReceived!!.dataTypeCondition.comparisonType)
            .isEqualTo(ComparisonType.GREATER_THAN)
    }

    @Test
    fun receivesHealthEvent() {
        context.bindService(
            Intent(context, FakeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        val listenerEvent = PassiveListenerEvent.createHealthEventResponse(
            HealthEventResponse(
                HealthEvent(
                    FALL_DETECTED,
                    42.instant(),
                    DataPointContainer(listOf(DataPoints.heartRate(42.0, 84.duration())))
                )
            )
        )

        stub.onPassiveListenerEvent(listenerEvent)

        assertThat(service.healthEventReceived!!.type).isEqualTo(FALL_DETECTED)
        assertThat(service.healthEventReceived!!.eventTime).isEqualTo(42.instant())
        val hrDataPoint =
            service.healthEventReceived!!.metrics.getData(DataType.HEART_RATE_BPM).first()
        assertThat(hrDataPoint.value).isEqualTo(42.0)
    }

    @Test
    fun isNotifiedWhenPermissionsAreLost() {
        context.bindService(
            Intent(context, FakeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )

        stub.onPassiveListenerEvent(PassiveListenerEvent.createPermissionLostResponse())

        assertThat(service.permissionLostCount).isEqualTo(1)
    }

    class FakeService : PassiveListenerService() {
        var dataPointsReceived: DataPointContainer? = null
        var userActivityReceived: UserActivityInfo? = null
        var goalReceived: PassiveGoal? = null
        var healthEventReceived: HealthEvent? = null
        var permissionLostCount = 0

        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            dataPointsReceived = dataPoints
        }

        override fun onUserActivityInfoReceived(info: UserActivityInfo) {
            userActivityReceived = info
        }

        override fun onGoalCompleted(goal: PassiveGoal) {
            goalReceived = goal
        }

        override fun onHealthEventReceived(event: HealthEvent) {
            healthEventReceived = event
        }

        override fun onPermissionLost() {
            permissionLostCount++
        }
    }
}

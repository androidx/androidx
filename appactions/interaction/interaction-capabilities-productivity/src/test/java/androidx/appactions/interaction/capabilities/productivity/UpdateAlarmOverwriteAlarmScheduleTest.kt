/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.productivity

import android.util.SizeF
import androidx.appactions.builtintypes.types.Alarm
import androidx.appactions.builtintypes.types.DayOfWeek
import androidx.appactions.builtintypes.types.Schedule
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.productivity.UpdateAlarm.OverwriteAlarmSchedule
import androidx.appactions.interaction.capabilities.productivity.UpdateAlarm.OverwriteAlarmSchedule.Arguments
import androidx.appactions.interaction.capabilities.productivity.UpdateAlarm.OverwriteAlarmSchedule.Output
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.awaitSync
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TaskInfo
import androidx.appactions.interaction.protobuf.ListValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UpdateAlarmOverwriteAlarmScheduleTest {
    private val hostProperties =
        HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()

    @Test
    fun createCapability_expectedResult() {
        val argsDeferred = CompletableDeferred<Arguments>()
        val capability = OverwriteAlarmSchedule.CapabilityBuilder()
            .setId("overwrite alarm schedule")
            .setAlarmProperty(Property<Alarm>(isRequiredForExecution = true))
            .setScheduleProperty(Property<Schedule>(isRequiredForExecution = true))
            .setExecutionCallback(
                ExecutionCallback {
                    argsDeferred.complete(it)
                    ExecutionResult.Builder<Output>().build()
                })
            .build()
        val capabilitySession = capability.createSession("fakeSessionId", hostProperties)
        val args: MutableMap<String, ParamValue> = mutableMapOf(
            "targetAlarm" to ParamValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Alarm").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("abc").build())
                ).build(),
            "operation.@type" to ParamValue.newBuilder()
                .setIdentifier("OverwriteOperation")
                .build(),
            "operation.newValue" to ParamValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Schedule").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                        .putFields("startDate", Value.newBuilder()
                            .setStringValue("2023-06-14").build())
                        .putFields("startTime", Value.newBuilder().setStringValue("10:00").build())
                        .putFields("repeatFrequency", Value.newBuilder()
                            .setStringValue("PT5M").build())
                        .putFields(
                            "byDays",
                            Value.newBuilder()
                                .setListValue(
                                    ListValue.newBuilder()
                                        .addValues(
                                            Value.newBuilder()
                                                .setStringValue("http://schema.org/Sunday")
                                                .build())
                                        .addValues(
                                            Value.newBuilder()
                                                .setStringValue("http://schema.org/Monday")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build(),
            "operation.fieldPath" to ParamValue.newBuilder().setIdentifier("alarmSchedule").build()
        )
        capabilitySession.execute(ArgumentUtils.buildArgs(args), FakeCallbackInternal())

        assertThat(capability.appAction)
            .isEqualTo(
                AppAction.newBuilder()
                    .setIdentifier("overwrite alarm schedule")
                    .setName("actions.intent.UPDATE_ALARM")
                    .addParams(IntentParameter.newBuilder()
                        .setName("targetAlarm")
                        .setIsRequired(true))
                    .addParams(IntentParameter.newBuilder()
                        .setName("operation.newValue")
                        .setIsRequired(true))
                    .addParams(
                        IntentParameter.newBuilder()
                            .setName("operation.@type")
                            .addPossibleEntities(
                                Entity.newBuilder().setIdentifier("OverwriteOperation").build())
                            .setIsRequired(true)
                            .setEntityMatchRequired(true))
                    .addParams(
                        IntentParameter.newBuilder()
                            .setName("operation.fieldPath")
                            .addPossibleEntities(
                                Entity.newBuilder().setIdentifier("alarmSchedule").build())
                            .setIsRequired(true)
                            .setEntityMatchRequired(true))
                    .setTaskInfo(TaskInfo.getDefaultInstance())
                    .build()
            )
        assertThat(argsDeferred.awaitSync())
            .isEqualTo(
                Arguments.Builder()
                    .setAlarm(AlarmReference(Alarm.Builder().setIdentifier("abc").build()))
                    .setSchedule(
                        Schedule.Builder()
                            .setIdentifier("id")
                            .setStartDate(LocalDate.of(2023, 6, 14))
                            .setStartTime(LocalTime.of(10, 0))
                            .setRepeatFrequency(Duration.ofMinutes(5))
                            .addByDay(DayOfWeek.SUNDAY)
                            .addByDay(DayOfWeek.MONDAY)
                            .build())
                    .build()
            )
    }
}

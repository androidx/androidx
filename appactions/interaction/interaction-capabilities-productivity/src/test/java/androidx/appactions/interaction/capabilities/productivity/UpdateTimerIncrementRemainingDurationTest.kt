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
import androidx.appactions.builtintypes.types.Timer
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.productivity.UpdateTimer.IncrementRemainingDuration
import androidx.appactions.interaction.capabilities.productivity.UpdateTimer.IncrementRemainingDuration.Arguments
import androidx.appactions.interaction.capabilities.productivity.UpdateTimer.IncrementRemainingDuration.Output
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.awaitSync
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TaskInfo
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UpdateTimerIncrementRemainingDurationTest {
    private val hostProperties =
        HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()

    @Test
    fun createCapability_expectedResult() {
        val argsDeferred = CompletableDeferred<Arguments>()
        val capability = IncrementRemainingDuration.CapabilityBuilder()
            .setId("increment remaining duration")
            .setTimerProperty(Property<Timer>(isRequiredForExecution = true))
            .setDurationProperty(Property<Duration>(isRequiredForExecution = true))
            .setExecutionCallback(
                ExecutionCallback {
                    argsDeferred.complete(it)
                    ExecutionResult.Builder<Output>().build()
                }
            )
            .build()
        val capabilitySession = capability.createSession("fakeSessionId", hostProperties)
        val args: MutableMap<String, ParamValue> = mutableMapOf(
            "targetTimer" to ParamValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder()
                        .putFields("@type", Value.newBuilder()
                            .setStringValue("Timer").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("abc").build())
                )
                .build(),
            "operation.@type" to ParamValue.newBuilder()
                .setIdentifier("IncrementOperation").build(),
            "operation.change" to ParamValue.newBuilder().setStringValue("PT5M").build(),
            "operation.fieldPath" to ParamValue.newBuilder()
                .setIdentifier("remainingDuration").build()
        )
        capabilitySession.execute(ArgumentUtils.buildArgs(args), FakeCallbackInternal())

        assertThat(capability.appAction)
            .isEqualTo(
                AppAction.newBuilder()
                    .setIdentifier("increment remaining duration")
                    .setName("actions.intent.UPDATE_TIMER")
                    .addParams(IntentParameter.newBuilder()
                        .setName("targetTimer")
                        .setIsRequired(true))
                    .addParams(
                        IntentParameter.newBuilder()
                            .setName("operation.change").setIsRequired(true)
                    )
                    .addParams(
                        IntentParameter.newBuilder()
                            .setName("operation.@type")
                            .addPossibleEntities(
                                Entity.newBuilder().setIdentifier("IncrementOperation").build())
                            .setIsRequired(true).setEntityMatchRequired(true)
                    )
                    .addParams(
                        IntentParameter.newBuilder()
                            .setName("operation.fieldPath")
                            .addPossibleEntities(
                                Entity.newBuilder().setIdentifier("remainingDuration").build()
                            )
                            .setIsRequired(true)
                            .setEntityMatchRequired(true)
                    )
                    .setTaskInfo(TaskInfo.getDefaultInstance())
                    .build()
            )
        assertThat(argsDeferred.awaitSync())
            .isEqualTo(
                Arguments.Builder()
                    .setTimer(TimerReference(Timer.Builder().setIdentifier("abc").build()))
                    .setDurationChange(Duration.ofMinutes(5))
                    .build()
            )
    }
}

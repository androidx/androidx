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
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.productivity.StartTimer.Arguments
import androidx.appactions.interaction.capabilities.productivity.StartTimer.Output
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.awaitSync
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TaskInfo
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StartTimerTest {
    private val hostProperties =
        HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()
    @Test
    fun createCapability_expectedResult() {
        val argsDeferred = CompletableDeferred<Arguments>()
        val capability =
            StartTimer.CapabilityBuilder()
                .setId("start timer")
                .setDurationProperty(Property<Duration>())
                .setExecutionCallback(
                    ExecutionCallback {
                        argsDeferred.complete(it)
                        ExecutionResult.Builder<Output>().build()
                    })
                .build()
        val capabilitySession = capability.createSession("fakeSessionId", hostProperties)
        val args: MutableMap<String, ParamValue> = mutableMapOf(
            "timer.duration" to ParamValue.newBuilder().setStringValue("PT5M").build(),
            "timer.identifier" to ParamValue.newBuilder().setStringValue("abc123").build(),
            "timer.name" to ParamValue.newBuilder().setStringValue("cooking").build()
        )
        // "Start timer for 5 minutes"
        capabilitySession.execute(ArgumentUtils.buildArgs(args), FakeCallbackInternal())

        assertThat(capability.appAction)
            .isEqualTo(
                AppAction.newBuilder()
                    .setIdentifier("start timer")
                    .setName("actions.intent.START_TIMER")
                    .addParams(IntentParameter.newBuilder().setName("timer.duration"))
                    .setTaskInfo(TaskInfo.getDefaultInstance())
                    .build()
            )
        assertThat(argsDeferred.awaitSync())
            .isEqualTo(
                Arguments.Builder()
                    .setDuration(Duration.ofMinutes(5))
                    .setIdentifier("abc123")
                    .setName("cooking")
                    .build()
            )
    }
}

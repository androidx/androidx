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

package androidx.appactions.interaction.capabilities.testing

import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.productivity.StartTimer
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentParam
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue
import androidx.appactions.interaction.proto.ParamValue
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CapabilityTestUtilsTest {
  // triggers StartTimer companion object init
  val unusedCapability = StartTimer.CapabilityBuilder()
    .setId("unusedStartTimer")
    .setExecutionCallback(ExecutionCallback {
        ExecutionResult.Builder<StartTimer.Output>().build()
    })
    .build()

  @Test
  fun invalidArgs_throwsException() {
    val thrown = assertThrows(IllegalArgumentException::class.java, {
      CapabilityTestUtils.createCapabilityRequest(Any())
    })
    assertThat(thrown).hasMessageThat().isEqualTo(
      "Failed to find associated capability class for arguments of class class java.lang.Object."
    )
  }

  @Test
  fun startTimerArgs_serializesArgs() {
    val startTimerArgs = StartTimer.Arguments.Builder()
      .setIdentifier("timerId")
      .setName("my timer")
      .setDuration(Duration.ofDays(3))
      .build()

    val fulfillmentRequest = CapabilityTestUtils.createCapabilityRequest(
      startTimerArgs
    ).addCapabilityIdentifier("unusedStartTimer").fulfillmentRequest

    assertThat(fulfillmentRequest).isEqualTo(
      FulfillmentRequest.newBuilder()
        .addFulfillments(
          Fulfillment.newBuilder()
            .setName(StartTimer.CAPABILITY_NAME)
            .setIdentifier("unusedStartTimer")
            .addParams(
              FulfillmentParam.newBuilder().setName("timer.identifier").addFulfillmentValues(
                FulfillmentValue.newBuilder().setValue(
                  ParamValue.newBuilder().setStringValue("timerId")
                )
              )
            )
            .addParams(
              FulfillmentParam.newBuilder().setName("timer.name").addFulfillmentValues(
                FulfillmentValue.newBuilder().setValue(
                  ParamValue.newBuilder().setStringValue("my timer")
                )
              )
            )
            .addParams(
              FulfillmentParam.newBuilder().setName("timer.duration").addFulfillmentValues(
                FulfillmentValue.newBuilder().setValue(
                  ParamValue.newBuilder().setStringValue("PT72H")
                )
              )
            )
        )
        .build()
    )
  }
}

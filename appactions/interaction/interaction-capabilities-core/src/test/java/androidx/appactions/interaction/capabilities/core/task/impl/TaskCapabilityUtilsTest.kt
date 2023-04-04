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
package androidx.appactions.interaction.capabilities.core.impl.task

import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.ParamValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TaskCapabilityUtilsTest {
    @Test
    fun isSlotFillingComplete_allRequiredParamsFilled_returnsTrue() {
        val args: MutableMap<String, List<ParamValue>> = HashMap()
        args["required"] = listOf(ParamValue.newBuilder().setStringValue("Donald").build())
        val intentParameters: MutableList<IntentParameter> = ArrayList()
        intentParameters.add(
            IntentParameter.newBuilder().setName("required").setIsRequired(true).build()
        )
        assertThat(TaskCapabilityUtils.isSlotFillingComplete(args, intentParameters)).isTrue()
    }

    @Test
    fun isSlotFillingComplete_notAllRequiredParamsFilled_returnsFalse() {
        val intentParameters: MutableList<IntentParameter> = ArrayList()
        intentParameters.add(
            IntentParameter.newBuilder().setName("required").setIsRequired(true).build()
        )
        assertThat(TaskCapabilityUtils.isSlotFillingComplete(emptyMap(), intentParameters))
            .isFalse()
    }

    @Test
    fun canSkipSlotProcessing_true() {
        val currentValues =
            listOf(
                CurrentValue.newBuilder()
                    .setValue(ParamValue.newBuilder().setBoolValue(true).build())
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build()
            )
        val fulfillmentValues =
            listOf(
                FulfillmentRequest.Fulfillment.FulfillmentValue.newBuilder()
                    .setValue(ParamValue.newBuilder().setBoolValue(true).build())
                    .build()
            )
        assertThat(TaskCapabilityUtils.canSkipSlotProcessing(currentValues, fulfillmentValues))
            .isTrue()
    }

    @Test
    fun canSkipSlotProcessing_false_sizeDifference() {
        val currentValues =
            listOf(
                CurrentValue.newBuilder()
                    .setValue(ParamValue.newBuilder().setStringValue("a").build())
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build()
            )
        val fulfillmentValues: MutableList<FulfillmentRequest.Fulfillment.FulfillmentValue> =
            ArrayList()
        fulfillmentValues.add(
            FulfillmentRequest.Fulfillment.FulfillmentValue.newBuilder()
                .setValue(ParamValue.newBuilder().setStringValue("a").build())
                .build()
        )
        fulfillmentValues.add(
            FulfillmentRequest.Fulfillment.FulfillmentValue.newBuilder()
                .setValue(ParamValue.newBuilder().setStringValue("b").build())
                .build()
        )
        assertThat(TaskCapabilityUtils.canSkipSlotProcessing(currentValues, fulfillmentValues))
            .isFalse()
    }
}

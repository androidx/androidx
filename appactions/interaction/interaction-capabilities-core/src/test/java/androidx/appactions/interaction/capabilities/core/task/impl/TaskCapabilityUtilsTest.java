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

package androidx.appactions.interaction.capabilities.core.task.impl;

import static com.google.common.truth.Truth.assertThat;

import androidx.appactions.interaction.capabilities.core.impl.converters.PropertyConverter;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.CurrentValue;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue;
import androidx.appactions.interaction.proto.ParamValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public final class TaskCapabilityUtilsTest {

    @Test
    public void isSlotFillingComplete_allRequiredParamsFilled_returnsTrue() {
        Map<String, List<ParamValue>> args = new HashMap<>();
        args.put(
                "required",
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("Donald").build()));
        List<IntentParameter> intentParameters = new ArrayList<>();
        intentParameters.add(
                PropertyConverter.getIntentParameter(
                        "required", StringProperty.newBuilder().setIsRequired(true).build()));

        assertThat(TaskCapabilityUtils.isSlotFillingComplete(args, intentParameters)).isTrue();
    }

    @Test
    public void isSlotFillingComplete_notAllRequiredParamsFilled_returnsFalse() {
        List<IntentParameter> intentParameters = new ArrayList<>();
        intentParameters.add(
                PropertyConverter.getIntentParameter(
                        "required", StringProperty.newBuilder().setIsRequired(true).build()));

        assertThat(
                TaskCapabilityUtils.isSlotFillingComplete(Collections.emptyMap(), intentParameters))
                .isFalse();
    }

    @Test
    public void canSkipSlotProcessing_true() {
        List<CurrentValue> currentValues =
                Collections.singletonList(
                        CurrentValue.newBuilder()
                                .setValue(ParamValue.newBuilder().setBoolValue(true).build())
                                .setStatus(CurrentValue.Status.ACCEPTED)
                                .build());
        List<FulfillmentValue> fulfillmentValues =
                Collections.singletonList(
                        FulfillmentValue.newBuilder()
                                .setValue(ParamValue.newBuilder().setBoolValue(true).build())
                                .build());
        assertThat(TaskCapabilityUtils.canSkipSlotProcessing(currentValues, fulfillmentValues))
                .isTrue();
    }

    @Test
    public void canSkipSlotProcessing_false_sizeDifference() {
        List<CurrentValue> currentValues =
                Collections.singletonList(
                        CurrentValue.newBuilder()
                                .setValue(ParamValue.newBuilder().setStringValue("a").build())
                                .setStatus(CurrentValue.Status.ACCEPTED)
                                .build());
        List<FulfillmentValue> fulfillmentValues = new ArrayList<>();
        fulfillmentValues.add(
                FulfillmentValue.newBuilder()
                        .setValue(ParamValue.newBuilder().setStringValue("a").build())
                        .build());
        fulfillmentValues.add(
                FulfillmentValue.newBuilder()
                        .setValue(ParamValue.newBuilder().setStringValue("b").build())
                        .build());
        assertThat(TaskCapabilityUtils.canSkipSlotProcessing(currentValues, fulfillmentValues))
                .isFalse();
    }
}

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

package androidx.appactions.interaction.capabilities.core.impl.spec

import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.testing.spec.Arguments
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
public class ActionSpecRegistryTest {
  @Test
  fun actionSpecRegistry_retrieveRegisteredSpecForArguments() {
    val arguments = Arguments.Builder()
      .setRequiredStringField("a")
      .setRepeatedStringField(listOf("a", "b", "c"))
      .build()
    val actionSpec = ActionSpecRegistry.getActionSpecForArguments(arguments)!!
    assertThat(actionSpec.capabilityName).isEqualTo("actions.intent.TEST")
    assertThat(actionSpec.serializeArguments(arguments)).isEqualTo(
      mapOf(
        "requiredString" to listOf(
          ParamValue.newBuilder().setStringValue("a").build()
        ),
        "repeatedString" to listOf(
          ParamValue.newBuilder().setStringValue("a").build(),
          ParamValue.newBuilder().setStringValue("b").build(),
          ParamValue.newBuilder().setStringValue("c").build()
        )
      )
    )
  }

  @Test
  fun actionSpecRegistry_retrieveRegisteredSpecForOutput() {
    val output = Output.Builder()
      .setOptionalStringField("a")
      .setRepeatedStringField(listOf("a", "b", "c"))
      .build()
    val actionSpec = ActionSpecRegistry.getActionSpecForOutput(output)!!
    assertThat(actionSpec.capabilityName).isEqualTo("actions.intent.TEST")
    assertThat(
      actionSpec.convertOutputToProto(output).getOutputValuesList()
    ).containsExactly(
      OutputValue.newBuilder()
        .setName("repeatedStringOutput")
        .addValues(
          ParamValue.newBuilder().setStringValue("a")
        )
        .addValues(
          ParamValue.newBuilder().setStringValue("b")
        )
        .addValues(
          ParamValue.newBuilder().setStringValue("c")
        ).build(),
      OutputValue.newBuilder()
        .setName("optionalStringOutput")
        .addValues(
          ParamValue.newBuilder().setStringValue("a")
        ).build()
    )
  }

  companion object {
    val ACTION_SPEC: ActionSpec<Arguments, Output> =
      ActionSpecBuilder.ofCapabilityNamed("actions.intent.TEST")
        .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
        .setOutput(Output::class.java)
        .bindParameter(
          "requiredString",
          Arguments::requiredStringField,
          Arguments.Builder::setRequiredStringField,
          TypeConverters.STRING_PARAM_VALUE_CONVERTER)
        .bindParameter(
          "optionalString",
          Arguments::optionalStringField,
          Arguments.Builder::setOptionalStringField,
          TypeConverters.STRING_PARAM_VALUE_CONVERTER)
        .bindRepeatedParameter(
          "repeatedString",
          Arguments::repeatedStringField,
          Arguments.Builder::setRepeatedStringField,
          TypeConverters.STRING_PARAM_VALUE_CONVERTER)
        .bindOutput(
          "optionalStringOutput",
          Output::optionalStringField,
          TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue)
        .bindRepeatedOutput(
          "repeatedStringOutput",
          Output::repeatedStringField,
          TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue)
        .build()
    init {
      ActionSpecRegistry.registerActionSpec(
        Arguments::class,
        Output::class,
        ACTION_SPEC
      )
    }
  }
}

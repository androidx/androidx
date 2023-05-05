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

package androidx.appactions.interaction.capabilities.core.testing.spec

import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import java.util.Optional

private const val CAPABILITY_NAME = "actions.intent.TEST"

@CapabilityFactory(name = CAPABILITY_NAME)
class CapabilityTwoStrings {

    class Arguments internal constructor(
        val stringSlotA: String?,
        val stringSlotB: String?,
    ) {
        override fun toString(): String {
            return "Arguments(stringSlotA=$stringSlotA, " +
                "stringSlotB=$stringSlotB)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (stringSlotA != other.stringSlotA) return false
            if (stringSlotB != other.stringSlotB) return false
            return true
        }

        override fun hashCode(): Int {
            var result = stringSlotA.hashCode()
            result += 31 * stringSlotB.hashCode()
            return result
        }

        class Builder : BuilderOf<Arguments> {
            private var stringSlotA: String? = null
            private var stringSlotB: String? = null

            fun setStringSlotA(stringSlotA: String): Builder =
                apply { this.stringSlotA = stringSlotA }

            fun setStringSlotB(stringSlotB: String): Builder =
                apply { this.stringSlotB = stringSlotB }

            override fun build(): Arguments = Arguments(stringSlotA, stringSlotB)
        }
    }

    interface ExecutionSession : BaseExecutionSession<Arguments, Void>

    companion object {
        @Suppress("UNCHECKED_CAST")
        val ACTION_SPEC = ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
            .setArguments(Arguments::class.java, Arguments::Builder)
            .bindOptionalParameter(
                "stringSlotA",
                { properties ->
                    Optional.ofNullable(properties["stringSlotA"] as Property<StringValue>) },
                Arguments.Builder::setStringSlotA,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
            .bindOptionalParameter(
                "stringSlotB",
                { properties ->
                    Optional.ofNullable(properties["stringSlotB"] as Property<StringValue>) },
                Arguments.Builder::setStringSlotB,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
            .build()
    }
}
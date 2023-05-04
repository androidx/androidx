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

package androidx.appactions.interaction.capabilities.fitness.fitness

import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import java.util.Optional

private const val CAPABILITY_NAME = "actions.intent.RESUME_EXERCISE"

/** A capability corresponding to actions.intent.RESUME_EXERCISE */
@CapabilityFactory(name = CAPABILITY_NAME)
class ResumeExercise private constructor() {
    internal enum class PropertyMapStrings(val key: String) {
        NAME("exercise.name"),
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession
            >(ACTION_SPEC) {
        private var properties = mutableMapOf<String, Property<*>>()
        fun setName(name: Property<StringValue>): CapabilityBuilder =
            apply { properties[PropertyMapStrings.NAME.key] = name }

        override fun build(): Capability {
            super.setProperty(properties)
            return super.build()
        }
    }

    class Arguments internal constructor(
        val name: String?
    ) {
        override fun toString(): String {
            return "Arguments(name=$name)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Arguments

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        class Builder : BuilderOf<Arguments> {
            private var name: String? = null

            fun setName(name: String): Builder =
                apply { this.name = name }

            override fun build(): Arguments = Arguments(name)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        // TODO(b/273602015): Update to use Name property from builtintype library.
        @Suppress("UNCHECKED_CAST")
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder)
                .setOutput(Output::class.java)
                .bindOptionalParameter(
                    "exercise.name",
                    { properties ->
                        Optional.ofNullable(
                            properties[PropertyMapStrings.NAME.key]
                                as Property<StringValue>
                        )
                    },
                    Arguments.Builder::setName,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER
                )
                .build()
    }
}

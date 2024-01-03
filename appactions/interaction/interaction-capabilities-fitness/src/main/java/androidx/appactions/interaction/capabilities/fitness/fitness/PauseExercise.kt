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
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue

/** A capability corresponding to actions.intent.PAUSE_EXERCISE */
@CapabilityFactory(name = PauseExercise.CAPABILITY_NAME)
class PauseExercise private constructor() {
    internal enum class SlotMetadata(val path: String) {
        NAME("exercise.name")
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession
            >(ACTION_SPEC) {
        fun setNameProperty(name: Property<StringValue>): CapabilityBuilder = setProperty(
            SlotMetadata.NAME.path,
            name,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
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

        class Builder {
            private var name: String? = null

            fun setName(name: String): Builder =
                apply { this.name = name }

            fun build(): Arguments = Arguments(name)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        /** Canonical name for [PauseExercise] capability */
        const val CAPABILITY_NAME = "actions.intent.PAUSE_EXERCISE"
        // TODO(b/273602015): Update to use Name property from builtintype library.
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindParameter(
                    SlotMetadata.NAME.path,
                    Arguments::name,
                    Arguments.Builder::setName,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .build()
        init {
            ActionSpecRegistry.registerActionSpec(Arguments::class, Output::class, ACTION_SPEC)
        }
    }
}

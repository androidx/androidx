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
import java.time.LocalTime

/** A capability corresponding to actions.intent.GET_EXERCISE_OBSERVATION */
@CapabilityFactory(name = GetExerciseObservation.CAPABILITY_NAME)
class GetExerciseObservation private constructor() {
    internal enum class SlotMetadata(val path: String) {
        START_TIME("exerciseObservation.startTime"),
        END_TIME("exerciseObservation.endTime")
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder, Arguments, Output, Confirmation, ExecutionSession
            >(ACTION_SPEC) {
        fun setStartTimeProperty(startTime: Property<LocalTime>): CapabilityBuilder = setProperty(
            SlotMetadata.START_TIME.path,
            startTime,
            TypeConverters.LOCAL_TIME_ENTITY_CONVERTER
        )

        fun setEndTimeProperty(endTime: Property<LocalTime>): CapabilityBuilder = setProperty(
            SlotMetadata.END_TIME.path,
            endTime,
            TypeConverters.LOCAL_TIME_ENTITY_CONVERTER
        )
    }

    class Arguments internal constructor(
        val startTime: LocalTime?,
        val endTime: LocalTime?
    ) {
        override fun toString(): String {
            return "Arguments(startTime=$startTime, endTime=$endTime)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Arguments

            if (startTime != other.startTime) return false
            if (endTime != other.endTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = startTime.hashCode()
            result += 31 * endTime.hashCode()
            return result
        }

        class Builder {
            private var startTime: LocalTime? = null
            private var endTime: LocalTime? = null

            fun setStartTime(startTime: LocalTime): Builder =
                apply { this.startTime = startTime }

            fun setEndTime(endTime: LocalTime): Builder =
                apply { this.endTime = endTime }

            fun build(): Arguments = Arguments(startTime, endTime)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        /** Canonical name for [GetExerciseObservation] capability.  */
        const val CAPABILITY_NAME = "actions.intent.GET_EXERCISE_OBSERVATION"
        // TODO(b/273602015): Update to use Name property from builtintype library.
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(
                    Arguments::class.java,
                    Arguments::Builder,
                    Arguments.Builder::build
                )
                .setOutput(Output::class.java)
                .bindParameter(
                    SlotMetadata.START_TIME.path,
                    Arguments::startTime,
                    Arguments.Builder::setStartTime,
                    TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    SlotMetadata.END_TIME.path,
                    Arguments::endTime,
                    Arguments.Builder::setEndTime,
                    TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER
                )
                .build()
        init {
            ActionSpecRegistry.registerActionSpec(Arguments::class, Output::class, ACTION_SPEC)
        }
    }
}

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

import androidx.appactions.builtintypes.types.GenericErrorStatus
import androidx.appactions.builtintypes.types.SuccessStatus
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.serializers.types.GENERIC_ERROR_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.SUCCESS_STATUS_TYPE_SPEC
import java.time.Duration

/** A capability corresponding to actions.intent.START_TIMER */
@CapabilityFactory(name = StartTimer.CAPABILITY_NAME)
class StartTimer private constructor() {
    internal enum class SlotMetadata(val path: String) {
        IDENTIFIER("timer.identifier"),
        NAME("timer.name"),
        DURATION("timer.duration")
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession
            >(ACTION_SPEC) {
        fun setIdentifierProperty(
            identifier: Property<StringValue>
        ): CapabilityBuilder = setProperty(
            SlotMetadata.IDENTIFIER.path,
            identifier,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )

        fun setNameProperty(name: Property<StringValue>): CapabilityBuilder = setProperty(
            SlotMetadata.NAME.path,
            name,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )

        fun setDurationProperty(duration: Property<Duration>): CapabilityBuilder = setProperty(
            SlotMetadata.DURATION.path,
            duration,
            TypeConverters.DURATION_ENTITY_CONVERTER
        )
    }

    class Arguments internal constructor(
        val identifier: String?,
        val name: String?,
        val duration: Duration?
    ) {
        override fun toString(): String {
            return "Arguments(identifier=$identifier,name=$name,duration=$duration)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (identifier != other.identifier) return false
            if (name != other.name) return false
            if (duration != other.duration) return false

            return true
        }

        override fun hashCode(): Int {
            var result = identifier.hashCode()
            result += 31 * name.hashCode()
            result += 31 * duration.hashCode()
            return result
        }

        class Builder {
            private var identifier: String? = null
            private var name: String? = null
            private var duration: Duration? = null

            fun setIdentifier(identifier: String): Builder = apply { this.identifier = identifier }

            fun setName(name: String): Builder = apply { this.name = name }

            fun setDuration(duration: Duration): Builder = apply { this.duration = duration }

            fun build(): Arguments = Arguments(identifier, name, duration)
        }
    }
    class Output internal constructor(val executionStatus: ExecutionStatus?) {
        override fun toString(): String {
            return "Output(executionStatus=$executionStatus)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Output

            if (executionStatus != other.executionStatus) return false

            return true
        }

        override fun hashCode(): Int {
            return executionStatus.hashCode()
        }

        class Builder {
            private var executionStatus: ExecutionStatus? = null

            fun setExecutionStatus(successStatus: SuccessStatus) = setExecutionStatus(
                ExecutionStatus(successStatus)
            )

            fun setExecutionStatus(genericErrorStatus: GenericErrorStatus) = setExecutionStatus(
                ExecutionStatus(genericErrorStatus)
            )

            fun setExecutionStatus(executionStatus: ExecutionStatus): Builder = apply {
                this.executionStatus = executionStatus
            }

            fun build(): Output = Output(executionStatus)
        }
    }

    class ExecutionStatus {
        private var successStatus: SuccessStatus? = null
        private var genericErrorStatus: GenericErrorStatus? = null

        constructor(successStatus: SuccessStatus) {
            this.successStatus = successStatus
        }

        constructor(genericErrorStatus: GenericErrorStatus) {
            this.genericErrorStatus = genericErrorStatus
        }

        companion object {
            private val TYPE_SPEC = UnionTypeSpec.Builder<ExecutionStatus>()
                .bindMemberType(
                    memberGetter = ExecutionStatus::successStatus,
                    ctor = { ExecutionStatus(it) },
                    typeSpec = SUCCESS_STATUS_TYPE_SPEC
                ).bindMemberType(
                    memberGetter = ExecutionStatus::genericErrorStatus,
                    ctor = { ExecutionStatus(it) },
                    typeSpec = GENERIC_ERROR_STATUS_TYPE_SPEC
                ).build()
            internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
        }
    }

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>
    class Confirmation internal constructor()

    companion object {
        /** Canonical name for [StartTimer] capability */
        const val CAPABILITY_NAME = "actions.intent.START_TIMER"
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindParameter(
                    SlotMetadata.IDENTIFIER.path,
                    Arguments::identifier,
                    Arguments.Builder::setIdentifier,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    SlotMetadata.NAME.path,
                    Arguments::name,
                    Arguments.Builder::setName,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    SlotMetadata.DURATION.path,
                    Arguments::duration,
                    Arguments.Builder::setDuration,
                    TypeConverters.DURATION_PARAM_VALUE_CONVERTER
                )
                .bindOutput(
                    "executionStatus",
                    Output::executionStatus,
                    ExecutionStatus.PARAM_VALUE_CONVERTER
                )
                .build()
        init {
            ActionSpecRegistry.registerActionSpec(Arguments::class, Output::class, ACTION_SPEC)
        }
    }
}

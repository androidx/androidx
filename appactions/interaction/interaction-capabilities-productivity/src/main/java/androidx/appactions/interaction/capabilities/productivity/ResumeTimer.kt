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
import androidx.appactions.builtintypes.types.Timer
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.serializers.types.GENERIC_ERROR_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.SUCCESS_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.TIMER_TYPE_SPEC

/** A capability corresponding to actions.intent.RESUME_TIMER */
@CapabilityFactory(name = ResumeTimer.CAPABILITY_NAME)
class ResumeTimer private constructor() {
    internal enum class SlotMetadata(val path: String) {
        TIMER("timer")
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession
            >(ACTION_SPEC) {
        fun setTimerProperty(timer: Property<Timer>): CapabilityBuilder = setProperty(
            SlotMetadata.TIMER.path,
            timer,
            EntityConverter.of(TIMER_TYPE_SPEC)
        )
    }

    class Arguments internal constructor(val timerList: List<TimerReference>) {
        override fun toString(): String {
            return "Arguments(timerList=$timerList)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (timerList != other.timerList) return false

            return true
        }

        override fun hashCode(): Int {
            return timerList.hashCode()
        }

        class Builder {
            private var timerList: List<TimerReference> = emptyList()

            fun setTimerList(
                timerList: List<TimerReference>
            ): Builder = apply { this.timerList = timerList }

            fun build(): Arguments = Arguments(timerList)
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

    class Confirmation internal constructor()

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        /** Canonical name for [ResumeTimer] capability */
        const val CAPABILITY_NAME = "actions.intent.RESUME_TIMER"
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindRepeatedParameter(
                    SlotMetadata.TIMER.path,
                    Arguments::timerList,
                    Arguments.Builder::setTimerList,
                    TimerReference.PARAM_VALUE_CONVERTER
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

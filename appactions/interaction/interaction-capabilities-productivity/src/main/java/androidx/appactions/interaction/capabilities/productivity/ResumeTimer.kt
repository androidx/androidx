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

import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater
import androidx.appactions.interaction.capabilities.core.values.GenericErrorStatus
import androidx.appactions.interaction.capabilities.core.values.SuccessStatus
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import java.util.Optional

/** ResumeTimer.kt in interaction-capabilities-productivity */
private const val CAPABILITY_NAME = "actions.intent.RESUME_TIMER"

private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(ResumeTimer.Property::class.java)
        .setArgument(ResumeTimer.Argument::class.java, ResumeTimer.Argument::Builder)
        .setOutput(ResumeTimer.Output::class.java)
        .bindRepeatedParameter(
            "timer",
            { property -> Optional.ofNullable(property.timerList) },
            ResumeTimer.Argument.Builder::setTimerList,
            TimerValue.PARAM_VALUE_CONVERTER,
            TimerValue.ENTITY_CONVERTER
        )
        .bindOptionalOutput(
            "executionStatus",
            { output -> Optional.ofNullable(output.executionStatus) },
            ResumeTimer.ExecutionStatus::toParamValue
        )
        .build()

// TODO(b/267806701): Add capability factory annotation once the testing library is fully migrated.
class ResumeTimer private constructor() {

    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Argument, Output, Confirmation, TaskUpdater, Session
        >(ACTION_SPEC) {
        override fun build(): Capability {
            super.setProperty(Property.Builder().build())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property internal constructor(val timerList: TypeProperty<TimerValue>?) {
        override fun toString(): String {
            return "Property(timerList=$timerList}"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Property

            if (timerList != other.timerList) return false

            return true
        }

        override fun hashCode(): Int {
            return timerList.hashCode()
        }

        class Builder {
            private var timerList: TypeProperty<TimerValue>? = null

            fun setTimerList(timerList: TypeProperty<TimerValue>): Builder = apply {
                this.timerList = timerList
            }

            fun build(): Property = Property(timerList)
        }
    }

    class Argument internal constructor(val timerList: List<TimerValue>?) {
        override fun toString(): String {
            return "Argument(timerList=$timerList)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Argument

            if (timerList != other.timerList) return false

            return true
        }

        override fun hashCode(): Int {
            return timerList.hashCode()
        }

        class Builder : BuilderOf<Argument> {
            private var timerList: List<TimerValue>? = null

            fun setTimerList(
                timerList: List<TimerValue>,
            ): Builder = apply { this.timerList = timerList }

            override fun build(): Argument = Argument(timerList)
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

        internal fun toParamValue(): ParamValue {
            var status: String = ""
            if (successStatus != null) {
                status = successStatus.toString()
            }
            if (genericErrorStatus != null) {
                status = genericErrorStatus.toString()
            }
            val value: Value = Value.newBuilder().setStringValue(status).build()
            return ParamValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder().putFields(TypeConverters.FIELD_NAME_TYPE, value).build(),
                )
                .build()
        }
    }

    class Confirmation internal constructor()

    class TaskUpdater internal constructor() : AbstractTaskUpdater()

    sealed interface Session : BaseSession<Argument, Output>
}

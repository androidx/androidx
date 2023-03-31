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

import androidx.appactions.interaction.capabilities.core.ActionCapability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty
import androidx.appactions.interaction.capabilities.core.task.ValueListener
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater
import androidx.appactions.interaction.capabilities.core.values.GenericErrorStatus
import androidx.appactions.interaction.capabilities.core.values.SuccessStatus
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import java.time.Duration
import java.util.Optional

/** StartTimer.kt in interaction-capabilities-productivity */
private const val CAPABILITY_NAME = "actions.intent.START_TIMER"

private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(StartTimer.Property::class.java)
        .setArgument(StartTimer.Argument::class.java, StartTimer.Argument::Builder)
        .setOutput(StartTimer.Output::class.java)
        .bindOptionalParameter(
            "timer.identifier",
            { property -> Optional.ofNullable(property.identifier) },
            StartTimer.Argument.Builder::setIdentifier,
            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
        .bindOptionalParameter(
            "timer.name",
            { property -> Optional.ofNullable(property.name) },
            StartTimer.Argument.Builder::setName,
            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
        .bindOptionalParameter(
            "timer.duration",
            { property -> Optional.ofNullable(property.duration) },
            StartTimer.Argument.Builder::setDuration,
            TypeConverters.DURATION_PARAM_VALUE_CONVERTER,
            TypeConverters.DURATION_ENTITY_CONVERTER
        )
        .bindOptionalOutput(
            "executionStatus",
            { output -> Optional.ofNullable(output.executionStatus) },
            StartTimer.ExecutionStatus::toParamValue
        )
        .build()

// TODO(b/267806701): Add capability factory annotation once the testing library is fully migrated.
class StartTimer private constructor() {

    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Argument, Output, Confirmation, TaskUpdater, Session
        >(ACTION_SPEC) {

        fun setSessionFactory(): CapabilityBuilder {
            return this
        }

        override fun build(): ActionCapability {
            super.setProperty(Property.Builder().build())
            return super.build()
        }
    }

    fun interface SessionFactory {
        fun createSession(hostProperties: HostProperties): Session
    }

    interface Session : BaseSession<Argument, Output> {
        val nameListener: ValueListener<String>?
            get() = null
        val durationListener: ValueListener<Duration>?
            get() = null
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property
    internal constructor(
        val identifier: TypeProperty<StringValue>?,
        val name: TypeProperty<StringValue>?,
        val duration: TypeProperty<Duration>?
    ) {
        override fun toString(): String {
            return "Property(identifier=$identifier,name=$name,duration=$duration}"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Property

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
            private var identifier: TypeProperty<StringValue>? = null
            private var name: TypeProperty<StringValue>? = null
            private var duration: TypeProperty<Duration>? = null

            fun setIdentifier(identifier: TypeProperty<StringValue>): Builder = apply {
                this.identifier = identifier
            }

            fun setName(name: TypeProperty<StringValue>): Builder = apply { this.name = name }

            fun setDuration(duration: TypeProperty<Duration>): Builder = apply {
                this.duration = duration
            }

            fun build(): Property = Property(identifier, name, duration)
        }
    }

    class Argument
    internal constructor(val identifier: String?, val name: String?, val duration: Duration?) {
        override fun toString(): String {
            return "Argument(identifier=$identifier,name=$name,duration=$duration)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Argument

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

        class Builder : BuilderOf<Argument> {
            private var identifier: String? = null
            private var name: String? = null
            private var duration: Duration? = null

            fun setIdentifier(identifier: String): Builder = apply { this.identifier = identifier }

            fun setName(name: String): Builder = apply { this.name = name }

            fun setDuration(duration: Duration): Builder = apply { this.duration = duration }

            override fun build(): Argument = Argument(identifier, name, duration)
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
}

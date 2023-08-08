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

import androidx.appactions.builtintypes.types.Alarm
import androidx.appactions.builtintypes.types.GenericErrorStatus
import androidx.appactions.builtintypes.types.Schedule
import androidx.appactions.builtintypes.types.SuccessStatus
import androidx.appactions.builtintypes.types.UnsupportedOperationStatus
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.OperationType
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.serializers.types.ALARM_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.SCHEDULE_TYPE_SPEC
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value

@CapabilityFactory(name = UpdateAlarm.CAPABILITY_NAME)
class UpdateAlarm private constructor() {
    class OverwriteAlarmSchedule private constructor() {
        internal enum class SlotMetadata(val path: String) {
            ALARM("targetAlarm"),
            OPERATION("operation.@type"),
            NEW_VALUE("operation.newValue"),
            FIELD_PATH("operation.fieldPath")
        }

        class CapabilityBuilder :
            Capability.Builder<
                CapabilityBuilder,
                Arguments,
                Output,
                Confirmation,
                ExecutionSession
                >(ACTION_SPEC) {
                    fun setAlarmProperty(alarm: Property<Alarm>): CapabilityBuilder = setProperty(
                        SlotMetadata.ALARM.path,
                        alarm,
                        EntityConverter.of(ALARM_TYPE_SPEC)
                    )
                    fun setScheduleProperty(schedule: Property<Schedule>): CapabilityBuilder =
                        setProperty(
                            SlotMetadata.NEW_VALUE.path,
                            schedule,
                            EntityConverter.of(SCHEDULE_TYPE_SPEC)
                        )

                    override fun build(): Capability {
                        super.setProperty(
                            SlotMetadata.OPERATION.path,
                            Property<SupportedOperationType>(
                                possibleValues = listOf(SupportedOperationType.OVERWRITE_OPERATION),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true
                            ),
                            TypeConverters.createEnumEntityConverter(
                                listOf(SupportedOperationType.OVERWRITE_OPERATION)
                            )
                        )
                        super.setProperty(
                            SlotMetadata.FIELD_PATH.path,
                            Property<FieldPath>(
                                possibleValues = listOf(FieldPath.ALARM_SCHEDULE),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true
                            ),
                            TypeConverters.createEnumEntityConverter(
                                listOf(FieldPath.ALARM_SCHEDULE)
                            )
                        )
                        return super.build()
                    }
                }

        class Arguments internal constructor(val alarm: AlarmReference?, val schedule: Schedule?) {
            override fun toString(): String {
                return "Arguments(alarm=$alarm, schedule=$schedule)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Arguments

                if (alarm != other.alarm) return false
                if (schedule != other.schedule) return false

                return true
            }

            override fun hashCode(): Int {
                var result = alarm.hashCode()
                result += 31 * schedule.hashCode()
                return result
            }

            class Builder {
                private var alarm: AlarmReference? = null
                private var schedule: Schedule? = null

                fun setAlarm(alarm: AlarmReference): Builder = apply { this.alarm = alarm }
                fun setSchedule(schedule: Schedule): Builder = apply { this.schedule = schedule }

                fun build(): Arguments = Arguments(alarm, schedule)
            }
        }

        class Output internal constructor(
            val executionStatus: ExecutionStatus?,
            val alarm: Alarm?
        ) {
            override fun toString(): String {
                return "Output(executionStatus=$executionStatus,alarm=$alarm)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Output

                if (executionStatus != other.executionStatus) return false
                if (alarm != other.alarm) return false

                return true
            }

            override fun hashCode(): Int {
                var result = executionStatus.hashCode()
                result += 31 * alarm.hashCode()
                return result
            }

            class Builder {
                private var executionStatus: ExecutionStatus? = null
                private var alarm: Alarm? = null

                fun setExecutionStatus(executionStatus: ExecutionStatus): Builder = apply {
                    this.executionStatus = executionStatus
                }

                fun setExecutionStatus(successStatus: SuccessStatus): Builder = apply {
                    this.setExecutionStatus(ExecutionStatus(successStatus))
                }

                fun setExecutionStatus(genericErrorStatus: GenericErrorStatus): Builder = apply {
                    this.setExecutionStatus(ExecutionStatus(genericErrorStatus))
                }

                fun setExecutionStatus(unsupportedOperationStatus: UnsupportedOperationStatus):
                    Builder = apply {
                        this.setExecutionStatus(ExecutionStatus(unsupportedOperationStatus))
                }

                fun setAlarm(alarm: Alarm): Builder = apply { this.alarm = alarm }

                fun build(): Output = Output(executionStatus, alarm)
            }
        }

        class ExecutionStatus {
            private var successStatus: SuccessStatus? = null
            private var genericErrorStatus: GenericErrorStatus? = null
            private var unsupportedOperationStatus: UnsupportedOperationStatus? = null

            constructor(successStatus: SuccessStatus) {
                this.successStatus = successStatus
            }

            constructor(genericErrorStatus: GenericErrorStatus) {
                this.genericErrorStatus = genericErrorStatus
            }

            constructor(unsupportedOperationStatus: UnsupportedOperationStatus) {
                this.unsupportedOperationStatus = unsupportedOperationStatus
            }

            internal fun toParamValue(): ParamValue {
                var status: String = ""
                if (successStatus != null) {
                    status = successStatus.toString()
                }
                if (genericErrorStatus != null) {
                    status = genericErrorStatus.toString()
                }
                if (unsupportedOperationStatus != null) {
                    status = unsupportedOperationStatus.toString()
                }

                val value: Value = Value.newBuilder().setStringValue(status).build()
                return ParamValue.newBuilder()
                    .setStructValue(
                        Struct.newBuilder().putFields(TypeConverters.FIELD_NAME_TYPE, value).build()
                    )
                    .build()
            }
        }

        private class SupportedOperationType private constructor(val operationType: OperationType) {

            override fun toString(): String {
                return operationType.toString()
            }
            companion object {
                val OVERWRITE_OPERATION = SupportedOperationType(OperationType.OVERWRITE_OPERATION)
            }
        }

        private class FieldPath private constructor(val path: String) {

            override fun toString(): String {
                return path
            }
            companion object {
                val ALARM_SCHEDULE = FieldPath("alarmSchedule")
            }
        }

        sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>
        class Confirmation internal constructor()

        companion object {
            private val ACTION_SPEC =
                ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setArguments(Arguments::class.java,
                        Arguments::Builder,
                        Arguments.Builder::build
                    )
                    .setOutput(Output::class.java)
                    .bindParameter(
                        SlotMetadata.ALARM.path,
                        Arguments::alarm,
                        Arguments.Builder::setAlarm,
                        AlarmReference.PARAM_VALUE_CONVERTER
                    )
                    .bindParameter(
                        SlotMetadata.NEW_VALUE.path,
                        Arguments::schedule,
                        Arguments.Builder::setSchedule,
                        ParamValueConverter.of(SCHEDULE_TYPE_SPEC)
                    )
                    .bindParameter(
                        SlotMetadata.OPERATION.path,
                        { SupportedOperationType.OVERWRITE_OPERATION },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(SupportedOperationType.OVERWRITE_OPERATION)
                        )
                    )
                    .bindParameter(
                        SlotMetadata.FIELD_PATH.path,
                        { FieldPath.ALARM_SCHEDULE },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(FieldPath.ALARM_SCHEDULE)
                        )
                    )
                    .bindOutput(
                        "alarm",
                        Output::alarm,
                        ParamValueConverter.of(ALARM_TYPE_SPEC)::toParamValue
                    )
                    .bindOutput(
                        "executionStatus",
                        Output::executionStatus,
                        ExecutionStatus::toParamValue
                    )
                    .build()
            init {
                ActionSpecRegistry.registerActionSpec(Arguments::class, Output::class, ACTION_SPEC)
            }
        }
    }

    class OverwriteName private constructor() {
        internal enum class SlotMetadata(val path: String) {
            ALARM("targetAlarm"),
            OPERATION("operation.@type"),
            NEW_VALUE("operation.newValue"),
            FIELD_PATH("operation.fieldPath")
        }

        class CapabilityBuilder :
            Capability.Builder<
                CapabilityBuilder,
                Arguments,
                Output,
                Confirmation,
                ExecutionSession
                >(ACTION_SPEC) {
            fun setAlarmProperty(alarm: Property<Alarm>): CapabilityBuilder = setProperty(
                SlotMetadata.ALARM.path,
                alarm,
                EntityConverter.of(ALARM_TYPE_SPEC)
            )
            fun setNameProperty(schedule: Property<StringValue>): CapabilityBuilder =
                setProperty(
                    SlotMetadata.NEW_VALUE.path,
                    schedule,
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER
                )

            override fun build(): Capability {
                        super.setProperty(
                            SlotMetadata.OPERATION.path,
                            Property<SupportedOperationType>(
                                possibleValues = listOf(SupportedOperationType.OVERWRITE_OPERATION),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true
                            ),
                            TypeConverters.createEnumEntityConverter(
                                listOf(SupportedOperationType.OVERWRITE_OPERATION)
                            )
                        )
                        super.setProperty(
                            SlotMetadata.FIELD_PATH.path,
                            Property<FieldPath>(
                                possibleValues = listOf(FieldPath.NAME),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true
                            ),
                            TypeConverters.createEnumEntityConverter(listOf(FieldPath.NAME))
                        )
                return super.build()
            }
        }

        class Arguments internal constructor(val alarm: AlarmReference?, val name: String?) {
            override fun toString(): String {
                return "Arguments(alarm=$alarm,name=$name)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Arguments

                if (alarm != other.alarm) return false
                if (name != other.name) return false

                return true
            }

            override fun hashCode(): Int {
                var result = alarm.hashCode()
                result += 31 * name.hashCode()
                return result
            }

            class Builder {
                private var alarm: AlarmReference? = null
                private var name: String? = null

                fun setAlarm(alarm: AlarmReference): Builder = apply { this.alarm = alarm }
                fun setName(name: String): Builder = apply { this.name = name }

                fun build(): Arguments = Arguments(alarm, name)
            }
        }

        class Output internal constructor(
            val executionStatus: ExecutionStatus?,
            val alarm: Alarm?
        ) {
            override fun toString(): String {
                return "Output(executionStatus=$executionStatus,alarm=$alarm)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Output

                if (executionStatus != other.executionStatus) return false
                if (alarm != other.alarm) return false

                return true
            }

            override fun hashCode(): Int {
                var result = executionStatus.hashCode()
                result += 31 * alarm.hashCode()
                return result
            }

            class Builder {
                private var executionStatus: ExecutionStatus? = null
                private var alarm: Alarm? = null

                fun setExecutionStatus(executionStatus: ExecutionStatus): Builder = apply {
                    this.executionStatus = executionStatus
                }

                fun setExecutionStatus(successStatus: SuccessStatus): Builder = apply {
                    this.setExecutionStatus(ExecutionStatus(successStatus))
                }

                fun setExecutionStatus(genericErrorStatus: GenericErrorStatus): Builder = apply {
                    this.setExecutionStatus(ExecutionStatus(genericErrorStatus))
                }

                fun setExecutionStatus(unsupportedOperationStatus: UnsupportedOperationStatus):
                    Builder = apply {
                    this.setExecutionStatus(ExecutionStatus(unsupportedOperationStatus))
                }

                fun setAlarm(alarm: Alarm): Builder = apply { this.alarm = alarm }

                fun build(): Output = Output(executionStatus, alarm)
            }
        }

        class ExecutionStatus {
            private var successStatus: SuccessStatus? = null
            private var genericErrorStatus: GenericErrorStatus? = null
            private var unsupportedOperationStatus: UnsupportedOperationStatus? = null

            constructor(successStatus: SuccessStatus) {
                this.successStatus = successStatus
            }

            constructor(genericErrorStatus: GenericErrorStatus) {
                this.genericErrorStatus = genericErrorStatus
            }

            constructor(unsupportedOperationStatus: UnsupportedOperationStatus) {
                this.unsupportedOperationStatus = unsupportedOperationStatus
            }

            internal fun toParamValue(): ParamValue {
                var status: String = ""
                if (successStatus != null) {
                    status = successStatus.toString()
                }
                if (genericErrorStatus != null) {
                    status = genericErrorStatus.toString()
                }
                if (unsupportedOperationStatus != null) {
                    status = unsupportedOperationStatus.toString()
                }

                val value: Value = Value.newBuilder().setStringValue(status).build()
                return ParamValue.newBuilder()
                    .setStructValue(
                        Struct.newBuilder().putFields(TypeConverters.FIELD_NAME_TYPE, value).build()
                    )
                    .build()
            }
        }

        private class SupportedOperationType private constructor(val operationType: OperationType) {

            override fun toString(): String {
                return operationType.toString()
            }
            companion object {
                val OVERWRITE_OPERATION = SupportedOperationType(OperationType.OVERWRITE_OPERATION)
            }
        }

        private class FieldPath private constructor(val path: String) {

            override fun toString(): String {
                return path
            }
            companion object {
                val NAME = FieldPath("name")
            }
        }

        sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>
        class Confirmation internal constructor()

        companion object {
            private val ACTION_SPEC =
                ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setArguments(Arguments::class.java,
                        Arguments::Builder,
                        Arguments.Builder::build
                    )
                    .setOutput(Output::class.java)
                    .bindParameter(
                        SlotMetadata.ALARM.path,
                        Arguments::alarm,
                        Arguments.Builder::setAlarm,
                        AlarmReference.PARAM_VALUE_CONVERTER
                    )
                    .bindParameter(
                        SlotMetadata.NEW_VALUE.path,
                        Arguments::name,
                        Arguments.Builder::setName,
                        TypeConverters.STRING_PARAM_VALUE_CONVERTER
                    )
                    .bindParameter(
                        SlotMetadata.OPERATION.path,
                        { SupportedOperationType.OVERWRITE_OPERATION },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(SupportedOperationType.OVERWRITE_OPERATION)
                        )
                    )
                    .bindParameter(
                        SlotMetadata.FIELD_PATH.path,
                        { FieldPath.NAME },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(FieldPath.NAME)
                        )
                    )
                    .bindOutput(
                        "alarm",
                        Output::alarm,
                        ParamValueConverter.of(ALARM_TYPE_SPEC)::toParamValue
                    )
                    .bindOutput(
                        "executionStatus",
                        Output::executionStatus,
                        ExecutionStatus::toParamValue
                    )
                    .build()
            init {
                ActionSpecRegistry.registerActionSpec(Arguments::class, Output::class, ACTION_SPEC)
            }
        }
    }

    companion object {
        const val CAPABILITY_NAME = "actions.intent.UPDATE_ALARM"
    }
}

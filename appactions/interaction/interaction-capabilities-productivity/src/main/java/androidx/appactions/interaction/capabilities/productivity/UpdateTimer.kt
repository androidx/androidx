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
import androidx.appactions.builtintypes.types.UnsupportedOperationStatus
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.OperationType
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.serializers.types.GENERIC_ERROR_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.SUCCESS_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.TIMER_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.UNSUPPORTED_OPERATION_STATUS_TYPE_SPEC
import java.time.Duration

@CapabilityFactory(name = UpdateTimer.CAPABILITY_NAME)
class UpdateTimer private constructor() {
    class IncrementRemainingDuration private constructor() {
        internal enum class SlotMetadata(val path: String) {
            TIMER("targetTimer"),
            OPERATION("operation.@type"),
            CHANGE("operation.change"),
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
                    fun setTimerProperty(timer: Property<Timer>): CapabilityBuilder = setProperty(
                        SlotMetadata.TIMER.path,
                        timer,
                        EntityConverter.of(TIMER_TYPE_SPEC)
                    )
                    fun setDurationProperty(duration: Property<Duration>): CapabilityBuilder =
                        setProperty(
                            SlotMetadata.CHANGE.path,
                            duration,
                            TypeConverters.DURATION_ENTITY_CONVERTER
                        )

                    override fun build(): Capability {
                        super.setProperty(
                            SlotMetadata.OPERATION.path,
                            Property<SupportedOperationType>(
                                possibleValues = listOf(SupportedOperationType.INCREMENT_OPERATION),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true),
                            TypeConverters.createEnumEntityConverter(
                                listOf(SupportedOperationType.INCREMENT_OPERATION)
                            )
                        )
                        super.setProperty(
                            SlotMetadata.FIELD_PATH.path,
                            Property<FieldPath>(
                                possibleValues = listOf(FieldPath.REMAINING_DURATION),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true),
                            TypeConverters.createEnumEntityConverter(
                                listOf(FieldPath.REMAINING_DURATION)
                            )
                        )
                        return super.build()
                    }
                }

        class Arguments internal constructor(
            val timer: TimerReference?,
            val durationChange: Duration?,
        ) {
            override fun toString(): String {
                return "Arguments(timer=$timer,durationChange=$durationChange)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Arguments

                if (timer != other.timer) return false
                if (durationChange != other.durationChange) return false

                return true
            }

            override fun hashCode(): Int {
                var result = timer.hashCode()
                result += 31 * durationChange.hashCode()
                return result
            }

            class Builder {
                private var timer: TimerReference? = null
                private var durationChange: Duration? = null

                fun setTimer(timer: TimerReference): Builder = apply { this.timer = timer }
                fun setDurationChange(durationChange: Duration): Builder = apply {
                    this.durationChange = durationChange
                }

                fun build(): Arguments = Arguments(timer, durationChange)
            }
        }

        class Output internal constructor(
            val executionStatus: ExecutionStatus?,
            val timer: Timer?
        ) {
            override fun toString(): String {
                return "Output(executionStatus=$executionStatus,timer=$timer)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Output

                if (executionStatus != other.executionStatus) return false
                if (timer != other.timer) return false
                return true
            }

            override fun hashCode(): Int {
                var result = executionStatus.hashCode()
                result += 31 * timer.hashCode()
                return result
            }

            class Builder {
                private var executionStatus: ExecutionStatus? = null
                private var timer: Timer? = null

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
                fun setTimer(timer: Timer): Builder = apply { this.timer = timer }

                fun build(): Output = Output(executionStatus, timer)
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
                    ).bindMemberType(
                        memberGetter = ExecutionStatus::unsupportedOperationStatus,
                        ctor = { ExecutionStatus(it) },
                        typeSpec = UNSUPPORTED_OPERATION_STATUS_TYPE_SPEC
                    ).build()
                internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
            }
        }

        private class SupportedOperationType private constructor(val operationType: OperationType) {

            override fun toString(): String {
                return operationType.toString()
            }

            companion object {
                val INCREMENT_OPERATION = SupportedOperationType(OperationType.INCREMENT_OPERATION)
            }
        }

        private class FieldPath private constructor(val path: String) {

            override fun toString(): String {
                return path
            }

            companion object {
                val REMAINING_DURATION = FieldPath("remainingDuration")
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
                        SlotMetadata.TIMER.path,
                        Arguments::timer,
                        Arguments.Builder::setTimer,
                        TimerReference.PARAM_VALUE_CONVERTER
                    )
                    .bindParameter(
                        SlotMetadata.CHANGE.path,
                        Arguments::durationChange,
                        Arguments.Builder::setDurationChange,
                        TypeConverters.DURATION_PARAM_VALUE_CONVERTER
                    )
                    .bindParameter(
                        SlotMetadata.OPERATION.path,
                        { SupportedOperationType.INCREMENT_OPERATION },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(SupportedOperationType.INCREMENT_OPERATION)
                        )
                    )
                    .bindParameter(
                        SlotMetadata.FIELD_PATH.path,
                        { FieldPath.REMAINING_DURATION },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(FieldPath.REMAINING_DURATION)
                        )
                    )
                    .bindOutput(
                        "timer",
                        Output::timer,
                        ParamValueConverter.of(TIMER_TYPE_SPEC)
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

    class OverwriteRemainingDuration private constructor() {
        internal enum class SlotMetadata(val path: String) {
            TIMER("targetTimer"),
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
                    fun setTimerProperty(timer: Property<Timer>): CapabilityBuilder = setProperty(
                        SlotMetadata.TIMER.path,
                        timer,
                        EntityConverter.of(TIMER_TYPE_SPEC)
                    )

                    fun setDurationProperty(duration: Property<Duration>): CapabilityBuilder =
                        setProperty(
                            SlotMetadata.NEW_VALUE.path,
                            duration,
                            TypeConverters.DURATION_ENTITY_CONVERTER
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
                                possibleValues = listOf(FieldPath.REMAINING_DURATION),
                                isRequiredForExecution = true,
                                shouldMatchPossibleValues = true
                            ),
                            TypeConverters.createEnumEntityConverter(
                                listOf(FieldPath.REMAINING_DURATION)
                            )
                        )
                        return super.build()
                    }
        }

        class Arguments internal constructor(val timer: TimerReference?, val duration: Duration?) {
            override fun toString(): String {
                return "Arguments(timer=$timer,duration=$duration)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Arguments

                if (timer != other.timer) return false
                if (duration != other.duration) return false

                return true
            }

            override fun hashCode(): Int {
                var result = timer.hashCode()
                result += 31 * duration.hashCode()
                return result
            }

            class Builder {
                private var timer: TimerReference? = null
                private var duration: Duration? = null

                fun setTimer(timer: TimerReference): Builder = apply { this.timer = timer }
                fun setDuration(duration: Duration): Builder = apply { this.duration = duration }

                fun build(): Arguments = Arguments(timer, duration)
            }
        }

        class Output internal constructor(
            val executionStatus: ExecutionStatus?,
            val timer: Timer?
        ) {
            override fun toString(): String {
                return "Output(executionStatus=$executionStatus,timer=$timer)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Output

                if (executionStatus != other.executionStatus) return false
                if (timer != other.timer) return false
                return true
            }

            override fun hashCode(): Int {
                var result = executionStatus.hashCode()
                result += 31 * timer.hashCode()
                return result
            }

            class Builder {
                private var executionStatus: ExecutionStatus? = null
                private var timer: Timer? = null

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
                fun setTimer(timer: Timer): Builder = apply { this.timer = timer }

                fun build(): Output = Output(executionStatus, timer)
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
                    ).bindMemberType(
                        memberGetter = ExecutionStatus::unsupportedOperationStatus,
                        ctor = { ExecutionStatus(it) },
                        typeSpec = UNSUPPORTED_OPERATION_STATUS_TYPE_SPEC
                    ).build()
                internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
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
                val REMAINING_DURATION = FieldPath("remainingDuration")
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
                        SlotMetadata.TIMER.path,
                        Arguments::timer,
                        Arguments.Builder::setTimer,
                        TimerReference.PARAM_VALUE_CONVERTER
                    )
                    .bindParameter(
                        SlotMetadata.NEW_VALUE.path,
                        Arguments::duration,
                        Arguments.Builder::setDuration,
                        TypeConverters.DURATION_PARAM_VALUE_CONVERTER
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
                        { FieldPath.REMAINING_DURATION },
                        { _, _ -> },
                        TypeConverters.createEnumParamValueConverter(
                            listOf(FieldPath.REMAINING_DURATION)
                        )
                    )
                    .bindOutput(
                        "timer",
                        Output::timer,
                        ParamValueConverter.of(TIMER_TYPE_SPEC)
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

    class OverwriteName private constructor() {
        internal enum class SlotMetadata(val path: String) {
            TIMER("targetTimer"),
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
                    fun setTimerProperty(timer: Property<Timer>): CapabilityBuilder = setProperty(
                        SlotMetadata.TIMER.path,
                        timer,
                        EntityConverter.of(TIMER_TYPE_SPEC)
                    )

                    fun setNameProperty(name: Property<StringValue>): CapabilityBuilder =
                        setProperty(
                            SlotMetadata.NEW_VALUE.path,
                            name,
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

        class Arguments internal constructor(val timer: TimerReference?, val name: String?) {
            override fun toString(): String {
                return "Arguments(timer=$timer,name=$name)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Arguments

                if (timer != other.timer) return false
                if (name != other.name) return false

                return true
            }

            override fun hashCode(): Int {
                var result = timer.hashCode()
                result += 31 * name.hashCode()
                return result
            }

            class Builder {
                private var timer: TimerReference? = null
                private var name: String? = null

                fun setTimer(timer: TimerReference): Builder = apply { this.timer = timer }
                fun setName(name: String): Builder = apply { this.name = name }

                fun build(): Arguments = Arguments(timer, name)
            }
        }

        class Output internal constructor(
            val executionStatus: ExecutionStatus?,
            val timer: Timer?
        ) {
            override fun toString(): String {
                return "Output(executionStatus=$executionStatus,timer=$timer)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Output

                if (executionStatus != other.executionStatus) return false
                if (timer != other.timer) return false
                return true
            }

            override fun hashCode(): Int {
                var result = executionStatus.hashCode()
                result += 31 * timer.hashCode()
                return result
            }

            class Builder {
                private var executionStatus: ExecutionStatus? = null
                private var timer: Timer? = null

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
                fun setTimer(timer: Timer): Builder = apply { this.timer = timer }

                fun build(): Output = Output(executionStatus, timer)
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
                    ).bindMemberType(
                        memberGetter = ExecutionStatus::unsupportedOperationStatus,
                        ctor = { ExecutionStatus(it) },
                        typeSpec = UNSUPPORTED_OPERATION_STATUS_TYPE_SPEC
                    ).build()
                internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
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
                        SlotMetadata.TIMER.path,
                        Arguments::timer,
                        Arguments.Builder::setTimer,
                        TimerReference.PARAM_VALUE_CONVERTER
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
                        TypeConverters.createEnumParamValueConverter(listOf(FieldPath.NAME))
                    )
                    .bindOutput(
                        "timer",
                        Output::timer,
                        ParamValueConverter.of(TIMER_TYPE_SPEC)
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

    companion object {
        const val CAPABILITY_NAME = "actions.intent.UPDATE_TIMER"
    }
}

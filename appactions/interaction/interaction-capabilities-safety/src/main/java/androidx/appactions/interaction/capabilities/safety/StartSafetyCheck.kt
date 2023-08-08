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

package androidx.appactions.interaction.capabilities.safety

import androidx.appactions.builtintypes.experimental.types.ActionNotInProgress
import androidx.appactions.builtintypes.experimental.types.GenericErrorStatus
import androidx.appactions.builtintypes.experimental.types.NoInternetConnection
import androidx.appactions.builtintypes.experimental.types.SafetyCheck
import androidx.appactions.builtintypes.experimental.types.SuccessStatus
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.SAFETY_CHECK_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.safety.executionstatus.EmergencySharingInProgress
import androidx.appactions.interaction.capabilities.safety.executionstatus.SafetyAccountNotLoggedIn
import androidx.appactions.interaction.capabilities.safety.executionstatus.SafetyFeatureNotOnboarded
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import java.time.Duration
import java.time.ZonedDateTime

/** A capability corresponding to actions.intent.START_SAFETY_CHECK */
@CapabilityFactory(name = StartSafetyCheck.CAPABILITY_NAME)
class StartSafetyCheck private constructor() {
    internal enum class SlotMetadata(val path: String) {
        DURATION("safetycheck.duration"),
        CHECK_IN_TIME("safetycheck.checkInTime")
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder, Arguments, Output, Confirmation, ExecutionSession
            >(ACTION_SPEC) {
        fun setDurationProperty(duration: Property<Duration>): CapabilityBuilder = setProperty(
            SlotMetadata.DURATION.path,
            duration,
            TypeConverters.DURATION_ENTITY_CONVERTER
        )

        fun setCheckInTimeProperty(
            checkInTime: Property<ZonedDateTime>
        ): CapabilityBuilder = setProperty(
            SlotMetadata.CHECK_IN_TIME.path,
            checkInTime,
            TypeConverters.ZONED_DATE_TIME_ENTITY_CONVERTER
        )
    }

    class Arguments internal constructor(
        val duration: Duration?,
        val checkInTime: ZonedDateTime?
    ) {
        override fun toString(): String {
            return "Arguments(duration=$duration, checkInTime=$checkInTime)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (duration != other.duration) return false
            if (checkInTime != other.checkInTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = duration.hashCode()
            result = 31 * result + checkInTime.hashCode()
            return result
        }

        class Builder {
            private var duration: Duration? = null

            private var checkInTime: ZonedDateTime? = null

            fun setDuration(duration: Duration): Builder =
                apply { this.duration = duration }

            fun setCheckInTime(checkInTime: ZonedDateTime): Builder =
                apply { this.checkInTime = checkInTime }

            fun build(): Arguments = Arguments(duration, checkInTime)
        }
    }

    class Output internal constructor(
        val safetyCheck: SafetyCheck?,
        val executionStatus: ExecutionStatus?
    ) {
        override fun toString(): String {
            return "Output(safetyCheck=$safetyCheck, executionStatus=$executionStatus)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Output

            if (safetyCheck != other.safetyCheck) return false
            if (executionStatus != other.executionStatus) return false

            return true
        }

        override fun hashCode(): Int {
            var result = safetyCheck.hashCode()
            result = 31 * result + executionStatus.hashCode()
            return result
        }

        class Builder {
            private var safetyCheck: SafetyCheck? = null

            private var executionStatus: ExecutionStatus? = null

            fun setSafetyCheck(safetyCheck: SafetyCheck): Builder =
                apply { this.safetyCheck = safetyCheck }

            fun setExecutionStatus(executionStatus: ExecutionStatus): Builder =
                apply { this.executionStatus = executionStatus }

            fun build(): Output = Output(safetyCheck, executionStatus)
        }
    }

    class ExecutionStatus {
        private var successStatus: SuccessStatus? = null
        private var genericErrorStatus: GenericErrorStatus? = null
        private var actionNotInProgress: ActionNotInProgress? = null
        private var emergencySharingInProgress: EmergencySharingInProgress? = null
        private var safetyAccountNotLoggedIn: SafetyAccountNotLoggedIn? = null
        private var safetyFeatureNotOnboarded: SafetyFeatureNotOnboarded? = null
        private var noInternetConnection: NoInternetConnection? = null

        constructor(successStatus: SuccessStatus) {
            this.successStatus = successStatus
        }

        constructor(genericErrorStatus: GenericErrorStatus) {
            this.genericErrorStatus = genericErrorStatus
        }

        constructor(actionNotInProgress: ActionNotInProgress) {
            this.actionNotInProgress = actionNotInProgress
        }

        constructor(emergencySharingInProgress: EmergencySharingInProgress) {
            this.emergencySharingInProgress = emergencySharingInProgress
        }

        constructor(safetyAccountNotLoggedIn: SafetyAccountNotLoggedIn) {
            this.safetyAccountNotLoggedIn = safetyAccountNotLoggedIn
        }

        constructor(safetyFeatureNotOnboarded: SafetyFeatureNotOnboarded) {
            this.safetyFeatureNotOnboarded = safetyFeatureNotOnboarded
        }

        constructor(noInternetConnection: NoInternetConnection) {
            this.noInternetConnection = noInternetConnection
        }

        internal fun toParamValue(): ParamValue {
            var status: String = ""
            if (successStatus != null) {
                status = successStatus.toString()
            }
            if (genericErrorStatus != null) {
                status = genericErrorStatus.toString()
            }
            if (actionNotInProgress != null) {
                status = actionNotInProgress.toString()
            }
            if (emergencySharingInProgress != null) {
                status = emergencySharingInProgress.toString()
            }
            if (safetyAccountNotLoggedIn != null) {
                status = safetyAccountNotLoggedIn.toString()
            }
            if (safetyFeatureNotOnboarded != null) {
                status = safetyFeatureNotOnboarded.toString()
            }
            if (noInternetConnection != null) {
                status = noInternetConnection.toString()
            }
            val value: Value = Value.newBuilder().setStringValue(status).build()
            return ParamValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder()
                        .putFields(TypeConverters.FIELD_NAME_TYPE, value)
                        .build()
                )
                .build()
        }
    }

    class Confirmation internal constructor()

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        /** Canonical name for [StartSafetyCheck] capability */
        const val CAPABILITY_NAME = "actions.intent.START_SAFETY_CHECK"
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindParameter(
                    SlotMetadata.DURATION.path,
                    Arguments::duration,
                    Arguments.Builder::setDuration,
                    TypeConverters.DURATION_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    SlotMetadata.CHECK_IN_TIME.path,
                    Arguments::checkInTime,
                    Arguments.Builder::setCheckInTime,
                    TypeConverters.ZONED_DATE_TIME_PARAM_VALUE_CONVERTER
                )
                .bindOutput(
                    "safetyCheck",
                    Output::safetyCheck,
                    ParamValueConverter.of(SAFETY_CHECK_TYPE_SPEC)::toParamValue
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

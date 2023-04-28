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
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.SAFETY_CHECK_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.safety.executionstatus.EmergencySharingInProgress
import androidx.appactions.interaction.capabilities.safety.executionstatus.SafetyAccountNotLoggedIn
import androidx.appactions.interaction.capabilities.safety.executionstatus.SafetyFeatureNotOnboarded
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Optional

private const val CAPABILITY_NAME = "actions.intent.START_SAFETY_CHECK"

/** A capability corresponding to actions.intent.START_SAFETY_CHECK */
@CapabilityFactory(name = CAPABILITY_NAME)
class StartSafetyCheck private constructor() {
    internal enum class PropertyMapStrings(val key: String) {
        DURATION("safetycheck.duration"),
        CHECK_IN_TIME("safetycheck.checkInTime"),
    }

    // TODO(b/267805819): Update to include the SessionFactory once Session API is ready.
    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder, Arguments, Output, Confirmation, ExecutionSession
            >(ACTION_SPEC) {
        private var properties = mutableMapOf<String, Property<*>>()

        fun setDuration(duration: Property<Duration>): CapabilityBuilder =
            apply { properties[PropertyMapStrings.DURATION.key] = duration }

        fun setCheckInTime(checkInTime: Property<ZonedDateTime>): CapabilityBuilder =
            apply { properties[PropertyMapStrings.CHECK_IN_TIME.key] = checkInTime }

        override fun build(): Capability {
            super.setProperty(properties)
            return super.build()
        }
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

        class Builder : BuilderOf<Arguments> {
            private var duration: Duration? = null

            private var checkInTime: ZonedDateTime? = null

            fun setDuration(duration: Duration): Builder =
                apply { this.duration = duration }

            fun setCheckInTime(checkInTime: ZonedDateTime): Builder =
                apply { this.checkInTime = checkInTime }

            override fun build(): Arguments = Arguments(duration, checkInTime)
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
                        .build(),
                )
                .build()
        }
    }

    class Confirmation internal constructor()

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        @Suppress("UNCHECKED_CAST")
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder)
                .setOutput(Output::class.java)
                .bindOptionalParameter(
                    "safetyCheck.duration",
                    { properties ->
                        Optional.ofNullable(
                            properties[PropertyMapStrings.DURATION.key]
                                as Property<Duration>
                        )
                    },
                    Arguments.Builder::setDuration,
                    TypeConverters.DURATION_PARAM_VALUE_CONVERTER,
                    TypeConverters.DURATION_ENTITY_CONVERTER
                )
                .bindOptionalParameter(
                    "safetyCheck.checkInTime",
                    { property ->
                        Optional.ofNullable(
                            property[PropertyMapStrings.CHECK_IN_TIME.key]
                                as Property<ZonedDateTime>
                        )
                    },
                    Arguments.Builder::setCheckInTime,
                    TypeConverters.ZONED_DATETIME_PARAM_VALUE_CONVERTER,
                    TypeConverters.ZONED_DATETIME_ENTITY_CONVERTER
                )
                .bindOptionalOutput(
                    "safetyCheck",
                    { output -> Optional.ofNullable(output.safetyCheck) },
                    ParamValueConverter.of(SAFETY_CHECK_TYPE_SPEC)::toParamValue
                )
                .bindOptionalOutput(
                    "executionStatus",
                    { output -> Optional.ofNullable(output.executionStatus) },
                    ExecutionStatus::toParamValue
                )
                .build()
    }
}

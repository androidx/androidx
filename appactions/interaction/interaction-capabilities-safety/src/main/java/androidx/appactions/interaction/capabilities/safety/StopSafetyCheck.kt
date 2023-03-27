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

import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.ActionCapability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater
import androidx.appactions.interaction.capabilities.core.values.GenericErrorStatus
import androidx.appactions.interaction.capabilities.core.values.SuccessStatus
import androidx.appactions.interaction.capabilities.core.values.executionstatus.ActionNotInProgress
import androidx.appactions.interaction.capabilities.core.values.executionstatus.NoInternetConnection
import androidx.appactions.interaction.capabilities.safety.executionstatus.SafetyAccountNotLoggedIn
import androidx.appactions.interaction.capabilities.safety.executionstatus.SafetyFeatureNotOnboarded
import androidx.appactions.interaction.proto.ParamValue
import com.google.protobuf.Struct
import com.google.protobuf.Value
import java.util.Optional

/** StopSafetyCheck.kt in interaction-capabilities-safety */
private const val CAPABILITY_NAME = "actions.intent.STOP_SAFETY_CHECK"

private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(StopSafetyCheck.Property::class.java)
        .setArgument(StopSafetyCheck.Argument::class.java, StopSafetyCheck.Argument::Builder)
        .setOutput(StopSafetyCheck.Output::class.java)
        .bindOptionalOutput(
            "executionStatus",
            StopSafetyCheck.Output::executionStatus.getter,
            StopSafetyCheck.ExecutionStatus::toParamValue,
        )
        .build()

// TODO(b/267806701): Add capability factory annotation once the testing library is fully migrated.
class StopSafetyCheck private constructor() {
    // TODO(b/267805819): Update to include the SessionBuilder once Session API is ready.
    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Argument, Output, Confirmation, TaskUpdater, Session,
            >(ACTION_SPEC) {
        override fun build(): ActionCapability {
            super.setProperty(Property())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property internal constructor()

    class Argument internal constructor() {
        class Builder : BuilderOf<Argument> {
            override fun build(): Argument = Argument()
        }
    }

    // TODO(b/267533126): Remove the use of optional once ActionSpecBuilder supports nullability.
    class Output internal constructor(val executionStatus: Optional<ExecutionStatus>) {
        class Builder {
            private var executionStatus: ExecutionStatus? = null

            fun setExecutionStatus(executionStatus: ExecutionStatus): Builder =
                apply { this.executionStatus = executionStatus }

            fun setExecutionStatus(successStatus: SuccessStatus): Builder = apply {
                this.executionStatus = ExecutionStatus(successStatus)
            }

            fun setExecutionStatus(genericErrorStatus: GenericErrorStatus): Builder = apply {
                this.executionStatus = ExecutionStatus(genericErrorStatus)
            }

            fun setExecutionStatus(actionNotInProgress: ActionNotInProgress): Builder = apply {
                this.executionStatus = ExecutionStatus(actionNotInProgress)
            }

            fun setExecutionStatus(safetyAccountNotLoggedIn: SafetyAccountNotLoggedIn): Builder =
                apply {
                    this.executionStatus =
                        ExecutionStatus(safetyAccountNotLoggedIn)
                }

            fun setExecutionStatus(safetyFeatureNotOnboarded: SafetyFeatureNotOnboarded): Builder =
                apply {
                    this.executionStatus =
                        ExecutionStatus(safetyFeatureNotOnboarded)
                }

            fun setExecutionStatus(noInternetConnection: NoInternetConnection): Builder =
                apply {
                    this.executionStatus =
                        ExecutionStatus(noInternetConnection)
                }

            fun build(): Output = Output(Optional.ofNullable(executionStatus))
        }
    }

    class ExecutionStatus {

        private var successStatus: SuccessStatus? = null
        private var genericErrorStatus: GenericErrorStatus? = null
        private var actionNotInProgress: ActionNotInProgress? = null
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

    class TaskUpdater internal constructor() : AbstractTaskUpdater()

    sealed interface Session : BaseSession<Argument, Output>
}

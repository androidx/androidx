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
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.serializers.types.ALARM_TYPE_SPEC

@CapabilityFactory(name = GetAlarm.CAPABILITY_NAME)
class GetAlarm private constructor() {
    internal enum class SlotMetadata(val path: String) {
        ALARM("alarm")
    }

    class CapabilityBuilder : Capability.Builder<
        CapabilityBuilder,
        Arguments,
        Output,
        Confirmation,
        ExecutionSession
        >(ACTION_SPEC) {
            fun setAlarmProperty(alarm: Property<Alarm>) = setProperty(
                SlotMetadata.ALARM.path,
                alarm,
                EntityConverter.of(ALARM_TYPE_SPEC)
            )
        }

    class Arguments internal constructor(
        val alarm: AlarmReference?
    ) {
        override fun toString(): String {
            return "Arguments(alarm=$alarm)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (alarm != other.alarm) return false

            return true
        }

        override fun hashCode(): Int {
            return alarm.hashCode()
        }

        class Builder {
            private var alarm: AlarmReference? = null

            fun setAlarm(alarm: AlarmReference): Builder = apply { this.alarm = alarm }

            fun build(): Arguments = Arguments(alarm)
        }
    }

    class Output internal constructor()
    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>
    class Confirmation internal constructor()

    companion object {
        /** Canonical name for [GetAlarm] capability */
        const val CAPABILITY_NAME = "actions.intent.GET_ALARM"
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindParameter(
                    SlotMetadata.ALARM.path,
                    Arguments::alarm,
                    Arguments.Builder::setAlarm,
                    AlarmReference.PARAM_VALUE_CONVERTER
                )
                .build()
        init {
            ActionSpecRegistry.registerActionSpec(Arguments::class, Output::class, ACTION_SPEC)
        }
    }
}

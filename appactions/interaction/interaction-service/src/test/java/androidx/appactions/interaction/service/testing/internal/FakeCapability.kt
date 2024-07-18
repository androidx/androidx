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

package androidx.appactions.interaction.service.testing.internal

import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.ValueListener
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.task.SessionBridge
import androidx.appactions.interaction.capabilities.core.impl.task.TaskHandler
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue

private const val CAPABILITY_NAME = "actions.intent.FAKE_CAPABILITY"

class FakeCapability private constructor() {
    class Arguments internal constructor(
        val fieldOne: String?,
    ) {
        class Builder {
            private var fieldOne: String? = null
            fun setFieldOne(value: String) = apply {
                fieldOne = value
            }
            fun build() = Arguments(fieldOne)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    interface ExecutionSession : BaseExecutionSession<Arguments, Output> {
        val fieldOneListener: ValueListener<String>?
            get() = null
    }

    class CapabilityBuilder : Capability.Builder<
        CapabilityBuilder,
        Arguments,
        Output,
        Confirmation,
        ExecutionSession,
        >(ACTION_SPEC) {
        override val sessionBridge = SessionBridge<ExecutionSession, Arguments, Confirmation> {
                session ->
            val builder = TaskHandler.Builder<Arguments, Confirmation>()
            session.fieldOneListener?.let {
                builder.registerValueTaskParam(
                    "fieldOne",
                    it,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                )
            }
            builder.build()
        }

        public override fun setExecutionSessionFactory(
            sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession
        ) = super.setExecutionSessionFactory(sessionFactory)

        fun setFieldOne(fieldOne: Property<StringValue>) = setProperty(
            "fieldOne",
            fieldOne,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
    }

    companion object {
        private val ACTION_SPEC = ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
            .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
            .setOutput(Output::class.java)
            .bindParameter(
                "fieldOne",
                Arguments::fieldOne,
                Arguments.Builder::setFieldOne,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER
            )
            .build()
    }
}

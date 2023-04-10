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

import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.SessionFactory
import androidx.appactions.interaction.capabilities.core.ValueListener
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.ParamProperty
import androidx.appactions.interaction.capabilities.core.impl.task.SessionBridge
import androidx.appactions.interaction.capabilities.core.impl.task.TaskHandler
import java.util.Optional

private const val CAPABILITY_NAME = "actions.intent.FAKE_CAPABILITY"
private val ACTION_SPEC = ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
    .setDescriptor(FakeCapability.Property::class.java)
    .setArguments(FakeCapability.Arguments::class.java, FakeCapability.Arguments::Builder)
    .setOutput(FakeCapability.Output::class.java).bindOptionalParameter(
        "fieldOne",
        { property -> Optional.ofNullable(property.fieldOne) },
        FakeCapability.Arguments.Builder::setFieldOne,
        TypeConverters.STRING_PARAM_VALUE_CONVERTER,
        TypeConverters.STRING_VALUE_ENTITY_CONVERTER,
    ).build()

class FakeCapability private constructor() {
    class Property(
        val fieldOne: ParamProperty<StringValue>? = null,
    )

    class Arguments internal constructor(
        val fieldOne: String?,
    ) {
        class Builder : BuilderOf<Arguments> {
            private var fieldOne: String? = null
            fun setFieldOne(value: String) = apply {
                fieldOne = value
            }
            override fun build() = Arguments(fieldOne)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    interface Session : BaseSession<Arguments, Output> {
        val fieldOneListener: ValueListener<String>?
            get() = null
    }

    class CapabilityBuilder : CapabilityBuilderBase<
        CapabilityBuilder,
        Property,
        Arguments,
        Output,
        Confirmation,
        Session,
        >(ACTION_SPEC) {
        override val sessionBridge = SessionBridge<Session, Confirmation> {
                session ->
            val builder = TaskHandler.Builder<Confirmation>()
            session.fieldOneListener?.let {
                builder.registerValueTaskParam(
                    "fieldOne",
                    it,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                )
            }
            builder.build()
        }

        private var fieldOne: ParamProperty<StringValue>? = null

        fun setFieldOne(fieldOne: ParamProperty<StringValue>) = apply {
            this.fieldOne = fieldOne
        }

        public override fun setSessionFactory(
            sessionFactory: SessionFactory<Session>,
        ) = super.setSessionFactory(sessionFactory)

        override fun build(): Capability {
            super.setProperty(Property(fieldOne))
            return super.build()
        }
    }
}

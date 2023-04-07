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

package androidx.appactions.interaction.capabilities.fitness.fitness

import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.Property
import java.util.Optional

/** PauseExercise.kt in interaction-capabilities-fitness */
private const val CAPABILITY_NAME = "actions.intent.PAUSE_EXERCISE"

// TODO(b/273602015): Update to use Name property from builtintype library.
private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(PauseExercise.Properties::class.java)
        .setArguments(PauseExercise.Arguments::class.java, PauseExercise.Arguments::Builder)
        .setOutput(PauseExercise.Output::class.java)
        .bindOptionalParameter(
            "exercise.name",
            { property -> Optional.ofNullable(property.name) },
            PauseExercise.Arguments.Builder::setName,
            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
        .build()

@CapabilityFactory(name = CAPABILITY_NAME)
class PauseExercise private constructor() {
    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder, Properties, Arguments, Output, Confirmation, Session
            >(ACTION_SPEC) {
        private var propertyBuilder: Properties.Builder = Properties.Builder()
        fun setNameProperty(name: Property<StringValue>): CapabilityBuilder =
            apply {
                propertyBuilder.setName(name)
            }

        override fun build(): Capability {
            // TODO(b/268369632): Clean this up after Property is removed
            super.setProperty(propertyBuilder.build())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Properties internal constructor(
        val name: Property<StringValue>?,
    ) {
        override fun toString(): String {
            return "Property(name=$name)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Properties

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        class Builder {
            private var name: Property<StringValue>? = null

            fun setName(name: Property<StringValue>): Builder =
                apply { this.name = name }

            fun build(): Properties = Properties(name)
        }
    }

    class Arguments internal constructor(
        val name: String?
    ) {
        override fun toString(): String {
            return "Arguments(name=$name)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Arguments

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        class Builder : BuilderOf<Arguments> {
            private var name: String? = null

            fun setName(name: String): Builder =
                apply { this.name = name }

            override fun build(): Arguments = Arguments(name)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    sealed interface Session : BaseSession<Arguments, Output>
}

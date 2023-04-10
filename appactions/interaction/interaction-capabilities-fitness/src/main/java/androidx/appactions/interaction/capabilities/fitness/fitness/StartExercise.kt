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

import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.ParamProperty
import java.time.Duration
import java.util.Optional

/** StartExercise.kt in interaction-capabilities-fitness */
private const val CAPABILITY_NAME = "actions.intent.START_EXERCISE"

// TODO(b/273602015): Update to use Name property from builtintype library.
private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(StartExercise.Property::class.java)
        .setArguments(StartExercise.Arguments::class.java, StartExercise.Arguments::Builder)
        .setOutput(StartExercise.Output::class.java)
        .bindOptionalParameter(
            "exercise.duration",
            { property -> Optional.ofNullable(property.duration) },
            StartExercise.Arguments.Builder::setDuration,
            TypeConverters.DURATION_PARAM_VALUE_CONVERTER,
            TypeConverters.DURATION_ENTITY_CONVERTER
        )
        .bindOptionalParameter(
            "exercise.name",
            { property -> Optional.ofNullable(property.name) },
            StartExercise.Arguments.Builder::setName,
            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
        .build()

@CapabilityFactory(name = CAPABILITY_NAME)
class StartExercise private constructor() {
    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Arguments, Output, Confirmation, Session
            >(ACTION_SPEC) {
        fun setDurationProperty(duration: ParamProperty<Duration>): CapabilityBuilder =
            apply {
                Property.Builder().setDuration(duration).build()
            }

        fun setNameProperty(name: ParamProperty<StringValue>): CapabilityBuilder =
            apply {
                Property.Builder().setName(name).build()
            }

        override fun build(): Capability {
            // TODO(b/268369632): No-op remove empty property builder after Property od removed
            super.setProperty(Property.Builder().build())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property internal constructor(
        val duration: ParamProperty<Duration>?,
        val name: ParamProperty<StringValue>?
    ) {
        override fun toString(): String {
            return "Property(duration=$duration, name=$name)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Property

            if (duration != other.duration) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = duration.hashCode()
            result += 31 * name.hashCode()
            return result
        }

        class Builder {
            private var duration: ParamProperty<Duration>? = null
            private var name: ParamProperty<StringValue>? = null

            fun setDuration(duration: ParamProperty<Duration>): Builder =
                apply { this.duration = duration }

            fun setName(name: ParamProperty<StringValue>): Builder =
                apply { this.name = name }

            fun build(): Property = Property(duration, name)
        }
    }

    class Arguments internal constructor(
        val duration: Duration?,
        val name: String?
    ) {
        override fun toString(): String {
            return "Arguments(duration=$duration, name=$name)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Arguments

            if (duration != other.duration) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = duration.hashCode()
            result += 31 * name.hashCode()
            return result
        }

        class Builder : BuilderOf<Arguments> {
            private var duration: Duration? = null
            private var name: String? = null

            fun setDuration(duration: Duration): Builder =
                apply { this.duration = duration }

            fun setName(name: String): Builder =
                apply { this.name = name }

            override fun build(): Arguments = Arguments(duration, name)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    sealed interface Session : BaseSession<Arguments, Output>
}

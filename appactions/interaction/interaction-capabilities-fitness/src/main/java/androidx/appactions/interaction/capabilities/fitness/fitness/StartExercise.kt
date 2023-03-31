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
import androidx.appactions.interaction.capabilities.core.ActionCapability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater
import java.time.Duration
import java.util.Optional

/** StartExercise.kt in interaction-capabilities-fitness */
private const val CAPABILITY_NAME = "actions.intent.START_EXERCISE"

// TODO(b/273602015): Update to use Name property from builtintype library.
private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(StartExercise.Property::class.java)
        .setArgument(StartExercise.Argument::class.java, StartExercise.Argument::Builder)
        .setOutput(StartExercise.Output::class.java)
        .bindOptionalParameter(
            "exercise.duration",
            { property -> Optional.ofNullable(property.duration) },
            StartExercise.Argument.Builder::setDuration,
            TypeConverters.DURATION_PARAM_VALUE_CONVERTER,
            TypeConverters.DURATION_ENTITY_CONVERTER
        )
        .bindOptionalParameter(
            "exercise.name",
            { property -> Optional.ofNullable(property.name) },
            StartExercise.Argument.Builder::setName,
            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
        .build()

@CapabilityFactory(name = CAPABILITY_NAME)
class StartExercise private constructor() {
    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Argument, Output, Confirmation, TaskUpdater, Session
            >(ACTION_SPEC) {
        fun setDurationProperty(duration: TypeProperty<Duration>): CapabilityBuilder =
            apply {
                Property.Builder().setDuration(duration).build()
            }

        fun setNameProperty(name: TypeProperty<StringValue>): CapabilityBuilder =
            apply {
                Property.Builder().setName(name).build()
            }

        override fun build(): ActionCapability {
            // TODO(b/268369632): No-op remove empty property builder after Property od removed
            super.setProperty(Property.Builder().build())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property internal constructor(
        val duration: TypeProperty<Duration>?,
        val name: TypeProperty<StringValue>?
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
            private var duration: TypeProperty<Duration>? = null
            private var name: TypeProperty<StringValue>? = null

            fun setDuration(duration: TypeProperty<Duration>): Builder =
                apply { this.duration = duration }

            fun setName(name: TypeProperty<StringValue>): Builder =
                apply { this.name = name }

            fun build(): Property = Property(duration, name)
        }
    }

    class Argument internal constructor(
        val duration: Duration?,
        val name: String?
    ) {
        override fun toString(): String {
            return "Argument(duration=$duration, name=$name)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Argument

            if (duration != other.duration) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = duration.hashCode()
            result += 31 * name.hashCode()
            return result
        }

        class Builder : BuilderOf<Argument> {
            private var duration: Duration? = null
            private var name: String? = null

            fun setDuration(duration: Duration): Builder =
                apply { this.duration = duration }

            fun setName(name: String): Builder =
                apply { this.name = name }

            override fun build(): Argument = Argument(duration, name)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    class TaskUpdater internal constructor() : AbstractTaskUpdater()

    sealed interface Session : BaseSession<Argument, Output>
}

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
import androidx.appactions.interaction.capabilities.core.properties.ParamProperty
import java.time.LocalTime
import java.util.Optional

/** GetHealthObservation.kt in interaction-capabilities-fitness */
private const val CAPABILITY_NAME = "actions.intent.START_EXERCISE"

// TODO(b/273602015): Update to use Name property from builtintype library.
private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(GetHealthObservation.Property::class.java)
        .setArguments(
            GetHealthObservation.Arguments::class.java,
            GetHealthObservation.Arguments::Builder
        )
        .setOutput(GetHealthObservation.Output::class.java)
        .bindOptionalParameter(
            "exerciseObservation.startTime",
            { property -> Optional.ofNullable(property.startTime) },
            GetHealthObservation.Arguments.Builder::setStartTime,
            TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER,
            TypeConverters.LOCAL_TIME_ENTITY_CONVERTER
        )
        .bindOptionalParameter(
            "exerciseObservation.endTime",
            { property -> Optional.ofNullable(property.endTime) },
            GetHealthObservation.Arguments.Builder::setEndTime,
            TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER,
            TypeConverters.LOCAL_TIME_ENTITY_CONVERTER
        )
        .build()

@CapabilityFactory(name = CAPABILITY_NAME)
class GetHealthObservation private constructor() {
    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Arguments, Output, Confirmation, Session
            >(ACTION_SPEC) {
        private var propertyBuilder: Property.Builder = Property.Builder()
        fun setStartTimeProperty(startTime: ParamProperty<LocalTime>): CapabilityBuilder = apply {
            propertyBuilder.setEndTime(startTime)
        }

        fun setEndTimeProperty(endTime: ParamProperty<LocalTime>): CapabilityBuilder = apply {
            propertyBuilder.setEndTime(endTime)
        }

        override fun build(): Capability {
            // TODO(b/268369632): Clean this up after Property is removed
            super.setProperty(propertyBuilder.build())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property internal constructor(
        val startTime: ParamProperty<LocalTime>?,
        val endTime: ParamProperty<LocalTime>?
    ) {
        override fun toString(): String {
            return "Property(startTime=$startTime, endTime=$endTime)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Property

            if (startTime != other.startTime) return false
            if (endTime != other.endTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = startTime.hashCode()
            result += 31 * endTime.hashCode()
            return result
        }

        class Builder {
            private var startTime: ParamProperty<LocalTime>? = null
            private var endTime: ParamProperty<LocalTime>? = null

            fun setStartTime(startTime: ParamProperty<LocalTime>): Builder =
                apply { this.startTime = startTime }

            fun setEndTime(endTime: ParamProperty<LocalTime>): Builder =
                apply { this.endTime = endTime }

            fun build(): Property = Property(startTime, endTime)
        }
    }

    class Arguments internal constructor(
        val startTime: LocalTime?,
        val endTime: LocalTime?
    ) {
        override fun toString(): String {
            return "Arguments(startTime=$startTime, endTime=$endTime)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass !== other?.javaClass) return false

            other as Arguments

            if (startTime != other.startTime) return false
            if (endTime != other.endTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = startTime.hashCode()
            result += 31 * endTime.hashCode()
            return result
        }

        class Builder : BuilderOf<Arguments> {
            private var startTime: LocalTime? = null
            private var endTime: LocalTime? = null

            fun setStartTime(startTime: LocalTime): Builder =
                apply { this.startTime = startTime }

            fun setEndTime(endTime: LocalTime): Builder =
                apply { this.endTime = endTime }

            override fun build(): Arguments = Arguments(startTime, endTime)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    sealed interface Session : BaseSession<Arguments, Output>
}

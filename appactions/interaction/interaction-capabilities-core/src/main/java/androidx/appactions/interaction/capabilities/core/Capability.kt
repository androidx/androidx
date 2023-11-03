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

package androidx.appactions.interaction.capabilities.core

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.SingleTurnCapabilityImpl
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.BoundProperty
import androidx.appactions.interaction.capabilities.core.impl.task.EmptyTaskUpdater
import androidx.appactions.interaction.capabilities.core.impl.task.SessionBridge
import androidx.appactions.interaction.capabilities.core.impl.task.TaskCapabilityImpl
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.proto.AppActionsContext.AppAction

/**
 * A Capability represents a supported [built-in intent](https://developer.android.com/reference/app-actions/built-in-intents).
 * [Register](https://developer.android.com/guide/app-actions/intents) capabilities within an app to declare support for the capability.
 */
abstract class Capability internal constructor(
    /** Returns the unique Id of this capability declaration. */
    open val id: String
) {

    /**
     * Returns an app action proto describing how to fulfill this capability.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract val appAction: AppAction

    /**
     * Create a new capability session. The capability library doesn't maintain registry of
     * capabilities, so it's not going to assign any session id.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract fun createSession(
        sessionId: String,
        hostProperties: HostProperties
    ): CapabilitySession

    /**
     * An abstract Builder class for Capability.
     */
    @SuppressLint("StaticFinalBuilder")
    abstract class Builder<
        BuilderT :
        Builder<
            BuilderT,
            ArgumentsT,
            OutputT,
            ConfirmationT,
            ExecutionSessionT
            >,
        ArgumentsT,
        OutputT,
        ConfirmationT,
        ExecutionSessionT : BaseExecutionSession<ArgumentsT, OutputT>
        > private constructor() {
        private var id: String? = null
        private val boundPropertyMap = mutableMapOf<String, BoundProperty<*>>()
        private var executionCallback: ExecutionCallback<ArgumentsT, OutputT>? = null
        private var sessionFactory:
            ((hostProperties: HostProperties?) -> ExecutionSessionT)? = null
        private var actionSpec: ActionSpec<ArgumentsT, OutputT>? = null

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(actionSpec: ActionSpec<ArgumentsT, OutputT>) : this() {
            this.actionSpec = actionSpec
        }

        /**
         * The SessionBridge object, which is used to normalize Session instances to TaskHandler.
         * see SessionBridge documentation for more information.
         */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        protected open val sessionBridge: SessionBridge<
            ExecutionSessionT,
            ArgumentsT,
            ConfirmationT
        >? = null

        @Suppress("UNCHECKED_CAST")
        private fun asBuilder(): BuilderT {
            return this as BuilderT
        }

        /**
         * Sets the Id of the capability being built. The Id should be a non-null string that is
         * unique among all Capability, and should not change during/across activity lifecycles.
         */
        fun setId(id: String): BuilderT = asBuilder().apply {
            this.id = id
        }

        /**
         * Sets a single slot property for this capability, and associated entity converter. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        protected fun <T> setProperty(
            slotName: String,
            property: Property<T>,
            entityConverter: EntityConverter<T>,
        ) = asBuilder().apply {
            this.boundPropertyMap[slotName] = BoundProperty(slotName, property, entityConverter)
        }

        /**
         * Sets the ExecutionCallback for this capability.
         *
         * [setExecutionSessionFactory] and [setExecutionCallback] are mutually exclusive, so
         * calling one will nullify the other.
         *
         * This method accepts a coroutine-based ExecutionCallback instance. There is also an
         * overload which accepts the [ExecutionCallbackAsync] instead.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        fun setExecutionCallback(executionCallback: ExecutionCallback<ArgumentsT, OutputT>) =
            asBuilder().apply {
                this.executionCallback = executionCallback
            }

        /**
         * Sets the [ExecutionCallbackAsync] for this capability.
         *
         * [setExecutionSessionFactory] and [setExecutionCallback] are mutually exclusive, so
         * calling one will nullify the other.
         *
         * This method accepts the [ExecutionCallbackAsync] interface, which is the Future version
         * of [ExecutionCallback]
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        fun setExecutionCallback(
            executionCallbackAsync: ExecutionCallbackAsync<ArgumentsT, OutputT>
        ) = asBuilder().apply {
            this.executionCallback = executionCallbackAsync.toExecutionCallback()
        }

        /**
         * Sets the lambda used to create [ExecutionSessionT] instances for this
         * capability.
         *
         * [setExecutionSessionFactory] and [setExecutionCallback] are mutually exclusive, so
         * calling one will nullify the other.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        protected open fun setExecutionSessionFactory(
            sessionFactory: (hostProperties: HostProperties?) -> ExecutionSessionT
        ): BuilderT = asBuilder().apply {
            this.sessionFactory = sessionFactory
        }

        /**
         * Builds and returns this Capability.
         * [setId] must be called before [build].
         * either [setExecutionCabllack] or [setSessionFactory] must be called before [build].
         * child classes may enforce additional required properties to be set before [build].
         * An [IllegalStateException] will be thrown if above requirements are not met.
         */
        open fun build(): Capability {
            val checkedId = id ?: throw IllegalStateException("setId must be called before build")
            val boundProperties = boundPropertyMap.values.toList()
            if (executionCallback != null) {
                return SingleTurnCapabilityImpl(
                    checkedId,
                    actionSpec!!,
                    boundProperties,
                    executionCallback!!
                )
            } else if (sessionFactory != null) {
                return TaskCapabilityImpl(
                    checkedId,
                    actionSpec!!,
                    boundProperties,
                    sessionFactory!!,
                    sessionBridge!!,
                    ::EmptyTaskUpdater
                )
            } else {
                throw IllegalStateException(
                    """either setExecutionCallback or setExecutionSessionFactory must be called
                    before capability '$id' can be built"""
                )
            }
        }
    }
}

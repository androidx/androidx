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

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.SingleTurnCapabilityImpl
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
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
     *
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract val appAction: AppAction

    /**
     * Create a new capability session. The capability library doesn't maintain registry of
     * capabilities, so it's not going to assign any session id.
     *
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract fun createSession(
        sessionId: String,
        hostProperties: HostProperties
    ): CapabilitySession

    /**
     * An abstract Builder class for Capability.
     */
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
        private var property: Map<String, Property<*>>? = null
        private var executionCallback: ExecutionCallback<ArgumentsT, OutputT>? = null
        private var sessionFactory:
                (hostProperties: HostProperties?) -> ExecutionSessionT? = { _ -> null }
        private var actionSpec: ActionSpec<ArgumentsT, OutputT>? = null

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(actionSpec: ActionSpec<ArgumentsT, OutputT>) : this() {
            this.actionSpec = actionSpec
        }

        /**
         * The SessionBridge object, which is used to normalize Session instances to TaskHandler.
         * see SessionBridge documentation for more information.
         *
         * @suppress
         */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        protected open val sessionBridge: SessionBridge<ExecutionSessionT, ConfirmationT>? = null

        @Suppress("UNCHECKED_CAST")
        fun asBuilder(): BuilderT {
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
         * Sets the Property instance for this capability.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun setProperty(property: Map<String, Property<*>>) = asBuilder().apply {
            this.property = property
        }

        /**
         * Sets the ExecutionCallback for this capability.
         *
         * [setExecutionSessionFactory] and [setExecutionCallback] are mutually exclusive, so
         * calling one will nullify the other.
         *
         * This method accepts a coroutine-based ExecutionCallback instance. There is also an
         * overload which accepts the ExecutionCallbackAsync instead.
         */
        fun setExecutionCallback(executionCallback: ExecutionCallback<ArgumentsT, OutputT>) =
            asBuilder().apply {
                this.executionCallback = executionCallback
            }

        /**
         * Sets the ExecutionCallbackAsync for this capability.
         *
         * setExecutionSessionFactory and setExecutionCallback are mutually exclusive, so calling
         * one will nullify the other.
         *
         * This method accepts the ExecutionCallbackAsync interface which returns a
         * [ListenableFuture].
         */
        fun setExecutionCallback(
            executionCallbackAsync: ExecutionCallbackAsync<ArgumentsT, OutputT>
        ) = asBuilder().apply {
            this.executionCallback = executionCallbackAsync.toExecutionCallback()
        }

        /**
         * Sets the lambda used to create [ExecutionSession] instances for this
         * capability.
         *
         * [setExecutionSessionFactory] and [setExecutionCallback] are mutually exclusive, so
         * calling one will nullify the other.
         */
        open fun setExecutionSessionFactory(
            sessionFactory: (hostProperties: HostProperties?) -> ExecutionSessionT
        ): BuilderT = asBuilder().apply {
            this.sessionFactory = sessionFactory
        }

        /** Builds and returns this Capability. */
        open fun build(): Capability {
            val checkedId = requireNotNull(id) { "setId must be called before build" }
            val checkedProperty = requireNotNull(property) { "property must not be null." }
            if (executionCallback != null) {
                return SingleTurnCapabilityImpl(
                    checkedId,
                    actionSpec!!,
                    checkedProperty,
                    executionCallback!!
                )
            } else {
                val checkedSessionFactory = requireNotNull(sessionFactory) {
                    "either setExecutionCallback or setExecutionSessionFactory" +
                        " must be called before build"
                }
                return TaskCapabilityImpl(
                    checkedId,
                    actionSpec!!,
                    checkedProperty,
                    checkedSessionFactory,
                    sessionBridge!!,
                    ::EmptyTaskUpdater
                )
            }
        }
    }
}

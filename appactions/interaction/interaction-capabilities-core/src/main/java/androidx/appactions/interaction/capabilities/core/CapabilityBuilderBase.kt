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
import androidx.appactions.interaction.capabilities.core.ActionExecutorAsync.Companion.toActionExecutorAsync
import androidx.appactions.interaction.capabilities.core.impl.SingleTurnCapabilityImpl
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater
import androidx.appactions.interaction.capabilities.core.task.impl.SessionBridge
import androidx.appactions.interaction.capabilities.core.task.impl.TaskCapabilityImpl
import java.util.function.Supplier

/**
 * An abstract Builder class for Capability.
 */
abstract class CapabilityBuilderBase<
    BuilderT :
    CapabilityBuilderBase<
        BuilderT,
        PropertyT,
        ArgumentT,
        OutputT,
        ConfirmationT,
        SessionUpdaterT,
        SessionT,>,
    PropertyT,
    ArgumentT,
    OutputT,
    ConfirmationT,
    SessionUpdaterT : AbstractTaskUpdater,
    SessionT : BaseSession<ArgumentT, OutputT>,
    > protected constructor(
    private val actionSpec: ActionSpec<PropertyT, ArgumentT, OutputT>,
) {
    private var id: String? = null
    private var property: PropertyT? = null
    private var actionExecutorAsync: ActionExecutorAsync<ArgumentT, OutputT>? = null
    private var sessionFactory: SessionFactory<SessionT>? = null

    /**
     * The SessionBridge object, which is used to normalize Session instances to TaskHandler.
     * see SessionBridge documentation for more information.
     *
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected open val sessionBridge: SessionBridge<SessionT, ConfirmationT>? = null

    /** The supplier of SessionUpdaterT instances. */
    protected open val sessionUpdaterSupplier: Supplier<SessionUpdaterT>? = null

    @Suppress("UNCHECKED_CAST")
    fun asBuilder(): BuilderT {
        return this as BuilderT
    }

    /**
     * Sets the Id of the capability being built. The Id should be a non-null string that is unique
     * among all Capability, and should not change during/across activity lifecycles.
     */
    fun setId(id: String): BuilderT = asBuilder().apply {
        this.id = id
    }

    /**
     * Sets the Property instance for this capability. Must be called before {@link
     * CapabilityBuilderBase.build}.
     */
    protected fun setProperty(property: PropertyT) = asBuilder().apply {
        this.property = property
    }

    /**
     * Sets the ActionExecutor for this capability.
     *
     * setSessionFactory and setExecutor are mutually exclusive, so calling one will nullify
     * the other.
     *
     * This method accepts a coroutine-based ActionExecutor instance. There is also an overload
     * which accepts the ActionExecutorAsync instead.
     */
    fun setExecutor(actionExecutor: ActionExecutor<ArgumentT, OutputT>) = asBuilder().apply {
        this.actionExecutorAsync = actionExecutor.toActionExecutorAsync()
    }

    /**
     * Sets the ActionExecutorAsync for this capability.
     *
     * setSessionFactory and setExecutor are mutually exclusive, so calling one will nullify
     * the other.
     *
     * This method accepts the ActionExecutorAsync interface which returns a ListenableFuture.
     */
    fun setExecutor(
        actionExecutorAsync: ActionExecutorAsync<ArgumentT, OutputT>,
    ) = asBuilder().apply {
        this.actionExecutorAsync = actionExecutorAsync
    }

    /**
     * Sets the SessionBuilder instance which is used to create Session instaces for this
     * capability.
     *
     * setSessionFactory and setExecutor are mutually exclusive, so calling one will nullify the other.
     */
    protected open fun setSessionFactory(
        sessionFactory: SessionFactory<SessionT>,
    ): BuilderT = asBuilder().apply {
        this.sessionFactory = sessionFactory
    }

    /** Builds and returns this Capability. */
    open fun build(): Capability {
        val checkedId = requireNotNull(id, { "setId must be called before build" })
        val checkedProperty = requireNotNull(property, { "property must not be null." })
        if (actionExecutorAsync != null) {
            return SingleTurnCapabilityImpl(
                checkedId,
                actionSpec,
                checkedProperty,
                actionExecutorAsync!!,
            )
        } else {
            return TaskCapabilityImpl(
                checkedId,
                actionSpec,
                checkedProperty,
                requireNotNull(
                    sessionFactory,
                    { "either setExecutor or setSessionFactory must be called before build" },
                ),
                sessionBridge!!,
                sessionUpdaterSupplier!!,
            )
        }
    }
}

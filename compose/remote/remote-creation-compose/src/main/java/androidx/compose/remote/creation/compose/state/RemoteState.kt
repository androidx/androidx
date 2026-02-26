/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * A readable but not writable Remote Compose State value.
 *
 * `RemoteState` represents a value that is available during remote document creation. It may
 * represent either a constant value or a dynamic expression that evaluates on the remote rendering
 * engine.
 *
 * In Remote Compose recording mode, a type-specific ID is used to refer to this state within
 * [RemoteComposeCreationState].
 *
 * @param T The type of the value held by this state.
 */
@Stable
public interface RemoteState<T> {
    /**
     * Whether or not this remote state evaluates to a constant value.
     *
     * If true, [constantValue] will return the constant value.
     */
    @get:Suppress("GetterSetterNames")
    public val hasConstantValue: Boolean
        get() = constantValueOrNull != null

    /**
     * The constant value held by this state.
     *
     * @throws IllegalStateException if [hasConstantValue] is false.
     */
    public val constantValue: T
        get() = checkNotNull(constantValueOrNull) { "No constant value for this state" }

    /** The constant value held by this state, or `null` if the state is dynamic. */
    public val constantValueOrNull: T?

    /**
     * Represents the domain (namespace) for named remote states.
     *
     * Named states are used to identify variables that can be updated externally or shared across
     * different parts of a remote document.
     */
    public open class Domain internal constructor(internal val coreDomain: String?) {
        /**
         * The default user-defined domain.
         *
         * Recommended for application-specific state.
         */
        public object User : Domain(RemoteDomains.USER.toString())

        /** The system-defined domain, used for platform-level or framework state. */
        public object System : Domain(RemoteDomains.SYSTEM.toString())

        override fun toString(): String {
            return coreDomain ?: ""
        }

        override fun equals(other: Any?): Boolean {
            return other is Domain && other.coreDomain == coreDomain
        }

        override fun hashCode(): Int {
            return coreDomain.hashCode()
        }
    }
}

/** Common base interface for all Remote types. */
public abstract class BaseRemoteState<T> internal constructor() : RemoteState<T> {
    /** The constant value or null if there isn't one. */
    public abstract override val constantValueOrNull: T?

    /**
     * Returns a new or cached id for this [RemoteState] within the [RemoteComposeCreationState].
     *
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value, for the given [creationState].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getIdForCreationState(creationState: RemoteComposeCreationState): Int {
        return creationState.remoteVariableToId.getOrPut(this) { writeToDocument(creationState) }
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value, for the given [creationState] as a long.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getLongIdForCreationState(creationState: RemoteComposeCreationState): Long {
        return getIdForCreationState(creationState).toLong() + 0x100000000L
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value encoded in a Float NaN, for the given [creationState].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float =
        Utils.asNan(getIdForCreationState(creationState))

    /**
     * Writes the Remote Value to the [creationState] and returns the allocated ID.
     *
     * @param creationState The [RemoteComposeCreationState] to write to.
     * @return The ID allocated by the [RemoteComposeWriter].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun writeToDocument(creationState: RemoteComposeCreationState): Int
}

/**
 * A readable and writable Remote Compose State value.
 *
 * It represents a direct value (var) that can be modified, typically resulting in a variable being
 * allocated in the remote document.
 *
 * @param T The type of the value held by this state.
 */
@Stable public interface MutableRemoteState<T> : RemoteState<T>

/**
 * Remembers a named state value.
 *
 * This function retrieves a named state from the current [RemoteComposeCreationState]. If the state
 * does not already exist, it is created using the provided `function`. This ensures that the same
 * named state instance is reused across all compositions of the document, identified by its `name`
 * and `domain`.
 *
 * This method only caches the instance of the [RemoteState]. Avoiding writing the same value to the
 * document multiple times is handled by [BaseRemoteState.getIdForCreationState].
 *
 * @param T The type of the state object, which must extend [BaseRemoteState].
 * @param name A unique name to identify this state object within its domain.
 * @param domain The domain to which this named state belongs. See [RemoteState.Domain].
 * @param function A lambda that creates the state object if it doesn't already exist.
 * @return The existing or newly created state object of type [T].
 */
@RemoteComposable
@Composable
internal inline fun <reified T : RemoteState<*>> rememberNamedState(
    name: String,
    domain: RemoteState.Domain,
    noinline function: (RemoteComposeCreationState) -> T,
): T {
    return LocalRemoteComposeCreationState.current.getOrCreateNamedState(
        T::class.java,
        name,
        domain.coreDomain,
        function,
    )
}

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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

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

/** Common base interface for all Remote types. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseRemoteState<T> internal constructor() : RemoteState<T> {
    /** The constant value or null if there isn't one. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override abstract val constantValueOrNull: T?

    /** Whether or not this remote value always evaluates to the same result. */
    public open val hasConstantValue: Boolean
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = constantValueOrNull != null

    /**
     * The constant value or throws if null or unknown. Use should be checked by hasConstant value
     * first.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val constantValue: T
        get() =
            checkNotNull(constantValueOrNull) {
                "constantValue should only be accessed if hasConstantValue is true"
            }

    /**
     * Returns a new or cached id for this [RemoteState] within the RemoteComposeCreationState.
     *
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated
     * @return The ID of this remote value, for the given [creationState]
     */
    public open fun getIdForCreationState(creationState: RemoteComposeCreationState): Int {
        return creationState.remoteVariableToId.getOrPut(this) { writeToDocument(creationState) }
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated
     * @return The ID of this remote value, for the given [creationState] as a long
     */
    public open fun getLongIdForCreationState(creationState: RemoteComposeCreationState): Long {
        return getIdForCreationState(creationState).toLong() + 0x100000000L
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated
     * @return The ID of this remote value encoded in a Float NaN, for the given [creationState]
     */
    public open fun getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float =
        Utils.asNan(getIdForCreationState(creationState))

    /**
     * Writes the Remote Value to the [creationState] and returns the allocated ID.
     *
     * @param creationState The [RemoteComposeCreationState] to write to
     * @return The ID allocated by the [RemoteComposeWriter]
     */
    public abstract fun writeToDocument(creationState: RemoteComposeCreationState): Int
}

/**
 * A readable but not writable Remote Compose State value.
 *
 * It may represent either a mutable direct value (var), or some expression that might change
 * externally.
 *
 * In Remote Compose recording mode, the type specific id should be used.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public interface RemoteState<T> {
    /** The constant value or throws if null or unknown. */
    public val constantValue: T

    /** The constant value or null if there isn't one. */
    public val constantValueOrNull: T?

    /** Represents the domain (namespace) for named remote states. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open class Domain internal constructor(internal val coreDomain: String?) {
        /** The default user-defined domain. */
        public object User : Domain(RemoteDomains.USER.toString())

        /** The system-defined domain, used for platform-level states. */
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

/**
 * A readable and writable Remote Compose State value.
 *
 * It represents a direct value (var).
 *
 * In Remote Compose recording mode, the type specific id should be used.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public interface MutableRemoteState<T> : RemoteState<T> {}

/**
 * Remembers a named state value.
 *
 * This function retrieves a named state from the current [RemoteComposeCreationState]. If the state
 * does not already exist, it is created using the provided `function`. This ensures that the same
 * named state instance is reused across all compositions of the document, identified by its `name`
 * and `domain`.
 *
 * This method only caches the instance of the RemoteState. Avoiding writing the same value to the
 * document multiple times, is handled by [BaseRemoteState.getIdForCreationState]
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

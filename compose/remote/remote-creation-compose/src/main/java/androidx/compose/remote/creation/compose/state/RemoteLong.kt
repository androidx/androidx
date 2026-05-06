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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Abstract base class for all remote long representations. This class extends [RemoteState<Long>].
 */
@Stable
public abstract class RemoteLong internal constructor() : BaseRemoteState<Long>() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteLong] instance from a constant [Long] value. This value will be added as
         * a constant to the remote document.
         *
         * @param v The constant [Long] value.
         * @return A [MutableRemoteLong] representing the constant value.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(v: Long): RemoteLong {
            return MutableRemoteLong(v, cacheKey = RemoteConstantCacheKey(v)) { creationState ->
                creationState.document.addLong(v)
            }
        }

        /**
         * Creates a [RemoteLong] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteLong] referencing the ID.
         */
        internal fun createForId(id: Int): RemoteLong = MutableRemoteLong(id)

        /**
         * Creates a named [RemoteLong] with an initial value. Named remote longs can be set via
         * AndroidRemoteContext.setNamedLong.
         *
         * @param name The unique name for this remote long.
         * @param defaultValue The initial [Long] value for the named remote long.
         * @param domain The domain of the named long (defaults to [RemoteState.Domain.User]).
         * @return A [RemoteLong] representing the named long.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun createNamedRemoteLong(
            name: String,
            defaultValue: Long,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteLong {
            return MutableRemoteLong(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                creationState.document.addNamedLong(domain.prefixed(name), defaultValue)
            }
        }
    }
}

/**
 * A mutable implementation of [RemoteLong].
 *
 * @param constantValueOrNull A nullable value if this [MutableRemoteLong] is constant.
 */
public class MutableRemoteLong
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Long?,
    internal override val cacheKey: RemoteStateCacheKey,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteLong(), MutableRemoteState<Long> {

    /**
     * Constructor for [MutableRemoteLong] that allows specifying an optional initial ID. If no ID
     * is provided, a new float variable ID is reserved.
     *
     * @param id An optional explicit ID for this mutable long. If `null`, a new ID is reserved.
     */
    internal constructor(
        id: Int
    ) : this(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id), { id })

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    public override fun toString(): String {
        return "MutableRemoteLong@${this.hashCode()} =" + constantValueOrNull
    }

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteLong] instance.
         */
        public fun createMutable(initialValue: Long): MutableRemoteLong {
            return MutableRemoteLong(
                constantValueOrNull = null,
                cacheKey = RemoteStateInstanceKey(),
            ) { creationState ->
                creationState.document.addLong(initialValue)
            }
        }

        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteLong] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Int): MutableRemoteLong = MutableRemoteLong(id)
    }
}

/**
 * Factory composable for mutable remote long state.
 *
 * @param initialValue The initial [Long] value.
 * @return A [MutableRemoteLong] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberMutableRemoteLong(initialValue: Long): MutableRemoteLong {
    return remember {
        MutableRemoteLong(
            constantValueOrNull = null,
            cacheKey = RemoteStateInstanceKey(),
            idProvider = { creationState -> creationState.document.addLong(initialValue) },
        )
    }
}

/** Factory composable for mutable remote long state. */
@Composable
@Deprecated("Use rememberMutableRemoteLong(value())")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRemoteLongValue(value: () -> Long): MutableRemoteLong =
    rememberMutableRemoteLong(value())

/**
 * Remembers a named remote long expression.
 *
 * @param name The unique name for this remote long.
 * @param domain The domain of the named long (defaults to [RemoteState.Domain.User]).
 * @param defaultValue The initial long value.
 * @return A [RemoteLong] representing the named remote long expression.
 */
@Composable
public fun rememberNamedRemoteLong(
    name: String,
    defaultValue: Long,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteLong {
    return rememberNamedState(name, domain) {
        MutableRemoteLong(
            constantValueOrNull = null,
            cacheKey = RemoteNamedCacheKey(domain, name),
        ) { creationState ->
            creationState.document.addNamedLong(domain.prefixed(name), defaultValue)
        }
    }
}

/** A Composable function to remember and provide a **named** mutable remote long value. */
@Composable
@Deprecated("Use rememberNamedRemoteLong(name, domain, defaultValue = content)")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRemoteLongValue(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> Long,
): RemoteLong {
    return rememberNamedState(name, domain) {
        val initial = value()
        MutableRemoteLong(
            constantValueOrNull = null,
            cacheKey = RemoteNamedCacheKey(domain, name),
        ) { creationState ->
            val id = creationState.document.addNamedLong(domain.prefixed(name), initial)
            creationState.document.setStringName(id, domain.prefixed(name))
            id
        }
    }
}

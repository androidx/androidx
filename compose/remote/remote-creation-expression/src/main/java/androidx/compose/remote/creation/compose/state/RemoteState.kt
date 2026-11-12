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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Stable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public interface RemoteState<T> {
    @get:Suppress("GetterSetterNames")
    public val hasConstantValue: Boolean
        get() = constantValueOrNull != null

    public val constantValue: T
        get() = checkNotNull(constantValueOrNull) { "No constant value for this state" }

    public val constantValueOrNull: T?

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val asEncoded: RemoteState<*>
        get() = this

    /** Returns the expression as a human-readable string. */
    @Suppress("HiddenAbstractMethodInInterface")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toDebugString(): String

    @Stable
    public open class Domain(namespace: String?) {
        internal val coreDomain: String? = namespace

        init {
            if (namespace != null) {
                require(namespace.isNotEmpty()) {
                    "Domain namespace cannot be empty; use Domain.None for unprefixed names"
                }
                require(':' !in namespace) { "Domain namespace cannot contain ':' ($namespace)" }
            }
        }

        /**
         * The string prefix (`"<namespace>:"`) prepended to variable names in this domain, or `""`
         * for [None].
         */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val prefix: String
            get() = if (coreDomain != null) "$coreDomain:" else ""

        public fun prefixed(name: String): String {
            return "$prefix$name"
        }

        public object User : Domain(USER_DOMAIN)

        public object System : Domain(SYSTEM_DOMAIN)

        public object None : Domain(null)

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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseRemoteState<T : Any>
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(initialCacheKey: RemoteStateCacheKey) : RemoteState<T> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val cacheKey: RemoteStateCacheKey = initialCacheKey

    init {
        // Register with BaseRemoteStateCacheKey.
        if (initialCacheKey is BaseRemoteStateCacheKey) {
            initialCacheKey.setState(this)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String = cacheKey.toDebugString()

    public abstract override val constantValueOrNull: T?

    /**
     * Returns `true` if this [BaseRemoteState] is structurally equal to [other].
     *
     * Two remote states are structurally equal if:
     * - They are the same instance (`this === other`).
     * - Both evaluate to constant values and those constant values are equal.
     * - Both are dynamic and have equal [cacheKey]s representing identical AST DAGs.
     *
     * @param other The other [BaseRemoteState] to compare with.
     * @return `true` if structurally equal, `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun isStructurallyEqual(other: BaseRemoteState<*>?): Boolean {
        if (this === other) return true
        if (other == null) return false
        return cacheKey == other.cacheKey
    }

    /**
     * Traverses this [BaseRemoteState] expression DAG using the provided [visitor].
     *
     * @param visitor The visitor that inspects each node.
     * @return The result of visiting this node.
     */
    internal open fun <R> accept(visitor: RemoteStateVisitor<R>): R =
        cacheKey.accept(visitor, mutableMapOf())

    /**
     * Traverses this [BaseRemoteState] expression DAG using the provided [visitor].
     *
     * @param memo A memoization map to ensure shared DAG nodes are visited once.
     * @param visitor The visitor that inspects each node.
     * @return The result of visiting this node.
     */
    internal open fun <R> accept(
        memo: MutableMap<RemoteStateCacheKey, R>,
        visitor: RemoteStateVisitor<R>,
    ): R = cacheKey.accept(visitor, memo)

    /**
     * Returns a new or cached id for this [RemoteState] within the [RemoteComposeCreationState].
     *
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value, for the given [creationState].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getIdForCreationState(creationState: RemoteComposeCreationState): Int {
        return creationState.getOrPutVariableId(cacheKey) { writeToDocument(creationState) }
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
     * @return The ID allocated by the [androidx.compose.remote.creation.RemoteComposeWriter].
     */
    internal open fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        throw UnsupportedOperationException(
            "${this::class.simpleName} must override writeToDocument or getIdForCreationState"
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public interface MutableRemoteState<T> : RemoteState<T> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val asEncodedMutable: MutableRemoteState<*>
        get() = this
}

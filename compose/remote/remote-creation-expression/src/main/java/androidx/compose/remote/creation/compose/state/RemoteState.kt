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

/**
 * A read-only Remote Compose state value.
 *
 * [RemoteState] represents a value available during remote document creation — either a known
 * constant or a dynamic expression evaluated on the remote player at runtime. Typed subtypes (such
 * as [RemoteFloat] and [RemoteInt]) are read-only supertypes, while [MutableRemoteState] subtypes
 * (such as [MutableRemoteFloat] and [MutableRemoteInt]) represent writable variables, analogous to
 * `State` and `MutableState`.
 *
 * **Dynamic evaluation and constant folding:** Operations on [RemoteState] values typically record
 * expressions that are evaluated dynamically on the remote player at runtime as inputs change.
 * However, if all input operands have a known constant value ([hasConstantValue] is `true`),
 * constant folding evaluates the result immediately at creation time into a constant [RemoteState].
 *
 * In Remote Compose recording mode, a type-specific ID is used to refer to a dynamic state within
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

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val asEncoded: RemoteState<*>
        get() = this

    /** Returns the expression as a human-readable string. */
    @Suppress("HiddenAbstractMethodInInterface")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toDebugString(): String

    /**
     * The [Domain] namespaces named [RemoteState] variables so document-specific, host-defined, and
     * shared values do not collide.
     *
     * Named states — created via APIs such as [RemoteFloat.createNamedRemoteFloat],
     * [RemoteInt.createNamedRemoteInt], [RemoteBoolean.createNamedRemoteBoolean],
     * [RemoteString.createNamedRemoteString], and [RemoteColor.createNamedRemoteColor] — are
     * recorded in a remote document under `<namespace>:<name>` (or bare `<name>` for [None]; see
     * [prefixed]). Hosts update these variables at playback without re-recording the document, and
     * can inspect a document's declared variable names to push only the domains that document
     * reads:
     * - [User] (`"USER"`): Document-defined state supplied by the host specifically for a single
     *   document. Its meaning is only known for that document (for example, `"shutter.is_open"` for
     *   a card's bound device slot).
     * - [System] (`"SYSTEM"`): Host-defined state whose meaning is defined for a specific host
     *   across documents, providing values such as current heart rate BPM
     *   (`"SYSTEM:heart_rate_bpm"`).
     * - Custom domains (`Domain("HOME")`): Shared namespaces defined for an application ecosystem
     *   so multiple documents played in the same host can read well-known shared values alongside
     *   their own [User] slots.
     * - [None] (`null`): Unprefixed variables stored directly under their bare name without a `':'`
     *   prefix.
     *
     * For example, a home automation dashboard can define a `"HOME"` domain for home-wide values
     * alongside per-card [User] device slots:
     * ```kotlin
     * val Home = RemoteState.Domain("HOME")
     *
     * // Document-defined slot in USER ("USER:shutter.is_open"), bound per card instance:
     * val isShutterOpen =
     *     RemoteBoolean.createNamedRemoteBoolean(name = "shutter.is_open", defaultValue = false)
     *
     * // Shared home values in HOME ("HOME:sun.elevation", "HOME:sun.above_horizon"), meaningful
     * // to any card on the dashboard and defaulting to daytime when solar data is unavailable:
     * val sunElevation =
     *     RemoteFloat.createNamedRemoteFloat(
     *         name = "sun.elevation",
     *         defaultValue = 45f,
     *         domain = Home,
     *     )
     * val isSunUp =
     *     RemoteBoolean.createNamedRemoteBoolean(
     *         name = "sun.above_horizon",
     *         defaultValue = true,
     *         domain = Home,
     *     )
     *
     * // Host-defined value in SYSTEM ("SYSTEM:heart_rate_bpm"), provided by a wearable host:
     * val heartRateBpm =
     *     RemoteFloat.createNamedRemoteFloat(
     *         name = "heart_rate_bpm",
     *         defaultValue = 70f,
     *         domain = RemoteState.Domain.System,
     *     )
     * ```
     *
     * @param namespace non-empty domain identifier without `':'` characters (for example,
     *   `"HOME"`), or `null` for an unprefixed domain equivalent to [None]
     * @throws IllegalArgumentException if [namespace] is empty or contains `':'`
     */
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

        /**
         * Prepends this domain's namespace and `':'` to [name] (for example,
         * `"HOME:sun.elevation"`), or returns [name] unchanged for [None].
         *
         * @param name variable name within this domain
         * @return qualified variable name stored in the remote document
         */
        public fun prefixed(name: String): String {
            return "$prefix$name"
        }

        /**
         * [User] defines the default `"USER"` domain for document-specific named state.
         *
         * Variables in [User] are defined by the document and expected to be provided by the host
         * specifically for this document; their meaning is only known for a specific document (for
         * example, `"USER:shutter.is_open"` or `"USER:target_temperature"` for a dashboard card's
         * configured device).
         */
        public object User : Domain(USER_DOMAIN)

        /**
         * [System] defines the `"SYSTEM"` domain for host-defined state shared across documents.
         *
         * Variables in [System] have a meaning defined for a specific host, providing live values
         * such as current heart rate BPM (`"SYSTEM:heart_rate_bpm"`).
         */
        public object System : Domain(SYSTEM_DOMAIN)

        /**
         * [None] defines an unprefixed domain where variables are stored directly under their bare
         * name without a `':'` namespace prefix.
         */
        public object None : Domain(null)

        /**
         * Returns this domain's namespace (for example, `"USER"`, `"SYSTEM"`, or `"HOME"`), or `""`
         * for [None].
         *
         * @return the domain namespace string, or `""` if unprefixed
         */
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

/** Common base class for all Remote types. */
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

    /** The constant value or null if there isn't one. */
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

/**
 * A readable and writable Remote Compose State value.
 *
 * It represents a direct value (var) that can be modified, typically resulting in a variable being
 * allocated in the remote document.
 *
 * @param T The type of the value held by this state.
 */
@Stable
public interface MutableRemoteState<T> : RemoteState<T> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val asEncodedMutable: MutableRemoteState<*>
        get() = this
}

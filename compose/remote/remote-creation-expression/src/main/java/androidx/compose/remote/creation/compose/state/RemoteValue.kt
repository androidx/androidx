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
import androidx.compose.remote.document.RemoteDocumentWriter
import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

/**
 * A read-only Remote Compose value.
 *
 * [RemoteValue] represents a value available during remote document creation — either a known
 * constant or a dynamic expression evaluated on the remote player at runtime. Typed subtypes (such
 * as [RemoteFloat] and [RemoteInt]) are read-only supertypes, while [MutableRemoteValue] subtypes
 * (such as [MutableRemoteFloat] and [MutableRemoteInt]) represent writable variables, analogous to
 * `State` and `MutableState`.
 *
 * **Dynamic evaluation and constant folding:** Operations on [RemoteValue] values typically record
 * expressions that are evaluated dynamically on the remote player at runtime as inputs change.
 * However, if all input operands have a known constant value ([hasConstantValue] is `true`),
 * constant folding evaluates the result immediately at creation time into a constant [RemoteValue].
 *
 * **Structural equality (`==`):** Comparing two read-only [RemoteValue] values with `==` ([equals])
 * checks structural equality rather than reference identity: constant values are equal when their
 * types and constant values match, and dynamic expressions are equal when their types and
 * expression trees match (for example, identical operations over identical operands or named
 * variables with the same domain and name). By contrast, [MutableRemoteValue] instances compare by
 * their underlying mutable variable rather than expression equivalence, so separately created
 * mutable variables are never `==` and a read-only [RemoteValue] is never `==` to a
 * [MutableRemoteValue] (even when wrapping the same backing value). Use [RemoteFloat.isEqualTo] (or
 * the equivalent `isEqualTo` method on typed subclasses) when you need a runtime [RemoteBoolean]
 * evaluated on the remote player.
 *
 * Custom external implementations of [RemoteValue] are not supported; implementations must come
 * from the library's `Remote*` types.
 *
 * In Remote Compose recording mode, a type-specific ID is used to refer to a dynamic [RemoteValue]
 * within [RemoteComposeCreationState].
 *
 * @param T The type of the value held by this [RemoteValue].
 */
// TODO(b/587746359): Support custom implementations of RemoteValue.
@Stable
public interface RemoteValue<T> {
    /**
     * Whether or not this [RemoteValue] evaluates to a constant value.
     *
     * If true, [constantValue] will return the constant value.
     */
    @get:Suppress("GetterSetterNames")
    public val hasConstantValue: Boolean
        get() = constantValueOrNull != null

    /**
     * The constant value held by this [RemoteValue].
     *
     * @throws IllegalStateException if [hasConstantValue] is false.
     */
    public val constantValue: T
        get() = checkNotNull(constantValueOrNull) { "No constant value for this RemoteValue" }

    /** The constant value held by this [RemoteValue], or `null` if it is dynamic. */
    public val constantValueOrNull: T?

    /**
     * The [Domain] namespaces named [RemoteValue] variables so document-specific, host-defined, and
     * shared values do not collide.
     *
     * Named values — created via APIs such as [RemoteFloat.createNamedRemoteFloat],
     * [RemoteInt.createNamedRemoteInt], [RemoteBoolean.createNamedRemoteBoolean],
     * [RemoteString.createNamedRemoteString], and [RemoteColor.createNamedRemoteColor] — are
     * recorded in a remote document under `<namespace>:<name>` (or bare `<name>` for [None]; see
     * [prefixed]). Hosts update these variables at playback without re-recording the document, and
     * can inspect a document's declared variable names to push only the domains that document
     * reads:
     * - [User] (`"USER"`): Document-defined value supplied by the host specifically for a single
     *   document. Its meaning is only known for that document (for example, `"shutter.is_open"` for
     *   a card's bound device slot).
     * - [System] (`"SYSTEM"`): Host-defined value whose meaning is defined for a specific host
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
     * val Home = RemoteValue.Domain("HOME")
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
     *         domain = RemoteValue.Domain.System,
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
         * [User] defines the default `"USER"` domain for document-specific named values.
         *
         * Variables in [User] are defined by the document and expected to be provided by the host
         * specifically for this document; their meaning is only known for a specific document (for
         * example, `"USER:shutter.is_open"` or `"USER:target_temperature"` for a dashboard card's
         * configured device).
         */
        public object User : Domain(USER_DOMAIN)

        /**
         * [System] defines the `"SYSTEM"` domain for host-defined values shared across documents.
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

/**
 * Common base class for all Remote types.
 *
 * Implements structural [equals] and [hashCode] using [equalityType], mutability
 * ([MutableRemoteValue]), and [cacheKey].
 */
internal abstract class BaseRemoteValue<T : Any>(initialCacheKey: RemoteStateCacheKey) :
    RemoteValue<T>, RemoteValueAccess {
    override val cacheKey: RemoteStateCacheKey = initialCacheKey

    init {
        // Register with BaseRemoteStateCacheKey.
        if (initialCacheKey is BaseRemoteStateCacheKey) {
            initialCacheKey.setState(this)
        }
    }

    internal open val equalityType: KClass<*>
        get() = this::class

    override fun adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
        if (equalityType.isInstance(candidate)) candidate else this

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is BaseRemoteValue<*> &&
                equalityType == other.equalityType &&
                (this is MutableRemoteValue<*>) == (other is MutableRemoteValue<*>) &&
                cacheKey == other.cacheKey)

    override fun hashCode(): Int {
        var result = equalityType.hashCode()
        result = 31 * result + (this is MutableRemoteValue<*>).hashCode()
        result = 31 * result + cacheKey.hashCode()
        return result
    }

    override fun toString(): String = cacheKey.toDebugString()

    /**
     * Traverses this [BaseRemoteValue] expression DAG using the provided [visitor].
     *
     * @param visitor The visitor that inspects each node.
     * @return The result of visiting this node.
     */
    open fun <R> accept(visitor: RemoteStateVisitor<R>): R =
        cacheKey.accept(visitor, mutableMapOf())

    /**
     * Traverses this [BaseRemoteValue] expression DAG using the provided [visitor].
     *
     * @param memo A memoization map to ensure shared DAG nodes are visited once.
     * @param visitor The visitor that inspects each node.
     * @return The result of visiting this node.
     */
    open fun <R> accept(
        memo: MutableMap<RemoteStateCacheKey, R>,
        visitor: RemoteStateVisitor<R>,
    ): R = cacheKey.accept(visitor, memo)

    /**
     * Returns a new or cached id for this [RemoteValue] within the [RemoteComposeCreationState].
     *
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value, for the given [creationState].
     */
    override fun getId(creationState: RemoteComposeCreationState): Int {
        return creationState.getOrPutVariableId(cacheKey) { writeToDocument(creationState) }
    }

    /**
     * Writes the Remote Value to the [creationState] and returns the allocated ID.
     *
     * @param creationState The [RemoteComposeCreationState] to write to.
     * @return The ID allocated by the [RemoteDocumentWriter].
     */
    open fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        throw UnsupportedOperationException(
            "${this::class.simpleName} must override writeToDocument or getId"
        )
    }
}

/**
 * A readable and writable Remote Compose value.
 *
 * It represents a direct value (var) that can be modified, typically resulting in a variable being
 * allocated in the remote document.
 *
 * @param T The type of the value held by this [MutableRemoteValue].
 */
@Stable public interface MutableRemoteValue<T> : RemoteValue<T>

/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.collection.MutableObjectIntMap
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.layout.RemoteComponentToken
import androidx.compose.remote.creation.compose.state.BaseRemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.nanIdContentEquals
import androidx.compose.remote.creation.compose.state.nanIdContentHashCode

/** Provider for resolving a layout component's [RemoteComponentToken] during rendering. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemoteComponentTokenProvider {
    /** Returns the [RemoteComponentToken] for the active layout component, or `null` if none. */
    public fun getOrCreateComponentToken(): RemoteComponentToken?
}

/** Scope providing access to a [RemoteComposeWriter] document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteDocumentScope {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val document: RemoteComposeWriter

    /** Resolves the active [RemoteComponentToken] for this scope, or `null` if none is bound. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun resolveComponentToken(): RemoteComponentToken? = null
}

/** Base state for remote creation, holding expression caches and variable ID mappings. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteComposeCreationState : RemoteDocumentScope {
    public override lateinit var document: RemoteComposeWriter
    public open lateinit var remoteDensity: RemoteDensity

    /**
     * Ids of float expressions already written to [document], keyed by their lowered RPN array and
     * optional animation. Used to write each distinct expression only once.
     */
    internal val floatExpressionIds: MutableObjectIntMap<LoweredFloatExpressionKey> =
        MutableObjectIntMap()

    /**
     * Ids of integer expressions already written to [document], keyed by their lowered RPN array.
     * Used to write each distinct expression only once.
     */
    internal val intExpressionIds: MutableObjectIntMap<LoweredIntExpressionKey> =
        MutableObjectIntMap()
    public var ready: Boolean = true
    public val remoteVariableToId: MutableObjectIntMap<RemoteStateCacheKey> = MutableObjectIntMap()
    public val floatArrayCache: HashMap<RemoteStateCacheKey, FloatArray> = HashMap()
    public val longArrayCache: HashMap<RemoteStateCacheKey, LongArray> = HashMap()

    /**
     * Per-document emission state for a [RemoteComponentToken] during a single render pass.
     *
     * Cleared by [clearDocumentCaches] when starting a new document capture.
     */
    private class TokenComponentState {
        /**
         * Allocated variable IDs indexed by `RemoteComponentToken.PROP_*`.
         *
         * [NO_VARIABLE_ID] (`-1`) means no variable has been allocated for that property in the
         * current [document] yet; otherwise holds the integer variable ID reserved or emitted for
         * that component property.
         */
        val variableIds: IntArray = IntArray(RemoteComponentToken.PROP_COUNT) { NO_VARIABLE_ID }

        /**
         * Bitmask indexed by `RemoteComponentToken.PROP_*` (`1 shl propIndex`) tracking which
         * `ComponentValue` operations have already been written into the owning component's body in
         * [document].
         */
        var emittedMask: Int = 0

        /**
         * `true` while the [document] writer cursor is directly inside the owning component's body
         * (before any child components are emitted), when `document.addComponent*Value()` binds to
         * the owning component's `lastComponentId`. Set back to `false` by
         * [disableDirectComponentEmission] before child nodes render and by [endComponentContent].
         */
        var canEmitDirectly: Boolean = false

        /**
         * Explicit wire component ID to target if `document.buffer.lastComponentId` has moved away
         * from the owning component when [endComponentContent] flushes deferred values, or `-1`
         * when `LayoutComponent.inflate()` rewrites `ComponentValue` IDs from the container body.
         */
        var componentId: Int = -1

        companion object {
            const val NO_VARIABLE_ID: Int = -1
        }
    }

    private val tokenComponentStates: HashMap<RemoteComponentToken, TokenComponentState> = HashMap()

    /**
     * The [RemoteComponentTokenProvider] currently rendering its content or canvas commands, used
     * to resolve component tokens from scopes that are not tied to a specific modifier node.
     */
    public var currentRenderingNode: RemoteComponentTokenProvider? = null

    public override fun resolveComponentToken(): RemoteComponentToken? =
        currentRenderingNode?.getOrCreateComponentToken()

    /**
     * Marks the start of [token]'s component body in [document] and emits `ComponentValue`
     * operations for any properties already requested on [token] (either from an earlier expression
     * evaluation in this render pass or recorded on [token] during a prior pass).
     */
    public fun beginComponentContent(token: RemoteComponentToken, componentId: Int = -1) {
        val state = tokenComponentStates.getOrPut(token) { TokenComponentState() }
        if (componentId != -1) {
            state.componentId = componentId
        }
        state.canEmitDirectly = true
        for (propIndex in 0 until RemoteComponentToken.PROP_COUNT) {
            val bit = 1 shl propIndex
            if ((state.emittedMask and bit) != 0) continue
            val existingId = state.variableIds[propIndex]
            if (existingId != TokenComponentState.NO_VARIABLE_ID) {
                document.buffer.addComponentValue(
                    existingId,
                    RemoteComponentToken.toComponentValueType(propIndex),
                )
                state.emittedMask = state.emittedMask or bit
            } else if (token.isRequested(propIndex)) {
                state.variableIds[propIndex] = emitDirectComponentValueId(propIndex)
                state.emittedMask = state.emittedMask or bit
            }
        }
    }

    /**
     * Disables direct `ComponentValue` emission for [token] before child components are rendered.
     *
     * Rendering child components updates `document.buffer.lastComponentId` to each child's ID, so
     * any [getComponentValue] call for [token] triggered lazily while rendering descendants must
     * reserve a float variable ID instead of emitting directly, leaving the `ComponentValue`
     * operation to be emitted in [endComponentContent] inside [token]'s own container body.
     */
    public fun disableDirectComponentEmission(token: RemoteComponentToken) {
        tokenComponentStates[token]?.canEmitDirectly = false
    }

    /**
     * Flushes any deferred `ComponentValue` operations reserved for [token] while rendering its
     * children, writing them into [token]'s component body before the container closes.
     */
    public fun endComponentContent(token: RemoteComponentToken) {
        val state = tokenComponentStates[token] ?: return
        state.canEmitDirectly = false
        for (propIndex in 0 until RemoteComponentToken.PROP_COUNT) {
            val bit = 1 shl propIndex
            if ((state.emittedMask and bit) != 0) continue
            val reservedId = state.variableIds[propIndex]
            if (reservedId != TokenComponentState.NO_VARIABLE_ID) {
                val type = RemoteComponentToken.toComponentValueType(propIndex)
                val buffer = document.buffer
                if (state.componentId == -1 || buffer.lastComponentId == state.componentId) {
                    buffer.addComponentValue(reservedId, type)
                } else {
                    ComponentValue.apply(buffer.buffer, type, state.componentId, reservedId)
                }
                state.emittedMask = state.emittedMask or bit
            }
        }
    }

    /**
     * Returns the NaN-encoded float variable value representing [propIndex] (`PROP_WIDTH`,
     * `PROP_HEIGHT`, etc.) for the layout component identified by [token].
     *
     * Reuses the existing variable ID if already allocated in this [document]. Otherwise, if the
     * writer is currently directly inside [token]'s body ([TokenComponentState.canEmitDirectly]),
     * emits the `ComponentValue` operation immediately; if called before [beginComponentContent] or
     * while rendering descendants, reserves a float variable via `reserveFloatVariable()` so
     * [beginComponentContent] or [endComponentContent] binds it to [token]'s component body.
     */
    internal fun getComponentValue(token: RemoteComponentToken, propIndex: Int): Float {
        token.markRequested(propIndex)
        val state = tokenComponentStates.getOrPut(token) { TokenComponentState() }
        val existingId = state.variableIds[propIndex]
        if (existingId != TokenComponentState.NO_VARIABLE_ID) {
            return Utils.asNan(existingId)
        }
        val allocatedId =
            if (state.canEmitDirectly) {
                state.emittedMask = state.emittedMask or (1 shl propIndex)
                emitDirectComponentValueId(propIndex)
            } else {
                Utils.idFromNan(document.reserveFloatVariable())
            }
        state.variableIds[propIndex] = allocatedId
        return Utils.asNan(allocatedId)
    }

    /**
     * Emits the `ComponentValue` operation for [propIndex] onto [document]'s current component via
     * `RemoteComposeWriter.addComponent*Value()` (which returns a NaN-encoded float variable ID)
     * and decodes the returned NaN value to its integer variable ID via [Utils.idFromNan].
     */
    private fun emitDirectComponentValueId(propIndex: Int): Int {
        val nanEncodedId =
            when (propIndex) {
                RemoteComponentToken.PROP_WIDTH -> document.addComponentWidthValue()
                RemoteComponentToken.PROP_HEIGHT -> document.addComponentHeightValue()
                RemoteComponentToken.PROP_X -> document.addComponentXValue()
                RemoteComponentToken.PROP_Y -> document.addComponentYValue()
                RemoteComponentToken.PROP_ROOT_X -> document.addComponentRootXValue()
                RemoteComponentToken.PROP_ROOT_Y -> document.addComponentRootYValue()
                else -> throw IllegalArgumentException("Unknown property index: $propIndex")
            }
        return Utils.idFromNan(nanEncodedId)
    }

    private class CanvasOperationsScopeFrame {
        val floatExpressionKeys: ArrayList<LoweredFloatExpressionKey> = ArrayList()
        val intExpressionKeys: ArrayList<LoweredIntExpressionKey> = ArrayList()
        val variableKeys: ArrayList<RemoteStateCacheKey> = ArrayList()
        val floatArrayKeys: ArrayList<RemoteStateCacheKey> = ArrayList()
        val longArrayKeys: ArrayList<RemoteStateCacheKey> = ArrayList()
    }

    private val canvasOperationsScopes: ArrayList<CanvasOperationsScopeFrame> = ArrayList()

    /**
     * Enters a paint-scoped canvas block (`CanvasOperations` or `CanvasContent`).
     *
     * Expressions emitted into [document] while a canvas block is active live inside that canvas
     * container's operation list and are only re-evaluated during `paint()`, not during
     * `measure()`. Tracking and evicting newly cached computed expressions when the block exits
     * preserves common subexpression elimination within the canvas block while preventing
     * subsequent sibling or child layout/modifier expressions from referencing paint-only
     * expression IDs.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun enterCanvasOperationsScope() {
        canvasOperationsScopes.add(CanvasOperationsScopeFrame())
    }

    /** Exits the current paint-scoped canvas block and evicts computed expressions added in it. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun exitCanvasOperationsScope() {
        val frame = canvasOperationsScopes.removeAt(canvasOperationsScopes.lastIndex)
        for (i in 0 until frame.floatExpressionKeys.size) {
            floatExpressionIds.remove(frame.floatExpressionKeys[i])
        }
        for (i in 0 until frame.intExpressionKeys.size) {
            intExpressionIds.remove(frame.intExpressionKeys[i])
        }
        for (i in 0 until frame.variableKeys.size) {
            remoteVariableToId.remove(frame.variableKeys[i])
        }
        for (i in 0 until frame.floatArrayKeys.size) {
            floatArrayCache.remove(frame.floatArrayKeys[i])
        }
        for (i in 0 until frame.longArrayKeys.size) {
            longArrayCache.remove(frame.longArrayKeys[i])
        }
    }

    /**
     * Runs [block] inside a paint-scoped canvas expression cache frame.
     *
     * @see enterCanvasOperationsScope
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public inline fun <T> withCanvasOperationsScope(block: () -> T): T {
        enterCanvasOperationsScope()
        try {
            return block()
        } finally {
            exitCanvasOperationsScope()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun recordScopedFloatArrayKey(key: RemoteStateCacheKey) {
        if (canvasOperationsScopes.isNotEmpty()) {
            canvasOperationsScopes[canvasOperationsScopes.lastIndex].floatArrayKeys.add(key)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun recordScopedLongArrayKey(key: RemoteStateCacheKey) {
        if (canvasOperationsScopes.isNotEmpty()) {
            canvasOperationsScopes[canvasOperationsScopes.lastIndex].longArrayKeys.add(key)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun recordScopedVariableKey(key: RemoteStateCacheKey) {
        if (canvasOperationsScopes.isNotEmpty() && key.isCanvasScopedExpressionKey()) {
            canvasOperationsScopes[canvasOperationsScopes.lastIndex].variableKeys.add(key)
        }
    }

    internal fun recordScopedFloatExpressionKey(key: LoweredFloatExpressionKey) {
        if (canvasOperationsScopes.isNotEmpty()) {
            canvasOperationsScopes[canvasOperationsScopes.lastIndex].floatExpressionKeys.add(key)
        }
    }

    internal fun recordScopedIntExpressionKey(key: LoweredIntExpressionKey) {
        if (canvasOperationsScopes.isNotEmpty()) {
            canvasOperationsScopes[canvasOperationsScopes.lastIndex].intExpressionKeys.add(key)
        }
    }

    /**
     * Returns the cached [FloatArray] for [key], or computes and caches it using [compute].
     *
     * @param key cache key identifying the float expression
     * @param compute provider invoked on cache miss
     */
    public inline fun getOrPutFloatArray(
        key: RemoteStateCacheKey,
        crossinline compute: () -> FloatArray,
    ): FloatArray {
        floatArrayCache[key]?.let {
            return it
        }
        val computed = compute()
        floatArrayCache[key] = computed
        recordScopedFloatArrayKey(key)
        return computed
    }

    /**
     * Returns the cached [LongArray] for [key], or computes and caches it using [compute].
     *
     * @param key cache key identifying the integer expression
     * @param compute provider invoked on cache miss
     */
    public inline fun getOrPutLongArray(
        key: RemoteStateCacheKey,
        crossinline compute: () -> LongArray,
    ): LongArray {
        longArrayCache[key]?.let {
            return it
        }
        val computed = compute()
        longArrayCache[key] = computed
        recordScopedLongArrayKey(key)
        return computed
    }

    /**
     * Returns the id of a float expression with the same lowered [array] and [animation] that was
     * already written to [document], or calls [write] to write it and caches the resulting id.
     */
    internal inline fun getOrPutFloatExpressionId(
        array: FloatArray,
        animation: FloatArray?,
        crossinline write: () -> Int,
    ): Int {
        val key = LoweredFloatExpressionKey(array, animation)
        val existing = floatExpressionIds.getOrDefault(key, -1)
        if (existing != -1) {
            return existing
        }
        val id = write()
        floatExpressionIds.put(key, id)
        recordScopedFloatExpressionKey(key)
        return id
    }

    /**
     * Returns the id of an integer expression with the same lowered [array] that was already
     * written to [document], or calls [write] to write it and caches the resulting id.
     */
    internal inline fun getOrPutIntExpressionId(
        array: LongArray,
        crossinline write: () -> Int,
    ): Int {
        val key = LoweredIntExpressionKey(array)
        val existing = intExpressionIds.getOrDefault(key, -1)
        if (existing != -1) {
            return existing
        }
        val id = write()
        intExpressionIds.put(key, id)
        recordScopedIntExpressionKey(key)
        return id
    }

    /** Clears all caches tied to the current [document]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun clearDocumentCaches() {
        floatExpressionIds.clear()
        intExpressionIds.clear()
        remoteVariableToId.clear()
        floatArrayCache.clear()
        longArrayCache.clear()
        tokenComponentStates.clear()
        canvasOperationsScopes.clear()
        currentRenderingNode = null
    }

    /**
     * Returns the variable ID cached for [key], or allocates and caches one using [compute].
     *
     * @param key cache key identifying the remote state
     * @param compute allocator invoked on cache miss
     */
    public inline fun getOrPutVariableId(
        key: RemoteStateCacheKey,
        crossinline compute: () -> Int,
    ): Int {
        val id = remoteVariableToId.getOrDefault(key, -1)
        if (id == -1) {
            val nextId = compute()
            remoteVariableToId.put(key, nextId)
            recordScopedVariableKey(key)
            return nextId
        }
        return id
    }
}

private fun RemoteStateCacheKey.isCanvasScopedExpressionKey(): Boolean {
    if (this is RemoteOperationCacheKey) return true
    if (this is RemoteConstantCacheKey || this is RemoteStateIdKey) return false
    val state = (this as? BaseRemoteStateCacheKey)?.getState() ?: return false
    return (state is RemoteFloat && state !is MutableRemoteFloat) ||
        (state is RemoteInt && state !is MutableRemoteInt)
}

/**
 * Key for a lowered float expression: its RPN [array] plus optional [animation].
 *
 * The arrays contain NaN-encoded variable ids and operators, so equality uses [nanIdContentEquals];
 * [FloatArray.contentEquals] would treat e.g. `[a, b, ADD]` and `[c, d, MUL]` as equal.
 */
internal class LoweredFloatExpressionKey(
    private val array: FloatArray,
    private val animation: FloatArray?,
) {
    private val hashCode = 31 * array.nanIdContentHashCode() + animation.nanIdContentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoweredFloatExpressionKey) return false
        return hashCode == other.hashCode &&
            array.nanIdContentEquals(other.array) &&
            animation.nanIdContentEquals(other.animation)
    }

    override fun hashCode(): Int = hashCode
}

/** Key for a lowered integer expression, identified by its RPN [array]. */
internal class LoweredIntExpressionKey(private val array: LongArray) {
    private val hashCode = array.contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoweredIntExpressionKey) return false
        return hashCode == other.hashCode && array.contentEquals(other.array)
    }

    override fun hashCode(): Int = hashCode
}

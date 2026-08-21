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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp

/**
 * Traverses a [BaseRemoteState] expression DAG to inspect, collect, or rewrite nodes.
 *
 * Implementations process nodes bottom-up (post-order). Shared sub-expressions in the DAG are
 * visited once per traversal via the memoization map in [BaseRemoteState.accept].
 *
 * @param R The return type produced by visiting each node.
 */
internal fun interface RemoteStateVisitor<R> {
    /**
     * Visits a [RemoteStateCacheKey] node in the AST DAG.
     *
     * @param key The cache key being visited.
     * @param state The associated [BaseRemoteState], or `null` if not yet instantiated.
     * @param visitedArgs The results from visiting each child argument in
     *   [RemoteStateCacheKey.args] (empty for leaf nodes).
     * @return The result of visiting this node.
     */
    fun visit(key: RemoteStateCacheKey, state: BaseRemoteState<*>?, visitedArgs: List<R>): R
}

/**
 * Transforms this [BaseRemoteState] expression DAG bottom-up.
 *
 * Wrappers ([RemoteBoolean], [RemoteDp], [RemoteEnum]) are automatically unwrapped and re-wrapped.
 * If [transform] returns `null` for a node, default transformation behavior is applied (preserving
 * unchanged nodes, reconstructing operations with transformed arguments, resolving constants,
 * etc.).
 *
 * @param transform A lambda that inspects each [BaseRemoteState] node in the AST DAG and returns a
 *   replacement [BaseRemoteState], or `null` to preserve the node or use default reconstruction.
 * @return The transformed [BaseRemoteState] instance.
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <S : BaseRemoteState<*>> S.transform(
    transform: (state: BaseRemoteState<*>) -> BaseRemoteState<*>?
): S {
    if (hasConstantValue) {
        val replaced = transform(this)
        return (replaced ?: this) as S
    }

    return (when (this) {
        is RemoteBoolean -> {
            val newInt = intValue.transform(transform)
            if (newInt === intValue) this else RemoteBoolean(newInt)
        }
        is RemoteDp -> {
            val newValue = value.transform(transform)
            if (newValue === value) this else RemoteDp(newValue)
        }
        is RemoteEnum<*> -> {
            val newInt = intValue.transform(transform)
            if (newInt === intValue) this else RemoteEnum(newInt, enumEntries)
        }
        else -> {
            accept<BaseRemoteState<*>> { key, state, visitedArgs ->
                val node =
                    state
                        ?: when (key) {
                            is RemoteConstantCacheKey -> resolveConstantState(key)
                            is FloatArrayCacheKey -> FloatArrayRemoteState(key.floatArray, key)
                            else -> null
                        }
                val custom = if (node != null) transform(node) else null
                if (custom != null) {
                    when (custom) {
                        is RemoteBoolean -> custom.intValue
                        is RemoteDp -> custom.value
                        is RemoteEnum<*> -> custom.intValue
                        else -> custom
                    }
                } else {
                    when (key) {
                        is RemoteOperationCacheKey -> {
                            if (node != null && !hasArgsChanged(key.args, visitedArgs)) {
                                node
                            } else {
                                (key.op as RemoteOperation).reconstruct(visitedArgs)
                            }
                        }
                        is RemoteConstantCacheKey -> node ?: resolveConstantState(key)
                        is FloatArrayCacheKey -> node ?: FloatArrayRemoteState(key.floatArray, key)
                        is RemoteNamedCacheKey ->
                            node
                                ?: throw IllegalStateException(
                                    "Named cache key missing state: $key"
                                )
                        is RemoteStateIdKey,
                        is RemoteStateInstanceKey,
                        is RemoteStateArrayKey,
                        is RemoteComponentCacheKey ->
                            node ?: throw IllegalStateException("Cache key missing state: $key")
                        else -> throw UnsupportedOperationException("Unknown cache key: $key")
                    }
                }
            }
        }
    })
        as S
}

private fun hasArgsChanged(
    original: List<RemoteStateCacheKey>,
    visited: List<BaseRemoteState<*>>,
): Boolean {
    for (i in original.indices) {
        if (visited[i].cacheKey != original[i]) return true
    }
    return false
}

/** Resolves or creates a [BaseRemoteState] corresponding to the given [RemoteConstantCacheKey]. */
internal fun resolveConstantState(key: RemoteConstantCacheKey): BaseRemoteState<*> {
    return when (val v = key.value) {
        is Float -> RemoteFloat(v)
        is Int -> RemoteInt(v)
        is Boolean -> RemoteBoolean(v)
        is String -> RemoteString(v)
        is Color -> RemoteColor(v)
        is Dp -> RemoteDp(RemoteFloat(v.value))
        is ImageBitmap -> RemoteImageBitmap(v)
        is Double -> RemoteFloat(v.toFloat())
        is Long -> RemoteLong(v)
        is Number -> RemoteFloat(v.toFloat())
        null -> RemoteString("null")
        else -> throw IllegalArgumentException("Unsupported constant type: ${v.javaClass}")
    }
}

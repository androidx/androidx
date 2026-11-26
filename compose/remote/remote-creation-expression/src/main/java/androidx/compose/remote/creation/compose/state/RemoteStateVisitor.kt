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

/**
 * Traverses a [RemoteValue] expression DAG to inspect, collect, or rewrite nodes.
 *
 * Implementations process nodes bottom-up (post-order). Shared sub-expressions in the DAG are
 * visited once per traversal via the memoization map in [RemoteStateCacheKey.accept].
 *
 * @param R The return type produced by visiting each node.
 */
internal fun interface RemoteStateVisitor<R> {
    /**
     * Visits a [RemoteStateCacheKey] node in the AST DAG.
     *
     * @param key The cache key being visited.
     * @param state The associated [RemoteValue], or `null` if not yet instantiated.
     * @param visitedArgs The results from visiting each child argument in
     *   [RemoteStateCacheKey.args] (empty for leaf nodes).
     * @return The result of visiting this node.
     */
    fun visit(key: RemoteStateCacheKey, state: RemoteValue<*>?, visitedArgs: List<R>): R
}

/**
 * Transforms this [RemoteValue] expression DAG bottom-up.
 *
 * Wrappers ([RemoteDp]) are automatically unwrapped and re-wrapped. If [transform] returns `null`
 * for a node, default transformation behavior is applied (preserving unchanged nodes,
 * reconstructing operations with transformed arguments, resolving constants, etc.).
 *
 * @param transform A lambda that inspects each [RemoteValue] node in the AST DAG and returns a
 *   replacement [RemoteValue], or `null` to preserve the node or use default reconstruction.
 * @return The transformed [RemoteValue] instance.
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <S : RemoteValue<*>> S.transform(
    transform: (state: RemoteValue<*>) -> RemoteValue<*>?
): S {
    if (hasConstantValue) {
        val replaced = transform(this)
        return (replaced ?: this) as S
    }

    return (when (this) {
        is RemoteDp -> {
            val newValue = value.transform(transform)
            if (newValue === value) this else RemoteDp(newValue)
        }
        else -> {
            accept<RemoteValue<*>> { key, state, visitedArgs ->
                val node = state ?: key.getOrCreateValueOrNull { null }
                val custom =
                    if (node != null) {
                        transform(node)
                            ?: if (key is RemoteNamedCacheKey && node is RemoteEnum<*>) {
                                (transform(node.intValue) as? RemoteInt)?.let {
                                    RemoteEnum(it, node.enumEntries)
                                }
                            } else {
                                null
                            }
                    } else {
                        null
                    }
                if (custom != null) {
                    unwrapWrapper(custom)
                } else if (key is RemoteOperationCacheKey) {
                    if (node != null && !hasArgsChanged(key.args, visitedArgs)) {
                        node
                    } else if (key.op == EnumOperationKey.RemoteEnum && node is RemoteEnum<*>) {
                        RemoteEnum(visitedArgs[0] as RemoteInt, node.enumEntries)
                    } else {
                        unwrapWrapper((key.op as RemoteOperation).reconstruct(visitedArgs))
                    }
                } else {
                    node
                        ?: when (key) {
                            is RemoteNamedCacheKey ->
                                throw IllegalStateException("Named cache key missing state: $key")
                            is BaseRemoteStateCacheKey ->
                                throw IllegalStateException("Cache key missing state: $key")
                            else -> throw UnsupportedOperationException("Unknown cache key: $key")
                        }
                }
            }
        }
    })
        as S
}

private fun unwrapWrapper(value: RemoteValue<*>): RemoteValue<*> =
    when (value) {
        is RemoteDp -> value.value
        else -> value
    }

private fun hasArgsChanged(
    original: List<RemoteStateCacheKey>,
    visited: List<RemoteValue<*>>,
): Boolean {
    for (i in 0 until original.size) {
        if (visited[i].cacheKey != original[i]) return true
    }
    return false
}

/** Resolves or creates a [RemoteValue] corresponding to the given [RemoteConstantCacheKey]. */
internal fun resolveConstantState(key: RemoteConstantCacheKey): RemoteValue<*> {
    return when (val v = key.value) {
        is Float -> RemoteFloat(v)
        is Int -> RemoteInt(v)
        is Short -> RemoteInt(v.toInt())
        is Byte -> RemoteInt(v.toInt())
        is Boolean -> RemoteBoolean(v)
        is String -> RemoteString(v)
        is Double -> RemoteFloat(v.toFloat())
        is Long -> RemoteLong(v)
        is Number -> RemoteFloat(v.toFloat())
        null -> RemoteString("null")
        else -> throw IllegalArgumentException("Unsupported constant type: ${v.javaClass}")
    }
}

internal fun <R> RemoteValue<*>.accept(visitor: RemoteStateVisitor<R>): R =
    (this as BaseRemoteValue<*>).accept(visitor)

internal fun RemoteValue<*>.writeToDocument(creationState: RemoteComposeCreationState): Int =
    (this as BaseRemoteValue<*>).writeToDocument(creationState)

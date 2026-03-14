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

import androidx.compose.runtime.Immutable

/**
 * Represents a key used for caching [BaseRemoteState] instances or expressions in
 * [androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState].
 *
 * Caching based on these keys prevents redundant operations and document writes when the same
 * expression or state is encountered multiple times during document creation.
 */
@Immutable internal interface RemoteStateCacheKey

/** A fallback cache key based on the identity of this key instance. */
internal class RemoteStateInstanceKey : RemoteStateCacheKey

/** A cache key for constant values (primitive types, strings, etc.). */
internal data class RemoteConstantCacheKey(private val value: Any?) : RemoteStateCacheKey {
    init {
        if (value is Float) {
            check(!value.isNaN()) { "Float constant value cannot be NaN" }
        }
        if (value is Double) {
            check(!value.isNaN()) { "Double constant value cannot be NaN" }
        }
    }
}

/** A cache key for named variables, identified by their [name] and [domain]. */
internal data class RemoteNamedCacheKey(
    private val domain: RemoteState.Domain,
    private val name: String,
) : RemoteStateCacheKey

/** A cache key for variable by id. */
internal data class RemoteStateIdKey(private val id: Int) : RemoteStateCacheKey

internal class FloatArrayCacheKey(private val floatArray: FloatArray) : RemoteStateCacheKey {
    override fun equals(other: Any?): Boolean {
        return other is FloatArrayCacheKey && floatArray.contentEquals(other.floatArray)
    }

    override fun hashCode(): Int {
        return floatArray.contentHashCode()
    }
}

/**
 * A cache key for component-specific values (like width/height/center), identified by the
 * [componentId] and the [type] of value.
 */
internal data class RemoteComponentCacheKey(
    private val componentId: Int,
    private val type: String,
) : RemoteStateCacheKey

/**
 * A cache key for operations performed on other remote states, identified by the operation [op]
 * (usually an [Enum]) and its [args].
 */
internal data class RemoteOperationCacheKey(
    private val op: Enum<*>,
    private val args: List<RemoteStateCacheKey>,
) : RemoteStateCacheKey {
    companion object {
        /** Creates a [RemoteOperationCacheKey] by converting [args] to a list of cache keys. */
        public fun create(op: Enum<*>, vararg args: Any): RemoteOperationCacheKey =
            RemoteOperationCacheKey(op, toCacheKeyList(args))
    }
}

/**
 * Converts a list of arguments into a list of [RemoteStateCacheKey]s. Any argument that is not
 * already a [RemoteStateCacheKey] is wrapped in a [RemoteConstantCacheKey].
 */
internal fun toCacheKeyList(args: Array<out Any>): List<RemoteStateCacheKey> {
    return args.map {
        when (it) {
            is RemoteState<*> -> it.cacheKey
            is RemoteStateCacheKey -> it
            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double,
            is Boolean,
            is Char,
            is String,
            is Enum<*> -> RemoteConstantCacheKey(it)
            else ->
                throw IllegalArgumentException(
                    "Unsupported cache key type: ${it.javaClass}. " +
                        "Only primitives, Strings, Enums and RemoteStates are supported."
                )
        }
    }
}

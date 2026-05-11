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

/** Base class for [RemoteStateCacheKey] implementations that provides a memoized [hashCode]. */
internal abstract class BaseRemoteStateCacheKey : RemoteStateCacheKey {
    private var _hashCode: Int = 0

    final override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = hashCodeImpl()
            if (_hashCode == 0) _hashCode = 1
        }
        return _hashCode
    }

    protected abstract fun hashCodeImpl(): Int

    abstract override fun equals(other: Any?): Boolean
}

/** A fallback cache key based on the identity of this key instance. */
internal class RemoteStateInstanceKey : BaseRemoteStateCacheKey() {
    override fun hashCodeImpl(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = this === other
}

/** A cache key for constant values (primitive types, strings, etc.). */
internal class RemoteConstantCacheKey(private val value: Any?) : BaseRemoteStateCacheKey() {
    init {
        if (value is Float) {
            check(!value.isNaN()) { "Float constant value cannot be NaN" }
        }
        if (value is Double) {
            check(!value.isNaN()) { "Double constant value cannot be NaN" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteConstantCacheKey) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCodeImpl(): Int = value?.hashCode() ?: 0

    override fun toString(): String = "RemoteConstantCacheKey(value=$value)"
}

/** A cache key for named variables, identified by their [name] and [domain]. */
internal class RemoteNamedCacheKey(
    private val domain: RemoteState.Domain,
    private val name: String,
) : BaseRemoteStateCacheKey() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteNamedCacheKey) return false
        if (domain != other.domain) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCodeImpl(): Int {
        var result = domain.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "RemoteNamedCacheKey(domain=$domain, name=$name)"
}

/** A cache key for variable by id. */
internal class RemoteStateIdKey(private val id: Int) : BaseRemoteStateCacheKey() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteStateIdKey) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCodeImpl(): Int = id

    override fun toString(): String = "RemoteStateIdKey(id=$id)"
}

internal class FloatArrayCacheKey(private val floatArray: FloatArray) : BaseRemoteStateCacheKey() {
    override fun equals(other: Any?): Boolean {
        return other is FloatArrayCacheKey && floatArray.contentEquals(other.floatArray)
    }

    override fun hashCodeImpl(): Int = floatArray.contentHashCode()
}

/**
 * A cache key for component-specific values (like width/height/center), identified by the
 * [componentId] and the [type] of value.
 */
internal class RemoteComponentCacheKey(private val componentId: Int, private val type: String) :
    BaseRemoteStateCacheKey() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteComponentCacheKey) return false
        if (componentId != other.componentId) return false
        if (type != other.type) return false
        return true
    }

    override fun hashCodeImpl(): Int {
        var result = componentId
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String =
        "RemoteComponentCacheKey(componentId=$componentId, type=$type)"
}

/**
 * A cache key for operations performed on other remote states, identified by the operation [op]
 * (usually an [Enum]) and its [args].
 */
internal class RemoteOperationCacheKey(
    internal val op: Enum<*>,
    private val args: List<RemoteStateCacheKey>,
) : BaseRemoteStateCacheKey() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteOperationCacheKey) return false
        if (op != other.op) return false
        if (args != other.args) return false
        return true
    }

    override fun hashCodeImpl(): Int {
        var result = op.hashCode()
        result = 31 * result + args.hashCode()
        return result
    }

    override fun toString(): String = "RemoteOperationCacheKey(op=$op, args=$args)"

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
            is RemoteConstantCacheKey -> it
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

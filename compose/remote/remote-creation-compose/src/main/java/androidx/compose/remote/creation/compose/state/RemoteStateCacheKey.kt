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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.runtime.Immutable

/**
 * Represents a key used for caching [BaseRemoteState] instances or expressions in
 * [androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState].
 *
 * Caching based on these keys prevents redundant operations and document writes when the same
 * expression or state is encountered multiple times during document creation.
 */
@Immutable
internal interface RemoteStateCacheKey {
    fun toDebugString(): String
}

internal fun List<RemoteStateCacheKey>.joinToDebugString(
    separator: String = ", ",
    startIndex: Int = 0,
): String = buildString {
    for (i in startIndex until this@joinToDebugString.size) {
        if (i > startIndex) {
            append(separator)
        }
        append(this@joinToDebugString[i].toDebugString())
    }
}

internal fun List<RemoteStateCacheKey>.formatOp(opSymbol: String, precedence: Int) =
    this[0].toOperandString(precedence) +
        " $opSymbol " +
        this[1].toOperandString(precedence, isRightOperand = true)

internal fun List<RemoteStateCacheKey>.formatArrayAccess(precedence: Int) =
    this[0].toOperandString(precedence) + "[" + this[1].toDebugString() + "]"

internal fun List<RemoteStateCacheKey>.formatSelect(opSymbol: String): String {
    val cond =
        "${this[0].toOperandString(1)} $opSymbol " +
            this[1].toOperandString(1, isRightOperand = true)
    val trueBranch = this[2].toOperandString(0, isRightOperand = true)
    val falseBranch = this[3].toOperandString(0, isRightOperand = true)
    return "$cond ? $trueBranch : $falseBranch"
}

internal fun List<String>.fastJoinToString(separator: String = ", "): String = buildString {
    for (i in this@fastJoinToString.indices) {
        if (i > 0) {
            append(separator)
        }
        append(this@fastJoinToString[i])
    }
}

internal fun Enum<*>.formatCamelCaseFunction(args: List<RemoteStateCacheKey>): String =
    "${name.replaceFirstChar { it.lowercase() }}(${args.joinToDebugString()})"

/**
 * Interface implemented by remote operation keys (e.g. `RemoteFloat.OperationKey`) to define custom
 * AST debug formatting (e.g. `user:a + user:b`).
 */
internal interface DebuggableOperation {
    val precedence: Int
        get() = 100

    /** Formats this operation AST using its evaluated operand [args]. */
    fun toDebugString(args: List<RemoteStateCacheKey>): String
}

/**
 * Formats this cache key as an operand string within an outer operation.
 *
 * If this key represents a nested operation whose [DebuggableOperation.precedence] is lower than
 * [parentPrecedence] (or equal, if [isRightOperand] is true), the resulting debug string is wrapped
 * in parentheses to accurately preserve evaluation order and associativity.
 */
internal fun RemoteStateCacheKey.toOperandString(
    parentPrecedence: Int,
    isRightOperand: Boolean = false,
): String {
    val str = toDebugString()
    if (this is RemoteOperationCacheKey) {
        val childOp = op as? DebuggableOperation
        if (childOp != null) {
            val needsParentheses =
                childOp.precedence < parentPrecedence ||
                    (childOp.precedence == parentPrecedence && isRightOperand) ||
                    (childOp.precedence == 0 && parentPrecedence == 0)
            if (needsParentheses) {
                return "($str)"
            }
        }
    }
    return str
}

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

    override fun toDebugString(): String = "instance"
}

internal class RemoteStateArrayKey(val size: Int) : BaseRemoteStateCacheKey() {
    override fun hashCodeImpl(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = this === other

    override fun toDebugString(): String = "mutableFloatArray(size=$size)"
}

/** A cache key for constant values (primitive types, strings, etc.). */
internal class RemoteConstantCacheKey(internal val value: Any?) : BaseRemoteStateCacheKey() {
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

    override fun toDebugString(): String =
        when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Enum<*> -> "${value.javaClass.simpleName}.${value.name}"
            else -> value.toString()
        }
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

    override fun toDebugString(): String = "${domain.prefix.lowercase()}$name"
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

    override fun toDebugString(): String {
        return when (id) {
            RemoteContext.ID_CONTINUOUS_SEC -> "context:continuous_sec"
            RemoteContext.ID_TIME_IN_SEC -> "context:time_in_sec"
            RemoteContext.ID_TIME_IN_MIN -> "context:time_in_min"
            RemoteContext.ID_TIME_IN_HR -> "context:time_in_hr"
            RemoteContext.ID_WINDOW_WIDTH -> "context:window_width"
            RemoteContext.ID_WINDOW_HEIGHT -> "context:window_height"
            RemoteContext.ID_COMPONENT_WIDTH -> "context:component_width"
            RemoteContext.ID_COMPONENT_HEIGHT -> "context:component_height"
            RemoteContext.ID_CALENDAR_MONTH -> "context:calendar_month"
            RemoteContext.ID_OFFSET_TO_UTC -> "context:offset_to_utc"
            RemoteContext.ID_WEEK_DAY -> "context:week_day"
            RemoteContext.ID_DAY_OF_MONTH -> "context:day_of_month"
            RemoteContext.ID_DAY_OF_YEAR -> "context:day_of_year"
            RemoteContext.ID_YEAR -> "context:year"
            RemoteContext.ID_TOUCH_POS_X -> "context:touch_pos_x"
            RemoteContext.ID_TOUCH_POS_Y -> "context:touch_pos_y"
            RemoteContext.ID_TOUCH_VEL_X -> "context:touch_vel_x"
            RemoteContext.ID_TOUCH_VEL_Y -> "context:touch_vel_y"
            RemoteContext.ID_ACCELERATION_X -> "context:acceleration_x"
            RemoteContext.ID_ACCELERATION_Y -> "context:acceleration_y"
            RemoteContext.ID_ACCELERATION_Z -> "context:acceleration_z"
            RemoteContext.ID_GYRO_ROT_X -> "context:gyro_rot_x"
            RemoteContext.ID_GYRO_ROT_Y -> "context:gyro_rot_y"
            RemoteContext.ID_GYRO_ROT_Z -> "context:gyro_rot_z"
            RemoteContext.ID_MAGNETIC_X -> "context:magnetic_x"
            RemoteContext.ID_MAGNETIC_Y -> "context:magnetic_y"
            RemoteContext.ID_MAGNETIC_Z -> "context:magnetic_z"
            RemoteContext.ID_LIGHT -> "context:light"
            RemoteContext.ID_DENSITY -> "context:density"
            RemoteContext.ID_API_LEVEL -> "context:api_level"
            RemoteContext.ID_TOUCH_EVENT_TIME -> "context:touch_event_time"
            RemoteContext.ID_ANIMATION_TIME -> "context:animation_time"
            RemoteContext.ID_ANIMATION_DELTA_TIME -> "context:animation_delta_time"
            RemoteContext.ID_EPOCH_SECOND -> "context:epoch_second"
            RemoteContext.ID_FONT_SIZE -> "context:font_size"
            else -> "context:#$id"
        }
    }
}

internal class FloatArrayCacheKey(private val floatArray: FloatArray) : BaseRemoteStateCacheKey() {
    override fun equals(other: Any?): Boolean {
        return other is FloatArrayCacheKey && floatArray.contentEquals(other.floatArray)
    }

    override fun hashCodeImpl(): Int = floatArray.contentHashCode()

    override fun toDebugString(): String = floatArray.contentToString()
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

    override fun toDebugString(): String = "component:$componentId.$type"
}

/**
 * A cache key for operations performed on other remote states, identified by the operation [op]
 * (usually an [Enum]) and its [args].
 */
internal class RemoteOperationCacheKey(
    internal val op: Enum<*>,
    internal val args: List<RemoteStateCacheKey>,
) : BaseRemoteStateCacheKey() {

    /** Parent RemoteState, used for common sub expression elimination. */
    internal var state: RemoteState<*>? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("state can only be set once.")
            }
            field = value
        }

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

    override fun toDebugString(): String {
        return if (op is DebuggableOperation) {
            (op as DebuggableOperation).toDebugString(args)
        } else {
            "Operation($op, args=[${args.joinToDebugString()}])"
        }
    }

    companion object {
        /** Creates a [RemoteOperationCacheKey] by converting [args] to a list of cache keys. */
        public fun create(op: Enum<*>, vararg args: Any?): RemoteOperationCacheKey =
            RemoteOperationCacheKey(op, toCacheKeyList(args))
    }
}

/**
 * Converts a list of arguments into a list of [RemoteStateCacheKey]s. Any argument that is not
 * already a [RemoteStateCacheKey] is wrapped in a [RemoteConstantCacheKey].
 */
internal fun toCacheKeyList(args: Array<out Any?>): List<RemoteStateCacheKey> {
    return args.map {
        when (it) {
            null -> RemoteConstantCacheKey(null)
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

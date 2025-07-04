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
@file:JvmName("DynamicDataMapUtil")

package androidx.wear.protolayout.expression

import android.graphics.Color
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import java.time.Duration
import java.time.Instant

/**
 * A heterogeneous map of [DynamicType] keys to their corresponding values. This (and
 * [MutableDynamicDataMap]) act like [Map] (and [MutableMap]), but with type safety for key and
 * value pairs.
 */
public open class DynamicDataMap
internal constructor(internal open val map: Map<DynamicDataKey<*>, DynamicDataValue<*>>) {

    internal constructor(
        vararg pairs: DynamicDataPair<*>
    ) : this(mapOf(*pairs.map { it.asPair() }.toTypedArray()))

    /** Returns the number of key/value pairs in the map. */
    public val size: Int
        get() = map.size

    /** Returns a read-only [Set] of all keys in this map. */
    public val keys: Set<DynamicDataKey<out DynamicType>>
        get() = map.toMap().keys

    /**
     * Returns a read-only [Collection] of all values in this map. Note that this collection may
     * contain duplicate values.
     */
    public val values: Collection<DynamicDataValue<out DynamicType>>
        get() = map.toMap().values

    /** Returns a read-only [Set] of all key/value pairs in this map. */
    public val entries:
        Set<Map.Entry<DynamicDataKey<out DynamicType>, DynamicDataValue<out DynamicType>>>
        get() = map.toMap().entries

    /**
     * Returns true if this map contains the specified [key] and it is associated with the specified
     * [type].
     *
     * @param key the key to check for.
     * @param type the type to match the value's type against. Dynamic and raw types can be used
     *   interchangeably. For example [DynamicInt32] and [Int] both return true if the associated
     *   value is of [DynamicInt32] type.
     */
    public fun <T : DynamicType> contains(key: DynamicDataKey<T>, type: Class<T>): Boolean =
        map[key]?.hasValueOfType(type) == true

    /**
     * Returns true if this map contains the specified [key] and it is associated with the specified
     * type [T].
     *
     * @param key the key to check for.
     * @param T the type to match the value's type against. Dynamic and raw types can be used
     *   interchangeably. For example [DynamicInt32] and [Int] both return true if the associated
     *   value is of [DynamicInt32] type.
     */
    public inline operator fun <reified T : DynamicType> contains(key: DynamicDataKey<T>): Boolean =
        contains(key, T::class.java)

    /**
     * Gets the boolean value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalStateException if a non-boolean value is stored with the same name as [key].
     */
    @Suppress("AutoBoxing")
    public operator fun get(key: DynamicDataKey<DynamicBool>): Boolean? = map[key]?.boolValue

    /**
     * Gets the color value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalStateException if a non-color value is stored with the same name as [key].
     */
    public operator fun get(key: DynamicDataKey<DynamicColor>): Color? =
        map[key]?.colorValue?.let { Color.valueOf(it) }

    /**
     * Gets the [Duration] value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalStateException if a non-[Duration] value is stored with the same name as
     *   [key].
     */
    public operator fun get(key: DynamicDataKey<DynamicDuration>): Duration? =
        map[key]?.durationValue

    /**
     * Gets the float value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalArgumentException if the key is not stored in the map.
     */
    @Suppress("AutoBoxing")
    public operator fun get(key: DynamicDataKey<DynamicFloat>): Float? = map[key]?.floatValue

    /**
     * Gets the [Instant] value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalStateException if a non-[Instant] value is stored with the same name as [key].
     */
    public operator fun get(key: DynamicDataKey<DynamicInstant>): Instant? = map[key]?.instantValue

    /**
     * Gets the integer value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalStateException if a non-integer value is stored with the same name as [key].
     */
    @Suppress("AutoBoxing")
    public operator fun get(key: DynamicDataKey<DynamicInt32>): Int? = map[key]?.intValue

    /**
     * Gets the string value mapped to [key]. If there isn't any mapped value for [key], returns
     * null.
     *
     * @throws IllegalStateException if a non-string value is stored with the same name as [key].
     */
    public operator fun get(key: DynamicDataKey<DynamicString>): String? = map[key]?.stringValue

    public operator fun plus(rhs: DynamicDataMap): DynamicDataMap = DynamicDataMap(map + rhs.map)

    override fun equals(other: Any?): Boolean {
        if (other !is DynamicDataMap) {
            return false
        }
        return map == other.map
    }

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = map.toString()
}

/** Creates a [DynamicDataMap]. */
public fun dynamicDataMapOf(vararg pairs: DynamicDataPair<*>): DynamicDataMap =
    DynamicDataMap(*pairs)

/**
 * A mutable heterogeneous map of [DynamicType] keys to their corresponding values. This (and
 * [DynamicDataMap]) act like [MutableMap] (and [Map]), but with type safety for key and value
 * pairs.
 */
public class MutableDynamicDataMap(vararg pairs: DynamicDataPair<*>) : DynamicDataMap() {
    override val map =
        mutableMapOf<DynamicDataKey<*>, DynamicDataValue<*>>(
            *pairs.map { it.asPair() }.toTypedArray()
        )

    /**
     * Associates a boolean [value] with a [DynamicBool] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public operator fun set(key: DynamicDataKey<DynamicBool>, value: Boolean) {
        map[key] = DynamicDataValue.fromBool(value)
    }

    /**
     * Associates a color [value] with a [DynamicColor] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public operator fun set(key: DynamicDataKey<DynamicColor>, value: Color) {
        map[key] = DynamicDataValue.fromColor(value.toArgb())
    }

    /**
     * Associates a [Duration] [value] with a [DynamicDuration] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public operator fun set(key: DynamicDataKey<DynamicDuration>, value: Duration) {
        map[key] = DynamicDataValue.fromDuration(value)
    }

    /**
     * Associates a float [value] with a [DynamicFloat] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public operator fun set(key: DynamicDataKey<DynamicFloat>, value: Float) {
        map[key] = DynamicDataValue.fromFloat(value)
    }

    /**
     * Associates a [Instant] [value] with a [DynamicInstant] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public operator fun set(key: DynamicDataKey<DynamicInstant>, value: Instant) {
        map[key] = DynamicDataValue.fromInstant(value)
    }

    /**
     * Associates a integer [value] with a [DynamicInt32] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public operator fun set(key: DynamicDataKey<DynamicInt32>, value: Int) {
        map[key] = DynamicDataValue.fromInt(value)
    }

    /**
     * Associates a string [value] with a [DynamicString] [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public operator fun set(key: DynamicDataKey<DynamicString>, value: String) {
        map[key] = DynamicDataValue.fromString(value)
    }

    /**
     * Associates [value] with [key] in this map.
     *
     * @param key represent a key that other APIs can use to reference [value]s value.
     * @param value is the static value to use for resolving references to [key].
     */
    public operator fun <T : DynamicType> set(key: DynamicDataKey<T>, value: DynamicDataValue<T>) {
        map[key] = value
    }

    /** Adds all key/values from [other] to this map. Potentially overwriting any common key. */
    public fun putAll(other: DynamicDataMap) {
        map.putAll(other.map)
    }
}

/** Creates a [MutableDynamicDataMap]. */
public fun mutableDynamicDataMapOf(vararg pairs: DynamicDataPair<*>): MutableDynamicDataMap =
    MutableDynamicDataMap(*pairs)

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

package androidx.privacysandbox.databridge.core

import androidx.annotation.RestrictTo

/**
 * Key class for the values stored.
 *
 * Use the following factory methods to create instances of [Key]: [createIntKey],
 * [createDoubleKey], [createLongKey], [createFloatKey], [createBooleanKey], [createStringKey],
 * [createStringSetKey], and [createByteArrayKey].
 *
 * The type of the Key is used in [DataBridgeClient.getValue]
 *
 * Eg: val key: Key = Key.createIntKey("int_key").
 */
public class Key
internal constructor(
    public val name: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val type: Type,
) {

    public companion object {
        /**
         * Get the key for an integer value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createIntKey(name: String): Key = Key(name, Type.INT)

        /**
         * Get the key for a double value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createDoubleKey(name: String): Key = Key(name, Type.DOUBLE)

        /**
         * Get the key for a long value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createLongKey(name: String): Key = Key(name, Type.LONG)

        /**
         * Get the key for a float value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createFloatKey(name: String): Key = Key(name, Type.FLOAT)

        /**
         * Get the key for a boolean value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createBooleanKey(name: String): Key = Key(name, Type.BOOLEAN)

        /**
         * Get the key for a string value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createStringKey(name: String): Key = Key(name, Type.STRING)

        /**
         * Get the key for a set of string value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createStringSetKey(name: String): Key = Key(name, Type.STRING_SET)

        /**
         * Get the key for a byte array value.
         *
         * @param name: The name of the key
         * @return Key for [name]
         */
        @JvmStatic public fun createByteArrayKey(name: String): Key = Key(name, Type.BYTE_ARRAY)
    }

    override fun equals(other: Any?): Boolean =
        if (other is Key) {
            name == other.name
        } else {
            false
        }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "${Key::class.java.simpleName} : {name=$name}"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class Type {
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    STRING,
    STRING_SET,
    BYTE_ARRAY,
}

/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.serialization

import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import java.io.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind

internal actual fun SerialDescriptor.parseEnum(): NavType<*> =
    NavType.parseSerializableOrParcelableType(getClass(), false) ?: UNKNOWN

internal actual fun SerialDescriptor.parseNullableEnum(): NavType<*> {
    val clazz = getClass()
    return if (Enum::class.java.isAssignableFrom(clazz)) {
        @Suppress("UNCHECKED_CAST")
        InternalAndroidNavType.EnumNullableType(clazz as Class<Enum<*>?>)
    } else UNKNOWN
}

@OptIn(ExperimentalSerializationApi::class)
internal actual fun SerialDescriptor.parseEnumList(): NavType<*> {
    @Suppress("UNCHECKED_CAST")
    return InternalAndroidNavType.EnumListType(getElementDescriptor(0).getClass() as Class<Enum<*>>)
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getClass(): Class<*> {
    var className = serialName.replace("?", "")
    // first try to get class with original class name
    try {
        return Class.forName(className)
    } catch (_: ClassNotFoundException) {}
    // Otherwise, it might be nested Class. Try incrementally replacing last `.` with `$`
    // until we find the correct enum class name.
    while (className.contains(".")) {
        className = Regex("(\\.+)(?!.*\\.)").replace(className, "\\$")
        try {
            return Class.forName(className)
        } catch (_: ClassNotFoundException) {}
    }
    var errorMsg =
        "Cannot find class with name \"$serialName\". Ensure that the " +
            "serialName for this argument is the default fully qualified name"
    if (kind is SerialKind.ENUM) {
        errorMsg =
            "$errorMsg.\nIf the build is minified, try annotating the Enum class with \"androidx.annotation.Keep\" to ensure the Enum is not removed."
    }
    throw IllegalArgumentException(errorMsg)
}

internal object InternalAndroidNavType {
    class EnumNullableType<D : Enum<*>?>(type: Class<D?>) : SerializableNullableType<D?>(type) {
        private val type: Class<D?>

        /** Constructs a NavType that supports a given Enum type. */
        init {
            require(type.isEnum) { "$type is not an Enum type." }
            this.type = type
        }

        override val name: String
            get() = type.name

        override fun parseValue(value: String): D? {
            return if (value == "null") {
                null
            } else {
                type.enumConstants!!.firstOrNull { constant ->
                    constant!!.name.equals(value, ignoreCase = true)
                }
                    ?: throw IllegalArgumentException(
                        "Enum value $value not found for type ${type.name}."
                    )
            }
        }
    }

    // Base Serializable class to support nullable EnumNullableType
    open class SerializableNullableType<D : Serializable?>(private val type: Class<D?>) :
        NavType<D?>(true) {

        override val name: String
            get() = type.name

        init {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
        }

        override fun put(bundle: SavedState, key: String, value: D?) {
            bundle.putSerializable(key, type.cast(value))
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        override fun get(bundle: SavedState, key: String): D? {
            return bundle[key] as? D
        }

        /**
         * @throws UnsupportedOperationException since Serializables do not support default values
         */
        public override fun parseValue(value: String): D? {
            throw UnsupportedOperationException("Serializables don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SerializableNullableType<*>) return false
            return type == other.type
        }

        public override fun hashCode(): Int {
            return type.hashCode()
        }
    }

    class EnumListType<D : Enum<*>>(type: Class<D>) : CollectionNavType<List<D>?>(true) {
        private val enumNavType = NavType.EnumType(type)

        override val name: String
            get() = "List<${enumNavType.name}}>"

        override fun put(bundle: SavedState, key: String, value: List<D>?) {
            bundle.putSerializable(key, value?.let { ArrayList(value) })
        }

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        override fun get(bundle: SavedState, key: String): List<D>? = (bundle[key] as? List<D>?)

        override fun parseValue(value: String): List<D> = listOf(enumNavType.parseValue(value))

        override fun parseValue(value: String, previousValue: List<D>?): List<D>? =
            previousValue?.plus(parseValue(value)) ?: parseValue(value)

        override fun valueEquals(value: List<D>?, other: List<D>?): Boolean {
            val valueArrayList = value?.let { ArrayList(value) }
            val otherArrayList = other?.let { ArrayList(other) }
            return valueArrayList == otherArrayList
        }

        override fun serializeAsValues(value: List<D>?): List<String> =
            value?.map { it.toString() } ?: emptyList()

        override fun emptyCollection(): List<D> = emptyList()

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EnumListType<*>) return false
            return enumNavType == other.enumNavType
        }

        public override fun hashCode(): Int {
            return enumNavType.hashCode()
        }
    }
}

/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.navigation

import android.os.Parcelable
import androidx.annotation.AnyRes
import androidx.annotation.RestrictTo
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import java.io.Serializable

public actual abstract class NavType<T>
actual constructor(public actual open val isNullableAllowed: Boolean) {

    public actual abstract fun put(bundle: SavedState, key: String, value: T)

    public actual abstract operator fun get(bundle: SavedState, key: String): T?

    public actual abstract fun parseValue(value: String): T

    public actual open fun parseValue(value: String, previousValue: T): T = parseValue(value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(bundle: SavedState, key: String, value: String): T =
        navTypeParseAndPut(bundle, key, value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(
        bundle: SavedState,
        key: String,
        value: String?,
        previousValue: T,
    ): T = navTypeParseAndPut(bundle, key, value, previousValue)

    public actual open fun serializeAsValue(value: T): String = value.toString()

    public actual open val name: String = "nav_type"

    public actual open fun valueEquals(value: T, other: T): Boolean = value == other

    override fun toString(): String = name

    public actual companion object {
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT", "UNCHECKED_CAST") // this needs to be open to
        // maintain api compatibility and type cast are unchecked
        @JvmStatic
        public actual open fun fromArgType(type: String?, packageName: String?): NavType<*> {
            return navTypeFromArgType(type)
                ?: when {
                    ReferenceType.name == type -> return ReferenceType
                    !type.isNullOrEmpty() -> {
                        try {
                            var className: String
                            className =
                                if (type.startsWith(".") && packageName != null) {
                                    packageName + type
                                } else {
                                    type
                                }
                            val isArray = type.endsWith("[]")
                            if (isArray) className = className.substring(0, className.length - 2)
                            val clazz = Class.forName(className)
                            return requireNotNull(
                                parseSerializableOrParcelableType(clazz, isArray)
                            ) {
                                "$className is not Serializable or Parcelable."
                            }
                        } catch (e: ClassNotFoundException) {
                            throw RuntimeException(e)
                        }
                    }
                    else -> StringType
                }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun parseSerializableOrParcelableType(
            clazz: Class<*>,
            isArray: Boolean,
        ): NavType<*>? =
            when {
                Parcelable::class.java.isAssignableFrom(clazz) -> {
                    if (isArray) {
                        ParcelableArrayType(clazz as Class<Parcelable>)
                    } else {
                        ParcelableType(clazz as Class<Any?>)
                    }
                }
                Enum::class.java.isAssignableFrom(clazz) && !isArray ->
                    EnumType(clazz as Class<Enum<*>>)
                Serializable::class.java.isAssignableFrom(clazz) -> {
                    if (isArray) {
                        SerializableArrayType(clazz as Class<Serializable>)
                    } else {
                        SerializableType(clazz as Class<Serializable>)
                    }
                }
                else -> null
            }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun inferFromValue(value: String): NavType<Any> = navTypeInferFromValue(value)

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun inferFromValueType(value: Any?): NavType<Any> {
            return navTypeInferFromValueType(value)
                ?: when {
                    value is Array<*> && value.isArrayOf<String>() ->
                        StringArrayType as NavType<Any>
                    value!!.javaClass.isArray &&
                        Parcelable::class
                            .java
                            .isAssignableFrom(value.javaClass.componentType!!) -> {
                        ParcelableArrayType(value.javaClass.componentType as Class<Parcelable>)
                            as NavType<Any>
                    }
                    value.javaClass.isArray &&
                        Serializable::class
                            .java
                            .isAssignableFrom(value.javaClass.componentType!!) -> {
                        SerializableArrayType(value.javaClass.componentType as Class<Serializable>)
                            as NavType<Any>
                    }
                    value is Parcelable -> ParcelableType(value.javaClass) as NavType<Any>
                    value is Enum<*> -> EnumType(value.javaClass) as NavType<Any>
                    value is Serializable -> SerializableType(value.javaClass) as NavType<Any>
                    else -> {
                        throw IllegalArgumentException(
                            "Object of type ${value.javaClass.name} is not supported for navigation " +
                                "arguments."
                        )
                    }
                }
        }

        @JvmField public actual val IntType: NavType<Int> = IntNavType()

        /**
         * NavType for storing integer values representing resource ids, corresponding with the
         * "reference" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val ReferenceType: NavType<Int> =
            object : NavType<Int>(false) {
                override val name: String
                    get() = "reference"

                override fun put(bundle: SavedState, key: String, @AnyRes value: Int) {
                    bundle.write { putInt(key, value) }
                }

                @AnyRes
                @Suppress("DEPRECATION")
                override fun get(bundle: SavedState, key: String): Int = bundle.read { getInt(key) }

                override fun parseValue(value: String): Int {
                    return if (value.startsWith("0x")) {
                        value.substring(2).toInt(16)
                    } else {
                        value.toInt()
                    }
                }
            }

        @JvmField public actual val IntArrayType: NavType<IntArray?> = IntArrayNavType()
        @JvmField public actual val IntListType: NavType<List<Int>?> = IntListNavType()
        @JvmField public actual val LongType: NavType<Long> = LongNavType()
        @JvmField public actual val LongArrayType: NavType<LongArray?> = LongArrayNavType()
        @JvmField public actual val LongListType: NavType<List<Long>?> = LongListNavType()
        @JvmField public actual val FloatType: NavType<Float> = FloatNavType()
        @JvmField public actual val FloatArrayType: NavType<FloatArray?> = FloatArrayNavType()
        @JvmField public actual val FloatListType: NavType<List<Float>?> = FloatListNavType()
        @JvmField public actual val BoolType: NavType<Boolean> = BoolNavType()
        @JvmField public actual val BoolArrayType: NavType<BooleanArray?> = BoolArrayNavType()
        @JvmField public actual val BoolListType: NavType<List<Boolean>?> = BoolListNavType()
        @JvmField public actual val StringType: NavType<String?> = StringNavType()
        @JvmField public actual val StringArrayType: NavType<Array<String>?> = StringArrayNavType()
        @JvmField public actual val StringListType: NavType<List<String>?> = StringListNavType()
    }

    /**
     * ParcelableType is used for passing Parcelables in [NavArgument]s.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @param type the Parcelable class that is supported by this NavType
     */
    public class ParcelableType<D>(type: Class<D>) : NavType<D>(true) {
        private val type: Class<D>

        public override val name: String
            get() = type.name

        public override fun put(bundle: SavedState, key: String, value: D) {
            type.cast(value)
            if (value == null || value is Parcelable) {
                bundle.putParcelable(key, value as Parcelable?)
            } else if (value is Serializable) {
                bundle.putSerializable(key, value as Serializable?)
            }
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: SavedState, key: String): D? {
            return bundle[key] as D?
        }

        /** @throws UnsupportedOperationException since Parcelables do not support default values */
        public override fun parseValue(value: String): D {
            throw UnsupportedOperationException("Parcelables don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ParcelableType<*>
            return type == that.type
        }

        public override fun hashCode(): Int {
            return type.hashCode()
        }

        /** Constructs a NavType that supports a given Parcelable type. */
        init {
            require(
                Parcelable::class.java.isAssignableFrom(type) ||
                    Serializable::class.java.isAssignableFrom(type)
            ) {
                "$type does not implement Parcelable or Serializable."
            }
            this.type = type
        }
    }

    /**
     * ParcelableArrayType is used for [NavArgument]s which hold arrays of Parcelables.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @param type the type of Parcelable component class of the array
     */
    public class ParcelableArrayType<D : Parcelable>(type: Class<D>) : NavType<Array<D>?>(true) {
        private val arrayType: Class<Array<D>>

        public override val name: String
            get() = arrayType.name

        public override fun put(bundle: SavedState, key: String, value: Array<D>?) {
            arrayType.cast(value)
            bundle.putParcelableArray(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: SavedState, key: String): Array<D>? {
            return bundle[key] as Array<D>?
        }

        /** @throws UnsupportedOperationException since Arrays do not support default values */
        public override fun parseValue(value: String): Array<D> {
            throw UnsupportedOperationException("Arrays don't support default values.")
        }

        @Suppress("UNCHECKED_CAST")
        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ParcelableArrayType<Parcelable>
            return arrayType == that.arrayType
        }

        public override fun hashCode(): Int {
            return arrayType.hashCode()
        }

        override fun valueEquals(
            @Suppress("ArrayReturn") value: Array<D>?,
            @Suppress("ArrayReturn") other: Array<D>?,
        ): Boolean = value.contentDeepEquals(other)

        /** Constructs a NavType that supports arrays of a given Parcelable type. */
        init {
            require(Parcelable::class.java.isAssignableFrom(type)) {
                "$type does not implement Parcelable."
            }
            val arrayType: Class<Array<D>> =
                try {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName("[L${type.name};") as Class<Array<D>>
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e) // should never happen
                }
            this.arrayType = arrayType
        }
    }

    /**
     * SerializableType is used for Serializable [NavArgument]s. For handling Enums you must use
     * [EnumType] instead.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @see EnumType
     */
    public open class SerializableType<D : Serializable> : NavType<D> {
        private val type: Class<D>

        public override val name: String
            get() = type.name

        /**
         * Constructs a NavType that supports a given Serializable type.
         *
         * @param type class that is a subtype of Serializable
         */
        public constructor(type: Class<D>) : super(true) {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
            require(!type.isEnum) { "$type is an Enum. You should use EnumType instead." }
            this.type = type
        }

        internal constructor(nullableAllowed: Boolean, type: Class<D>) : super(nullableAllowed) {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
            this.type = type
        }

        public override fun put(bundle: SavedState, key: String, value: D) {
            type.cast(value)
            bundle.putSerializable(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: SavedState, key: String): D? {
            return bundle[key] as D?
        }

        /**
         * @throws UnsupportedOperationException since Serializables do not support default values
         */
        public override fun parseValue(value: String): D {
            throw UnsupportedOperationException("Serializables don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SerializableType<*>) return false
            return type == other.type
        }

        public override fun hashCode(): Int {
            return type.hashCode()
        }
    }

    /**
     * EnumType is used for [NavArgument]s holding enum values.
     *
     * Null values are not supported. To specify a default value in a Navigation XML file, simply
     * use the enum constant without the class name, e.g. `app:defaultValue="MONDAY"`.
     *
     * @param type the Enum class that is supported by this NavType
     */
    public class EnumType<D : Enum<*>>(type: Class<D>) : SerializableType<D>(false, type) {
        private val type: Class<D>

        public override val name: String
            get() = type.name

        /**
         * Parse a value of this type from a String.
         *
         * @param value string representation of a value of this type
         * @return parsed value of the type represented by this NavType
         * @throws IllegalArgumentException if value cannot be parsed into this type
         */
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        public override fun parseValue(value: String): D {
            return type.enumConstants.firstOrNull { constant ->
                constant.name.equals(value, ignoreCase = true)
            }
                ?: throw IllegalArgumentException(
                    "Enum value $value not found for type ${type.name}."
                )
        }

        /** Constructs a NavType that supports a given Enum type. */
        init {
            require(type.isEnum) { "$type is not an Enum type." }
            this.type = type
        }
    }

    /**
     * SerializableArrayType is used for [NavArgument]s that hold arrays of Serializables. This type
     * also supports arrays of Enums.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @param type the Serializable component class of the array
     */
    public class SerializableArrayType<D : Serializable>(type: Class<D>) :
        NavType<Array<D>?>(true) {
        private val arrayType: Class<Array<D>>

        public override val name: String
            get() = arrayType.name

        public override fun put(bundle: SavedState, key: String, value: Array<D>?) {
            arrayType.cast(value)
            bundle.putSerializable(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: SavedState, key: String): Array<D>? {
            return bundle[key] as Array<D>?
        }

        /** @throws UnsupportedOperationException since Arrays do not support default values */
        public override fun parseValue(value: String): Array<D> {
            throw UnsupportedOperationException("Arrays don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as SerializableArrayType<*>
            return arrayType == that.arrayType
        }

        public override fun hashCode(): Int {
            return arrayType.hashCode()
        }

        override fun valueEquals(
            @Suppress("ArrayReturn") value: Array<D>?,
            @Suppress("ArrayReturn") other: Array<D>?,
        ): Boolean = value.contentDeepEquals(other)

        /** Constructs a NavType that supports arrays of a given Serializable type. */
        init {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
            val arrayType: Class<Array<D>> =
                try {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName("[L${type.name};") as Class<Array<D>>
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e) // should never happen
                }
            this.arrayType = arrayType
        }
    }
}

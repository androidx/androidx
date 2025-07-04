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

package androidx.appfunctions.internal

import android.app.PendingIntent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionData

/**
 * An interface for factory classes that convert between a class annotated with
 * [androidx.appfunctions.AppFunctionSerializable] and [androidx.appfunctions.AppFunctionData].
 *
 * Each class annotated with [androidx.appfunctions.AppFunctionSerializable] will have a generated
 * class that implements this interface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppFunctionSerializableFactory<T : Any> {
    /**
     * Deserializes the given [androidx.appfunctions.AppFunctionData] into an instance of the
     * AppFunctionSerializable annotated class.
     *
     * Type mismatch: An [IllegalArgumentException] if a property is stored as a different type in
     * [appFunctionData].
     */
    public fun fromAppFunctionData(appFunctionData: AppFunctionData): T

    /** Serializes the given class into an [AppFunctionData]. */
    public fun toAppFunctionData(appFunctionSerializable: T): AppFunctionData

    /**
     * Contains the information about the type parameter.
     *
     * The class is used by [AppFunctionSerializableFactory] for generic serializable to resolve the
     * type information in runtime.
     */
    public sealed interface TypeParameter<T> {

        /** Sets the value in the [AppFunctionData] builder according to the TypeParameter. */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public fun setValueInAppFunctionData(
            appFunctionDataBuilder: AppFunctionData.Builder,
            key: String,
            value: T,
        )

        /** Gets the value from the [AppFunctionData] according to the TypeParameter. */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public fun getFromAppFunctionData(appFunctionData: AppFunctionData, key: String): T

        /** The [TypeParameter] for Kotlin primitive types. */
        public data class PrimitiveTypeParameter<T>(
            /** The type class. */
            val clazz: Class<T>
        ) : TypeParameter<T> {

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun setValueInAppFunctionData(
                appFunctionDataBuilder: AppFunctionData.Builder,
                key: String,
                value: T,
            ) {
                if (value == null) return

                when (clazz) {
                    Int::class.java -> appFunctionDataBuilder.setInt(key, value as Int)
                    Long::class.java -> appFunctionDataBuilder.setLong(key, value as Long)
                    Float::class.java -> appFunctionDataBuilder.setFloat(key, value as Float)
                    Double::class.java -> appFunctionDataBuilder.setDouble(key, value as Double)
                    Boolean::class.java -> appFunctionDataBuilder.setBoolean(key, value as Boolean)
                    String::class.java -> appFunctionDataBuilder.setString(key, value as String)
                    PendingIntent::class.java ->
                        appFunctionDataBuilder.setPendingIntent(key, value as PendingIntent)
                    IntArray::class.java ->
                        appFunctionDataBuilder.setIntArray(key, value as IntArray)
                    LongArray::class.java ->
                        appFunctionDataBuilder.setLongArray(key, value as LongArray)
                    FloatArray::class.java ->
                        appFunctionDataBuilder.setFloatArray(key, value as FloatArray)
                    DoubleArray::class.java ->
                        appFunctionDataBuilder.setDoubleArray(key, value as DoubleArray)
                    BooleanArray::class.java ->
                        appFunctionDataBuilder.setBooleanArray(key, value as BooleanArray)
                    ByteArray::class.java ->
                        appFunctionDataBuilder.setByteArray(key, value as ByteArray)
                    else -> throw IllegalStateException("Unsupported primitive type: $clazz.")
                }
            }

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            @Suppress("UNCHECKED_CAST")
            override fun getFromAppFunctionData(appFunctionData: AppFunctionData, key: String): T =
                when (clazz) {
                    Int::class.java -> appFunctionData.getInt(key)
                    Long::class.java -> appFunctionData.getLong(key)
                    Float::class.java -> appFunctionData.getFloat(key)
                    Double::class.java -> appFunctionData.getDouble(key)
                    Boolean::class.java -> appFunctionData.getBoolean(key)
                    String::class.java -> appFunctionData.getString(key)
                    PendingIntent::class.java -> appFunctionData.getPendingIntent(key)

                    IntArray::class.java -> appFunctionData.getIntArray(key)
                    LongArray::class.java -> appFunctionData.getLongArray(key)
                    FloatArray::class.java -> appFunctionData.getFloatArray(key)

                    DoubleArray::class.java -> appFunctionData.getDoubleArray(key)

                    BooleanArray::class.java -> appFunctionData.getBooleanArray(key)

                    ByteArray::class.java -> appFunctionData.getByteArray(key)
                    else -> throw IllegalStateException("Unsupported primitive type: $clazz.")
                }
                    as T
        }

        /** The [TypeParameter] for primitive [List] type. */
        public data class PrimitiveListTypeParameter<I, T : List<*>?>(
            /** The item type class. */
            val itemClazz: Class<I>
        ) : TypeParameter<T> {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun setValueInAppFunctionData(
                appFunctionDataBuilder: AppFunctionData.Builder,
                key: String,
                value: T,
            ) {
                if (value == null) return

                @Suppress("UNCHECKED_CAST")
                when (itemClazz) {
                    String::class.java ->
                        appFunctionDataBuilder.setStringList(key, value as List<String>)
                    PendingIntent::class.java ->
                        appFunctionDataBuilder.setPendingIntentList(
                            key,
                            value as List<PendingIntent>,
                        )
                    else ->
                        throw IllegalStateException(
                            "Unsupported item type for primitive list: $itemClazz."
                        )
                }
            }

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            @Suppress("UNCHECKED_CAST")
            override fun getFromAppFunctionData(appFunctionData: AppFunctionData, key: String): T =
                when (itemClazz) {
                    String::class.java -> appFunctionData.getStringList(key)
                    PendingIntent::class.java -> appFunctionData.getPendingIntentList(key)
                    else ->
                        throw IllegalStateException(
                            "Unsupported item type for primitive list: $itemClazz."
                        )
                }
                    as T
        }

        /** The [TypeParameter] for serializable types. */
        public data class SerializableTypeParameter<I : Any, T : I?>(
            /** The type class. */
            val clazz: Class<T>,
            /** The serializable factory for the type. */
            val serializableFactory: AppFunctionSerializableFactory<I>,
        ) : TypeParameter<T> {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun setValueInAppFunctionData(
                appFunctionDataBuilder: AppFunctionData.Builder,
                key: String,
                value: T,
            ) {
                if (value == null) return

                appFunctionDataBuilder.setAppFunctionData(
                    key,
                    serializableFactory.toAppFunctionData(value),
                )
            }

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            @Suppress("UNCHECKED_CAST")
            override fun getFromAppFunctionData(appFunctionData: AppFunctionData, key: String): T =
                appFunctionData.getAppFunctionData(key)?.let {
                    serializableFactory.fromAppFunctionData(it)
                } as T
        }

        /** The [TypeParameter] for serializable [List] type. */
        public data class SerializableListTypeParameter<I : Any, T : List<*>?>(
            /** The item type class. */
            val itemClazz: Class<I>,
            /** The serializable factory for the item type. */
            val serializableFactory: AppFunctionSerializableFactory<I>,
        ) : TypeParameter<T> {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            @Suppress("UNCHECKED_CAST")
            override fun setValueInAppFunctionData(
                appFunctionDataBuilder: AppFunctionData.Builder,
                key: String,
                value: T,
            ) {
                if (value == null) return

                appFunctionDataBuilder.setAppFunctionDataList(
                    key,
                    value.map { serializableFactory.toAppFunctionData(it as I) },
                )
            }

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            @Suppress("UNCHECKED_CAST")
            override fun getFromAppFunctionData(appFunctionData: AppFunctionData, key: String): T =
                appFunctionData.getAppFunctionDataList(key)?.map {
                    serializableFactory.fromAppFunctionData(it)
                } as T
        }
    }
}

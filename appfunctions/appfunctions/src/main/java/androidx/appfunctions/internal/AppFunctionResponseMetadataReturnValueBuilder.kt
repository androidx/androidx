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

import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataType
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata

/**
 * Specifies the response of an app function.
 *
 * @param type The [AppFunctionDataTypeMetadata] `TYPE_*` constant indicating the type of the
 *   response.
 * @param isNullable Whether the response can be null.
 * @param objectQualifiedName The fully qualified name of the class if [type] is
 *   [AppFunctionDataTypeMetadata.TYPE_OBJECT] or [AppFunctionDataTypeMetadata.TYPE_REFERENCE].
 * @param itemType The [AppFunctionDataTypeMetadata] `TYPE_*` constant of the items if [type] is
 *   [AppFunctionDataTypeMetadata.TYPE_ARRAY].
 * @param itemQualifiedName The fully qualified name of the class of the item if [itemType] is
 *   [AppFunctionDataTypeMetadata.TYPE_OBJECT] or [AppFunctionDataTypeMetadata.TYPE_REFERENCE].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionResponseSpec(
    @AppFunctionDataType public val type: Int,
    public val isNullable: Boolean,
    public val objectQualifiedName: String? = null,
    @AppFunctionDataType public val itemType: Int? = null,
    public val itemQualifiedName: String? = null,
) {
    init {
        if (
            type == AppFunctionDataTypeMetadata.TYPE_OBJECT ||
                type == AppFunctionDataTypeMetadata.TYPE_REFERENCE
        ) {
            requireNotNull(objectQualifiedName)
        }
        if (type == AppFunctionDataTypeMetadata.TYPE_ARRAY) {
            requireNotNull(itemType)
            if (
                itemType == AppFunctionDataTypeMetadata.TYPE_OBJECT ||
                    itemType == AppFunctionDataTypeMetadata.TYPE_REFERENCE
            ) {
                requireNotNull(itemQualifiedName)
            }
        }
    }
}

/**
 * Builds the return value [AppFunctionData] from the given [result] based on this spec.
 *
 * @throws AppFunctionAppUnknownException if the [result] is not valid according to this spec.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun AppFunctionResponseSpec.unsafeBuildReturnValue(result: Any?): AppFunctionData =
    try {
        if (result == null) {
            check(isNullable) { "Unexpected null for non-null return type" }
            AppFunctionData.EMPTY
        } else {
            // TODO(b/532491420): Pick the AppFunctionMetadata from the AppSearch
            val builder = AppFunctionData.Builder("")
            when (type) {
                AppFunctionDataTypeMetadata.TYPE_UNIT -> AppFunctionData.EMPTY
                AppFunctionDataTypeMetadata.TYPE_LONG ->
                    builder
                        .setLong(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as Long,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_INT ->
                    builder
                        .setInt(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as Int,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_DOUBLE ->
                    builder
                        .setDouble(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as Double,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_FLOAT ->
                    builder
                        .setFloat(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as Float,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_BOOLEAN ->
                    builder
                        .setBoolean(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as Boolean,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_STRING ->
                    builder
                        .setString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as String,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_BYTES ->
                    builder
                        .setByteArray(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as ByteArray,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_PARCELABLE ->
                    builder
                        .setParcelable(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            result as Parcelable,
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_OBJECT,
                AppFunctionDataTypeMetadata.TYPE_REFERENCE ->
                    builder
                        .setAppFunctionData(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                            AppFunctionData.serialize(result, checkNotNull(objectQualifiedName)),
                        )
                        .build()
                AppFunctionDataTypeMetadata.TYPE_ARRAY -> buildArrayReturnValue(builder, result)
                else -> throw AppFunctionAppUnknownException("Unknown DataType type: $type")
            }
        }
    } catch (e: AppFunctionAppUnknownException) {
        throw e
    } catch (e: Exception) {
        Log.d(APP_FUNCTIONS_TAG, "Something went wrong when building the return value", e)
        throw AppFunctionAppUnknownException("Something went wrong when executing an app function")
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("UNCHECKED_CAST")
private fun AppFunctionResponseSpec.buildArrayReturnValue(
    builder: AppFunctionData.Builder,
    result: Any,
): AppFunctionData {
    return when (itemType) {
        AppFunctionDataTypeMetadata.TYPE_LONG ->
            builder
                .setLongArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as LongArray,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_INT ->
            builder
                .setIntArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as IntArray,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_DOUBLE ->
            builder
                .setDoubleArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as DoubleArray,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_FLOAT ->
            builder
                .setFloatArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as FloatArray,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_BOOLEAN ->
            builder
                .setBooleanArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as BooleanArray,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_STRING ->
            builder
                .setStringList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as List<String>,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_PARCELABLE ->
            builder
                .setParcelableList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as List<Parcelable>,
                )
                .build()
        AppFunctionDataTypeMetadata.TYPE_OBJECT,
        AppFunctionDataTypeMetadata.TYPE_REFERENCE -> {
            val serializableList = result as List<Any>
            val appFunctionDataList =
                serializableList.map {
                    AppFunctionData.serialize(it, checkNotNull(itemQualifiedName))
                }
            builder
                .setAppFunctionDataList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    appFunctionDataList,
                )
                .build()
        }
        else -> throw AppFunctionAppUnknownException("Unknown array item type: $itemType")
    }
}

/**
 * Builds [AppFunctionData] from [result] based on [AppFunctionResponseMetadata].
 *
 * @throws [AppFunctionAppUnknownException] if unable to build the return value based on
 *   [AppFunctionResponseMetadata].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun AppFunctionResponseMetadata.unsafeBuildReturnValue(
    result: Any?,
    componentsMetadata: AppFunctionComponentsMetadata,
): AppFunctionData =
    try {
        if (result == null) {
            check(valueType.isNullable) { "Unexpected null for non-null return type" }
            AppFunctionData.EMPTY
        } else {
            valueType.unsafeBuildReturnValue(result, this, componentsMetadata)
        }
    } catch (e: Exception) {
        Log.d(APP_FUNCTIONS_TAG, "Something went wrong when building the return value", e)
        throw AppFunctionAppUnknownException("Something went wrong when executing an app function")
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionDataTypeMetadata.unsafeBuildReturnValue(
    result: Any,
    responseMetadata: AppFunctionResponseMetadata,
    componentsMetadata: AppFunctionComponentsMetadata,
): AppFunctionData {
    val builder = AppFunctionData.Builder(responseMetadata, componentsMetadata)
    return when (this) {
        is AppFunctionUnitTypeMetadata -> {
            AppFunctionData.EMPTY
        }
        is AppFunctionLongTypeMetadata -> {
            builder
                .setLong(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, result as Long)
                .build()
        }
        is AppFunctionIntTypeMetadata -> {
            builder
                .setInt(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, result as Int)
                .build()
        }
        is AppFunctionDoubleTypeMetadata -> {
            builder
                .setDouble(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as Double,
                )
                .build()
        }
        is AppFunctionFloatTypeMetadata -> {
            builder
                .setFloat(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, result as Float)
                .build()
        }
        is AppFunctionBooleanTypeMetadata -> {
            builder
                .setBoolean(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as Boolean,
                )
                .build()
        }
        is AppFunctionStringTypeMetadata -> {
            val stringValue =
                when (result) {
                    is Uri -> result.toString()
                    is String -> result
                    else ->
                        throw AppFunctionAppUnknownException(
                            "Expected String or Uri, got ${result::class.java.name}"
                        )
                }
            builder
                .setString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, stringValue)
                .build()
        }
        is AppFunctionBytesTypeMetadata -> {
            throw IllegalStateException("Type of a single byte is not supported")
        }
        is AppFunctionParcelableTypeMetadata -> {
            builder
                .setParcelable(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as Parcelable,
                )
                .build()
        }
        is AppFunctionObjectTypeMetadata -> {
            builder
                .setAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    AppFunctionData.serialize(result, checkNotNull(this.qualifiedName)),
                )
                .build()
        }
        is AppFunctionAllOfTypeMetadata -> {
            builder
                .setAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    AppFunctionData.serialize(result, checkNotNull(this.qualifiedName)),
                )
                .build()
        }
        is AppFunctionReferenceTypeMetadata -> {
            builder
                .setAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    AppFunctionData.serialize(result, checkNotNull(this.referenceDataType)),
                )
                .build()
        }
        is AppFunctionArrayTypeMetadata -> {
            this.unsafeBuildReturnValue(builder, result)
        }
        else -> {
            throw IllegalStateException("Unknown DataTypeMetadata: ${this::class.java}")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionArrayTypeMetadata.unsafeBuildReturnValue(
    builder: AppFunctionData.Builder,
    result: Any,
): AppFunctionData {
    return when (val castItemType = itemType) {
        is AppFunctionLongTypeMetadata -> {
            builder
                .setLongArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as LongArray,
                )
                .build()
        }
        is AppFunctionIntTypeMetadata -> {
            builder
                .setIntArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as IntArray,
                )
                .build()
        }
        is AppFunctionDoubleTypeMetadata -> {
            builder
                .setDoubleArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as DoubleArray,
                )
                .build()
        }
        is AppFunctionFloatTypeMetadata -> {
            builder
                .setFloatArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as FloatArray,
                )
                .build()
        }
        is AppFunctionBooleanTypeMetadata -> {
            builder
                .setBooleanArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as BooleanArray,
                )
                .build()
        }
        is AppFunctionStringTypeMetadata -> {
            @Suppress("UNCHECKED_CAST")
            val stringList =
                (result as? List<*>)?.map { item ->
                    when (item) {
                        is Uri -> item.toString()
                        is String -> item
                        null ->
                            throw AppFunctionAppUnknownException(
                                "Null elements in return arrays are not supported"
                            )
                        else ->
                            throw AppFunctionAppUnknownException(
                                "Expected String or Uri in list, got ${item.javaClass.name}"
                            )
                    }
                }
                    ?: (result as? Array<*>)?.map { item ->
                        when (item) {
                            is Uri -> item.toString()
                            is String -> item
                            null ->
                                throw AppFunctionAppUnknownException(
                                    "Null elements in return arrays are not supported"
                                )
                            else ->
                                throw AppFunctionAppUnknownException(
                                    "Expected String or Uri in array, got ${item.javaClass.name}"
                                )
                        }
                    }
                    ?: throw AppFunctionAppUnknownException(
                        "Expected List or Array for string list return"
                    )
            builder
                .setStringList(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, stringList)
                .build()
        }
        is AppFunctionBytesTypeMetadata -> {
            builder
                .setByteArray(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as ByteArray,
                )
                .build()
        }
        is AppFunctionParcelableTypeMetadata -> {
            @Suppress("UNCHECKED_CAST")
            builder
                .setParcelableList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as List<Parcelable>,
                )
                .build()
        }
        is AppFunctionObjectTypeMetadata -> {
            @Suppress("UNCHECKED_CAST")
            builder
                .setAppFunctionDataList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    (result as List<Any>).map {
                        AppFunctionData.serialize(it, checkNotNull(castItemType.qualifiedName))
                    },
                )
                .build()
        }
        is AppFunctionAllOfTypeMetadata -> {
            @Suppress("UNCHECKED_CAST")
            builder
                .setAppFunctionDataList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    (result as List<Any>).map {
                        AppFunctionData.serialize(it, checkNotNull(castItemType.qualifiedName))
                    },
                )
                .build()
        }
        is AppFunctionReferenceTypeMetadata -> {
            @Suppress("UNCHECKED_CAST")
            builder
                .setAppFunctionDataList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    (result as List<Any>).map {
                        AppFunctionData.serialize(it, checkNotNull(castItemType.referenceDataType))
                    },
                )
                .build()
        }
        else -> {
            throw IllegalStateException(
                "Unknown item DataTypeMetadata: ${castItemType::class.java}"
            )
        }
    }
}

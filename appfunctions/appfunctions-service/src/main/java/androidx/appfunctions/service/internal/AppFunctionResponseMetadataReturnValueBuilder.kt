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

package androidx.appfunctions.service.internal

import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata.Companion.TYPE as TYPE_OBJECT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BYTES
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_FLOAT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_INT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_PENDING_INTENT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_UNIT
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata

/**
 * Builds [AppFunctionData] from [result] based on [AppFunctionResponseMetadata].
 *
 * @throws [AppFunctionAppUnknownException] if unable to build the return value based on
 *   [AppFunctionResponseMetadata].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun AppFunctionResponseMetadata.unsafeBuildReturnValue(result: Any?): AppFunctionData =
    try {
        if (result == null) {
            check(valueType.isNullable) { "Unexpected null for non-null return type" }
            AppFunctionData.EMPTY
        } else {
            valueType.unsafeBuildReturnValue(result)
        }
    } catch (e: Exception) {
        Log.d(APP_FUNCTIONS_TAG, "Something went wrong when building the return value", e)
        throw AppFunctionAppUnknownException("Something went wrong when executing an app function")
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionDataTypeMetadata.unsafeBuildReturnValue(result: Any): AppFunctionData {
    return when (this) {
        is AppFunctionPrimitiveTypeMetadata -> {
            unsafeBuildSingleReturnValue(this.type, result)
        }
        is AppFunctionObjectTypeMetadata -> {
            unsafeBuildSingleReturnValue(TYPE_OBJECT, result, this.qualifiedName)
        }
        is AppFunctionArrayTypeMetadata -> {
            this.unsafeBuildReturnValue(result)
        }
        is AppFunctionReferenceTypeMetadata -> {
            unsafeBuildSingleReturnValue(TYPE_OBJECT, result, this.referenceDataType)
        }
        else -> {
            throw IllegalStateException("Unknown DataTypeMetadata: ${this::class.java}")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionArrayTypeMetadata.unsafeBuildReturnValue(result: Any): AppFunctionData {
    return when (val castItemType = itemType) {
        is AppFunctionPrimitiveTypeMetadata -> {
            unsafeBuildCollectionReturnValue(castItemType.type, result)
        }
        is AppFunctionObjectTypeMetadata -> {
            unsafeBuildCollectionReturnValue(TYPE_OBJECT, result, castItemType.qualifiedName)
        }
        is AppFunctionReferenceTypeMetadata -> {
            unsafeBuildCollectionReturnValue(TYPE_OBJECT, result, castItemType.referenceDataType)
        }
        else -> {
            throw IllegalStateException(
                "Unknown item DataTypeMetadata: ${castItemType::class.java}"
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun unsafeBuildSingleReturnValue(
    type: Int,
    result: Any,
    objectQualifiedName: String? = null,
): AppFunctionData {
    val builder = AppFunctionData.Builder("")
    when (type) {
        TYPE_LONG -> {
            builder.setLong(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as Long,
            )
        }
        TYPE_INT -> {
            builder.setInt(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, result as Int)
        }
        TYPE_DOUBLE -> {
            builder.setDouble(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as Double,
            )
        }
        TYPE_FLOAT -> {
            builder.setFloat(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as Float,
            )
        }
        TYPE_BOOLEAN -> {
            builder.setBoolean(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as Boolean,
            )
        }
        TYPE_STRING -> {
            builder.setString(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as String,
            )
        }
        TYPE_BYTES -> {
            throw IllegalStateException("Type of a single byte is not supported")
        }
        TYPE_PENDING_INTENT -> {
            builder.setPendingIntent(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as PendingIntent,
            )
        }
        TYPE_OBJECT -> {
            builder.setAppFunctionData(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                AppFunctionData.serialize(result, checkNotNull(objectQualifiedName)),
            )
        }
        TYPE_UNIT -> {
            // no-op
        }
        else -> {
            throw IllegalStateException("Unknown data type $type")
        }
    }
    return builder.build()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun unsafeBuildCollectionReturnValue(
    type: Int,
    result: Any,
    objectQualifiedName: String? = null,
): AppFunctionData {
    val builder = AppFunctionData.Builder("")
    when (type) {
        TYPE_LONG -> {
            builder.setLongArray(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as LongArray,
            )
        }
        TYPE_INT -> {
            builder.setIntArray(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as IntArray,
            )
        }
        TYPE_DOUBLE -> {
            builder.setDoubleArray(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as DoubleArray,
            )
        }
        TYPE_FLOAT -> {
            builder.setFloatArray(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as FloatArray,
            )
        }
        TYPE_BOOLEAN -> {
            builder.setBooleanArray(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as BooleanArray,
            )
        }
        TYPE_STRING -> {
            @Suppress("UNCHECKED_CAST")
            builder.setStringList(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as List<String>,
            )
        }
        TYPE_BYTES -> {
            builder.setByteArray(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as ByteArray,
            )
        }
        TYPE_PENDING_INTENT -> {
            @Suppress("UNCHECKED_CAST")
            builder.setPendingIntentList(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                result as List<PendingIntent>,
            )
        }
        TYPE_OBJECT -> {
            @Suppress("UNCHECKED_CAST")
            builder.setAppFunctionDataList(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                (result as List<Any>).map {
                    AppFunctionData.serialize(it, checkNotNull(objectQualifiedName))
                },
            )
        }
        else -> {
            throw IllegalStateException("Unknown data type $type")
        }
    }
    return builder.build()
}

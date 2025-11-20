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
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPendingIntentTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata

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
            builder
                .setString(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as String,
                )
                .build()
        }
        is AppFunctionBytesTypeMetadata -> {
            throw IllegalStateException("Type of a single byte is not supported")
        }
        is AppFunctionPendingIntentTypeMetadata -> {
            builder
                .setPendingIntent(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as PendingIntent,
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
            builder
                .setStringList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as List<String>,
                )
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
        is AppFunctionPendingIntentTypeMetadata -> {
            @Suppress("UNCHECKED_CAST")
            builder
                .setPendingIntentList(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    result as List<PendingIntent>,
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

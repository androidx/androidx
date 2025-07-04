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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata.Companion.TYPE as TYPE_OBJECT
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BYTES
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_FLOAT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_INT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_PENDING_INTENT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata

/**
 * Gets the parameter value from [AppFunctionData] based on [parameterMetadata].
 *
 * @throws [AppFunctionInvalidArgumentException] if the parameter in [AppFunctionData] is not valid
 *   according to [parameterMetadata].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun AppFunctionData.unsafeGetParameterValue(
    parameterMetadata: AppFunctionParameterMetadata
): Any? =
    try {
        val value =
            when (val castDataType = parameterMetadata.dataType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    unsafeGetSingleProperty(parameterMetadata.name, castDataType.type)
                }
                is AppFunctionObjectTypeMetadata -> {
                    unsafeGetSingleProperty(
                        parameterMetadata.name,
                        TYPE_OBJECT,
                        castDataType.qualifiedName,
                    )
                }
                is AppFunctionArrayTypeMetadata -> {
                    getArrayTypeParameterValue(parameterMetadata.name, castDataType)
                }
                is AppFunctionReferenceTypeMetadata -> {
                    unsafeGetSingleProperty(
                        parameterMetadata.name,
                        TYPE_OBJECT,
                        castDataType.referenceDataType,
                    )
                }
                else ->
                    throw IllegalStateException(
                        "Unknown DataTypeMetadata: ${castDataType::class.java}"
                    )
            }
        if (value == null) {
            require(!parameterMetadata.isRequired) {
                Log.d(APP_FUNCTIONS_TAG, "Parameter ${parameterMetadata.name} is required")
                "Parameter ${parameterMetadata.name} is required"
            }
        }
        value
    } catch (e: IllegalArgumentException) {
        Log.d(
            APP_FUNCTIONS_TAG,
            "Parameter ${parameterMetadata.name} should be the type of ${parameterMetadata.dataType}",
            e,
        )
        throw AppFunctionInvalidArgumentException(
            "Parameter ${parameterMetadata.name} should be the type of ${parameterMetadata.dataType}"
        )
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionData.getArrayTypeParameterValue(
    key: String,
    arrayDataTypeMetadata: AppFunctionArrayTypeMetadata,
): Any? {
    val itemType = arrayDataTypeMetadata.itemType
    return when (itemType) {
        is AppFunctionPrimitiveTypeMetadata -> {
            unsafeGetCollectionProperty(key, itemType.type)
        }
        is AppFunctionObjectTypeMetadata -> {
            unsafeGetCollectionProperty(key, TYPE_OBJECT, itemType.qualifiedName)
        }
        is AppFunctionReferenceTypeMetadata -> {
            unsafeGetCollectionProperty(key, TYPE_OBJECT, itemType.referenceDataType)
        }
        else ->
            throw IllegalStateException("Unknown item DataTypeMetadata: ${itemType::class.java}")
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionData.unsafeGetSingleProperty(
    key: String,
    type: Int,
    objectQualifiedName: String? = null,
): Any? {
    return when (type) {
        TYPE_INT -> {
            getIntOrNull(key)
        }
        TYPE_LONG -> {
            getLongOrNull(key)
        }
        TYPE_FLOAT -> {
            getFloatOrNull(key)
        }
        TYPE_DOUBLE -> {
            getDoubleOrNull(key)
        }
        TYPE_BOOLEAN -> {
            getBooleanOrNull(key)
        }
        TYPE_BYTES -> {
            throw IllegalStateException("Type of a single byte is not supported")
        }
        TYPE_STRING -> {
            getString(key)
        }
        TYPE_PENDING_INTENT -> {
            getPendingIntent(key)
        }
        TYPE_OBJECT -> {
            getAppFunctionData(key)?.deserialize(checkNotNull(objectQualifiedName))
        }
        else -> throw IllegalStateException("Unknown data type $type")
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionData.unsafeGetCollectionProperty(
    key: String,
    type: Int,
    objectQualifiedName: String? = null,
): Any? {
    return when (type) {
        TYPE_INT -> {
            getIntArray(key)
        }
        TYPE_LONG -> {
            getLongArray(key)
        }
        TYPE_FLOAT -> {
            getFloatArray(key)
        }
        TYPE_DOUBLE -> {
            getDoubleArray(key)
        }
        TYPE_BOOLEAN -> {
            getBooleanArray(key)
        }
        TYPE_BYTES -> {
            getByteArray(key)
        }
        TYPE_STRING -> {
            getStringList(key)
        }
        TYPE_PENDING_INTENT -> {
            getPendingIntentList(key)
        }
        TYPE_OBJECT -> {
            getAppFunctionDataList(key)?.map {
                it.deserialize<Any>(checkNotNull(objectQualifiedName))
            }
        }
        else -> throw IllegalStateException("Unknown data type $type")
    }
}

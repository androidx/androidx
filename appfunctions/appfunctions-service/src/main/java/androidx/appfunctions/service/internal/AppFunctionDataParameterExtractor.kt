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

// TODO(b/429588205): Generate a pseudo AppFunctionSerializable class to represent a function
// input. This would allow the infra to reuse AppFunctionDataFactory to supply default values.
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
                    unsafeGetSingleProperty(
                        key = parameterMetadata.name,
                        type = castDataType.type,
                        isNullable = castDataType.isNullable,
                        isRequired = parameterMetadata.isRequired,
                    )
                }
                is AppFunctionObjectTypeMetadata -> {
                    unsafeGetSingleProperty(
                        key = parameterMetadata.name,
                        type = TYPE_OBJECT,
                        isNullable = castDataType.isNullable,
                        isRequired = parameterMetadata.isRequired,
                        objectQualifiedName = castDataType.qualifiedName,
                    )
                }
                is AppFunctionArrayTypeMetadata -> {
                    getArrayTypeParameterValue(
                        key = parameterMetadata.name,
                        arrayDataTypeMetadata = castDataType,
                        isNullable = castDataType.isNullable,
                        isRequired = parameterMetadata.isRequired,
                    )
                }
                is AppFunctionReferenceTypeMetadata -> {
                    unsafeGetSingleProperty(
                        key = parameterMetadata.name,
                        type = TYPE_OBJECT,
                        isNullable = castDataType.isNullable,
                        isRequired = parameterMetadata.isRequired,
                        objectQualifiedName = castDataType.referenceDataType,
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
    isNullable: Boolean,
    isRequired: Boolean,
): Any? {
    val itemType = arrayDataTypeMetadata.itemType
    return when (itemType) {
        is AppFunctionPrimitiveTypeMetadata -> {
            unsafeGetCollectionProperty(
                key = key,
                type = itemType.type,
                isNullable = isNullable,
                isRequired = isRequired,
            )
        }
        is AppFunctionObjectTypeMetadata -> {
            unsafeGetCollectionProperty(
                key = key,
                type = TYPE_OBJECT,
                isNullable = isNullable,
                isRequired = isRequired,
                objectQualifiedName = itemType.qualifiedName,
            )
        }
        is AppFunctionReferenceTypeMetadata -> {
            unsafeGetCollectionProperty(
                key = key,
                type = TYPE_OBJECT,
                isNullable = isNullable,
                isRequired = isRequired,
                objectQualifiedName = itemType.referenceDataType,
            )
        }
        else ->
            throw IllegalStateException("Unknown item DataTypeMetadata: ${itemType::class.java}")
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionData.unsafeGetSingleProperty(
    key: String,
    type: Int,
    isNullable: Boolean,
    isRequired: Boolean,
    objectQualifiedName: String? = null,
): Any? {
    return when (type) {
        TYPE_INT -> {
            if (!isRequired && !isNullable) {
                getIntOrNull(key) ?: 0
            } else {
                getIntOrNull(key)
            }
        }
        TYPE_LONG -> {
            if (!isRequired && !isNullable) {
                getLongOrNull(key) ?: 0L
            } else {
                getLongOrNull(key)
            }
        }
        TYPE_FLOAT -> {
            if (!isRequired && !isNullable) {
                getFloatOrNull(key) ?: 0.0f
            } else {
                getFloatOrNull(key)
            }
        }
        TYPE_DOUBLE -> {
            if (!isRequired && !isNullable) {
                getDoubleOrNull(key) ?: 0.0
            } else {
                getDoubleOrNull(key)
            }
        }
        TYPE_BOOLEAN -> {
            if (!isRequired && !isNullable) {
                getBooleanOrNull(key) ?: false
            } else {
                getBooleanOrNull(key)
            }
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
    isNullable: Boolean,
    isRequired: Boolean,
    objectQualifiedName: String? = null,
): Any? {
    return when (type) {
        TYPE_INT -> {
            if (!isRequired && !isNullable) {
                getIntArray(key) ?: intArrayOf()
            } else {
                getIntArray(key)
            }
        }
        TYPE_LONG -> {
            if (!isRequired && !isNullable) {
                getLongArray(key) ?: longArrayOf()
            } else {
                getLongArray(key)
            }
        }
        TYPE_FLOAT -> {
            if (!isRequired && !isNullable) {
                getFloatArray(key) ?: floatArrayOf()
            } else {
                getFloatArray(key)
            }
        }
        TYPE_DOUBLE -> {
            if (!isRequired && !isNullable) {
                getDoubleArray(key) ?: doubleArrayOf()
            } else {
                getDoubleArray(key)
            }
        }
        TYPE_BOOLEAN -> {
            if (!isRequired && !isNullable) {
                getBooleanArray(key) ?: booleanArrayOf()
            } else {
                getBooleanArray(key)
            }
        }
        TYPE_BYTES -> {
            if (!isRequired && !isNullable) {
                getByteArray(key) ?: byteArrayOf()
            } else {
                getByteArray(key)
            }
        }
        TYPE_STRING -> {
            if (!isRequired && !isNullable) {
                getStringList(key) ?: emptyList<String>()
            } else {
                getStringList(key)
            }
        }
        TYPE_PENDING_INTENT -> {
            if (!isRequired && !isNullable) {
                getPendingIntentList(key) ?: emptyList<PendingIntent>()
            } else {
                getPendingIntentList(key)
            }
        }
        TYPE_OBJECT -> {
            if (!isRequired && !isNullable) {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(objectQualifiedName))
                } ?: emptyList()
            } else {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(objectQualifiedName))
                }
            }
        }
        else -> throw IllegalStateException("Unknown data type $type")
    }
}

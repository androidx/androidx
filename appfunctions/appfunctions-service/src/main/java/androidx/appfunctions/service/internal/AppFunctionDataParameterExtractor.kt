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
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata

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
        val key = parameterMetadata.name
        val isRequired = parameterMetadata.isRequired
        val isNullable = parameterMetadata.dataType.isNullable
        val value =
            when (val castDataType = parameterMetadata.dataType) {
                is AppFunctionIntTypeMetadata -> {
                    if (!isRequired && !isNullable) {
                        getIntOrNull(key) ?: 0
                    } else {
                        getIntOrNull(key)
                    }
                }
                is AppFunctionLongTypeMetadata -> {
                    if (!isRequired && !isNullable) {
                        getLongOrNull(key) ?: 0L
                    } else {
                        getLongOrNull(key)
                    }
                }
                is AppFunctionFloatTypeMetadata -> {
                    if (!isRequired && !isNullable) {
                        getFloatOrNull(key) ?: 0.0f
                    } else {
                        getFloatOrNull(key)
                    }
                }
                is AppFunctionDoubleTypeMetadata -> {
                    if (!isRequired && !isNullable) {
                        getDoubleOrNull(key) ?: 0.0
                    } else {
                        getDoubleOrNull(key)
                    }
                }
                is AppFunctionBooleanTypeMetadata -> {
                    if (!isRequired && !isNullable) {
                        getBooleanOrNull(key) ?: false
                    } else {
                        getBooleanOrNull(key)
                    }
                }
                is AppFunctionBytesTypeMetadata -> {
                    if (!isRequired && !isNullable) {
                        getByteArray(key) ?: byteArrayOf()
                    } else {
                        getByteArray(key)
                    }
                }
                is AppFunctionStringTypeMetadata -> {
                    getString(key)
                }
                is AppFunctionParcelableTypeMetadata -> {
                    val parcelableClass = getParcelableClass(castDataType.qualifiedName)
                    getParcelable(key, parcelableClass)
                }
                is AppFunctionObjectTypeMetadata -> {
                    getAppFunctionData(key)?.deserialize(checkNotNull(castDataType.qualifiedName))
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
                    getAppFunctionData(key)
                        ?.deserialize(checkNotNull(castDataType.referenceDataType))
                }
                else ->
                    throw IllegalStateException(
                        "Unknown DataTypeMetadata: ${castDataType.javaClass}"
                    )
            }
        if (value == null) {
            require(!parameterMetadata.isRequired || parameterMetadata.dataType.isNullable) {
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
        is AppFunctionIntTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getIntArray(key) ?: intArrayOf()
            } else {
                getIntArray(key)
            }
        }
        is AppFunctionLongTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getLongArray(key) ?: longArrayOf()
            } else {
                getLongArray(key)
            }
        }
        is AppFunctionFloatTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getFloatArray(key) ?: floatArrayOf()
            } else {
                getFloatArray(key)
            }
        }
        is AppFunctionDoubleTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getDoubleArray(key) ?: doubleArrayOf()
            } else {
                getDoubleArray(key)
            }
        }
        is AppFunctionBooleanTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getBooleanArray(key) ?: booleanArrayOf()
            } else {
                getBooleanArray(key)
            }
        }
        is AppFunctionBytesTypeMetadata -> {
            throw IllegalStateException("List<ByteArray> is not supported")
        }
        is AppFunctionStringTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getStringList(key) ?: emptyList<String>()
            } else {
                getStringList(key)
            }
        }
        is AppFunctionParcelableTypeMetadata -> {
            val parcelableClass = getParcelableClass(itemType.qualifiedName)
            if (!isRequired && !isNullable) {
                getParcelableList(key, parcelableClass) ?: emptyList()
            } else {
                getParcelableList(key, parcelableClass)
            }
        }
        is AppFunctionObjectTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(itemType.qualifiedName))
                } ?: emptyList()
            } else {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(itemType.qualifiedName))
                }
            }
        }
        is AppFunctionReferenceTypeMetadata -> {
            if (!isRequired && !isNullable) {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(itemType.referenceDataType))
                } ?: emptyList()
            } else {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(itemType.referenceDataType))
                }
            }
        }
        else -> throw IllegalStateException("Unknown item DataTypeMetadata: ${itemType.javaClass}")
    }
}

private fun getParcelableClass(className: String): Class<Parcelable> {
    val rawClass =
        try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Class '$className' could not be found.", e)
        }

    if (!Parcelable::class.java.isAssignableFrom(rawClass)) {
        throw IllegalStateException("Class '$className' is not a Parcelable.")
    }

    @Suppress("UNCHECKED_CAST") val parcelableClass = rawClass as Class<Parcelable>

    return parcelableClass
}

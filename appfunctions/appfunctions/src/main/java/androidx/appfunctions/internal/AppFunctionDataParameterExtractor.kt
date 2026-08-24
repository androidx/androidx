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

import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataType
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata

/**
 * Specifies the parameter of an app function.
 *
 * @param type The [AppFunctionDataTypeMetadata] `TYPE_*` constant indicating the type of the
 *   parameter.
 * @param name The name of the parameter.
 * @param isRequired Whether the parameter is required.
 * @param isNullable Whether the parameter can be null.
 * @param objectQualifiedName The fully qualified name of the class if [type] is
 *   [AppFunctionDataTypeMetadata.TYPE_OBJECT] or [AppFunctionDataTypeMetadata.TYPE_REFERENCE].
 * @param itemType The [AppFunctionDataTypeMetadata] `TYPE_*` constant of the items if [type] is
 *   [AppFunctionDataTypeMetadata.TYPE_ARRAY].
 * @param itemQualifiedName The fully qualified name of the class of the item if [itemType] is
 *   [AppFunctionDataTypeMetadata.TYPE_OBJECT] or [AppFunctionDataTypeMetadata.TYPE_REFERENCE].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionParameterSpec(
    @AppFunctionDataType public val type: Int,
    public val name: String,
    public val isRequired: Boolean,
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
 * Gets the parameter value from [AppFunctionData] based on [spec].
 *
 * @throws AppFunctionInvalidArgumentException if the parameter in [AppFunctionData] is not valid
 *   according to [spec].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun AppFunctionData.unsafeGetParameterValue(spec: AppFunctionParameterSpec): Any? =
    try {
        val key = spec.name
        val isRequired = spec.isRequired
        val isNullable = spec.isNullable
        val value =
            when (spec.type) {
                AppFunctionDataTypeMetadata.TYPE_INT -> {
                    if (!isRequired && !isNullable) {
                        getIntOrNull(key) ?: 0
                    } else {
                        getIntOrNull(key)
                    }
                }
                AppFunctionDataTypeMetadata.TYPE_LONG -> {
                    if (!isRequired && !isNullable) {
                        getLongOrNull(key) ?: 0L
                    } else {
                        getLongOrNull(key)
                    }
                }
                AppFunctionDataTypeMetadata.TYPE_FLOAT -> {
                    if (!isRequired && !isNullable) {
                        getFloatOrNull(key) ?: 0.0f
                    } else {
                        getFloatOrNull(key)
                    }
                }
                AppFunctionDataTypeMetadata.TYPE_DOUBLE -> {
                    if (!isRequired && !isNullable) {
                        getDoubleOrNull(key) ?: 0.0
                    } else {
                        getDoubleOrNull(key)
                    }
                }
                AppFunctionDataTypeMetadata.TYPE_BOOLEAN -> {
                    if (!isRequired && !isNullable) {
                        getBooleanOrNull(key) ?: false
                    } else {
                        getBooleanOrNull(key)
                    }
                }
                AppFunctionDataTypeMetadata.TYPE_BYTES -> {
                    if (!isRequired && !isNullable) {
                        getByteArray(key) ?: byteArrayOf()
                    } else {
                        getByteArray(key)
                    }
                }
                AppFunctionDataTypeMetadata.TYPE_STRING -> {
                    getString(key)
                }
                AppFunctionDataTypeMetadata.TYPE_PARCELABLE -> {
                    val parcelableClass = getParcelableClass(checkNotNull(spec.objectQualifiedName))
                    getParcelable(key, parcelableClass)
                }
                AppFunctionDataTypeMetadata.TYPE_OBJECT,
                AppFunctionDataTypeMetadata.TYPE_REFERENCE -> {
                    getAppFunctionData(key)?.deserialize(checkNotNull(spec.objectQualifiedName))
                }
                AppFunctionDataTypeMetadata.TYPE_ARRAY -> {
                    getArrayTypeParameterValue(
                        key = spec.name,
                        itemType = checkNotNull(spec.itemType),
                        itemQualifiedName = spec.itemQualifiedName,
                        isNullable = spec.isNullable,
                        isRequired = spec.isRequired,
                    )
                }
                else ->
                    throw AppFunctionInvalidArgumentException(
                        "Unknown DataTypeMetadata type: ${spec.type}"
                    )
            }
        if (value == null) {
            require(!spec.isRequired || spec.isNullable) {
                Log.d(APP_FUNCTIONS_TAG, "Parameter ${spec.name} is required")
                "Parameter ${spec.name} is required"
            }
        }
        value
    } catch (e: AppFunctionInvalidArgumentException) {
        throw e
    } catch (e: Exception) {
        throw AppFunctionInvalidArgumentException(
            "Unable to get parameter ${spec.name} from AppFunctionData."
        )
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AppFunctionData.getArrayTypeParameterValue(
    key: String,
    itemType: Int,
    itemQualifiedName: String?,
    isNullable: Boolean,
    isRequired: Boolean,
): Any? {
    return when (itemType) {
        AppFunctionDataTypeMetadata.TYPE_INT -> {
            if (!isRequired && !isNullable) {
                getIntArray(key) ?: intArrayOf()
            } else {
                getIntArray(key)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_LONG -> {
            if (!isRequired && !isNullable) {
                getLongArray(key) ?: longArrayOf()
            } else {
                getLongArray(key)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_FLOAT -> {
            if (!isRequired && !isNullable) {
                getFloatArray(key) ?: floatArrayOf()
            } else {
                getFloatArray(key)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_DOUBLE -> {
            if (!isRequired && !isNullable) {
                getDoubleArray(key) ?: doubleArrayOf()
            } else {
                getDoubleArray(key)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_BOOLEAN -> {
            if (!isRequired && !isNullable) {
                getBooleanArray(key) ?: booleanArrayOf()
            } else {
                getBooleanArray(key)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_BYTES -> {
            throw AppFunctionInvalidArgumentException("List<ByteArray> is not supported")
        }
        AppFunctionDataTypeMetadata.TYPE_STRING -> {
            if (!isRequired && !isNullable) {
                getStringList(key) ?: emptyList<String>()
            } else {
                getStringList(key)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_PARCELABLE -> {
            val parcelableClass = getParcelableClass(checkNotNull(itemQualifiedName))
            if (!isRequired && !isNullable) {
                getParcelableList(key, parcelableClass) ?: emptyList()
            } else {
                getParcelableList(key, parcelableClass)
            }
        }
        AppFunctionDataTypeMetadata.TYPE_OBJECT,
        AppFunctionDataTypeMetadata.TYPE_REFERENCE -> {
            if (!isRequired && !isNullable) {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(itemQualifiedName))
                } ?: emptyList()
            } else {
                getAppFunctionDataList(key)?.map {
                    it.deserialize<Any>(checkNotNull(itemQualifiedName))
                }
            }
        }
        else -> throw AppFunctionInvalidArgumentException("Unknown item type: $itemType")
    }
}

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
            throw AppFunctionAppUnknownException("Class '$className' could not be found.")
        }

    if (!Parcelable::class.java.isAssignableFrom(rawClass)) {
        throw AppFunctionAppUnknownException("Class '$className' is not a Parcelable.")
    }

    @Suppress("UNCHECKED_CAST") val parcelableClass = rawClass as Class<Parcelable>

    return parcelableClass
}

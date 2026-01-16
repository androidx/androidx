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

package androidx.appfunctions.compiler.core.metadata

import androidx.appfunctions.compiler.core.IntrospectionHelper
import androidx.appfunctions.compiler.core.findAnnotation
import androidx.appfunctions.compiler.core.requirePropertyValueOfType
import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.reflect.cast

abstract class AppFunctionDataTypeMetadata() {
    abstract val isNullable: Boolean
    abstract val description: String

    abstract fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument

    companion object {
        internal const val TYPE_UNIT: Int = 0
        internal const val TYPE_BOOLEAN: Int = 1
        internal const val TYPE_BYTES: Int = 2
        internal const val TYPE_OBJECT: Int = 3
        internal const val TYPE_DOUBLE: Int = 4
        internal const val TYPE_FLOAT: Int = 5
        internal const val TYPE_LONG: Int = 6
        internal const val TYPE_INT: Int = 7
        internal const val TYPE_STRING: Int = 8
        internal const val TYPE_ARRAY: Int = 10
        internal const val TYPE_REFERENCE: Int = 11
        internal const val TYPE_ALL_OF: Int = 12
        internal const val TYPE_PARCELABLE: Int = 13
        internal const val TYPE_ONE_OF = 14
    }
}

data class AppFunctionArrayTypeMetadata(
    val itemType: AppFunctionDataTypeMetadata,
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            itemType = itemType.toAppFunctionDataTypeMetadataDocument(),
            type = TYPE,
            isNullable = isNullable,
            description = description,
        )
    }

    companion object {
        const val TYPE: Int = TYPE_ARRAY
    }
}

data class AppFunctionParcelableTypeMetadata(
    val qualifiedName: String?,
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument() =
        AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            isNullable = isNullable,
            description = description,
            objectQualifiedName = qualifiedName,
        )

    companion object {
        const val TYPE: Int = TYPE_PARCELABLE
    }
}

data class AppFunctionAllOfTypeMetadata(
    val matchAll: List<AppFunctionDataTypeMetadata>,
    val qualifiedName: String?,
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        val allOfDocuments = matchAll.map { it.toAppFunctionDataTypeMetadataDocument() }
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            allOf = allOfDocuments,
            isNullable = isNullable,
            objectQualifiedName = qualifiedName,
            description = description,
        )
    }

    companion object {
        const val TYPE: Int = TYPE_ALL_OF
    }
}

data class AppFunctionOneOfTypeMetadata(
    val matchOneOf: List<AppFunctionDataTypeMetadata>,
    val qualifiedName: String?,
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        val oneOfDocuments = matchOneOf.map { it.toAppFunctionDataTypeMetadataDocument() }
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            oneOf = oneOfDocuments,
            isNullable = isNullable,
            objectQualifiedName = qualifiedName,
            description = description,
        )
    }

    companion object {
        const val TYPE: Int = TYPE_ONE_OF
    }
}

data class AppFunctionObjectTypeMetadata(
    val properties: Map<String, AppFunctionDataTypeMetadata>,
    val required: List<String>,
    val qualifiedName: String?,
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        val properties =
            properties.map { (name, dataType) ->
                AppFunctionNamedDataTypeMetadataDocument(
                    name = checkNotNull(name),
                    dataTypeMetadata = dataType.toAppFunctionDataTypeMetadataDocument(),
                )
            }
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            properties = properties,
            required = required,
            objectQualifiedName = qualifiedName,
            isNullable = isNullable,
            description = description,
        )
    }

    companion object {
        const val TYPE: Int = TYPE_OBJECT
    }
}

data class AppFunctionReferenceTypeMetadata(
    val referenceDataType: String,
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            dataTypeReference = referenceDataType,
            isNullable = isNullable,
            description = description,
        )
    }

    companion object {
        const val TYPE: Int = TYPE_REFERENCE
    }
}

data class AppFunctionIntTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
    val enumValues: Set<Int>? = null,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_INT,
            isNullable = isNullable,
            description = description,
            enumValues = enumValues.orEmpty().map { it.toString() },
        )
    }

    companion object {
        fun create(
            isNullable: Boolean,
            description: String,
            annotations: Sequence<KSAnnotation>,
        ): AppFunctionIntTypeMetadata {
            return AppFunctionIntTypeMetadata(
                isNullable,
                description,
                annotations
                    .findAnnotation(
                        IntrospectionHelper.AppFunctionIntValueConstraintAnnotation.CLASS_NAME
                    )
                    ?.requirePropertyValueOfType(
                        IntrospectionHelper.AppFunctionIntValueConstraintAnnotation
                            .PROPERTY_ENUM_VALUES,
                        // Array properties are returned as ArrayList from KSP.
                        java.util.ArrayList::class,
                    )
                    ?.map { Int::class.cast(it) }
                    ?.toSet()
                    ?.ifEmpty { null },
            )
        }
    }
}

data class AppFunctionLongTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_LONG,
            isNullable = isNullable,
            description = description,
        )
    }
}

data class AppFunctionFloatTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_FLOAT,
            isNullable = isNullable,
            description = description,
        )
    }
}

data class AppFunctionDoubleTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            isNullable = isNullable,
            description = description,
        )
    }
}

data class AppFunctionStringTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
    val enumValues: Set<String>? = null,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_STRING,
            isNullable = isNullable,
            description = description,
            enumValues = enumValues.orEmpty().toList(),
        )
    }

    companion object {
        fun create(
            isNullable: Boolean,
            description: String,
            annotations: Sequence<KSAnnotation>,
        ): AppFunctionStringTypeMetadata {
            return AppFunctionStringTypeMetadata(
                isNullable,
                description,
                annotations
                    .findAnnotation(
                        IntrospectionHelper.AppFunctionStringValueConstraintAnnotation.CLASS_NAME
                    )
                    ?.requirePropertyValueOfType(
                        IntrospectionHelper.AppFunctionStringValueConstraintAnnotation
                            .PROPERTY_ENUM_VALUES,
                        // Array properties are returned as ArrayList from KSP.
                        java.util.ArrayList::class,
                    )
                    ?.map { String::class.cast(it) }
                    ?.toSet()
                    ?.ifEmpty { null },
            )
        }
    }
}

data class AppFunctionBooleanTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            isNullable = isNullable,
            description = description,
        )
    }
}

data class AppFunctionBytesTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_BYTES,
            isNullable = isNullable,
            description = description,
        )
    }
}

data class AppFunctionUnitTypeMetadata(
    override val isNullable: Boolean,
    override val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_UNIT,
            isNullable = isNullable,
            description = description,
        )
    }
}

data class AppFunctionNamedDataTypeMetadataDocument(
    val namespace: String = APP_FUNCTION_NAMESPACE,
    val id: String = APP_FUNCTION_ID_EMPTY,
    val name: String,
    val dataTypeMetadata: AppFunctionDataTypeMetadataDocument,
)

data class AppFunctionDataTypeMetadataDocument(
    val namespace: String = APP_FUNCTION_NAMESPACE,
    val id: String = APP_FUNCTION_ID_EMPTY,
    val type: Int,
    val itemType: AppFunctionDataTypeMetadataDocument? = null,
    val properties: List<AppFunctionNamedDataTypeMetadataDocument> = emptyList(),
    val allOf: List<AppFunctionDataTypeMetadataDocument> = emptyList(),
    val oneOf: List<AppFunctionDataTypeMetadataDocument> = emptyList(),
    val required: List<String> = emptyList(),
    val dataTypeReference: String? = null,
    val isNullable: Boolean = false,
    val objectQualifiedName: String? = null,
    val description: String = "",
    val enumValues: List<String> = emptyList(),
) {
    fun toAppFunctionDataTypeMetadata(): AppFunctionDataTypeMetadata =
        when (type) {
            AppFunctionDataTypeMetadata.TYPE_ARRAY -> {
                val itemType = checkNotNull(itemType) { "Item type must be present for array type" }
                AppFunctionArrayTypeMetadata(
                    itemType = itemType.toAppFunctionDataTypeMetadata(),
                    isNullable = isNullable,
                    description = description,
                )
            }
            AppFunctionDataTypeMetadata.TYPE_OBJECT -> {
                check(properties.isNotEmpty()) {
                    "Properties must be present for object type can't be empty"
                }
                val propertiesMap =
                    properties.associate {
                        it.name to it.dataTypeMetadata.toAppFunctionDataTypeMetadata()
                    }
                AppFunctionObjectTypeMetadata(
                    properties = propertiesMap,
                    required = required,
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description,
                )
            }
            AppFunctionDataTypeMetadata.TYPE_REFERENCE ->
                AppFunctionReferenceTypeMetadata(
                    referenceDataType =
                        checkNotNull(dataTypeReference) {
                            "Data type reference must be present for reference type"
                        },
                    isNullable = isNullable,
                    description = description,
                )
            AppFunctionDataTypeMetadata.TYPE_ALL_OF ->
                AppFunctionAllOfTypeMetadata(
                    matchAll = allOf.map { it.toAppFunctionDataTypeMetadata() },
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description,
                )
            AppFunctionDataTypeMetadata.TYPE_INT ->
                AppFunctionIntTypeMetadata(
                    isNullable = isNullable,
                    description = description,
                    enumValues.map { it.toInt() }.toSet().ifEmpty { null },
                )

            AppFunctionDataTypeMetadata.TYPE_LONG ->
                AppFunctionLongTypeMetadata(isNullable = isNullable, description = description)

            AppFunctionDataTypeMetadata.TYPE_FLOAT ->
                AppFunctionFloatTypeMetadata(isNullable = isNullable, description = description)

            AppFunctionDataTypeMetadata.TYPE_DOUBLE ->
                AppFunctionDoubleTypeMetadata(isNullable = isNullable, description = description)

            AppFunctionDataTypeMetadata.TYPE_STRING ->
                AppFunctionStringTypeMetadata(
                    isNullable = isNullable,
                    description = description,
                    enumValues = enumValues.toSet().ifEmpty { null },
                )

            AppFunctionDataTypeMetadata.TYPE_BOOLEAN ->
                AppFunctionBooleanTypeMetadata(isNullable = isNullable, description = description)

            AppFunctionDataTypeMetadata.TYPE_BYTES ->
                AppFunctionBytesTypeMetadata(isNullable = isNullable, description = description)

            AppFunctionDataTypeMetadata.TYPE_UNIT ->
                AppFunctionUnitTypeMetadata(isNullable = isNullable, description = description)

            AppFunctionDataTypeMetadata.TYPE_PARCELABLE ->
                AppFunctionParcelableTypeMetadata(
                    isNullable = isNullable,
                    description = description,
                    qualifiedName = objectQualifiedName,
                )
            AppFunctionDataTypeMetadata.TYPE_ONE_OF ->
                AppFunctionOneOfTypeMetadata(
                    matchOneOf = oneOf.map { it.toAppFunctionDataTypeMetadata() },
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description,
                )
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
}

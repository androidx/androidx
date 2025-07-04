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

import androidx.annotation.IntDef

@IntDef(
    AppFunctionDataTypeMetadata.TYPE_UNIT,
    AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
    AppFunctionDataTypeMetadata.TYPE_BYTES,
    AppFunctionDataTypeMetadata.TYPE_DOUBLE,
    AppFunctionDataTypeMetadata.TYPE_FLOAT,
    AppFunctionDataTypeMetadata.TYPE_LONG,
    AppFunctionDataTypeMetadata.TYPE_INT,
    AppFunctionDataTypeMetadata.TYPE_STRING,
    AppFunctionDataTypeMetadata.TYPE_PENDING_INTENT,
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class AppFunctionPrimitiveType

abstract class AppFunctionDataTypeMetadata {
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
        internal const val TYPE_PENDING_INTENT: Int = 13

        internal val PRIMITIVE_TYPES =
            setOf(
                TYPE_UNIT,
                TYPE_BOOLEAN,
                TYPE_BYTES,
                TYPE_DOUBLE,
                TYPE_FLOAT,
                TYPE_LONG,
                TYPE_INT,
                TYPE_STRING,
                TYPE_PENDING_INTENT,
            )
    }
}

data class AppFunctionArrayTypeMetadata(
    val itemType: AppFunctionDataTypeMetadata,
    val isNullable: Boolean,
    val description: String,
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

data class AppFunctionAllOfTypeMetadata(
    val matchAll: List<AppFunctionDataTypeMetadata>,
    val qualifiedName: String?,
    val isNullable: Boolean,
    val description: String,
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

data class AppFunctionObjectTypeMetadata(
    val properties: Map<String, AppFunctionDataTypeMetadata>,
    val required: List<String>,
    val qualifiedName: String?,
    val isNullable: Boolean,
    val description: String,
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
    val isNullable: Boolean,
    val description: String,
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

data class AppFunctionPrimitiveTypeMetadata(
    @AppFunctionPrimitiveType val type: Int,
    val isNullable: Boolean,
    val description: String,
) : AppFunctionDataTypeMetadata() {
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = type,
            isNullable = isNullable,
            description = description,
        )
    }

    companion object {
        const val TYPE_UNIT: Int = AppFunctionDataTypeMetadata.TYPE_UNIT
        const val TYPE_BOOLEAN: Int = AppFunctionDataTypeMetadata.TYPE_BOOLEAN
        const val TYPE_BYTES: Int = AppFunctionDataTypeMetadata.TYPE_BYTES
        const val TYPE_DOUBLE: Int = AppFunctionDataTypeMetadata.TYPE_DOUBLE
        const val TYPE_FLOAT: Int = AppFunctionDataTypeMetadata.TYPE_FLOAT
        const val TYPE_LONG: Int = AppFunctionDataTypeMetadata.TYPE_LONG
        const val TYPE_INT: Int = AppFunctionDataTypeMetadata.TYPE_INT
        const val TYPE_STRING: Int = AppFunctionDataTypeMetadata.TYPE_STRING
        const val TYPE_PENDING_INTENT: Int = AppFunctionDataTypeMetadata.TYPE_PENDING_INTENT
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
    val required: List<String> = emptyList(),
    val dataTypeReference: String? = null,
    val isNullable: Boolean = false,
    val objectQualifiedName: String? = null,
    val description: String = "",
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
            in AppFunctionDataTypeMetadata.PRIMITIVE_TYPES ->
                AppFunctionPrimitiveTypeMetadata(
                    type = type,
                    isNullable = isNullable,
                    description = description,
                )
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
}

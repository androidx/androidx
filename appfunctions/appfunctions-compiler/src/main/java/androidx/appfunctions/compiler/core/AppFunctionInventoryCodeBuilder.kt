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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDeprecationMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionOneOfTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.APP_FUNCTION_METADATA_PROPERTY_NAME
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.DEPRECATION_METADATA_PROPERTY_NAME
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.FUNCTION_ID_TO_METADATA_MAP_PROPERTY_NAME
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.INVENTORY_COMPONENTS_METADATA_PROPERTY_NAME
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.PARAMETER_METADATA_LIST_PROPERTY_NAME
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.RESPONSE_METADATA_PROPERTY_NAME
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor.Companion.SCHEMA_METADATA_PROPERTY_NAME
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

/** The helper class to build AppFunctionInventory class. */
class AppFunctionInventoryCodeBuilder(private val inventoryClassBuilder: TypeSpec.Builder) {
    /**
     * Adds properties to the `AppFunctionInventory` class for the list of
     * [CompileTimeAppFunctionMetadata].
     *
     * @param appFunctionMetadataList The list of CompileTimeAppFunctionMetadata.
     */
    fun addFunctionMetadataProperties(
        appFunctionMetadataList: List<CompileTimeAppFunctionMetadata>
    ) {
        for (functionMetadata in appFunctionMetadataList) {
            val functionMetadataObjectClassBuilder =
                TypeSpec.objectBuilder(getFunctionMetadataObjectClassName(functionMetadata.id))
                    .addModifiers(KModifier.PRIVATE)
            addSchemaMetadataPropertyForFunction(
                functionMetadataObjectClassBuilder,
                functionMetadata.schema,
            )
            addPropertiesForParameterMetadataList(
                functionMetadataObjectClassBuilder,
                functionMetadata.parameters,
            )
            addPropertyForResponseMetadata(
                functionMetadataObjectClassBuilder,
                functionMetadata.response,
            )
            addDeprecationMetadataPropertyForFunction(
                functionMetadataObjectClassBuilder,
                functionMetadata.deprecation,
            )
            addPropertyForAppFunctionMetadata(functionMetadataObjectClassBuilder, functionMetadata)
            inventoryClassBuilder.addType(functionMetadataObjectClassBuilder.build())
        }
        addFunctionIdToMetadataMapProperty(inventoryClassBuilder, appFunctionMetadataList)
        addComponentsMetadataProperty(
            inventoryClassBuilder,
            AppFunctionComponentsMetadata(
                dataTypes =
                    appFunctionMetadataList
                        .map { it.components.dataTypes }
                        .reduce { acc, map -> acc + map }
            ),
        )
    }

    private fun addPropertyForAppFunctionMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        functionMetadata: CompileTimeAppFunctionMetadata,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    APP_FUNCTION_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_METADATA_CLASS,
                )
                .addModifiers(KModifier.PUBLIC)
                .initializer(
                    buildCodeBlock {
                        indent()
                        add("%T(\n", IntrospectionHelper.APP_FUNCTION_METADATA_CLASS)
                        indent()
                        add("id = %S,\n", functionMetadata.id)
                        add("isEnabledByDefault = %L,\n", functionMetadata.isEnabledByDefault)
                        add("schema =  %L,\n", SCHEMA_METADATA_PROPERTY_NAME)
                        add("parameters = %L,\n", PARAMETER_METADATA_LIST_PROPERTY_NAME)
                        add("response = %L,\n", RESPONSE_METADATA_PROPERTY_NAME)
                        if (functionMetadata.deprecation != null) {
                            add("deprecation = %L,\n", DEPRECATION_METADATA_PROPERTY_NAME)
                        }
                        unindent()
                        unindent()
                        add(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertyForComponentsDataTypes(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        dataTypes: Map<String, AppFunctionDataTypeMetadata>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asClassName(),
                            IntrospectionHelper.APP_FUNCTION_DATA_TYPE_METADATA,
                        ),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("mapOf(")
                        indent()
                        for ((componentReferenceKey, componentReferenceTypeMetadata) in
                            dataTypes.entries.sortedBy { it.key }) {
                            val datatypeVariableName =
                                when (componentReferenceTypeMetadata) {
                                    is AppFunctionObjectTypeMetadata -> {
                                        val objectTypeMetadataPropertyName =
                                            getObjectTypeMetadataPropertyNameForComponent(
                                                componentReferenceKey
                                            )
                                        addPropertyForObjectTypeMetadata(
                                            objectTypeMetadataPropertyName,
                                            functionMetadataObjectClassBuilder,
                                            componentReferenceTypeMetadata,
                                        )
                                        objectTypeMetadataPropertyName
                                    }
                                    is AppFunctionAllOfTypeMetadata -> {
                                        val allOfTypeMetadataPropertyName =
                                            getAllOfTypeMetadataPropertyNameForComponent(
                                                componentReferenceKey
                                            )
                                        addPropertyForAllOfTypeMetadata(
                                            allOfTypeMetadataPropertyName,
                                            functionMetadataObjectClassBuilder,
                                            componentReferenceTypeMetadata,
                                        )
                                        allOfTypeMetadataPropertyName
                                    }
                                    is AppFunctionOneOfTypeMetadata -> {
                                        val oneOfTypeMetadataPropertyName =
                                            getOneOfTypeMetadataPropertyNameForComponent(
                                                componentReferenceKey
                                            )
                                        addPropertyForOneOfTypeMetadata(
                                            oneOfTypeMetadataPropertyName,
                                            functionMetadataObjectClassBuilder,
                                            componentReferenceTypeMetadata,
                                        )
                                        oneOfTypeMetadataPropertyName
                                    }
                                    else -> {
                                        // TODO provide KSNode to improve error message
                                        throw ProcessingException(
                                            "Component types contains unsupported datatype: " +
                                                componentReferenceTypeMetadata,
                                            null,
                                        )
                                    }
                                }
                            addStatement(
                                """
                                %S to %L,
                                """
                                    .trimIndent(),
                                componentReferenceKey,
                                datatypeVariableName,
                            )
                        }
                        addStatement(")")
                        unindent()
                    }
                )
                .build()
        )
    }

    private fun addPropertyForResponseMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        appFunctionResponseMetadata: AppFunctionResponseMetadata,
    ) {
        val responseMetadataValueTypeName =
            when (val castDataType = appFunctionResponseMetadata.valueType) {
                is AppFunctionIntTypeMetadata,
                is AppFunctionLongTypeMetadata,
                is AppFunctionStringTypeMetadata,
                is AppFunctionBooleanTypeMetadata,
                is AppFunctionBytesTypeMetadata,
                is AppFunctionDoubleTypeMetadata,
                is AppFunctionFloatTypeMetadata,
                is AppFunctionUnitTypeMetadata -> {
                    val primitiveReturnTypeMetadataPropertyName = "PRIMITIVE_RESPONSE_VALUE_TYPE"
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    primitiveReturnTypeMetadataPropertyName
                }
                is AppFunctionArrayTypeMetadata -> {
                    val arrayReturnTypeMetadataPropertyName = "ARRAY_RESPONSE_VALUE_TYPE"
                    addPropertyForArrayTypeMetadata(
                        arrayReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    arrayReturnTypeMetadataPropertyName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectReturnTypeMetadataPropertyName = "OBJECT_RESPONSE_VALUE_TYPE"
                    addPropertyForObjectTypeMetadata(
                        objectReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    objectReturnTypeMetadataPropertyName
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val referenceReturnTypeMetadataPropertyName = "REFERENCE_RESPONSE_VALUE_TYPE"
                    addPropertyForReferenceTypeMetadata(
                        referenceReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    referenceReturnTypeMetadataPropertyName
                }
                is AppFunctionOneOfTypeMetadata -> {
                    val oneOfTypeReturnTypeMetadataPropertyName = "ONE_OF_RESPONSE_VALUE_TYPE"
                    addPropertyForOneOfTypeMetadata(
                        oneOfTypeReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    oneOfTypeReturnTypeMetadataPropertyName
                }
                is AppFunctionParcelableTypeMetadata -> {
                    val parcelableTypeReturnTypeMetadataPropertyName =
                        "PARCELABLE_RESPONSE_VALUE_TYPE"
                    addPropertyForParcelableTypeMetadata(
                        parcelableTypeReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    parcelableTypeReturnTypeMetadataPropertyName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter metadata for unknown datatype: $castDataType",
                        null,
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    RESPONSE_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_RESPONSE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                valueType = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_RESPONSE_METADATA_CLASS,
                            responseMetadataValueTypeName,
                            appFunctionResponseMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForParcelableTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        parcelableTypeMetadata: AppFunctionParcelableTypeMetadata,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_PARCELABLE_TYPE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                qualifiedName = %S,
                                isNullable = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_PARCELABLE_TYPE_METADATA_CLASS,
                            parcelableTypeMetadata.qualifiedName,
                            parcelableTypeMetadata.isNullable,
                            parcelableTypeMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForOneOfTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        oneOfTypeMetadata: AppFunctionOneOfTypeMetadata,
    ) {
        val matchOneOfListPropertyName = propertyName + "_MATCH_ONE_OF_LIST"
        addPropertyForMatchOneOfList(
            matchOneOfListPropertyName,
            functionMetadataObjectClassBuilder,
            oneOfTypeMetadata.matchOneOf,
        )
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_ONE_OF_TYPE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                matchOneOf = %L,
                                qualifiedName = %S,
                                isNullable = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_ONE_OF_TYPE_METADATA_CLASS,
                            matchOneOfListPropertyName,
                            oneOfTypeMetadata.qualifiedName,
                            oneOfTypeMetadata.isNullable,
                            oneOfTypeMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForMatchOneOfList(
        matchOneOfListPropertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        matchOneOf: List<AppFunctionDataTypeMetadata>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    matchOneOfListPropertyName,
                    List::class.asClassName()
                        .parameterizedBy(IntrospectionHelper.APP_FUNCTION_DATA_TYPE_METADATA),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for ((index, dataTypeToMatch) in matchOneOf.withIndex()) {
                            val dataTypeToMatchPropertyName =
                                matchOneOfListPropertyName + "_ITEM_${index}"
                            addPropertyForDataTypeToMatch(
                                dataTypeToMatchPropertyName,
                                functionMetadataObjectClassBuilder,
                                dataTypeToMatch,
                            )
                            addStatement("%L,", dataTypeToMatchPropertyName)
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertiesForParameterMetadataList(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        parameterMetadataList: List<AppFunctionParameterMetadata>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    PARAMETER_METADATA_LIST_PROPERTY_NAME,
                    List::class.asClassName()
                        .parameterizedBy(IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for (parameterMetadata in parameterMetadataList) {
                            addPropertiesForParameterMetadata(
                                parameterMetadata,
                                functionMetadataObjectClassBuilder,
                            )
                            addStatement(
                                "%L,",
                                "${parameterMetadata.name.uppercase()}_PARAMETER_METADATA",
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertiesForParameterMetadata(
        parameterMetadata: AppFunctionParameterMetadata,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
    ) {
        val parameterMetadataPropertyName =
            "${parameterMetadata.name.uppercase()}_PARAMETER_METADATA"
        val datatypeVariableName =
            when (val castDataType = parameterMetadata.dataType) {
                is AppFunctionIntTypeMetadata,
                is AppFunctionLongTypeMetadata,
                is AppFunctionStringTypeMetadata,
                is AppFunctionBooleanTypeMetadata,
                is AppFunctionBytesTypeMetadata,
                is AppFunctionDoubleTypeMetadata,
                is AppFunctionFloatTypeMetadata,
                is AppFunctionUnitTypeMetadata -> {
                    val primitiveTypeMetadataPropertyName =
                        getPrimitiveTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    primitiveTypeMetadataPropertyName
                }
                is AppFunctionArrayTypeMetadata -> {
                    val arrayTypeMetadataPropertyName =
                        getArrayTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForArrayTypeMetadata(
                        arrayTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    arrayTypeMetadataPropertyName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectTypeMetadataPropertyName =
                        getObjectTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForObjectTypeMetadata(
                        objectTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    objectTypeMetadataPropertyName
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val referenceTypeMetadataPropertyName =
                        getReferenceTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForReferenceTypeMetadata(
                        referenceTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    referenceTypeMetadataPropertyName
                }
                is AppFunctionParcelableTypeMetadata -> {
                    val parcelableTypeReturnTypeMetadataPropertyName =
                        getParcelableTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForParcelableTypeMetadata(
                        parcelableTypeReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType,
                    )
                    parcelableTypeReturnTypeMetadataPropertyName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter metadata for unknown datatype: $castDataType",
                        null,
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    parameterMetadataPropertyName,
                    IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                name = %S,
                                isRequired = %L,
                                dataType = %L,
                                description = %S,
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS,
                            parameterMetadata.name,
                            parameterMetadata.isRequired,
                            datatypeVariableName,
                            parameterMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForPrimitiveTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        primitiveTypeMetadata: AppFunctionDataTypeMetadata,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            primitiveTypeMetadata.toPrimitiveMetadataPropertySpec(propertyName)
        )
    }

    private fun AppFunctionDataTypeMetadata.toPrimitiveMetadataPropertySpec(
        propertyName: String
    ): PropertySpec {
        return when (this) {
            is AppFunctionIntTypeMetadata ->
                PropertySpec.builder(
                        propertyName,
                        IntrospectionHelper.APP_FUNCTION_INT_TYPE_METADATA_CLASS,
                    )
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(
                        buildCodeBlock {
                            addStatement(
                                """
                                %T(
                                    isNullable = %L,
                                    description = %S,
                                    enumValues = %L,
                                )
                                """
                                    .trimIndent(),
                                IntrospectionHelper.APP_FUNCTION_INT_TYPE_METADATA_CLASS,
                                isNullable,
                                description,
                                enumValues?.joinToString(prefix = "setOf(", postfix = ")"),
                            )
                        }
                    )
                    .build()

            is AppFunctionStringTypeMetadata ->
                PropertySpec.builder(
                        propertyName,
                        IntrospectionHelper.APP_FUNCTION_STRING_TYPE_METADATA_CLASS,
                    )
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(
                        buildCodeBlock {
                            addStatement(
                                """
                                %T(
                                    isNullable = %L,
                                    description = %S,
                                    enumValues = %L,
                                )
                                """
                                    .trimIndent(),
                                IntrospectionHelper.APP_FUNCTION_STRING_TYPE_METADATA_CLASS,
                                isNullable,
                                description,
                                enumValues?.joinToString(
                                    prefix = "setOf(",
                                    postfix = ")",
                                    transform = { "\"$it\"" },
                                ),
                            )
                        }
                    )
                    .build()

            else ->
                PropertySpec.builder(propertyName, toMetadataClassName())
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(
                        buildCodeBlock {
                            addStatement(
                                """
                                %T(
                                    isNullable = %L,
                                    description = %S
                                )
                                """
                                    .trimIndent(),
                                toMetadataClassName(),
                                isNullable,
                                description,
                            )
                        }
                    )
                    .build()
        }
    }

    /**
     * Maps [AppFunctionDataTypeMetadata] to the corresponding metadata class names defined in app
     * functions metadata package.
     */
    private fun AppFunctionDataTypeMetadata.toMetadataClassName(): ClassName =
        when (this) {
            is AppFunctionIntTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_INT_TYPE_METADATA_CLASS
            is AppFunctionBooleanTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_BOOLEAN_TYPE_METADATA_CLASS
            is AppFunctionBytesTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_BYTES_TYPE_METADATA_CLASS
            is AppFunctionDoubleTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_DOUBLE_TYPE_METADATA_CLASS
            is AppFunctionFloatTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_FLOAT_TYPE_METADATA_CLASS
            is AppFunctionLongTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_LONG_TYPE_METADATA_CLASS
            is AppFunctionStringTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_STRING_TYPE_METADATA_CLASS
            is AppFunctionUnitTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_UNIT_TYPE_METADATA_CLASS
            is AppFunctionParcelableTypeMetadata ->
                IntrospectionHelper.APP_FUNCTION_PARCELABLE_TYPE_METADATA_CLASS
            else ->
                throw IllegalArgumentException(
                    "Unsupported or non-primitive type in AppFunctionDataTypeMetadata: ${this::class.simpleName}"
                )
        }

    private fun addPropertyForArrayTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        arrayTypeMetadata: AppFunctionArrayTypeMetadata,
    ) {
        val itemTypeVariableName =
            when (val castItemType = arrayTypeMetadata.itemType) {
                is AppFunctionIntTypeMetadata,
                is AppFunctionLongTypeMetadata,
                is AppFunctionStringTypeMetadata,
                is AppFunctionBooleanTypeMetadata,
                is AppFunctionBytesTypeMetadata,
                is AppFunctionDoubleTypeMetadata,
                is AppFunctionFloatTypeMetadata,
                is AppFunctionUnitTypeMetadata -> {
                    val primitiveItemTypeVariableName = propertyName + "_PRIMITIVE_ITEM_TYPE"
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType,
                    )
                    primitiveItemTypeVariableName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectItemTypeVariableName = propertyName + "_OBJECT_ITEM_TYPE"
                    addPropertyForObjectTypeMetadata(
                        objectItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType,
                    )
                    objectItemTypeVariableName
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val referenceItemTypeVariableName = propertyName + "_REFERENCE_ITEM_TYPE"
                    addPropertyForReferenceTypeMetadata(
                        referenceItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType,
                    )
                    referenceItemTypeVariableName
                }

                is AppFunctionParcelableTypeMetadata -> {
                    val parcelableItemTypeVariableName = propertyName + "_PARCELABLE_ITEM_TYPE"
                    addPropertyForParcelableTypeMetadata(
                        parcelableItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType,
                    )
                    parcelableItemTypeVariableName
                }

                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter item type metadata for unknown itemType: " +
                            "$castItemType",
                        null,
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                itemType = %L,
                                isNullable = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS,
                            itemTypeVariableName,
                            arrayTypeMetadata.isNullable,
                            arrayTypeMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForReferenceTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        referenceTypeMetadata: AppFunctionReferenceTypeMetadata,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_REFERENCE_TYPE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                referenceDataType = %S,
                                isNullable = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_REFERENCE_TYPE_METADATA_CLASS,
                            referenceTypeMetadata.referenceDataType,
                            referenceTypeMetadata.isNullable,
                            referenceTypeMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForObjectTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        objectTypeMetadata: AppFunctionObjectTypeMetadata,
    ) {
        val objectPropertiesMapPropertyName = propertyName + "_PROPERTIES_MAP"
        addPropertyForObjectPropertiesMap(
            objectPropertiesMapPropertyName,
            functionMetadataObjectClassBuilder,
            objectTypeMetadata.properties,
        )
        val requiredPropertiesListPropertyName = propertyName + "_REQUIRED_PROPERTIES_LIST"
        addPropertyForListOfRequiredObjectProperties(
            requiredPropertiesListPropertyName,
            functionMetadataObjectClassBuilder,
            objectTypeMetadata.required,
        )
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                properties = %L,
                                required = %L,
                                qualifiedName = %S,
                                isNullable = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS,
                            objectPropertiesMapPropertyName,
                            requiredPropertiesListPropertyName,
                            objectTypeMetadata.qualifiedName,
                            objectTypeMetadata.isNullable,
                            objectTypeMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForAllOfTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        allOfTypeMetadata: AppFunctionAllOfTypeMetadata,
    ) {
        val matchAllListPropertyName = propertyName + "_MATCH_ALL_LIST"
        addPropertyForMatchAllList(
            matchAllListPropertyName,
            functionMetadataObjectClassBuilder,
            allOfTypeMetadata.matchAll,
        )
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_ALL_OF_TYPE_METADATA_CLASS,
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                matchAll = %L,
                                qualifiedName = %S,
                                isNullable = %L,
                                description = %S
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_ALL_OF_TYPE_METADATA_CLASS,
                            matchAllListPropertyName,
                            allOfTypeMetadata.qualifiedName,
                            allOfTypeMetadata.isNullable,
                            allOfTypeMetadata.description,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForListOfRequiredObjectProperties(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        requiredProperties: List<String>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    List::class.asClassName().parameterizedBy(String::class.asClassName()),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for (requiredProperty in requiredProperties) {
                            addStatement("%S,", requiredProperty)
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertyForObjectPropertiesMap(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        propertiesMap: Map<String, AppFunctionDataTypeMetadata>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asClassName(),
                            IntrospectionHelper.APP_FUNCTION_DATA_TYPE_METADATA,
                        ),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("mapOf(")
                        indent()
                        for ((objectPropertyName, objectPropertyTypeMetadata) in
                            propertiesMap.entries.sortedBy { it.key }) {
                            val dataTypeVariableName =
                                propertyName + "_${objectPropertyName.uppercase()}"
                            when (objectPropertyTypeMetadata) {
                                is AppFunctionIntTypeMetadata,
                                is AppFunctionLongTypeMetadata,
                                is AppFunctionStringTypeMetadata,
                                is AppFunctionBooleanTypeMetadata,
                                is AppFunctionBytesTypeMetadata,
                                is AppFunctionDoubleTypeMetadata,
                                is AppFunctionFloatTypeMetadata,
                                is AppFunctionUnitTypeMetadata ->
                                    addPropertyForPrimitiveTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata,
                                    )
                                is AppFunctionArrayTypeMetadata ->
                                    addPropertyForArrayTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata,
                                    )
                                is AppFunctionObjectTypeMetadata ->
                                    addPropertyForObjectTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata,
                                    )
                                is AppFunctionReferenceTypeMetadata ->
                                    addPropertyForReferenceTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata,
                                    )
                                is AppFunctionParcelableTypeMetadata ->
                                    addPropertyForParcelableTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata,
                                    )
                                else -> {
                                    // TODO provide KSNode to improve error message
                                    throw ProcessingException(
                                        "Unable to build metadata for unknown object property " +
                                            "datatype: $objectPropertyTypeMetadata",
                                        null,
                                    )
                                }
                            }
                            addStatement(
                                """
                                %S to %L,
                                """
                                    .trimIndent(),
                                objectPropertyName,
                                dataTypeVariableName,
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertyForMatchAllList(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        matchAllList: List<AppFunctionDataTypeMetadata>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    List::class.asClassName()
                        .parameterizedBy(IntrospectionHelper.APP_FUNCTION_DATA_TYPE_METADATA),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for ((index, dataTypeToMatch) in matchAllList.withIndex()) {
                            val dataTypeToMatchPropertyName = propertyName + "_ITEM_${index}"
                            addPropertyForDataTypeToMatch(
                                dataTypeToMatchPropertyName,
                                functionMetadataObjectClassBuilder,
                                dataTypeToMatch,
                            )
                            addStatement("%L,", dataTypeToMatchPropertyName)
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertyForDataTypeToMatch(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        dataTypeToMatch: AppFunctionDataTypeMetadata,
    ) {
        when (dataTypeToMatch) {
            is AppFunctionReferenceTypeMetadata ->
                addPropertyForReferenceTypeMetadata(
                    propertyName,
                    functionMetadataObjectClassBuilder,
                    dataTypeToMatch,
                )
            is AppFunctionObjectTypeMetadata ->
                addPropertyForObjectTypeMetadata(
                    propertyName,
                    functionMetadataObjectClassBuilder,
                    dataTypeToMatch,
                )
            else ->
                // TODO provide KSNode to improve error message
                throw ProcessingException(
                    "Invalid datatype metadata to match in allOf type. Only object and reference " +
                        "types are supported: $dataTypeToMatch",
                    null,
                )
        }
    }

    private fun addComponentsMetadataProperty(
        inventoryClassBuilder: TypeSpec.Builder,
        componentsMetadata: AppFunctionComponentsMetadata,
    ) {
        val dataTypesMapPropertyName = INVENTORY_COMPONENTS_METADATA_PROPERTY_NAME + "DataTypesMap"
        addPropertyForComponentsDataTypes(
            propertyName = dataTypesMapPropertyName,
            functionMetadataObjectClassBuilder = inventoryClassBuilder,
            dataTypes = componentsMetadata.dataTypes,
        )
        inventoryClassBuilder.addProperty(
            PropertySpec.builder(
                    INVENTORY_COMPONENTS_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_COMPONENTS_METADATA_CLASS,
                )
                .addModifiers(KModifier.OVERRIDE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                dataTypes = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_COMPONENTS_METADATA_CLASS,
                            dataTypesMapPropertyName,
                        )
                    }
                )
                .build()
        )
    }

    /** Creates the `functionIdToMetadataMap` property of the `AppFunctionInventory`. */
    private fun addFunctionIdToMetadataMapProperty(
        inventoryClassBuilder: TypeSpec.Builder,
        appFunctionMetadataList: List<CompileTimeAppFunctionMetadata>,
    ) {
        inventoryClassBuilder.addProperty(
            PropertySpec.builder(
                    FUNCTION_ID_TO_METADATA_MAP_PROPERTY_NAME,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asClassName(),
                            IntrospectionHelper.APP_FUNCTION_METADATA_CLASS,
                        ),
                )
                .addModifiers(KModifier.OVERRIDE)
                .initializer(
                    buildCodeBlock {
                        addStatement("mapOf(")
                        indent()
                        for (appFunctionMetadata in appFunctionMetadataList) {
                            addStatement(
                                """
                                %S to %L.%L,
                                """
                                    .trimIndent(),
                                appFunctionMetadata.id,
                                getFunctionMetadataObjectClassName(appFunctionMetadata.id),
                                APP_FUNCTION_METADATA_PROPERTY_NAME,
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addSchemaMetadataPropertyForFunction(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        schemaMetadata: AppFunctionSchemaMetadata?,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    SCHEMA_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_SCHEMA_METADATA_CLASS.copy(nullable = true),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        if (schemaMetadata == null) {
                            addStatement("%L", null)
                        } else {
                            addStatement(
                                "%T(category= %S, name=%S, version=%L)",
                                IntrospectionHelper.APP_FUNCTION_SCHEMA_METADATA_CLASS,
                                schemaMetadata.category,
                                schemaMetadata.name,
                                schemaMetadata.version,
                            )
                        }
                    }
                )
                .build()
        )
    }

    private fun addDeprecationMetadataPropertyForFunction(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        deprecationMetadata: AppFunctionDeprecationMetadata?,
    ) {
        if (deprecationMetadata == null) return
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    DEPRECATION_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_DEPRECATION_METADATA_CLASS.copy(
                        nullable = true
                    ),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            "%T(message= %S)",
                            IntrospectionHelper.APP_FUNCTION_DEPRECATION_METADATA_CLASS,
                            deprecationMetadata.message,
                        )
                    }
                )
                .build()
        )
    }

    /**
     * Generates the name of the class for the metadata object of a function.
     *
     * @param functionId The ID of the function.
     * @return The name of the class.
     */
    private fun getFunctionMetadataObjectClassName(functionId: String): String {
        return functionId.replace("[^A-Za-z0-9]".toRegex(), "_").split("_").joinToString("") {
            it.replaceFirstChar { it.uppercase() }
        } + "MetadataObject"
    }

    /**
     * Generates the name of the property for the primitive type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getPrimitiveTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_PRIMITIVE_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the array type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getArrayTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_ARRAY_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the object type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getObjectTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_OBJECT_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the reference type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getReferenceTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_REFERENCE_DATA_TYPE"
    }

    private fun getParcelableTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_PARCELABLE_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the object type metadata of a component.
     *
     * @param componentName The name of the component.
     * @return The name of the property.
     */
    private fun getObjectTypeMetadataPropertyNameForComponent(componentName: String): String {
        return "${componentName.uppercase().replace(Regex("[.<>$]"), "_").replace("?", "_NULLABLE")}_OBJECT_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the all of type metadata of a component.
     *
     * @param componentName The name of the component.
     * @return The name of the property.
     */
    private fun getAllOfTypeMetadataPropertyNameForComponent(componentName: String): String {
        return "${componentName.uppercase().replace(Regex("[.<>$]"), "_")}_ALL_OF_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the one of type metadata of a component.
     *
     * @param componentName The name of the component.
     * @return The name of the property.
     */
    private fun getOneOfTypeMetadataPropertyNameForComponent(componentName: String): String {
        return "${componentName.uppercase().replace(Regex("[.<>$]"), "_")}_ONE_OF_DATA_TYPE"
    }
}

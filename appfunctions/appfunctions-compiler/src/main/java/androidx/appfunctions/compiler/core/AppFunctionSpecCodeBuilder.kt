/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionParameterSpecClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionResponseSpecClass
import androidx.appfunctions.compiler.core.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionUnitTypeMetadata
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

/** The helper class to build AppFunction parameter and response specs. */
class AppFunctionSpecCodeBuilder {

    internal fun addPropertyForResponseSpec(
        classBuilder: TypeSpec.Builder,
        responseMetadata: AppFunctionResponseMetadata,
        annotationSpecs: List<AnnotationSpec> = emptyList(),
    ) {
        val details = getTypeSpecDetails(responseMetadata.valueType)
        classBuilder.addProperty(
            PropertySpec.builder("RESPONSE_SPEC", AppFunctionResponseSpecClass.CLASS_NAME)
                .addModifiers(KModifier.PRIVATE)
                .addAnnotations(annotationSpecs)
                .initializer(
                    buildCodeBlock {
                        addStatement("%T(", AppFunctionResponseSpecClass.CLASS_NAME)
                        indent()
                        addStatement(
                            "%L = %L,",
                            AppFunctionResponseSpecClass.PROPERTY_TYPE,
                            details.typeCodeBlock,
                        )
                        addStatement(
                            "%L = %L,",
                            AppFunctionResponseSpecClass.PROPERTY_IS_NULLABLE,
                            responseMetadata.valueType.isNullable,
                        )
                        if (details.objectQualifiedName != null) {
                            addStatement(
                                "%L = %S,",
                                AppFunctionResponseSpecClass.PROPERTY_OBJECT_QUALIFIED_NAME,
                                details.objectQualifiedName,
                            )
                        }
                        if (details.itemTypeCodeBlock != null) {
                            addStatement(
                                "%L = %L,",
                                AppFunctionResponseSpecClass.PROPERTY_ITEM_TYPE,
                                details.itemTypeCodeBlock,
                            )
                        }
                        if (details.itemQualifiedName != null) {
                            addStatement(
                                "%L = %S,",
                                AppFunctionResponseSpecClass.PROPERTY_ITEM_QUALIFIED_NAME,
                                details.itemQualifiedName,
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    internal fun addPropertiesForParameterSpec(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        parameterMetadataList: List<AppFunctionParameterMetadata>,
        propertyModifiers: List<KModifier> = listOf(KModifier.PRIVATE),
        annotationSpecs: List<AnnotationSpec> = emptyList(),
    ) {
        for (parameterMetadata in parameterMetadataList) {
            addPropertyForParameterSpec(
                parameterMetadata,
                functionMetadataObjectClassBuilder,
                propertyModifiers,
                annotationSpecs,
            )
        }
    }

    internal data class TypeSpecDetails(
        val typeCodeBlock: CodeBlock,
        val objectQualifiedName: String? = null,
        val itemTypeCodeBlock: CodeBlock? = null,
        val itemQualifiedName: String? = null,
    )

    internal fun getTypeSpecDetails(dataType: AppFunctionDataTypeMetadata): TypeSpecDetails {
        return when (dataType) {
            is AppFunctionIntTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_INT)
                )
            is AppFunctionLongTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_LONG)
                )
            is AppFunctionStringTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_STRING)
                )
            is AppFunctionBooleanTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_BOOLEAN)
                )
            is AppFunctionBytesTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_BYTES)
                )
            is AppFunctionDoubleTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_DOUBLE)
                )
            is AppFunctionFloatTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_FLOAT)
                )
            is AppFunctionUnitTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_UNIT)
                )
            is AppFunctionObjectTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_OBJECT),
                    objectQualifiedName = checkNotNull(dataType.qualifiedName),
                )
            is AppFunctionReferenceTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_OBJECT),
                    objectQualifiedName = checkNotNull(dataType.referenceDataType),
                )
            is AppFunctionParcelableTypeMetadata ->
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_PARCELABLE),
                    objectQualifiedName = checkNotNull(dataType.qualifiedName),
                )
            is AppFunctionArrayTypeMetadata -> {
                val itemDetails = getTypeSpecDetails(dataType.itemType)
                TypeSpecDetails(
                    typeCodeBlock = CodeBlock.of("%L", AppFunctionDataTypeMetadata.TYPE_ARRAY),
                    itemTypeCodeBlock = itemDetails.typeCodeBlock,
                    itemQualifiedName = itemDetails.objectQualifiedName,
                )
            }
            else ->
                throw ProcessingException(
                    "Unsupported data type for parameter spec: $dataType",
                    null,
                )
        }
    }

    internal fun addPropertyForParameterSpec(
        parameterMetadata: AppFunctionParameterMetadata,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        propertyModifiers: List<KModifier>,
        annotationSpecs: List<AnnotationSpec> = emptyList(),
    ) {
        val parameterSpecPropertyName = "${parameterMetadata.name.uppercase()}_PARAMETER_SPEC"
        val details = getTypeSpecDetails(parameterMetadata.dataType)

        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    parameterSpecPropertyName,
                    AppFunctionParameterSpecClass.CLASS_NAME,
                )
                .addModifiers(propertyModifiers)
                .addAnnotations(annotationSpecs)
                .initializer(
                    buildCodeBlock {
                        addStatement("%T(", AppFunctionParameterSpecClass.CLASS_NAME)
                        indent()
                        addStatement(
                            "%L = %L,",
                            AppFunctionParameterSpecClass.PROPERTY_TYPE,
                            details.typeCodeBlock,
                        )
                        addStatement(
                            "%L = %S,",
                            AppFunctionParameterSpecClass.PROPERTY_NAME,
                            parameterMetadata.name,
                        )
                        addStatement(
                            "%L = %L,",
                            AppFunctionParameterSpecClass.PROPERTY_IS_REQUIRED,
                            parameterMetadata.isRequired,
                        )
                        addStatement(
                            "%L = %L,",
                            AppFunctionParameterSpecClass.PROPERTY_IS_NULLABLE,
                            parameterMetadata.dataType.isNullable,
                        )
                        if (details.objectQualifiedName != null) {
                            addStatement(
                                "%L = %S,",
                                AppFunctionParameterSpecClass.PROPERTY_OBJECT_QUALIFIED_NAME,
                                details.objectQualifiedName,
                            )
                        }
                        if (details.itemTypeCodeBlock != null) {
                            addStatement(
                                "%L = %L,",
                                AppFunctionParameterSpecClass.PROPERTY_ITEM_TYPE,
                                details.itemTypeCodeBlock,
                            )
                        }
                        if (details.itemQualifiedName != null) {
                            addStatement(
                                "%L = %S,",
                                AppFunctionParameterSpecClass.PROPERTY_ITEM_QUALIFIED_NAME,
                                details.itemQualifiedName,
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    companion object {
        fun getGeneratedClassName(functionClassName: String): String {
            return "$${functionClassName}_AppFunctionAdapter"
        }
    }
}

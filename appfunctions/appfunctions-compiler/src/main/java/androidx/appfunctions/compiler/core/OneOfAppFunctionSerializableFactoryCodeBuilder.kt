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

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionDataClass.APP_FUNCTION_DATA_QUALIFIED_NAME_PROPERTY
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.APP_FUNCTION_DATA_PARAM_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.ToAppFunctionDataMethod.APP_FUNCTION_SERIALIZABLE_PARAM_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.RESTRICT_API_TO_33_ANNOTATION
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.buildFromAppFunctionDataFunction
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.buildToAppFunctionDataFunction
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

class OneOfAppFunctionSerializableFactoryCodeBuilder(
    private val oneOfClass: AnnotatedOneOfAppFunctionSerializable
) : AppFunctionSerializableType.FactoryCodeBuilder {

    fun generateFromAppFunctionDataMethodBody() = buildCodeBlock {
        beginControlFlow(
            """
            return when(%L.%L)
            """
                .trimIndent(),
            APP_FUNCTION_DATA_PARAM_NAME,
            APP_FUNCTION_DATA_QUALIFIED_NAME_PROPERTY,
        )
        for (oneOfSerializable in oneOfClass.oneOfSerializables) {
            beginControlFlow(
                "%T::class.java.name -> ",
                oneOfSerializable.appFunctionSerializableTypeClassDeclaration.originalClassName,
            )
            addFactoryInitStatement(oneOfSerializable)
            val formatStringMap =
                mapOf<String, Any>(
                    "factory_variable_name" to oneOfSerializable.factoryVariableName,
                    "from_app_function_data_method" to
                        AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.METHOD_NAME,
                    "app_function_data_variable" to APP_FUNCTION_DATA_PARAM_NAME,
                )
            addNamed(
                "%factory_variable_name:L.%from_app_function_data_method:L(%app_function_data_variable:L)",
                formatStringMap,
            )
            endControlFlow()
        }
        addStatement(
            "else -> throw %T(%P)",
            IllegalArgumentException::class,
            "Unknown qualifiedName: \${appFunctionData.qualifiedName}",
        )
        endControlFlow()
    }

    private fun CodeBlock.Builder.addFactoryInitStatement(
        serializable: AppFunctionSerializableType
    ) {
        addStatement(
            "val %L = %T()",
            serializable.factoryVariableName,
            serializable.factoryClassName,
        )
    }

    fun generateToAppFunctionDataMethodBody() = buildCodeBlock {
        beginControlFlow(
            """
            return when(%L)
            """
                .trimIndent(),
            APP_FUNCTION_SERIALIZABLE_PARAM_NAME,
        )
        for (oneOfSerializable in oneOfClass.oneOfSerializables) {
            beginControlFlow(
                "is %T -> ",
                oneOfSerializable.appFunctionSerializableTypeClassDeclaration.originalClassName,
            )
            addFactoryInitStatement(oneOfSerializable)
            val formatStringMap =
                mapOf<String, Any>(
                    "factory_variable_name" to oneOfSerializable.factoryVariableName,
                    "to_app_function_data_method" to
                        AppFunctionSerializableFactoryClass.ToAppFunctionDataMethod.METHOD_NAME,
                    "serializable_variable_name" to APP_FUNCTION_SERIALIZABLE_PARAM_NAME,
                )
            addNamed(
                "%factory_variable_name:L.%to_app_function_data_method:L(%serializable_variable_name:L)",
                formatStringMap,
            )
            endControlFlow()
        }
        endControlFlow()
    }

    override fun buildAppFunctionSerializableFactoryClass(): FileSpec {
        val superInterfaceClass =
            AppFunctionSerializableFactoryClass.CLASS_NAME.parameterizedBy(
                listOf(oneOfClass.appFunctionSerializableTypeClassDeclaration.typeName)
            )
        val generatedFactoryClassName = oneOfClass.factoryClassName.simpleName
        return FileSpec.builder(
                oneOfClass.appFunctionSerializableTypeClassDeclaration.originalClassName
                    .packageName,
                generatedFactoryClassName,
            )
            .addType(
                TypeSpec.classBuilder(generatedFactoryClassName)
                    .addAnnotation(RESTRICT_API_TO_33_ANNOTATION)
                    .addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
                    .addSuperinterface(superInterfaceClass)
                    .apply {
                        if (
                            oneOfClass.appFunctionSerializableTypeClassDeclaration.modifiers
                                .contains(Modifier.INTERNAL)
                        ) {
                            addModifiers(KModifier.INTERNAL)
                        }
                    }
                    .addFunction(
                        buildFromAppFunctionDataFunction(
                            generateFromAppFunctionDataMethodBody(),
                            returnType =
                                oneOfClass.appFunctionSerializableTypeClassDeclaration.typeName,
                        )
                    )
                    .addFunction(
                        buildToAppFunctionDataFunction(
                            generateToAppFunctionDataMethodBody(),
                            parameterType =
                                oneOfClass.appFunctionSerializableTypeClassDeclaration.typeName,
                        )
                    )
                    .build()
            )
            .build()
    }
}

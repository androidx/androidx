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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializable
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.SUPPORTED_ARRAY_PRIMITIVE_TYPES
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.isAppFunctionSerializableType
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.isSupportedType
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.APP_FUNCTION_DATA_PARAM_NAME
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.ensureQualifiedTypeName
import androidx.appfunctions.compiler.core.isOfType
import androidx.appfunctions.compiler.core.resolveListParameterizedType
import androidx.appfunctions.compiler.core.toTypeName
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.buildCodeBlock
import kotlin.text.replaceFirstChar

/**
 * Wraps methods to build the [CodeBlock]s that make up the method bodies of the generated
 * AppFunctionSerializableFactory.
 */
class AppFunctionSerializableFactoryCodeBuilder(
    val annotatedClass: AnnotatedAppFunctionSerializable
) {
    /** Builds and appends the method body of fromAppFunctionData to the given code block. */
    fun appendFromAppFunctionDataMethodBody(): CodeBlock {
        return buildCodeBlock {
            add(factoryInitStatements)
            for (property in annotatedClass.getProperties()) {
                appendGetterStatement(property)
            }
            appendGetterReturnStatement(
                annotatedClass.originalClassName,
                annotatedClass.getProperties()
            )
        }
    }

    /** Builds and appends the method body of fromAppFunctionData to the given code block. */
    fun appendToAppFunctionDataMethodBody(): CodeBlock {
        return buildCodeBlock {
            add(factoryInitStatements)
            val qualifiedClassName =
                checkNotNull(annotatedClass.appFunctionSerializableClass.qualifiedName).asString()
            addStatement("val builder = %T(%S)", AppFunctionData.Builder::class, qualifiedClassName)
            for (property in annotatedClass.getProperties()) {
                appendSetterStatement(property)
            }
            add("\nreturn builder.build()")
        }
    }

    private fun CodeBlock.Builder.appendGetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        if (!isSupportedType(param.type)) {
            throw ProcessingException(
                "Unsupported parameter type ${param.type.toTypeName()}",
                param
            )
        }

        if (isAppFunctionSerializableType(param.type)) {
            return if (param.type.isOfType(LIST)) {
                appendSerializableListGetterStatement(param)
            } else {
                appendSerializableGetterStatement(param)
            }
        }
        return appendPrimitiveGetterStatement(param)
    }

    private fun CodeBlock.Builder.appendPrimitiveGetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val getterName =
            if (param.type.isStringList()) "getStringList"
            else "get${param.type.ensureQualifiedTypeName().getShortName()}"
        val defaultValuePostfix =
            if (param.type.isStringList()) {
                " ?: emptyList()"
            } else {
                if (param.type.isPrimitiveArray()) {
                    " ?: ${param.type.ensureQualifiedTypeName().getShortName()}(0)"
                } else {
                    ""
                }
            }
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to getterName,
                "default_value_postfix" to defaultValuePostfix
            )
        addNamed(
            "val %param_name:L = %app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableGetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val typeName = param.type.ensureQualifiedTypeName().getShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "factory_name" to "${typeName}Factory".lowerFirstChar(),
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to "getAppFunctionData",
                "from_app_function_data_method_name" to FromAppFunctionDataMethod.METHOD_NAME
            )

        addNamed(
            "val %param_name:L = %factory_name:L.%from_app_function_data_method_name:L(" +
                "%app_function_data_param_name:L.%getter_name:L(\"%param_name:L\"))\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableListGetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val parametrizedTypeName =
            param.type.resolveListParameterizedType().ensureQualifiedTypeName().getShortName()
        val factoryName = parametrizedTypeName + "Factory"
        val factoryInstanceName = factoryName.lowerFirstChar()

        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "temp_list_name" to checkNotNull(param.name).asString() + "Data",
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to "getAppFunctionDataList",
                "default_value_postfix" to " ?: emptyList()"
            )

        addNamed(
                "val %temp_list_name:L = %app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L\n",
                formatStringMap
            )
            .addNamed(
                "val %param_name:L = %temp_list_name:L.map {data -> ${factoryInstanceName}.fromAppFunctionData(data)}\n",
                formatStringMap
            )
        return this
    }

    private fun CodeBlock.Builder.appendGetterReturnStatement(
        originalClassName: ClassName,
        params: List<KSValueParameter>
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "original_class_name" to originalClassName,
                "params_list" to
                    params.joinToString(", ") { param -> checkNotNull(param.name).asString() }
            )

        addNamed("\nreturn %original_class_name:T(%params_list:L)", formatStringMap)
        return this
    }

    private fun CodeBlock.Builder.appendSetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        if (!isSupportedType(param.type)) {
            throw ProcessingException(
                "Unsupported parameter type ${param.type.toTypeName()}",
                param
            )
        }
        if (isAppFunctionSerializableType(param.type)) {
            return if (param.type.isOfType(LIST)) {
                appendSerializableListSetterStatement(param)
            } else {
                appendSerializableSetterStatement(param)
            }
        }
        return appendPrimitiveSetterStatement(param)
    }

    private fun CodeBlock.Builder.appendPrimitiveSetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val typeName = param.type.ensureQualifiedTypeName().getShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "setter_name" to
                    if (param.type.isStringList()) "setStringList" else "set${typeName}",
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.replaceFirstChar { it ->
                        it.lowercase()
                    }
            )
        addNamed(
            "builder.%setter_name:L(\"%param_name:L\", %annotated_class_instance:L.%param_name:L)\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableSetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val typeName = param.type.ensureQualifiedTypeName().getShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "factory_name" to "${typeName}Factory".lowerFirstChar(),
                "setter_name" to "setAppFunctionData",
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.replaceFirstChar { it ->
                        it.lowercase()
                    }
            )

        addNamed(
            "builder.%setter_name:L(\"%param_name:L\", %factory_name:L.toAppFunctionData(%annotated_class_instance:L.%param_name:L))\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableListSetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val parametrizedTypeName =
            param.type.resolveListParameterizedType().ensureQualifiedTypeName().getShortName()

        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "factory_name" to "${parametrizedTypeName}Factory".lowerFirstChar(),
                "setter_name" to "setAppFunctionDataList",
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.replaceFirstChar { it ->
                        it.lowercase()
                    },
                "lambda_param_name" to parametrizedTypeName.lowerFirstChar()
            )

        addNamed(
            "builder.%setter_name:L(\"%param_name:L\", " +
                "%annotated_class_instance:L.%param_name:L" +
                ".map{ %lambda_param_name:L -> %factory_name:L.toAppFunctionData(%lambda_param_name:L) })",
            formatStringMap
        )
        return this
    }

    private fun KSTypeReference.isStringList(): Boolean {
        return isOfType(LIST) &&
            resolveListParameterizedType().ensureQualifiedTypeName().asString() ==
                String::class.qualifiedName
    }

    private fun KSTypeReference.isPrimitiveArray(): Boolean {
        return SUPPORTED_ARRAY_PRIMITIVE_TYPES.contains(ensureQualifiedTypeName().asString())
    }

    private fun String.lowerFirstChar(): String {
        return replaceFirstChar { it -> it.lowercase() }
    }

    private val factoryInitStatements = buildCodeBlock {
        val factoryInstanceNameToClassMap: Map<String, ClassName> = buildMap {
            for (serializableType in annotatedClass.getSerializablePropertyTypes()) {
                val qualifiedName = serializableType.ensureQualifiedTypeName()
                put(
                    "${qualifiedName.getShortName().lowerFirstChar()}Factory",
                    ClassName(
                        qualifiedName.getQualifier(),
                        "`\$${qualifiedName.getShortName()}Factory`"
                    )
                )
            }
        }
        for (entry in factoryInstanceNameToClassMap) {
            addStatement("val %L = %T()", entry.key, entry.value)
        }
        add("\n")
    }
}

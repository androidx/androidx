/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.generator.SpecNames.bundleClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.Types.asNonNull
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/** Utility to generate [CodeBlock]s that convert values to/from their binder equivalent. */
abstract class BinderCodeConverter(private val api: ParsedApi) {
    /**
     * Generate a block that converts the given expression from the binder representation to its
     * model equivalent.
     *
     * @param type the type of the resulting expression. Can reference a primitive on annotated
     * interface/value.
     * @param expression the expression to be converted.
     * @return a [CodeBlock] containing the generated code to perform the conversion.
     */
    fun convertToModelCode(type: Type, expression: String): CodeBlock {
        require(type != Types.unit) { "Cannot convert Unit." }
        if (type.isNullable) {
            if (Types.primitiveTypes.contains(type.asNonNull())) {
                return CodeBlock.of("%L.firstOrNull()", expression)
            }
            return CodeBlock.of(
                "%L?.let { notNullValue -> %L }",
                expression,
                convertToModelCode(type.asNonNull(), "notNullValue")
            )
        }
        val value = api.valueMap[type]
        if (value != null) {
            return convertToValueModelCode(value, expression)
        }
        val callback = api.callbackMap[type]
        if (callback != null) {
            return CodeBlock.of(
                "%T(%L, %N)",
                callback.clientProxyNameSpec(),
                expression,
                contextPropertyName
            )
        }
        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return convertToInterfaceModelCode(sandboxInterface, expression)
        }
        if (type.qualifiedName == List::class.qualifiedName) {
            val convertToModelCodeBlock = convertToModelCode(type.typeParameters[0], "it")
            return CodeBlock.of(
                "%L%L.toList()",
                expression,
                // Only convert the list elements if necessary.
                if (convertToModelCodeBlock == CodeBlock.of("it"))
                    CodeBlock.of("")
                else
                    CodeBlock.of(".map { %L }", convertToModelCodeBlock)
            )
        }
        if (type.qualifiedName == Types.sdkActivityLauncher.qualifiedName) {
            return convertToActivityLauncherModelCode(expression)
        }
        if (type == Types.short) {
            return CodeBlock.of("%L.toShort()", expression)
        }
        return CodeBlock.of(expression)
    }

    /**
     * Generate a block that converts the given expression from the model representation to its
     * binder equivalent.
     *
     * @param type the type of the given expression. Can reference a primitive on annotated
     * interface/value.
     * @param expression the expression to be converted.
     * @return a [CodeBlock] containing the generated code to perform the conversion.
     */
    fun convertToBinderCode(type: Type, expression: String): CodeBlock {
        require(type != Types.unit) { "Cannot convert to Unit." }
        if (type.isNullable) {
            val nonNullType = type.asNonNull()
            if (Types.primitiveTypes.contains(nonNullType)) {
                val createBinderListFunction = createBinderList(nonNullType)
                return CodeBlock.of(
                    "if (%L == null) %L() else %L(%L)",
                    expression,
                    createBinderListFunction,
                    createBinderListFunction,
                    expression
                )
            }
            return CodeBlock.of(
                "%L?.let { notNullValue -> %L }",
                expression,
                convertToBinderCode(nonNullType, "notNullValue")
            )
        }
        val value = api.valueMap[type]
        if (value != null) {
            return convertToValueBinderCode(value, expression)
        }
        val callback = api.callbackMap[type]
        if (callback != null) {
            return CodeBlock.of("%T(%L)", callback.stubDelegateNameSpec(), expression)
        }
        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return convertToInterfaceBinderCode(sandboxInterface, expression)
        }
        if (type.qualifiedName == List::class.qualifiedName) {
            val convertToBinderCodeBlock = convertToBinderCode(type.typeParameters[0], "it")
            return CodeBlock.of(
                "%L%L.%L()",
                expression,
                // Only convert the list elements if necessary.
                if (convertToBinderCodeBlock == CodeBlock.of("it"))
                    CodeBlock.of("")
                else
                    CodeBlock.of(".map { %L }", convertToBinderCodeBlock),
                toBinderList(type.typeParameters[0])
            )
        }
        if (type.qualifiedName == Types.sdkActivityLauncher.qualifiedName) {
            return convertToActivityLauncherBinderCode(expression)
        }
        if (type == Types.short) {
            return CodeBlock.of("%L.toInt()", expression)
        }
        return CodeBlock.of(expression)
    }

    protected abstract fun convertToInterfaceModelCode(
        annotatedInterface: AnnotatedInterface,
        expression: String
    ): CodeBlock

    protected abstract fun convertToInterfaceBinderCode(
        annotatedInterface: AnnotatedInterface,
        expression: String
    ): CodeBlock

    protected abstract fun convertToValueModelCode(
        value: AnnotatedValue,
        expression: String
    ): CodeBlock

    protected abstract fun convertToValueBinderCode(
        value: AnnotatedValue,
        expression: String
    ): CodeBlock

    protected abstract fun convertToActivityLauncherBinderCode(expression: String): CodeBlock

    protected abstract fun convertToActivityLauncherModelCode(expression: String): CodeBlock

    protected abstract fun convertToInterfaceBinderType(
        annotatedInterface: AnnotatedInterface
    ): TypeName

    private fun toBinderList(type: Type) = when (type) {
        Types.boolean -> "toBooleanArray"
        Types.int -> "toIntArray"
        Types.long -> "toLongArray"
        Types.short -> "toIntArray"
        Types.float -> "toFloatArray"
        Types.double -> "toDoubleArray"
        Types.char -> "toCharArray"
        else -> "toTypedArray"
    }

    private fun createBinderList(type: Type) = when (type) {
        Types.boolean -> "booleanArrayOf"
        Types.int -> "intArrayOf"
        Types.long -> "longArrayOf"
        Types.short -> "intArrayOf"
        Types.float -> "floatArrayOf"
        Types.double -> "doubleArrayOf"
        Types.char -> "charArrayOf"
        else -> "arrayOf"
    }

    /** Convert the given model type declaration to its binder equivalent. */
    fun convertToBinderType(type: Type): TypeName {
        if (type.isNullable) {
            val nonNullType = type.asNonNull()
            if (Types.primitiveTypes.contains(nonNullType)) {
                return convertToBinderListType(nonNullType)
            }
            return convertToBinderType(nonNullType).copy(nullable = true)
        }
        val value = api.valueMap[type]
        if (value != null) {
            return value.parcelableNameSpec()
        }
        val callback = api.callbackMap[type]
        if (callback != null) {
            return callback.aidlType().innerType.poetTypeName()
        }
        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return convertToInterfaceBinderType(sandboxInterface)
        }
        if (type.qualifiedName == List::class.qualifiedName)
            return convertToBinderListType(type.typeParameters[0])
        if (type.qualifiedName == Types.sdkActivityLauncher.qualifiedName)
            return bundleClass
        return type.poetTypeName()
    }

    private fun convertToBinderListType(type: Type): TypeName =
        when (type) {
            Types.boolean -> ClassName("kotlin", "BooleanArray")
            Types.int -> ClassName("kotlin", "IntArray")
            Types.long -> ClassName("kotlin", "LongArray")
            Types.short -> ClassName("kotlin", "IntArray")
            Types.float -> ClassName("kotlin", "FloatArray")
            Types.double -> ClassName("kotlin", "DoubleArray")
            Types.char -> ClassName("kotlin", "CharArray")
            else -> ClassName("kotlin", "Array")
                .parameterizedBy(convertToBinderType(type))
        }
}

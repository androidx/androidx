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

import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

/** Utility to generate [CodeBlock]s that convert values to/from their binder equivalent. */
class BinderCodeConverter(private val api: ParsedApi) {

    /**
     * Generate a block that converts the given expression from the binder representation to its
     * model equivalent in the client.
     *
     * @param type the type of the resulting expression. Can reference a primitive on annotated
     * interface/value.
     * @param expression the expression to be converted.
     * @return a [CodeBlock] containing the generated code to perform the conversion.
     */
    fun convertToModelCodeInClient(type: Type, expression: String): CodeBlock {
        val codeBlock = convertToModelCodeCommon(type, expression)
        if (codeBlock != null) {
            return codeBlock
        }

        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return CodeBlock.of(
                "%T(%L)", sandboxInterface.clientProxyNameSpec(), expression)
        }

        return CodeBlock.of(expression)
    }

    /**
     * Generate a block that converts the given expression from the binder representation to its
     * model equivalent in the server.
     *
     * @param parameter the type of the resulting expression and the expression to be converted.
     * @return a [CodeBlock] containing the generated code to perform the conversion.
     */
    fun convertToModelCodeInServer(parameter: Parameter): CodeBlock {
        val type = parameter.type
        val expression = parameter.name
        val codeBlock = convertToModelCodeCommon(type, expression)
        if (codeBlock != null) {
            return codeBlock
        }

        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return CodeBlock.of(
                "(%L as %T).delegate", expression, sandboxInterface.stubDelegateNameSpec())
        }

        return CodeBlock.of(expression)
    }

    private fun convertToModelCodeCommon(type: Type, expression: String): CodeBlock? {
        require(type != Types.unit) { "Cannot convert Unit." }
        val value = api.valueMap[type]
        if (value != null) {
            return CodeBlock.of("%M(%L)", value.fromParcelableNameSpec(), expression)
        }
        val callback = api.callbackMap[type]
        if (callback != null) {
            return CodeBlock.of("%T(%L)", callback.clientProxyNameSpec(), expression)
        }
        return null
    }

    /**
     * Generate a block that converts the given expression from the model representation to its
     * binder equivalent in the server.
     *
     * @param type the type of the given expression. Can reference a primitive on annotated
     * interface/value.
     * @param expression the expression to be converted.
     * @return a [CodeBlock] containing the generated code to perform the conversion.
     */
    fun convertToBinderCodeInServer(type: Type, expression: String): CodeBlock {
        val codeBlock = convertToBinderCodeCommon(type, expression)
        if (codeBlock != null) {
            return codeBlock
        }

        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return CodeBlock.of("%T(%L)", sandboxInterface.stubDelegateNameSpec(), expression)
        }

        return CodeBlock.of(expression)
    }

    /**
     * Generate a block that converts the given expression from the model representation to its
     * binder equivalent in the client.
     *
     * @param parameter the type of the resulting expression and the expression to be converted.
     * @return a [CodeBlock] containing the generated code to perform the conversion.
     */
    fun convertToBinderCodeInClient(parameter: Parameter): CodeBlock {
        val type = parameter.type
        val expression = parameter.name
        val codeBlock = convertToBinderCodeCommon(type, expression)
        if (codeBlock != null) {
            return codeBlock
        }

        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return CodeBlock.of(
                "(%L as %T).remote", expression, sandboxInterface.clientProxyNameSpec())
        }

        return CodeBlock.of(expression)
    }

    private fun convertToBinderCodeCommon(type: Type, expression: String): CodeBlock? {
        require(type != Types.unit) { "Cannot convert to Unit." }
        val value = api.valueMap[type]
        if (value != null) {
            return CodeBlock.of("%M(%L)", value.toParcelableNameSpec(), expression)
        }
        val callback = api.callbackMap[type]
        if (callback != null) {
            return CodeBlock.of("%T(%L)", callback.stubDelegateNameSpec(), expression)
        }
        return null
    }

    /** Convert the given model type declaration to its binder equivalent. */
    fun convertToBinderType(type: Type): ClassName {
        val value = api.valueMap[type]
        if (value != null) {
            return value.parcelableNameSpec()
        }
        val callback = api.callbackMap[type]
        if (callback != null) {
            return callback.aidlType().innerType.poetSpec()
        }
        val sandboxInterface = api.interfaceMap[type]
        if (sandboxInterface != null) {
            return sandboxInterface.aidlType().innerType.poetSpec()
        }
        return type.poetSpec()
    }
}
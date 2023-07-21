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

import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.SpecNames.toCoreLibInfoMethod
import androidx.privacysandbox.tools.core.generator.ValueConverterFileGenerator.Companion.fromParcelableMethodName
import androidx.privacysandbox.tools.core.generator.ValueConverterFileGenerator.Companion.toParcelableMethodName
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

class ServerBinderCodeConverter(private val api: ParsedApi) : BinderCodeConverter(api) {
    override fun convertToInterfaceModelCode(
        annotatedInterface: AnnotatedInterface,
        expression: String
    ): CodeBlock {
        if (annotatedInterface.inheritsSandboxedUiAdapter) {
            return CodeBlock.of(
                "(%L.binder as %T).delegate", expression, annotatedInterface.stubDelegateNameSpec()
            )
        }
        return CodeBlock.of(
            "(%L as %T).delegate", expression, annotatedInterface.stubDelegateNameSpec()
        )
    }

    override fun convertToInterfaceBinderCode(
        annotatedInterface: AnnotatedInterface,
        expression: String
    ): CodeBlock {
        if (annotatedInterface.inheritsSandboxedUiAdapter) {
            return CodeBlock.builder().build {
                addNamed(
                    "%coreLibInfoConverter:T.%toParcelable:N(" +
                        "%interface:L.%toCoreLibInfo:M(%context:N), " +
                        "%stubDelegate:T(%interface:L, %context:N)" +
                        ")",
                    hashMapOf<String, Any>(
                        "coreLibInfoConverter" to ClassName(
                            annotatedInterface.type.packageName,
                            annotatedInterface.coreLibInfoConverterName()
                        ),
                        "toParcelable" to toParcelableMethodName,
                        "interface" to expression,
                        "toCoreLibInfo" to toCoreLibInfoMethod,
                        "context" to contextPropertyName,
                        "stubDelegate" to annotatedInterface.stubDelegateNameSpec()
                    )
                )
            }
        }
        return CodeBlock.of(
            "%T(%L, %N)",
            annotatedInterface.stubDelegateNameSpec(),
            expression,
            contextPropertyName,
        )
    }

    override fun convertToInterfaceBinderType(annotatedInterface: AnnotatedInterface): TypeName {
        if (annotatedInterface.inheritsSandboxedUiAdapter) {
            return annotatedInterface.uiAdapterAidlWrapper().poetTypeName()
        }
        return annotatedInterface.aidlType().innerType.poetTypeName()
    }

    override fun convertToValueBinderCode(value: AnnotatedValue, expression: String): CodeBlock =
        CodeBlock.of(
            "%T(%N).%N(%L)",
            value.converterNameSpec(),
            contextPropertyName,
            toParcelableMethodName,
            expression
        )

    override fun convertToValueModelCode(value: AnnotatedValue, expression: String): CodeBlock =
        CodeBlock.of(
            "%T(%N).%N(%L)",
            value.converterNameSpec(),
            contextPropertyName,
            fromParcelableMethodName,
            expression,
        )
}
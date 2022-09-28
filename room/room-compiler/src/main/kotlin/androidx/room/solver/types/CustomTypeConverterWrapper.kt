/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.solver.types

import androidx.room.ProvidedTypeConverter
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.apply
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.ext.decapitalize
import androidx.room.solver.CodeGenScope
import androidx.room.vo.CustomTypeConverter
import androidx.room.writer.DaoWriter
import androidx.room.writer.TypeWriter
import java.util.Locale
import javax.lang.model.element.Modifier

/**
 * Wraps a type converter specified by the developer and forwards calls to it.
 */
class CustomTypeConverterWrapper(
    val custom: CustomTypeConverter
) : SingleStatementTypeConverter(custom.from, custom.to) {
    override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
        return if (custom.isEnclosingClassKotlinObject) {
            when (scope.language) {
                CodeLanguage.JAVA -> XCodeBlock.of(
                    scope.language,
                    "%T.INSTANCE.%L(%L)",
                    custom.className,
                    custom.methodName,
                    inputVarName
                )
                CodeLanguage.KOTLIN -> XCodeBlock.of(
                    scope.language,
                    "%T.%L(%L)",
                    custom.className,
                    custom.methodName,
                    inputVarName
                )
            }
        } else if (custom.isStatic) {
            XCodeBlock.of(
                scope.language,
                "%T.%L(%L)",
                custom.className,
                custom.methodName,
                inputVarName
            )
        } else {
            if (custom.isProvidedConverter) {
                XCodeBlock.of(
                    scope.language,
                    "%N().%L(%L)",
                    providedTypeConverter(scope),
                    custom.methodName,
                    inputVarName
                )
            } else {
                XCodeBlock.of(
                    scope.language,
                    "%N.%L(%L)",
                    typeConverter(scope),
                    custom.methodName,
                    inputVarName
                )
            }
        }
    }

    private fun providedTypeConverter(scope: CodeGenScope): XFunSpec {
        val className = custom.className
        val baseName = className.simpleNames.last().decapitalize(Locale.US)
        val converterClassName = custom.className
        scope.writer.addRequiredTypeConverter(converterClassName)
        val converterField = scope.writer.getOrCreateProperty(
            object : TypeWriter.SharedPropertySpec(
                baseName, custom.className
            ) {
                override val isMutable = true

                override fun getUniqueKey(): String {
                    return "converter_${custom.className}"
                }

                override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {
                }
            }
        )
        val funSpec = object : TypeWriter.SharedFunctionSpec(baseName) {
            override fun getUniqueKey(): String {
                return "converterMethod_${custom.className}"
            }

            override fun prepare(
                methodName: String,
                writer: TypeWriter,
                builder: XFunSpec.Builder
            ) {
                val body = buildConvertFunctionBody(builder.language)
                builder.apply(
                    // Apply synchronized modifier for Java
                    javaMethodBuilder = {
                        addModifiers(Modifier.SYNCHRONIZED)
                        builder.addCode(body)
                    },
                    // Use synchronized std-lib function for Kotlin
                    kotlinFunBuilder = {
                        beginControlFlow("return synchronized")
                        builder.addCode(body)
                        endControlFlow()
                    }
                )
                builder.returns(custom.className)
            }

            private fun buildConvertFunctionBody(language: CodeLanguage): XCodeBlock {
                return XCodeBlock.builder(language).apply {
                    beginControlFlow("if (%N == null)", converterField)
                    addStatement(
                        "%N = %L.getTypeConverter(%L)",
                        converterField,
                        DaoWriter.dbFieldName,
                        XCodeBlock.ofJavaClassLiteral(language, custom.className)
                    )
                    endControlFlow()
                    when (language) {
                        CodeLanguage.JAVA ->
                            addStatement("return %N", converterField)
                        CodeLanguage.KOTLIN ->
                            addStatement("return@synchronized %N", converterField)
                    }
                }.build()
            }
        }
        return scope.writer.getOrCreateFunction(funSpec)
    }

    private fun typeConverter(scope: CodeGenScope): XPropertySpec {
        val baseName = custom.className.simpleNames.last().decapitalize(Locale.US)
        val propertySpec = object : TypeWriter.SharedPropertySpec(
            baseName, custom.className
        ) {
            override fun getUniqueKey(): String {
                return "converter_${custom.className}"
            }

            override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {
                builder.initializer(
                    XCodeBlock.ofNewInstance(builder.language, custom.className)
                )
            }
        }
        return scope.writer.getOrCreateProperty(propertySpec)
    }
}

fun TypeWriter.addRequiredTypeConverter(className: XClassName) {
    this[ProvidedTypeConverter::class] = getRequiredTypeConverters() + setOf(className)
}

fun TypeWriter.getRequiredTypeConverters(): Set<XClassName> {
    return this.get<Set<XClassName>>(ProvidedTypeConverter::class) ?: emptySet()
}

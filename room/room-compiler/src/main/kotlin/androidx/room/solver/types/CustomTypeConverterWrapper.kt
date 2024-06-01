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
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.decapitalize
import androidx.room.solver.CodeGenScope
import androidx.room.vo.CustomTypeConverter
import androidx.room.writer.DaoWriter
import androidx.room.writer.TypeWriter
import java.util.Locale
import javax.lang.model.element.Modifier

/** Wraps a type converter specified by the developer and forwards calls to it. */
class CustomTypeConverterWrapper(val custom: CustomTypeConverter) :
    SingleStatementTypeConverter(custom.from, custom.to) {
    override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
        return if (custom.isEnclosingClassKotlinObject) {
            when (scope.language) {
                CodeLanguage.JAVA ->
                    XCodeBlock.of(
                        scope.language,
                        "%T.INSTANCE.%L(%L)",
                        custom.className,
                        custom.getMethodName(scope.language),
                        inputVarName
                    )
                CodeLanguage.KOTLIN ->
                    XCodeBlock.of(
                        scope.language,
                        "%T.%L(%L)",
                        custom.className,
                        custom.getMethodName(scope.language),
                        inputVarName
                    )
            }
        } else if (custom.isStatic) {
            XCodeBlock.of(
                scope.language,
                "%T.%L(%L)",
                custom.className,
                custom.getMethodName(scope.language),
                inputVarName
            )
        } else {
            if (custom.isProvidedConverter) {
                XCodeBlock.of(
                    scope.language,
                    "%N().%L(%L)",
                    providedTypeConverter(scope),
                    custom.getMethodName(scope.language),
                    inputVarName
                )
            } else {
                XCodeBlock.of(
                    scope.language,
                    "%N.%L(%L)",
                    typeConverter(scope),
                    custom.getMethodName(scope.language),
                    inputVarName
                )
            }
        }
    }

    private fun providedTypeConverter(scope: CodeGenScope): XFunSpec {
        val fieldTypeName =
            when (scope.language) {
                CodeLanguage.JAVA -> custom.className
                CodeLanguage.KOTLIN -> KotlinTypeNames.LAZY.parametrizedBy(custom.className)
            }
        val baseName = custom.className.simpleNames.last().decapitalize(Locale.US)
        val converterClassName = custom.className
        scope.writer.addRequiredTypeConverter(converterClassName)
        val converterField =
            scope.writer.getOrCreateProperty(
                object : TypeWriter.SharedPropertySpec(baseName, fieldTypeName) {
                    override val isMutable = scope.language == CodeLanguage.JAVA

                    override fun getUniqueKey(): String {
                        return "converter_${custom.className}"
                    }

                    override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {
                        // For Kotlin we'll rely on kotlin.Lazy while for Java we'll memoize the
                        // provided converter in the getter.
                        if (builder.language == CodeLanguage.KOTLIN) {
                            builder.apply {
                                initializer(
                                    XCodeBlock.builder(language)
                                        .apply {
                                            beginControlFlow("lazy")
                                            addStatement(
                                                "checkNotNull(%L.getTypeConverter(%L))",
                                                DaoWriter.DB_PROPERTY_NAME,
                                                XCodeBlock.ofKotlinClassLiteral(
                                                    language,
                                                    custom.className
                                                )
                                            )
                                            endControlFlow()
                                        }
                                        .build()
                                )
                            }
                        }
                    }
                }
            )
        val funSpec =
            object : TypeWriter.SharedFunctionSpec(baseName) {
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
                        // Apply synchronized modifier for Java since function checks and sets the
                        // converter in the shared field.
                        javaMethodBuilder = { addModifiers(Modifier.SYNCHRONIZED) },
                        kotlinFunctionBuilder = {}
                    )
                    builder.addCode(body)
                    builder.returns(custom.className)
                }

                private fun buildConvertFunctionBody(language: CodeLanguage) =
                    XCodeBlock.builder(language)
                        .apply {
                            // For Java we implement the memoization logic in the converter getter,
                            // meanwhile
                            // for Kotlin we rely on kotlin.Lazy so the getter just delegates to it.
                            when (language) {
                                CodeLanguage.JAVA -> {
                                    beginControlFlow("if (%N == null)", converterField).apply {
                                        addStatement(
                                            "%N = %L.getTypeConverter(%L)",
                                            converterField,
                                            DaoWriter.DB_PROPERTY_NAME,
                                            XCodeBlock.ofJavaClassLiteral(
                                                language,
                                                custom.className
                                            )
                                        )
                                    }
                                    endControlFlow()
                                    addStatement("return %N", converterField)
                                }
                                CodeLanguage.KOTLIN -> {
                                    addStatement("return %N.value", converterField)
                                }
                            }
                        }
                        .build()
            }
        return scope.writer.getOrCreateFunction(funSpec)
    }

    private fun typeConverter(scope: CodeGenScope): XPropertySpec {
        val baseName = custom.className.simpleNames.last().decapitalize(Locale.US)
        val propertySpec =
            object : TypeWriter.SharedPropertySpec(baseName, custom.className) {
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

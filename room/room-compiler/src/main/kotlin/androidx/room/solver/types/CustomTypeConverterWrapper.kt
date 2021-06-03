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
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.vo.CustomTypeConverter
import androidx.room.writer.ClassWriter
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import decapitalize
import java.util.Locale
import javax.lang.model.element.Modifier

/**
 * Wraps a type converter specified by the developer and forwards calls to it.
 */
class CustomTypeConverterWrapper(val custom: CustomTypeConverter) :
    TypeConverter(custom.from, custom.to) {

    override fun convert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            if (custom.isEnclosingClassKotlinObject) {
                addStatement(
                    "$L = $T.INSTANCE.$L($L)",
                    outputVarName, custom.typeName,
                    custom.methodName, inputVarName
                )
            } else if (custom.isStatic) {
                addStatement(
                    "$L = $T.$L($L)",
                    outputVarName, custom.typeName,
                    custom.methodName, inputVarName
                )
            } else {
                if (custom.isProvidedConverter) {
                    addStatement(
                        "$L = $N().$L($L)",
                        outputVarName, providedTypeConverter(scope),
                        custom.methodName, inputVarName
                    )
                } else {
                    addStatement(
                        "$L = $N.$L($L)",
                        outputVarName, typeConverter(scope),
                        custom.methodName, inputVarName
                    )
                }
            }
        }
    }

    private fun providedTypeConverter(scope: CodeGenScope): MethodSpec {
        val className = custom.typeName as ClassName
        val baseName = className.simpleName().decapitalize(Locale.US)

        val converterClassName = custom.typeName as ClassName
        scope.writer.addRequiredTypeConverter(converterClassName)
        val converterField = scope.writer.getOrCreateField(object : ClassWriter.SharedFieldSpec(
            baseName, custom.typeName
        ) {
                override fun getUniqueKey(): String {
                    return "converter_${custom.typeName}"
                }

                override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
                    builder.addModifiers(Modifier.PRIVATE)
                }
            })

        return scope.writer.getOrCreateMethod(object : ClassWriter.SharedMethodSpec(baseName) {
            override fun getUniqueKey(): String {
                return "converterMethod_${custom.typeName}"
            }

            override fun prepare(
                methodName: String,
                writer: ClassWriter,
                builder: MethodSpec.Builder
            ) {
                builder.apply {
                    addModifiers(Modifier.PRIVATE)
                    addModifiers(Modifier.SYNCHRONIZED)
                    returns(custom.typeName)
                    addCode(buildConvertMethodBody(writer))
                }
            }

            private fun buildConvertMethodBody(
                writer: ClassWriter
            ): CodeBlock {
                val methodScope = CodeGenScope(writer)
                methodScope.builder().apply {
                    beginControlFlow("if ($N == null)", converterField)
                    addStatement(
                        "$N = $N.getTypeConverter($T.class)",
                        converterField,
                        DaoWriter.dbField,
                        custom.typeName
                    )
                    endControlFlow()
                    addStatement("return $N", converterField)
                }
                return methodScope.builder().build()
            }
        })
    }

    fun typeConverter(scope: CodeGenScope): FieldSpec {
        val baseName = (custom.typeName as ClassName).simpleName().decapitalize(Locale.US)
        return scope.writer.getOrCreateField(object : ClassWriter.SharedFieldSpec(
            baseName, custom.typeName
        ) {
                override fun getUniqueKey(): String {
                    return "converter_${custom.typeName}"
                }

                override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
                    builder.addModifiers(Modifier.PRIVATE)
                    builder.addModifiers(Modifier.FINAL)
                    builder.initializer("new $T()", custom.typeName)
                }
            })
    }
}

fun ClassWriter.addRequiredTypeConverter(className: ClassName) {
    this[ProvidedTypeConverter::class] = getRequiredTypeConverters() + setOf(className)
}

fun ClassWriter.getRequiredTypeConverters(): Set<ClassName> {
    return this.get<Set<ClassName>>(ProvidedTypeConverter::class) ?: emptySet()
}

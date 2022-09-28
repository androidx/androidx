/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XNullability
import androidx.room.parser.SQLTypeAffinity.TEXT
import androidx.room.solver.CodeGenScope
import androidx.room.writer.TypeWriter

/**
 * Uses enum string representation.
 */
class EnumColumnTypeAdapter(
    private val enumTypeElement: XEnumTypeElement
) : ColumnTypeAdapter(enumTypeElement.type, TEXT) {

    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        val stringToEnumMethod = stringToEnumMethod(scope)
        if (scope.language == CodeLanguage.KOTLIN && out.nullability == XNullability.NONNULL) {
            scope.builder.addStatement(
                "%L = checkNotNull(%N(%L.getString(%L)))",
                outVarName, stringToEnumMethod, cursorVarName, indexVarName
            )
        } else {
            scope.builder.addStatement(
                "%L = %N(%L.getString(%L))",
                outVarName, stringToEnumMethod, cursorVarName, indexVarName
            )
        }
    }

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope
    ) {
        val enumToStringMethod = enumToStringMethod(scope)
        scope.builder.apply {
            if (language == CodeLanguage.KOTLIN && out.nullability == XNullability.NONNULL) {
                addStatement(
                    "%L.bindString(%L, checkNotNull(%N(%L)))",
                    stmtName, indexVarName, enumToStringMethod, valueVarName
                )
            } else {
                beginControlFlow("if (%L == null)", valueVarName)
                    .addStatement("%L.bindNull(%L)", stmtName, indexVarName)
                nextControlFlow("else")
                    .addStatement(
                        "%L.bindString(%L, %N(%L))",
                        stmtName, indexVarName, enumToStringMethod, valueVarName
                    )
                endControlFlow()
            }
        }
    }

    private fun enumToStringMethod(scope: CodeGenScope): XFunSpec {
        val funSpec = object : TypeWriter.SharedFunctionSpec(
            out.typeElement!!.name + "_enumToString"
        ) {
            override fun getUniqueKey(): String {
                return "enumToString_" + out.asTypeName().toString()
            }

            override fun prepare(
                methodName: String,
                writer: TypeWriter,
                builder: XFunSpec.Builder
            ) {
                val body = XCodeBlock.builder(builder.language).apply {
                    val paramName = "_value"
                    beginControlFlow("if (%L == null)", paramName).apply {
                        addStatement("return null")
                    }
                    endControlFlow()
                    when (writer.codeLanguage) {
                        // Use a switch control flow
                        CodeLanguage.JAVA -> {
                            beginControlFlow("switch (%L)", paramName)
                            enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                                addStatement(
                                    "case %L: return %S",
                                    enumConstantName, enumConstantName
                                )
                            }
                            addStatement(
                                "default: throw new %T(%S + %L)",
                                ILLEGAL_ARG_EXCEPTION,
                                ENUM_TO_STRING_ERROR_MSG,
                                paramName
                            )
                            endControlFlow()
                        }
                        // Use a when control flow
                        CodeLanguage.KOTLIN -> {
                            beginControlFlow("return when (%L)", paramName)
                            enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                                addStatement("%L -> %S", enumConstantName, enumConstantName)
                            }
                            addStatement(
                                "else -> throw %T(%S + %L)",
                                ILLEGAL_ARG_EXCEPTION,
                                ENUM_TO_STRING_ERROR_MSG,
                                paramName
                            )
                            endControlFlow()
                        }
                    }
                }.build()
                builder.apply {
                    returns(String::class.asClassName().copy(nullable = true))
                    addParameter(
                        out.asTypeName().copy(nullable = true),
                        "_value"
                    )
                    addCode(body)
                }
            }
        }
        return scope.writer.getOrCreateFunction(funSpec)
    }

    private fun stringToEnumMethod(scope: CodeGenScope): XFunSpec {
        val funSpec = object : TypeWriter.SharedFunctionSpec(
            out.typeElement!!.name + "_stringToEnum"
        ) {
            override fun getUniqueKey(): String {
                return out.asTypeName().toString()
            }

            override fun prepare(
                methodName: String,
                writer: TypeWriter,
                builder: XFunSpec.Builder
            ) {
                val body = XCodeBlock.builder(builder.language).apply {
                    val paramName = "_value"
                    beginControlFlow("if (%L == null)", paramName).apply {
                        addStatement("return null")
                    }
                    endControlFlow()
                    when (writer.codeLanguage) {
                        // Use a switch control flow
                        CodeLanguage.JAVA -> {
                            beginControlFlow("switch (%L)", paramName)
                            enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                                addStatement(
                                    "case %S: return %T.%L",
                                    enumConstantName, out.asTypeName(), enumConstantName
                                )
                            }
                            addStatement(
                                "default: throw new %T(%S + %L)",
                                ILLEGAL_ARG_EXCEPTION,
                                STRING_TO_ENUM_ERROR_MSG,
                                paramName
                            )
                            endControlFlow()
                        }
                        // Use a when control flow
                        CodeLanguage.KOTLIN -> {
                            beginControlFlow("return when (%L)", paramName)
                            enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                                addStatement(
                                    "%S -> %T.%L",
                                    enumConstantName, out.asTypeName(), enumConstantName
                                )
                            }
                            addStatement(
                                "else -> throw %T(%S + %L)",
                                ILLEGAL_ARG_EXCEPTION,
                                STRING_TO_ENUM_ERROR_MSG,
                                paramName
                            )
                            endControlFlow()
                        }
                    }
                }.build()
                builder.apply {
                    returns(out.asTypeName().copy(nullable = true))
                    addParameter(
                        String::class.asClassName().copy(nullable = true),
                        "_value"
                    )
                    addCode(body)
                }
            }
        }
        return scope.writer.getOrCreateFunction(funSpec)
    }

    companion object {
        private val ILLEGAL_ARG_EXCEPTION = IllegalArgumentException::class.asClassName()
        private const val ENUM_TO_STRING_ERROR_MSG =
            "Can't convert enum to string, unknown enum value: "
        private const val STRING_TO_ENUM_ERROR_MSG =
            "Can't convert value to enum, unknown value: "
    }
}

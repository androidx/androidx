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
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.ExceptionTypeNames
import androidx.room.parser.SQLTypeAffinity.TEXT
import androidx.room.solver.CodeGenScope
import androidx.room.writer.TypeWriter

/**
 * Uses enum string representation.
 */
class EnumColumnTypeAdapter(
    private val enumTypeElement: XEnumTypeElement,
    out: XType
) : ColumnTypeAdapter(out, TEXT) {

    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        val stringToEnumMethod = stringToEnumMethod(scope)
        scope.builder.apply {
            fun XCodeBlock.Builder.addGetStringStatement() {
                addStatement(
                    "%L = %N(%L.getString(%L))",
                    outVarName,
                    stringToEnumMethod,
                    cursorVarName,
                    indexVarName
                )
            }
            if (out.nullability == XNullability.NONNULL) {
                addGetStringStatement()
            } else {
                beginControlFlow("if (%L.isNull(%L))", cursorVarName, indexVarName)
                    .addStatement("%L = null", outVarName)
                nextControlFlow("else")
                    .addGetStringStatement()
                endControlFlow()
            }
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
            fun XCodeBlock.Builder.addBindStringStatement() {
                addStatement(
                    "%L.bindString(%L, %N(%L))",
                    stmtName, indexVarName, enumToStringMethod, valueVarName,
                )
            }
            if (out.nullability == XNullability.NONNULL) {
                addBindStringStatement()
            } else {
                beginControlFlow("if (%L == null)", valueVarName)
                    .addStatement("%L.bindNull(%L)", stmtName, indexVarName)
                nextControlFlow("else")
                addBindStringStatement()
                endControlFlow()
            }
        }
    }

    private fun enumToStringMethod(scope: CodeGenScope): XFunSpec {
        val funSpec = object : TypeWriter.SharedFunctionSpec(
            out.typeElement!!.name + "_enumToString"
        ) {
            val paramName = "_value"

            override fun getUniqueKey(): String {
                return "enumToString_" + enumTypeElement.asClassName().toString()
            }

            override fun prepare(
                methodName: String,
                writer: TypeWriter,
                builder: XFunSpec.Builder
            ) {
                val body = XCodeBlock.builder(builder.language).apply {
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
                                ExceptionTypeNames.ILLEGAL_ARG_EXCEPTION,
                                ENUM_TO_STRING_ERROR_MSG,
                                paramName
                            )
                            endControlFlow()
                        }
                        // Use a when control flow, note that it is exhaustive and there is no need
                        // or an `else` case.
                        CodeLanguage.KOTLIN -> {
                            beginControlFlow("return when (%L)", paramName)
                            enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                                addStatement(
                                    "%T.%L -> %S",
                                    enumTypeElement.asClassName(),
                                    enumConstantName,
                                    enumConstantName
                                )
                            }
                            endControlFlow()
                        }
                    }
                }.build()
                builder.apply {
                    returns(CommonTypeNames.STRING.copy(nullable = false))
                    addParameter(
                        enumTypeElement.asClassName(),
                        paramName
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
            val paramName = "_value"

            override fun getUniqueKey(): String {
                return "stringToEnum_" + enumTypeElement.asClassName().toString()
            }

            override fun prepare(
                methodName: String,
                writer: TypeWriter,
                builder: XFunSpec.Builder
            ) {
                val body = XCodeBlock.builder(builder.language).apply {
                    when (writer.codeLanguage) {
                        // Use a switch control flow
                        CodeLanguage.JAVA -> {
                            beginControlFlow("switch (%L)", paramName)
                            enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                                addStatement(
                                    "case %S: return %T.%L",
                                    enumConstantName,
                                    enumTypeElement.asClassName(),
                                    enumConstantName
                                )
                            }
                            addStatement(
                                "default: throw new %T(%S + %L)",
                                ExceptionTypeNames.ILLEGAL_ARG_EXCEPTION,
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
                                    enumConstantName,
                                    enumTypeElement.asClassName(),
                                    enumConstantName
                                )
                            }
                            addStatement(
                                "else -> throw %T(%S + %L)",
                                ExceptionTypeNames.ILLEGAL_ARG_EXCEPTION,
                                STRING_TO_ENUM_ERROR_MSG,
                                paramName
                            )
                            endControlFlow()
                        }
                    }
                }.build()
                builder.apply {
                    returns(enumTypeElement.asClassName())
                    addParameter(
                        CommonTypeNames.STRING.copy(nullable = false),
                        paramName
                    )
                    addCode(body)
                }
            }
        }
        return scope.writer.getOrCreateFunction(funSpec)
    }

    companion object {
        private const val ENUM_TO_STRING_ERROR_MSG =
            "Can't convert enum to string, unknown enum value: "
        private const val STRING_TO_ENUM_ERROR_MSG =
            "Can't convert value to enum, unknown value: "
    }
}

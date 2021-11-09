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

import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.ext.CommonTypeNames.ILLEGAL_ARG_EXCEPTION
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.parser.SQLTypeAffinity.TEXT
import androidx.room.solver.CodeGenScope
import androidx.room.writer.ClassWriter
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import javax.lang.model.element.Modifier

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
        scope.builder()
            .addStatement(
                "$L = $N($L.getString($L))",
                outVarName, stringToEnumMethod, cursorVarName, indexVarName
            )
    }

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope
    ) {
        val enumToStringMethod = enumToStringMethod(scope)
        scope.builder().apply {
            beginControlFlow("if ($L == null)", valueVarName)
                .addStatement("$L.bindNull($L)", stmtName, indexVarName)
            nextControlFlow("else")
                .addStatement(
                    "$L.bindString($L, $N($L))",
                    stmtName, indexVarName, enumToStringMethod, valueVarName
                )
            endControlFlow()
        }
    }

    private fun enumToStringMethod(scope: CodeGenScope): MethodSpec {
        return scope.writer.getOrCreateMethod(object :
                ClassWriter.SharedMethodSpec(out.typeElement!!.name + "_enumToString") {
                override fun getUniqueKey(): String {
                    return "enumToString_" + out.typeName.toString()
                }

                override fun prepare(
                    methodName: String,
                    writer: ClassWriter,
                    builder: MethodSpec.Builder
                ) {
                    builder.apply {
                        addModifiers(Modifier.PRIVATE)
                        returns(String::class.java)
                        val param = ParameterSpec.builder(
                            out.typeName, "_value", Modifier.FINAL
                        ).build()
                        addParameter(param)
                        beginControlFlow("if ($N == null)", param)
                        addStatement("return null")
                        nextControlFlow("switch ($N)", param)
                        enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                            addStatement("case $L: return $S", enumConstantName, enumConstantName)
                        }
                        addStatement(
                            "default: throw new $T($S + $N)",
                            ILLEGAL_ARG_EXCEPTION,
                            "Can't convert enum to string, unknown enum value: ",
                            param
                        )
                        endControlFlow()
                    }
                }
            })
    }

    private fun stringToEnumMethod(scope: CodeGenScope): MethodSpec {
        return scope.writer.getOrCreateMethod(object :
                ClassWriter.SharedMethodSpec(out.typeElement!!.name + "_stringToEnum") {
                override fun getUniqueKey(): String {
                    return out.typeName.toString()
                }

                override fun prepare(
                    methodName: String,
                    writer: ClassWriter,
                    builder: MethodSpec.Builder
                ) {
                    builder.apply {
                        addModifiers(Modifier.PRIVATE)
                        returns(out.typeName)
                        val param = ParameterSpec.builder(
                            String::class.java, "_value", Modifier.FINAL
                        ).build()
                        addParameter(param)
                        beginControlFlow("if ($N == null)", param)
                        addStatement("return null")
                        nextControlFlow("switch ($N)", param)
                        enumTypeElement.entries.map { it.name }.forEach { enumConstantName ->
                            addStatement(
                                "case $S: return $T.$L",
                                enumConstantName, out.typeName, enumConstantName
                            )
                        }
                        addStatement(
                            "default: throw new $T($S + $N)",
                            ILLEGAL_ARG_EXCEPTION,
                            "Can't convert value to enum, unknown value: ",
                            param
                        )
                        endControlFlow()
                    }
                }
            })
    }
}

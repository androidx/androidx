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

import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XType
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
import java.util.Locale
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier

/**
 * Uses enum string representation.
 */
class EnumColumnTypeAdapter(out: XType) :
    ColumnTypeAdapter(out, TEXT) {
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
                ClassWriter.SharedMethodSpec(out.asTypeElement().name + "_enumToString") {
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
                        getEnumConstantElements().forEach { enumConstant ->
                            addStatement("case $L: return $S", enumConstant.name, enumConstant.name)
                        }
                        addStatement(
                            "default: throw new $T($S)",
                            ILLEGAL_ARG_EXCEPTION,
                            "Can't convert ${param.name} to string, unknown enum value."
                        )
                        endControlFlow()
                    }
                }
            })
    }

    private fun stringToEnumMethod(scope: CodeGenScope): MethodSpec {
        return scope.writer.getOrCreateMethod(object :
                ClassWriter.SharedMethodSpec(out.asTypeElement().name + "_stringToEnum") {
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
                        getEnumConstantElements().forEach {
                            enumConstant ->
                            addStatement(
                                "case $S: return $T.$L",
                                enumConstant.name, out.typeName, enumConstant.name
                            )
                        }
                        addStatement(
                            "default: throw new $T($S)",
                            ILLEGAL_ARG_EXCEPTION,
                            "Can't convert ${param.name} to enum, unknown value."
                        )
                        endControlFlow()
                    }
                }
            })
    }

    private fun getEnumConstantElements(): List<XFieldElement> {
        // TODO: Switch below logic to use`getDeclaredFields` when the
        //  functionality is available in the XTypeElement API
        val typeElementFields = out.asTypeElement().getAllFieldsIncludingPrivateSupers()
        return typeElementFields.filter {
            // TODO: (b/173236324) Add kind to the X abstraction API to avoid using kindName()
            ElementKind.ENUM_CONSTANT.toString().toLowerCase(Locale.US) == it.kindName()
        }
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.writer

import com.android.support.room.ext.AndroidTypeNames
import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.S
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.CallType.FIELD
import com.android.support.room.vo.CallType.METHOD
import com.android.support.room.vo.Entity
import com.android.support.room.vo.Field
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import stripNonJava
import javax.lang.model.element.Modifier.PRIVATE

class EntityCursorConverterWriter(val entity: Entity) : ClassWriter.SharedMethodSpec(
        "entityCursorConverter_${entity.typeName.toString().stripNonJava()}") {
    override fun getUniqueKey(): String {
        return "generic_entity_converter_of_${entity.element.qualifiedName}"
    }

    override fun prepare(writer: ClassWriter, builder: MethodSpec.Builder) {
        builder.apply {
            val cursorParam = ParameterSpec
                    .builder(AndroidTypeNames.CURSOR, "cursor").build()
            addParameter(cursorParam)
            addModifiers(PRIVATE)
            returns(entity.typeName)
            addCode(buildConvertMethodBody(writer, cursorParam))
        }
    }

    private fun buildConvertMethodBody(writer: ClassWriter, cursorParam: ParameterSpec)
            : CodeBlock {
        // TODO support arg constructor
        val scope = CodeGenScope(writer)
        val entityVar = scope.getTmpVar("_entity")
        scope.builder().apply {
            addStatement("$T $L = new $T()", entity.typeName, entityVar, entity.typeName)
            val colNameVar = scope.getTmpVar("_columnName")
            val colIndexVar = scope.getTmpVar("_columnIndex")
            addStatement("$T $L = 0", TypeName.INT, colIndexVar)
            beginControlFlow("for ($T $L : $N.getColumnNames())",
                    TypeName.get(String::class.java), colNameVar, cursorParam).apply {
                beginControlFlow("switch($L.hashCode())", colNameVar).apply {
                    entity.fields.groupBy { it.columnName.hashCode() }.forEach {
                        val hash = it.key
                        beginControlFlow("case $L:", hash).apply {
                            val fields = it.value
                            fields.forEach { field ->
                                beginControlFlow("if ($S.equals($L))", field.name, colNameVar)
                                readField(field, cursorParam, colIndexVar, entityVar, scope)
                                endControlFlow()
                            }
                        }
                        endControlFlow()
                    }
                }
                endControlFlow()
                addStatement("$L ++", colIndexVar)
            }
            endControlFlow()
            addStatement("return $L", entityVar)
        }
        return scope.builder().build()
    }

    private fun readField(field: Field, cursorParam: ParameterSpec,
                          indexVar: String, entityVar: String, scope: CodeGenScope) {
        scope.builder().apply {
            val reader = field.cursorValueReader
            when (field.setter.callType) {
                FIELD -> {
                    reader?.readFromCursor("$entityVar.${field.getter.name}", cursorParam.name,
                                    indexVar, scope)
                }
                METHOD -> {
                    val tmpField = scope.getTmpVar("_tmp${field.name.capitalize()}")
                    addStatement("final $T $L", field.getter.type.typeName(), tmpField)
                    reader?.readFromCursor(tmpField, cursorParam.name,
                            indexVar, scope)
                    addStatement("$L.$L($L)", entityVar, field.setter.name, tmpField)
                }
            }
        }
    }
}

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
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.Entity
import com.android.support.room.vo.Field
import com.android.support.room.vo.DecomposedField
import com.android.support.room.vo.FieldWithIndex
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

    private fun depth(parent: DecomposedField?): Int {
        return if (parent == null) {
            0
        } else {
            1 + depth(parent.parent)
        }
    }

    private fun buildConvertMethodBody(writer: ClassWriter, cursorParam: ParameterSpec)
            : CodeBlock {
        // TODO support arg constructor
        val scope = CodeGenScope(writer)
        val entityVar = scope.getTmpVar("_entity")
        scope.builder().apply {
            addStatement("$T $L = new $T()", entity.typeName, entityVar, entity.typeName)
            val allParents = FieldReadWriteWriter.getAllParents(entity.fields)
            val sortedParents = allParents
                    .sortedBy {
                        depth(it)
                    }
                    .associate {
                        Pair(it, scope.getTmpVar("_tmp${it.field.name}"))
                    }
            // for each field parent, create a not null var so that we can set it at the end
            val parentNotNullVars = declareParents(sortedParents, scope)

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
                                val subOwner = field.parent?.let {
                                    sortedParents[it]
                                } ?: entityVar
                                beginControlFlow("if ($S.equals($L))", field.columnName, colNameVar)
                                val notNullVar = field.parent?.let {
                                    parentNotNullVars[it]
                                }
                                if (notNullVar != null) {
                                    beginControlFlow("if (!cursor.isNull($L))", colIndexVar).apply {
                                        addStatement("$L = true", notNullVar)
                                        readField(field, cursorParam, colIndexVar, subOwner, scope)
                                    }
                                    endControlFlow()
                                } else {
                                    readField(field, cursorParam, colIndexVar, subOwner, scope)
                                }
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
            // assign parents
            assignParents(entityVar, parentNotNullVars, sortedParents, scope)
            addStatement("return $L", entityVar)
        }
        return scope.builder().build()
    }

    private fun declareParents(parentVars: Map<DecomposedField, String>,
                               scope: CodeGenScope): Map<DecomposedField, String> {
        val parentNotNullVars = hashMapOf<DecomposedField, String>()
        scope.builder().apply {
            parentVars.forEach {
                addStatement("final $T $L = new $T()", it.key.pojo.typeName,
                        it.value, it.key.pojo.typeName)
                val notNullVar = scope.getTmpVar("_notNull${it.key.field.name}")
                parentNotNullVars[it.key] = notNullVar
                addStatement("$T $L = false", TypeName.BOOLEAN, notNullVar)
            }
        }
        return parentNotNullVars
    }

    private fun assignParents(entityVar: String, parentNotNullVars: Map<DecomposedField, String>,
                              sortedParents: Map<DecomposedField, String>, scope: CodeGenScope) {
        scope.builder().apply {
            sortedParents.forEach {
                val parent = it.key
                val varName = it.value
                val allNotNullVars = parent.pojo.fields
                        .map { parentNotNullVars[it.parent] }.distinct()
                val ifCheck = allNotNullVars.joinToString(" || ")
                beginControlFlow("if ($L)", ifCheck).apply {
                    val grandParentVar = parent.parent?.let {
                        sortedParents[it]
                    } ?: entityVar
                    parent.field.setter.writeSet(grandParentVar, varName, this)
                }
                endControlFlow()
            }
        }
    }

    private fun readField(field: Field, cursorParam: ParameterSpec,
                          indexVar: String, ownerVar: String, scope: CodeGenScope) {
        scope.builder().apply {
            FieldReadWriteWriter(FieldWithIndex(field, indexVar)).readFromCursor(
                    ownerVar = ownerVar,
                    cursorVar = cursorParam.name,
                    scope = scope
            )
        }
    }
}

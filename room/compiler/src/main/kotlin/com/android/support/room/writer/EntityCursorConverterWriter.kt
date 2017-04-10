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
        val scope = CodeGenScope(writer)
        val entityVar = scope.getTmpVar("_entity")
        scope.builder().apply {
            scope.builder().addStatement("final $T $L", entity.typeName, entityVar)
            val fieldsWithIndices = entity.fields.map {
                val indexVar = scope.getTmpVar(
                        "_cursorIndexOf${it.name.stripNonJava().capitalize()}")
                scope.builder().addStatement("final $T $L = $N.getColumnIndex($S)",
                        TypeName.INT, indexVar, cursorParam, it.columnName)
                FieldWithIndex(field = it,
                        indexVar = indexVar,
                        alwaysExists = false)
            }
            FieldReadWriteWriter.readFromCursor(
                    outVar = entityVar,
                    outPojo = entity,
                    cursorVar = cursorParam.name,
                    fieldsWithIndices = fieldsWithIndices,
                    scope = scope)
            addStatement("return $L", entityVar)
        }
        return scope.builder().build()
    }
}

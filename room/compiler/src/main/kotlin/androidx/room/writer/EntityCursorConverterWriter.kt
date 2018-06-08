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

package androidx.room.writer

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Entity
import androidx.room.vo.FieldWithIndex
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

    override fun prepare(methodName: String, writer: ClassWriter, builder: MethodSpec.Builder) {
        builder.apply {
            val cursorParam = ParameterSpec
                    .builder(AndroidTypeNames.CURSOR, "cursor").build()
            addParameter(cursorParam)
            addModifiers(PRIVATE)
            returns(entity.typeName)
            addCode(buildConvertMethodBody(writer, cursorParam))
        }
    }

    private fun buildConvertMethodBody(writer: ClassWriter, cursorParam: ParameterSpec): CodeBlock {
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
                    relationCollectors = emptyList(), // no relationship for entities
                    scope = scope)
            addStatement("return $L", entityVar)
        }
        return scope.builder().build()
    }
}

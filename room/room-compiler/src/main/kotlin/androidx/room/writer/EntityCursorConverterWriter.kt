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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.ext.AndroidTypeNames.CURSOR
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Entity
import androidx.room.vo.FieldWithIndex
import java.util.Locale

class EntityCursorConverterWriter(val entity: Entity) : TypeWriter.SharedFunctionSpec(
    "entityCursorConverter_${entity.typeName.toString(CodeLanguage.JAVA).stripNonJava()}"
) {
    override fun getUniqueKey(): String {
        return "generic_entity_converter_of_${entity.element.qualifiedName}"
    }

    override fun prepare(methodName: String, writer: TypeWriter, builder: XFunSpec.Builder) {
        builder.apply {
            val cursorParamName = "cursor"
            addParameter(CURSOR, cursorParamName)
            returns(entity.typeName)
            addCode(buildConvertMethodBody(writer, cursorParamName))
        }
    }

    private fun buildConvertMethodBody(writer: TypeWriter, cursorParamName: String): XCodeBlock {
        val scope = CodeGenScope(writer)
        val entityVar = scope.getTmpVar("_entity")
        scope.builder.apply {
            addLocalVariable(
                entityVar,
                entity.typeName
            )
            val fieldsWithIndices = entity.fields.map {
                val indexVar = scope.getTmpVar(
                    "_cursorIndexOf${it.name.stripNonJava().capitalize(Locale.US)}"
                )
                addLocalVariable(
                    name = indexVar,
                    typeName = XTypeName.PRIMITIVE_INT,
                    assignExpr = XCodeBlock.of(
                        language,
                        "%M(%N, %S)",
                        RoomMemberNames.CURSOR_UTIL_GET_COLUMN_INDEX,
                        cursorParamName,
                        it.columnName
                    )
                )
                FieldWithIndex(
                    field = it,
                    indexVar = indexVar,
                    alwaysExists = false
                )
            }
            FieldReadWriteWriter.readFromCursor(
                outVar = entityVar,
                outPojo = entity,
                cursorVar = cursorParamName,
                fieldsWithIndices = fieldsWithIndices,
                relationCollectors = emptyList(), // no relationship for entities
                scope = scope
            )
            addStatement("return %L", entityVar)
        }
        return scope.generate()
    }
}

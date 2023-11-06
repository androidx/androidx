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

package androidx.room.writer

import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Fields
import androidx.room.vo.Pojo
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.columnNames

class EntityUpdateAdapterWriter private constructor(
    val tableName: String,
    val pojo: Pojo,
    val primaryKeyFields: Fields,
    val onConflict: String
) {
    companion object {
        fun create(entity: ShortcutEntity, onConflict: String) =
            EntityUpdateAdapterWriter(
                tableName = entity.tableName,
                pojo = entity.pojo,
                primaryKeyFields = entity.primaryKey.fields,
                onConflict = onConflict
            )
    }

    fun createAnonymous(typeWriter: TypeWriter, dbParam: String): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder(typeWriter.codeLanguage, "%L", dbParam).apply {
            superclass(RoomTypeNames.DELETE_OR_UPDATE_ADAPTER.parametrizedBy(pojo.typeName))
            addFunction(
                XFunSpec.builder(
                    language = language,
                    name = "createQuery",
                    visibility = VisibilityModifier.PROTECTED,
                    isOverride = true
                ).apply {
                    returns(CommonTypeNames.STRING)
                    val pojoCols = pojo.columnNames.joinToString(",") {
                        "`$it` = ?"
                    }
                    val pkFieldsCols = primaryKeyFields.columnNames.joinToString(" AND ") {
                        "`$it` = ?"
                    }
                    val query = buildString {
                        if (onConflict.isNotEmpty()) {
                            append("UPDATE OR $onConflict `$tableName` SET")
                        } else {
                            append("UPDATE `$tableName` SET")
                        }
                        append(" $pojoCols")
                        append(" WHERE")
                        append(" $pkFieldsCols")
                    }
                    addStatement("return %S", query)
                }.build()
            )
            addFunction(
                XFunSpec.builder(
                    language = language,
                    name = "bind",
                    visibility = VisibilityModifier.PROTECTED,
                    isOverride = true
                ).apply {
                    val stmtParam = "statement"
                    addParameter(SupportDbTypeNames.SQLITE_STMT, stmtParam)
                    val entityParam = "entity"
                    addParameter(pojo.typeName, entityParam)
                    val mappedField = FieldWithIndex.byOrder(pojo.fields)
                    val bindScope = CodeGenScope(typeWriter)
                    FieldReadWriteWriter.bindToStatement(
                        ownerVar = entityParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mappedField,
                        scope = bindScope
                    )
                    val pkeyStart = pojo.fields.size
                    val mappedPrimaryKeys = primaryKeyFields.mapIndexed { index, field ->
                        FieldWithIndex(
                            field = field,
                            indexVar = "${pkeyStart + index + 1}",
                            alwaysExists = true
                        )
                    }
                    FieldReadWriteWriter.bindToStatement(
                        ownerVar = entityParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mappedPrimaryKeys,
                        scope = bindScope
                    )
                    addCode(bindScope.generate())
                }.build()
            )
        }.build()
    }
}

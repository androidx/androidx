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
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.DataClass
import androidx.room.vo.Properties
import androidx.room.vo.PropertyWithIndex
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.columnNames

class EntityUpdateAdapterWriter
private constructor(
    val tableName: String,
    val dataClass: DataClass,
    val primaryKeyFields: Properties,
    val onConflict: String
) {
    companion object {
        fun create(entity: ShortcutEntity, onConflict: String) =
            EntityUpdateAdapterWriter(
                tableName = entity.tableName,
                dataClass = entity.dataClass,
                primaryKeyFields = entity.primaryKey.properties,
                onConflict = onConflict
            )
    }

    fun createAnonymous(typeWriter: TypeWriter): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder()
            .apply {
                superclass(
                    RoomTypeNames.DELETE_OR_UPDATE_ADAPTER.parametrizedBy(dataClass.typeName)
                )
                addFunction(
                    XFunSpec.builder(
                            name = "createQuery",
                            visibility = VisibilityModifier.PROTECTED,
                            isOverride = true
                        )
                        .apply {
                            returns(CommonTypeNames.STRING)
                            val dataClassCols =
                                dataClass.columnNames.joinToString(",") { "`$it` = ?" }
                            val pkFieldsCols =
                                primaryKeyFields.columnNames.joinToString(" AND ") { "`$it` = ?" }
                            val query = buildString {
                                if (onConflict.isNotEmpty()) {
                                    append("UPDATE OR $onConflict `$tableName` SET")
                                } else {
                                    append("UPDATE `$tableName` SET")
                                }
                                append(" $dataClassCols")
                                append(" WHERE")
                                append(" $pkFieldsCols")
                            }
                            addStatement("return %S", query)
                        }
                        .build()
                )
                addFunction(
                    XFunSpec.builder(
                            name = "bind",
                            visibility = VisibilityModifier.PROTECTED,
                            isOverride = true
                        )
                        .apply {
                            val stmtParam = "statement"
                            addParameter(stmtParam, SQLiteDriverTypeNames.STATEMENT)
                            val entityParam = "entity"
                            addParameter(entityParam, dataClass.typeName)
                            val mappedField = PropertyWithIndex.byOrder(dataClass.properties)
                            val bindScope = CodeGenScope(writer = typeWriter)
                            PropertyReadWriteWriter.bindToStatement(
                                ownerVar = entityParam,
                                stmtParamVar = stmtParam,
                                propertiesWithIndices = mappedField,
                                scope = bindScope
                            )
                            val pkeyStart = dataClass.properties.size
                            val mappedPrimaryKeys =
                                primaryKeyFields.mapIndexed { index, field ->
                                    PropertyWithIndex(
                                        property = field,
                                        indexVar = "${pkeyStart + index + 1}",
                                        alwaysExists = true
                                    )
                                }
                            PropertyReadWriteWriter.bindToStatement(
                                ownerVar = entityParam,
                                stmtParamVar = stmtParam,
                                propertiesWithIndices = mappedPrimaryKeys,
                                scope = bindScope
                            )
                            addCode(bindScope.generate())
                        }
                        .build()
                )
            }
            .build()
    }
}

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

package androidx.room3.writer

import androidx.room3.compiler.codegen.VisibilityModifier
import androidx.room3.compiler.codegen.XFunSpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.XTypeSpec
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.RoomTypeNames
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.vo.Properties
import androidx.room3.vo.PropertyWithIndex
import androidx.room3.vo.ShortcutEntity

class EntityDeleteAdapterWriter
private constructor(val tableName: String, val pojoTypeName: XTypeName, val fields: Properties) {
    companion object {
        fun create(entity: ShortcutEntity): EntityDeleteAdapterWriter {
            val fieldsToUse =
                if (entity.isPartialEntity) {
                    // When using partial entity, delete by values in pojo
                    entity.dataClass.properties
                } else {
                    // When using entity, delete by primary key
                    entity.primaryKey.properties
                }
            return EntityDeleteAdapterWriter(
                tableName = entity.tableName,
                pojoTypeName = entity.dataClass.typeName,
                fields = fieldsToUse,
            )
        }
    }

    fun createAnonymous(typeWriter: TypeWriter): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder()
            .apply {
                superclass(RoomTypeNames.DELETE_OR_UPDATE_ADAPTER.parametrizedBy(pojoTypeName))
                addFunction(
                    XFunSpec.builder(
                            name = "createQuery",
                            visibility = VisibilityModifier.PROTECTED,
                            isOverride = true,
                        )
                        .apply {
                            returns(CommonTypeNames.STRING)
                            val query =
                                "DELETE FROM `$tableName` WHERE " +
                                    fields.columnNames.joinToString(" AND ") { "`$it` = ?" }
                            addStatement("return %S", query)
                        }
                        .build()
                )
                addFunction(
                    XFunSpec.builder(
                            name = "bind",
                            visibility = VisibilityModifier.PROTECTED,
                            isOverride = true,
                        )
                        .apply {
                            val stmtParam = "statement"
                            addParameter(stmtParam, SQLiteDriverTypeNames.STATEMENT)
                            val entityParam = "entity"
                            addParameter(entityParam, pojoTypeName)
                            val mapped = PropertyWithIndex.byOrder(fields)
                            val bindScope = CodeGenScope(writer = typeWriter)
                            PropertyReadWriteWriter.bindToStatement(
                                ownerVar = entityParam,
                                stmtParamVar = stmtParam,
                                propertiesWithIndices = mapped,
                                scope = bindScope,
                            )
                            addCode(bindScope.generate())
                        }
                        .build()
                )
            }
            .build()
    }
}

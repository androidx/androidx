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
import androidx.room3.compiler.processing.XNullability
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.RoomTypeNames
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.vo.DataClass
import androidx.room3.vo.PropertyWithIndex
import androidx.room3.vo.ShortcutEntity
import androidx.room3.vo.columnNames

class EntityInsertAdapterWriter
private constructor(
    val tableName: String,
    val dataClass: DataClass,
    val primitiveAutoGenerateColumn: String?,
    val onConflict: String,
) {
    companion object {
        fun create(entity: ShortcutEntity, onConflict: String): EntityInsertAdapterWriter {
            // If there is an auto-increment primary key with primitive type, we consider 0 as
            // not set. For such fields, we must generate a slightly different insertion SQL.
            val primitiveAutoGenerateField =
                if (entity.primaryKey.autoGenerateId) {
                    entity.primaryKey.properties.firstOrNull()?.let { field ->
                        field.statementBinder?.typeMirror()?.let { binderType ->
                            if (binderType.nullability == XNullability.NONNULL) {
                                field
                            } else {
                                null
                            }
                        }
                    }
                } else {
                    null
                }
            return EntityInsertAdapterWriter(
                tableName = entity.tableName,
                dataClass = entity.dataClass,
                primitiveAutoGenerateColumn = primitiveAutoGenerateField?.columnName,
                onConflict = onConflict,
            )
        }
    }

    fun createAnonymous(typeWriter: TypeWriter): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder()
            .apply {
                superclass(RoomTypeNames.INSERT_ADAPTER.parametrizedBy(dataClass.typeName))
                addFunction(
                    XFunSpec.builder(
                            name = "createQuery",
                            visibility = VisibilityModifier.PROTECTED,
                            isOverride = true,
                        )
                        .apply {
                            returns(CommonTypeNames.STRING)
                            val query = buildString {
                                if (onConflict.isNotEmpty()) {
                                    append("INSERT OR $onConflict INTO `$tableName`")
                                } else {
                                    append("INSERT INTO `$tableName`")
                                }
                                append(" (${dataClass.columnNames.joinToString(",") { "`$it`" }})")
                                append(" VALUES (")
                                append(
                                    dataClass.properties.joinToString(",") {
                                        if (it.columnName == primitiveAutoGenerateColumn) {
                                            "nullif(?, 0)"
                                        } else {
                                            "?"
                                        }
                                    }
                                )
                                append(")")
                            }
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
                            returns(XTypeName.UNIT_VOID)
                            val stmtParam = "statement"
                            addParameter(stmtParam, SQLiteDriverTypeNames.STATEMENT)
                            val entityParam = "entity"
                            addParameter(entityParam, dataClass.typeName)
                            val mapped = PropertyWithIndex.byOrder(dataClass.properties)
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

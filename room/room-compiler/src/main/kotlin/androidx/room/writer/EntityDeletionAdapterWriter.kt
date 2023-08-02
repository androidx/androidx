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

import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Fields
import androidx.room.vo.ShortcutEntity

class EntityDeletionAdapterWriter private constructor(
    val tableName: String,
    val pojoTypeName: XTypeName,
    val fields: Fields
) {
    companion object {
        fun create(entity: ShortcutEntity): EntityDeletionAdapterWriter {
            val fieldsToUse = if (entity.isPartialEntity) {
                // When using partial entity, delete by values in pojo
                entity.pojo.fields
            } else {
                // When using entity, delete by primary key
                entity.primaryKey.fields
            }
            return EntityDeletionAdapterWriter(
                tableName = entity.tableName,
                pojoTypeName = entity.pojo.typeName,
                fields = fieldsToUse
            )
        }
    }

    fun createAnonymous(typeWriter: TypeWriter, dbParam: String): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder(
            typeWriter.codeLanguage, "%L", dbParam
        ).apply {
            superclass(RoomTypeNames.DELETE_OR_UPDATE_ADAPTER.parametrizedBy(pojoTypeName))
            addFunction(
                XFunSpec.builder(
                    language = language,
                    name = "createQuery",
                    visibility = VisibilityModifier.PROTECTED,
                    isOverride = true
                ).apply {
                    returns(CommonTypeNames.STRING)
                    val query = "DELETE FROM `$tableName` WHERE " +
                        fields.columnNames.joinToString(" AND ") { "`$it` = ?" }
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
                    addParameter(pojoTypeName, entityParam)
                    val mapped = FieldWithIndex.byOrder(fields)
                    val bindScope = CodeGenScope(typeWriter)
                    FieldReadWriteWriter.bindToStatement(
                        ownerVar = entityParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mapped,
                        scope = bindScope
                    )
                    addCode(bindScope.generate())
                }.build()
            )
        }.build()
    }
}

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

import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.SupportDbTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Fields
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.columnNames
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PUBLIC

class EntityDeletionAdapterWriter private constructor(
    val tableName: String,
    val pojoTypeName: TypeName,
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
                fields = fieldsToUse)
        }
    }

    fun createAnonymous(classWriter: ClassWriter, dbParam: String): TypeSpec {
        @Suppress("RemoveSingleExpressionStringTemplate")
        return TypeSpec.anonymousClassBuilder("$L", dbParam).apply {
            superclass(ParameterizedTypeName.get(RoomTypeNames.DELETE_OR_UPDATE_ADAPTER,
                pojoTypeName)
            )
            addMethod(MethodSpec.methodBuilder("createQuery").apply {
                addAnnotation(Override::class.java)
                returns(ClassName.get("java.lang", "String"))
                addModifiers(PUBLIC)
                val query = "DELETE FROM `$tableName` WHERE " +
                        fields.columnNames.joinToString(" AND ") { "`$it` = ?" }
                addStatement("return $S", query)
            }.build())
            addMethod(MethodSpec.methodBuilder("bind").apply {
                val bindScope = CodeGenScope(classWriter)
                addAnnotation(Override::class.java)
                val stmtParam = "stmt"
                addParameter(ParameterSpec.builder(SupportDbTypeNames.SQLITE_STMT,
                        stmtParam).build())
                val valueParam = "value"
                addParameter(ParameterSpec.builder(pojoTypeName, valueParam).build())
                returns(TypeName.VOID)
                addModifiers(PUBLIC)
                val mapped = FieldWithIndex.byOrder(fields)
                FieldReadWriteWriter.bindToStatement(ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mapped,
                        scope = bindScope)
                addCode(bindScope.builder().build())
            }.build())
        }.build()
    }
}

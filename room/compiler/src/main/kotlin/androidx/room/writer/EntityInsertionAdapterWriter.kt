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
import androidx.room.vo.Pojo
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.columnNames
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PUBLIC

class EntityInsertionAdapterWriter private constructor(
    val tableName: String,
    val pojo: Pojo,
    val primitiveAutoGenerateColumn: String?,
    val onConflict: String
) {
    companion object {
        fun create(entity: ShortcutEntity, onConflict: String): EntityInsertionAdapterWriter {
            // If there is an auto-increment primary key with primitive type, we consider 0 as
            // not set. For such fields, we must generate a slightly different insertion SQL.
            val primitiveAutoGenerateField = if (entity.primaryKey.autoGenerateId) {
                entity.primaryKey.fields.firstOrNull()?.let { field ->
                    field.statementBinder?.typeMirror()?.let { binderType ->
                        if (binderType.kind.isPrimitive) {
                            field
                        } else {
                            null
                        }
                    }
                }
            } else {
                null
            }
            return EntityInsertionAdapterWriter(
                tableName = entity.tableName,
                pojo = entity.pojo,
                primitiveAutoGenerateColumn = primitiveAutoGenerateField?.columnName,
                onConflict = onConflict)
        }
    }

    fun createAnonymous(classWriter: ClassWriter, dbParam: String): TypeSpec {
        @Suppress("RemoveSingleExpressionStringTemplate")
        return TypeSpec.anonymousClassBuilder("$L", dbParam).apply {
            superclass(ParameterizedTypeName.get(RoomTypeNames.INSERTION_ADAPTER, pojo.typeName))
            addMethod(MethodSpec.methodBuilder("createQuery").apply {
                addAnnotation(Override::class.java)
                addModifiers(PUBLIC)
                returns(ClassName.get("java.lang", "String"))
                val query = buildString {
                    append("INSERT OR $onConflict INTO `$tableName`")
                    append(" (${pojo.columnNames.joinToString(",") { "`$it`" }})")
                    append(" VALUES (")
                    append(pojo.fields.joinToString(",") {
                            if (it.columnName == primitiveAutoGenerateColumn) {
                                "nullif(?, 0)"
                            } else {
                                "?"
                            }
                        })
                    append(")")
                }
                addStatement("return $S", query)
            }.build())
            addMethod(MethodSpec.methodBuilder("bind").apply {
                val bindScope = CodeGenScope(classWriter)
                addAnnotation(Override::class.java)
                addModifiers(PUBLIC)
                returns(TypeName.VOID)
                val stmtParam = "stmt"
                addParameter(ParameterSpec.builder(SupportDbTypeNames.SQLITE_STMT,
                        stmtParam).build())
                val valueParam = "value"
                addParameter(ParameterSpec.builder(pojo.typeName, valueParam).build())
                val mapped = FieldWithIndex.byOrder(pojo.fields)
                FieldReadWriteWriter.bindToStatement(
                        ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mapped,
                        scope = bindScope
                )
                addCode(bindScope.builder().build())
            }.build())
        }.build()
    }
}

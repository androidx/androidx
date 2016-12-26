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

import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.S
import com.android.support.room.ext.SupportDbTypeNames
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Entity
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PUBLIC

class EntityInsertionAdapterWriter(val entity: Entity, val onConflict: String) {
    fun createAnonymous(dbParam : String): TypeSpec {
        @Suppress("RemoveSingleExpressionStringTemplate")
        return TypeSpec.anonymousClassBuilder("$L", dbParam).apply {
            superclass(
                    ParameterizedTypeName.get(RoomTypeNames.INSERTION_ADAPTER, entity.typeName)
            )
            addMethod(MethodSpec.methodBuilder("createInsertQuery").apply {
                addAnnotation(Override::class.java)
                returns(ClassName.get("java.lang", "String"))
                addModifiers(PUBLIC)
                val query =
                        "INSERT OR $onConflict INTO `${entity.tableName}`(" +
                                entity.fields.joinToString(",") {
                                    "`${it.columnName}`"
                                } + ") VALUES (" +
                                entity.fields.joinToString(",") {
                                    "?"
                                } + ")"
                addStatement("return $S", query)
            }.build())
            addMethod(MethodSpec.methodBuilder("bind").apply {
                val bindScope = CodeGenScope()
                addAnnotation(Override::class.java)
                val stmtParam = "stmt"
                addParameter(ParameterSpec.builder(SupportDbTypeNames.SQLITE_STMT,
                        stmtParam).build())
                val valueParam = "value"
                addParameter(ParameterSpec.builder(entity.typeName, valueParam).build())
                returns(TypeName.VOID)
                addModifiers(PUBLIC)
                entity.fields.forEachIndexed { index, field ->
                    field.getter.columnAdapter?.let { adapter ->
                        val varName = if (field.getter.callType == CallType.FIELD) {
                            "$valueParam.${field.name}"
                        } else {
                            "$valueParam.${field.getter.name}()"
                        }
                        adapter.bindToStmt(stmtParam, "${index + 1}", varName, bindScope)
                    }
                }
                addCode(bindScope.builder().build())
            }.build())
        }.build()
    }
}

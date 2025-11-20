/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.buildCodeBlock
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.KotlinCollectionMemberNames
import androidx.room3.ext.RoomMemberNames
import androidx.room3.ext.RoomTypeNames
import androidx.room3.ext.capitalize
import androidx.room3.ext.stripNonJava
import androidx.room3.vo.FtsEntity
import java.util.Locale

class FtsTableInfoValidationWriter(val entity: FtsEntity) : ValidationWriter() {
    override fun write(connectionParamName: String, scope: CountingCodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize(Locale.US)
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder.apply {
            val columnSetVar = scope.getTmpVar("_columns$suffix")
            val columnsSetType = CommonTypeNames.MUTABLE_SET.parametrizedBy(CommonTypeNames.STRING)
            addLocalVariable(
                name = columnSetVar,
                typeName = columnsSetType,
                assignExpr =
                    buildCodeBlock { language ->
                        when (language) {
                            CodeLanguage.JAVA ->
                                add(
                                    "new %T(%L)",
                                    CommonTypeNames.HASH_SET.parametrizedBy(CommonTypeNames.STRING),
                                    entity.properties.size,
                                )
                            CodeLanguage.KOTLIN ->
                                add("%M()", KotlinCollectionMemberNames.MUTABLE_SET_OF)
                        }
                    },
            )
            entity.nonHiddenProperties.forEach {
                addStatement("%L.add(%S)", columnSetVar, it.columnName)
            }

            addLocalVariable(
                name = expectedInfoVar,
                typeName = RoomTypeNames.FTS_TABLE_INFO,
                assignExpr =
                    XCodeBlock.ofNewInstance(
                        RoomTypeNames.FTS_TABLE_INFO,
                        "%S, %L, %S",
                        entity.tableName,
                        columnSetVar,
                        entity.createTableQuery,
                    ),
            )

            val existingVar = scope.getTmpVar("_existing$suffix")
            addLocalVal(
                existingVar,
                RoomTypeNames.FTS_TABLE_INFO,
                "%M(%L, %S)",
                RoomMemberNames.FTS_TABLE_INFO_READ,
                connectionParamName,
                entity.tableName,
            )

            beginControlFlow("if (!%L.equals(%L))", expectedInfoVar, existingVar).apply {
                addStatement(
                    "return %L",
                    XCodeBlock.ofNewInstance(
                        RoomTypeNames.ROOM_OPEN_DELEGATE_VALIDATION_RESULT,
                        "false, %S + %L + %S + %L",
                        "${entity.tableName}(${entity.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar,
                        "\n Found:\n",
                        existingVar,
                    ),
                )
            }
            endControlFlow()
        }
    }
}

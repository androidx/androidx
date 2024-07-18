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

package androidx.room.writer

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.vo.FtsEntity
import java.util.Locale

class FtsTableInfoValidationWriter(val entity: FtsEntity) : ValidationWriter() {
    override fun write(dbParamName: String, scope: CountingCodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize(Locale.US)
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder.apply {
            val columnSetVar = scope.getTmpVar("_columns$suffix")
            val columnsSetType = CommonTypeNames.HASH_SET.parametrizedBy(CommonTypeNames.STRING)
            addLocalVariable(
                name = columnSetVar,
                typeName = columnsSetType,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    columnsSetType,
                    "%L",
                    entity.fields.size
                )
            )
            entity.nonHiddenFields.forEach {
                addStatement("%L.add(%S)", columnSetVar, it.columnName)
            }

            addLocalVariable(
                name = expectedInfoVar,
                typeName = RoomTypeNames.FTS_TABLE_INFO,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    RoomTypeNames.FTS_TABLE_INFO,
                    "%S, %L, %S",
                    entity.tableName, columnSetVar, entity.createTableQuery

                )
            )

            val existingVar = scope.getTmpVar("_existing$suffix")
            addLocalVal(
                existingVar,
                RoomTypeNames.FTS_TABLE_INFO,
                "%M(%L, %S)",
                RoomMemberNames.FTS_TABLE_INFO_READ, dbParamName, entity.tableName
            )

            beginControlFlow("if (!%L.equals(%L))", expectedInfoVar, existingVar).apply {
                addStatement(
                    "return %L",
                    XCodeBlock.ofNewInstance(
                        language,
                        RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                        "false, %S + %L + %S + %L",
                        "${entity.tableName}(${entity.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar,
                        "\n Found:\n",
                        existingVar
                    )
                )
            }
            endControlFlow()
        }
    }
}

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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.Entity
import androidx.room.vo.columnNames
import java.util.Locale

class TableInfoValidationWriter(val entity: Entity) : ValidationWriter() {

    companion object {
        const val CREATED_FROM_ENTITY = "CREATED_FROM_ENTITY"
    }

    override fun write(dbParamName: String, scope: CountingCodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize(Locale.US)
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder.apply {
            val columnListVar = scope.getTmpVar("_columns$suffix")
            val columnListType = CommonTypeNames.HASH_MAP.parametrizedBy(
                CommonTypeNames.STRING,
                RoomTypeNames.TABLE_INFO_COLUMN
            )
            addLocalVariable(
                name = columnListVar,
                typeName = columnListType,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    columnListType,
                    "%L",
                    entity.fields.size
                )
            )
            entity.fields.forEach { field ->
                addStatement(
                    "%L.put(%S, %L)",
                    columnListVar,
                    field.columnName,
                    XCodeBlock.ofNewInstance(
                        language,
                        RoomTypeNames.TABLE_INFO_COLUMN,
                        "%S, %S, %L, %L, %S, %T.%L",
                        field.columnName, // name
                        field.affinity?.name ?: SQLTypeAffinity.TEXT.name, // type
                        field.nonNull, // nonNull
                        entity.primaryKey.fields.indexOf(field) + 1, // pkeyPos
                        field.defaultValue, // defaultValue
                        RoomTypeNames.TABLE_INFO, CREATED_FROM_ENTITY // createdFrom
                    )
                )
            }

            val foreignKeySetVar = scope.getTmpVar("_foreignKeys$suffix")
            val foreignKeySetType =
                CommonTypeNames.HASH_SET.parametrizedBy(RoomTypeNames.TABLE_INFO_FOREIGN_KEY)
            addLocalVariable(
                name = foreignKeySetVar,
                typeName = foreignKeySetType,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    foreignKeySetType,
                    "%L",
                    entity.foreignKeys.size
                )
            )
            entity.foreignKeys.forEach {
                addStatement(
                    "%L.add(%L)",
                    foreignKeySetVar,
                    XCodeBlock.ofNewInstance(
                        language,
                        RoomTypeNames.TABLE_INFO_FOREIGN_KEY,
                        "%S, %S, %S, %L, %L",
                        it.parentTable, // parent table
                        it.onDelete.sqlName, // on delete
                        it.onUpdate.sqlName, // on update
                        listOfStrings(it.childFields.map { it.columnName }), // parent names
                        listOfStrings(it.parentColumns) // parent column names
                    )
                )
            }

            val indicesSetVar = scope.getTmpVar("_indices$suffix")
            val indicesType =
                CommonTypeNames.HASH_SET.parametrizedBy(RoomTypeNames.TABLE_INFO_INDEX)
            addLocalVariable(
                name = indicesSetVar,
                typeName = indicesType,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    indicesType,
                    "%L",
                    entity.indices.size
                )
            )
            entity.indices.forEach { index ->
                val orders = if (index.orders.isEmpty()) {
                    index.columnNames.map { "ASC" }
                } else {
                    index.orders.map { it.name }
                }
                addStatement(
                    "%L.add(%L)",
                    indicesSetVar,
                    XCodeBlock.ofNewInstance(
                        language,
                        RoomTypeNames.TABLE_INFO_INDEX,
                        "%S, %L, %L, %L",
                        index.name, // name
                        index.unique, // unique
                        listOfStrings(index.columnNames), // columns
                        listOfStrings(orders) // orders
                    )
                )
            }

            addLocalVariable(
                name = expectedInfoVar,
                typeName = RoomTypeNames.TABLE_INFO,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    RoomTypeNames.TABLE_INFO,
                    "%S, %L, %L, %L",
                    entity.tableName, columnListVar, foreignKeySetVar, indicesSetVar
                )
            )

            val existingVar = scope.getTmpVar("_existing$suffix")
            addLocalVal(
                existingVar,
                RoomTypeNames.TABLE_INFO,
                "%M(%L, %S)",
                RoomMemberNames.TABLE_INFO_READ, dbParamName, entity.tableName
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

    private fun CodeBlockWrapper.listOfStrings(strings: List<String>): XCodeBlock {
        val placeholders = List(strings.size) { "%S" }.joinToString()
        val function: Any = when (language) {
            CodeLanguage.JAVA -> XCodeBlock.of(language, "%T.asList", CommonTypeNames.ARRAYS)
            CodeLanguage.KOTLIN -> "listOf"
        }
        return XCodeBlock.of(
            language,
            "%L($placeholders)",
            function, *strings.toTypedArray()
        )
    }
}

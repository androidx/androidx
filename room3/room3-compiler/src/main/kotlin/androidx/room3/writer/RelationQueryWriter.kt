/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.ext.CollectionTypeNames
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.RoomTypeNames
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.vo.QueryParameter
import androidx.room3.vo.Relation

/** Writes the SQL query and placeholder arguments for a relationship via @Relation. */
class RelationQueryWriter(val relation: Relation, val queryParam: QueryParameter) {

    private val usingLongSparseArray =
        queryParam.type.asTypeName().rawTypeName == CollectionTypeNames.LONG_SPARSE_ARRAY

    private val resultColumns =
        if (relation.junction != null) {
            val projectionColumns =
                relation.projection.map { "`${relation.entity.tableName}`.`$it` AS `$it`" }
            val keyColumnsColumns =
                relation.junction.parentProperties.map { "_junction.`${it.columnName}`" }
            (projectionColumns + keyColumnsColumns).toSet()
        } else {
            val projectionColumns = relation.projection.map { "`$it`" }
            val keyColumns = relation.entityProperties.map { "`${it.columnName}`" }
            (projectionColumns + keyColumns).toSet()
        }

    private val whereClauseColumns =
        if (relation.junction != null) {
            relation.junction.parentProperties.map { "_junction.`${it.columnName}`" }
        } else {
            relation.entityProperties.map { "`${it.columnName}`" }
        }

    // Creates SELECT relationship query with placeholder WHERE clause
    fun createLoadAllSql(): String = buildString {
        append(createSelectSql())
        // Create placeholder WHERE statement to validate query.
        // The real query is built at runtime due to dynamic amount of IN arguments.
        if (relation.hasCompositeKey) {
            val placeholders = List(whereClauseColumns.size) { ":p$it" }.joinToString(", ")
            append(" WHERE (${whereClauseColumns.joinToString(", ")}) IN (($placeholders))")
        } else {
            append(" WHERE ${whereClauseColumns.single()} IN (:args)")
        }
    }

    // Create SELECT part of relationship query
    private fun createSelectSql(): String = buildString {
        append("SELECT ${resultColumns.joinToString(", ")}")
        if (relation.junction != null) {
            append(" FROM `${relation.junction.entity.tableName}` AS _junction")
            append(" INNER JOIN `${relation.entity.tableName}` ON ")
            val joinConditions =
                relation.junction.entityProperties.mapIndexed { index, prop ->
                    "_junction.`${prop.columnName}` = `${relation.entity.tableName}`.`${relation.entityProperties[index].columnName}`"
                }
            append(joinConditions.joinToString(" AND "))
        } else {
            append(" FROM `${relation.entity.tableName}`")
        }
    }

    fun prepareStatementAndBindArgs(
        connectionVarName: String,
        outSqlQueryName: String,
        outStmtName: String,
        scope: CodeGenScope,
    ) {
        val inputVarName =
            if (usingLongSparseArray) {
                RelationCollectorFunctionWriter.PARAM_MAP_VARIABLE
            } else {
                RelationCollectorFunctionWriter.KEY_SET_VARIABLE
            }

        scope.builder.apply {
            val sbVar = scope.getTmpVar("_stringBuilder")
            addLocalVariable(
                name = sbVar,
                typeName = KotlinTypeNames.STRING_BUILDER,
                assignExpr = XCodeBlock.ofNewInstance(KotlinTypeNames.STRING_BUILDER),
            )
            addStatement("%L.append(%S)", sbVar, createSelectSql())

            val whereClause =
                if (relation.hasCompositeKey) {
                    " WHERE (${whereClauseColumns.joinToString(", ")}) IN ("
                } else {
                    " WHERE ${whereClauseColumns.single()} IN ("
                }
            addStatement("%L.append(%S)", sbVar, whereClause)

            val countVar = scope.getTmpVar("_inputSize")
            checkNotNull(queryParam.queryParamAdapter) {
                "RelationQueryWriter param adapter must not be null for relationships."
            }
            queryParam.queryParamAdapter.getArgCount(
                inputVarName = inputVarName,
                outputVarName = countVar,
                scope = scope,
            )

            if (relation.hasCompositeKey) {
                // Composite key placeholders (row values), e.g: (?, ?), (?, ?)
                val columnsSize =
                    if (relation.junction != null) {
                        relation.junction.parentProperties.size
                    } else {
                        relation.entityProperties.size
                    }
                addStatement(
                    "%M(%L, %L, %L)",
                    RoomTypeNames.STRING_UTIL.packageMember("appendRowValuePlaceholders"),
                    sbVar,
                    countVar,
                    columnsSize,
                )
            } else {
                // Single key placeholders: ?, ?, ?
                addStatement(
                    "%M(%L, %L)",
                    RoomTypeNames.STRING_UTIL.packageMember("appendPlaceholders"),
                    sbVar,
                    countVar,
                )
            }

            addStatement("%L.append(%S)", sbVar, ")")

            // Create SQL from string builder and prepare
            addLocalVal(outSqlQueryName, CommonTypeNames.STRING, "%L.toString()", sbVar)
            addLocalVal(
                outStmtName,
                SQLiteDriverTypeNames.STATEMENT,
                "%L.prepare(%L)",
                connectionVarName,
                outSqlQueryName,
            )

            // Bind arguments
            val argIndexVar = scope.getTmpVar("_argIndex")
            addLocalVariable(
                argIndexVar,
                XTypeName.PRIMITIVE_INT,
                isMutable = true,
                assignExpr = XCodeBlock.of("1"),
            )
            queryParam.queryParamAdapter.bindToStmt(
                inputVarName = inputVarName,
                stmtVarName = outStmtName,
                startIndexVarName = argIndexVar,
                scope = scope,
            )
        }
    }
}

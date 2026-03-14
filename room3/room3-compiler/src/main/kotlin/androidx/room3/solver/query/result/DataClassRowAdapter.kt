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

package androidx.room3.solver.query.result

import androidx.room3.compiler.processing.XType
import androidx.room3.ext.SQLiteDriverMemberNames
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.solver.CodeGenScope
import androidx.room3.verifier.QueryResultInfo
import androidx.room3.vo.ColumnIndexVar
import androidx.room3.vo.DataClass
import androidx.room3.vo.Property
import androidx.room3.vo.PropertyWithIndex
import androidx.room3.vo.RelationCollector
import androidx.room3.writer.PropertyReadWriteWriter

/**
 * Creates the entity from the given info.
 *
 * The info comes from the query processor so we know about the order of columns in the result etc.
 */
class DataClassRowAdapter(
    private val context: Context,
    private val info: QueryResultInfo?,
    private val query: ParsedQuery?,
    val dataClass: DataClass,
    out: XType,
) : QueryMappedRowAdapter(out) {
    override val mapping: DataClassMapping
    val relationCollectors: List<RelationCollector>

    private val indexAdapter: DataClassIndexAdapter

    // Set when statement is ready.
    private lateinit var propertiesWithIndices: List<PropertyWithIndex>

    init {
        val remainingProperties = dataClass.properties.toMutableList()
        val unusedColumns = arrayListOf<String>()
        val matchedProperties: List<Property>
        if (info != null) {
            matchedProperties =
                info.columns.mapNotNull { column ->
                    val property = remainingProperties.firstOrNull { it.columnName == column.name }
                    if (property == null) {
                        unusedColumns.add(column.name)
                        null
                    } else {
                        remainingProperties.remove(property)
                        property
                    }
                }
            val nonNulls = remainingProperties.filter { it.nonNull }
            if (nonNulls.isNotEmpty()) {
                context.logger.e(
                    ProcessorErrors.dataClassMissingNonNull(
                        dataClassTypeName = dataClass.typeName.toString(context.codeLanguage),
                        missingDataClassProperties = nonNulls.map { it.name },
                        allQueryColumns = info.columns.map { it.name },
                    )
                )
            }
            if (matchedProperties.isEmpty()) {
                context.logger.e(
                    ProcessorErrors.cannotFindQueryResultAdapter(
                        out.asTypeName().toString(context.codeLanguage)
                    )
                )
            }
        } else {
            matchedProperties = remainingProperties.map { it }
            remainingProperties.clear()
        }
        relationCollectors = RelationCollector.createCollectors(context, dataClass.relations)

        mapping =
            DataClassMapping(
                dataClass = dataClass,
                matchedProperties = matchedProperties,
                unusedColumns = unusedColumns,
                unusedProperties = remainingProperties,
            )

        indexAdapter = DataClassIndexAdapter(mapping, info, query)
    }

    fun relationTableNames(): List<String> {
        return relationCollectors
            .flatMap {
                val queryTableNames = it.loadAllQuery.tables.map { it.name }
                if (it.rowAdapter is DataClassRowAdapter) {
                    it.rowAdapter.relationTableNames() + queryTableNames
                } else {
                    queryTableNames
                }
            }
            .distinct()
    }

    override fun onStatementReady(
        stmtVarName: String,
        scope: CodeGenScope,
        indices: List<ColumnIndexVar>,
    ) {
        propertiesWithIndices =
            indices.map { (column, indexVar) ->
                val property = mapping.matchedProperties.first { it.columnName == column }
                PropertyWithIndex(
                    property = property,
                    indexVar = indexVar,
                    alwaysExists = info != null,
                )
            }
        emitRelationCollectorsReady(stmtVarName, scope)
    }

    private fun emitRelationCollectorsReady(stmtVarName: String, scope: CodeGenScope) {
        if (relationCollectors.isNotEmpty()) {
            relationCollectors.forEach { it.writeInitCode(scope) }
            scope.builder.apply {
                beginControlFlow(
                        "while (%L.%M())",
                        stmtVarName,
                        SQLiteDriverMemberNames.STATEMENT_STEP,
                    )
                    .apply {
                        relationCollectors.forEach {
                            it.writeReadParentKeyCode(stmtVarName, propertiesWithIndices, scope)
                        }
                    }
                endControlFlow()
                addStatement("%L.reset()", stmtVarName)
            }
            relationCollectors.forEach { it.writeFetchRelationCall(scope) }
        }
    }

    override fun convert(outVarName: String, stmtVarName: String, scope: CodeGenScope) {
        PropertyReadWriteWriter.readFromStatement(
            outVar = outVarName,
            outDataClass = dataClass,
            stmtVar = stmtVarName,
            propertiesWithIndices = propertiesWithIndices,
            relationCollectors = relationCollectors,
            scope = scope,
        )
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class DataClassMapping(
        val dataClass: DataClass,
        val matchedProperties: List<Property>,
        val unusedColumns: List<String>,
        val unusedProperties: List<Property>,
    ) : Mapping() {
        override val usedColumns = matchedProperties.map { it.columnName }
    }
}

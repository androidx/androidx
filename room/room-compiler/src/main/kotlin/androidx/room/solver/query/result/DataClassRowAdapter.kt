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

package androidx.room.solver.query.result

import androidx.room.compiler.processing.XType
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.DataClass
import androidx.room.vo.Property
import androidx.room.vo.PropertyWithIndex
import androidx.room.vo.RelationCollector
import androidx.room.writer.PropertyReadWriteWriter

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
    out: XType
) : QueryMappedRowAdapter(out) {
    override val mapping: DataClassMapping
    val relationCollectors: List<RelationCollector>

    private val indexAdapter: DataClassIndexAdapter

    // Set when statement is ready.
    private lateinit var fieldsWithIndices: List<PropertyWithIndex>

    init {
        val remainingFields = dataClass.properties.toMutableList()
        val unusedColumns = arrayListOf<String>()
        val matchedFields: List<Property>
        if (info != null) {
            matchedFields =
                info.columns.mapNotNull { column ->
                    val field = remainingFields.firstOrNull { it.columnName == column.name }
                    if (field == null) {
                        unusedColumns.add(column.name)
                        null
                    } else {
                        remainingFields.remove(field)
                        field
                    }
                }
            val nonNulls = remainingFields.filter { it.nonNull }
            if (nonNulls.isNotEmpty()) {
                context.logger.e(
                    ProcessorErrors.dataClassMissingNonNull(
                        dataClassTypeName = dataClass.typeName.toString(context.codeLanguage),
                        missingDataClassProperties = nonNulls.map { it.name },
                        allQueryColumns = info.columns.map { it.name }
                    )
                )
            }
            if (matchedFields.isEmpty()) {
                context.logger.e(
                    ProcessorErrors.cannotFindQueryResultAdapter(
                        out.asTypeName().toString(context.codeLanguage)
                    )
                )
            }
        } else {
            matchedFields = remainingFields.map { it }
            remainingFields.clear()
        }
        relationCollectors = RelationCollector.createCollectors(context, dataClass.relations)

        mapping =
            DataClassMapping(
                dataClass = dataClass,
                matchedFields = matchedFields,
                unusedColumns = unusedColumns,
                unusedFields = remainingFields
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
        indices: List<ColumnIndexVar>
    ) {
        fieldsWithIndices =
            indices.map { (column, indexVar) ->
                val field = mapping.matchedFields.first { it.columnName == column }
                PropertyWithIndex(
                    property = field,
                    indexVar = indexVar,
                    alwaysExists = info != null
                )
            }
        emitRelationCollectorsReady(stmtVarName, scope)
    }

    private fun emitRelationCollectorsReady(stmtVarName: String, scope: CodeGenScope) {
        if (relationCollectors.isNotEmpty()) {
            relationCollectors.forEach { it.writeInitCode(scope) }
            scope.builder.apply {
                beginControlFlow("while (%L.step())", stmtVarName).apply {
                    relationCollectors.forEach {
                        it.writeReadParentKeyCode(stmtVarName, fieldsWithIndices, scope)
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
            propertiesWithIndices = fieldsWithIndices,
            relationCollectors = relationCollectors,
            scope = scope
        )
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class DataClassMapping(
        val dataClass: DataClass,
        val matchedFields: List<Property>,
        val unusedColumns: List<String>,
        val unusedFields: List<Property>
    ) : Mapping() {
        override val usedColumns = matchedFields.map { it.columnName }
    }
}

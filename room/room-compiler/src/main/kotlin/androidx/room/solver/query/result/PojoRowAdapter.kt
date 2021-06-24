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
import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.Field
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Pojo
import androidx.room.vo.RelationCollector
import androidx.room.vo.findFieldByColumnName
import androidx.room.writer.FieldReadWriteWriter
import com.squareup.javapoet.TypeName
import java.util.Locale

/**
 * Creates the entity from the given info.
 * <p>
 * The info comes from the query processor so we know about the order of columns in the result etc.
 */
class PojoRowAdapter(
    context: Context,
    private val info: QueryResultInfo?,
    val pojo: Pojo,
    out: XType
) : RowAdapter(out) {
    val mapping: QueryMappedResultAdapter.Mapping
    val relationCollectors: List<RelationCollector>

    // Set when cursor is ready.
    lateinit var fieldsWithIndices: List<FieldWithIndex>

    init {

        // toMutableList documentation is not clear if it copies so lets be safe.
        val remainingFields = pojo.fields.mapTo(mutableListOf(), { it })
        val unusedColumns = arrayListOf<String>()
        val matchedFields: List<Field>
        if (info != null) {
            matchedFields = info.columns.mapNotNull { column ->
                // first check remaining, otherwise check any. maybe developer wants to map the same
                // column into 2 fields. (if they want to post process etc)
                val field = remainingFields.firstOrNull { it.columnName == column.name }
                    ?: pojo.findFieldByColumnName(column.name)
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
                    ProcessorErrors.pojoMissingNonNull(
                        pojoTypeName = pojo.typeName,
                        missingPojoFields = nonNulls.map { it.name },
                        allQueryColumns = info.columns.map { it.name }
                    )
                )
            }
            if (matchedFields.isEmpty()) {
                context.logger.e(ProcessorErrors.cannotFindQueryResultAdapter(out.typeName))
            }
        } else {
            matchedFields = remainingFields.map { it }
            remainingFields.clear()
        }
        relationCollectors = RelationCollector.createCollectors(context, pojo.relations)

        mapping = QueryMappedResultAdapter.Mapping(
            pojo = pojo,
            matchedFields = matchedFields,
            unusedColumns = unusedColumns,
            unusedFields = remainingFields
        )
    }

    fun relationTableNames(): List<String> {
        return relationCollectors.flatMap {
            val queryTableNames = it.loadAllQuery.tables.map { it.name }
            if (it.rowAdapter is PojoRowAdapter) {
                it.rowAdapter.relationTableNames() + queryTableNames
            } else {
                queryTableNames
            }
        }.distinct()
    }

    override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
        fieldsWithIndices = mapping.matchedFields.map {
            val indexVar = scope.getTmpVar(
                "_cursorIndexOf${it.name.stripNonJava().capitalize(Locale.US)}"
            )
            val indexMethod = if (info == null) {
                "getColumnIndex"
            } else {
                "getColumnIndexOrThrow"
            }
            scope.builder().addStatement(
                "final $T $L = $T.$L($L, $S)",
                TypeName.INT, indexVar, RoomTypeNames.CURSOR_UTIL, indexMethod, cursorVarName,
                it.columnName
            )
            FieldWithIndex(field = it, indexVar = indexVar, alwaysExists = info != null)
        }
        if (relationCollectors.isNotEmpty()) {
            relationCollectors.forEach { it.writeInitCode(scope) }
            scope.builder().apply {
                beginControlFlow("while ($L.moveToNext())", cursorVarName).apply {
                    relationCollectors.forEach {
                        it.writeReadParentKeyCode(cursorVarName, fieldsWithIndices, scope)
                    }
                }
                endControlFlow()
            }
            scope.builder().addStatement("$L.moveToPosition(-1)", cursorVarName)
            relationCollectors.forEach { it.writeCollectionCode(scope) }
        }
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            FieldReadWriteWriter.readFromCursor(
                outVar = outVarName,
                outPojo = pojo,
                cursorVar = cursorVarName,
                fieldsWithIndices = fieldsWithIndices,
                relationCollectors = relationCollectors,
                scope = scope
            )
        }
    }
}

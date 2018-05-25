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

import androidx.room.ext.L
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.Field
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Pojo
import androidx.room.vo.RelationCollector
import androidx.room.vo.Warning
import androidx.room.writer.FieldReadWriteWriter
import com.squareup.javapoet.TypeName
import stripNonJava
import javax.lang.model.type.TypeMirror

/**
 * Creates the entity from the given info.
 * <p>
 * The info comes from the query processor so we know about the order of columns in the result etc.
 */
class PojoRowAdapter(
        context: Context, private val info: QueryResultInfo?,
        val pojo: Pojo, out: TypeMirror) : RowAdapter(out) {
    val mapping: Mapping
    val relationCollectors: List<RelationCollector>

    init {
        // toMutableList documentation is not clear if it copies so lets be safe.
        val remainingFields = pojo.fields.mapTo(mutableListOf(), { it })
        val unusedColumns = arrayListOf<String>()
        val matchedFields: List<Field>
        if (info != null) {
            matchedFields = info.columns.mapNotNull { column ->
                // first check remaining, otherwise check any. maybe developer wants to map the same
                // column into 2 fields. (if they want to post process etc)
                val field = remainingFields.firstOrNull { it.columnName == column.name } ?:
                        pojo.fields.firstOrNull { it.columnName == column.name }
                if (field == null) {
                    unusedColumns.add(column.name)
                    null
                } else {
                    remainingFields.remove(field)
                    field
                }
            }
            if (unusedColumns.isNotEmpty() || remainingFields.isNotEmpty()) {
                val warningMsg = ProcessorErrors.cursorPojoMismatch(
                        pojoTypeName = pojo.typeName,
                        unusedColumns = unusedColumns,
                        allColumns = info.columns.map { it.name },
                        unusedFields = remainingFields,
                        allFields = pojo.fields
                )
                context.logger.w(Warning.CURSOR_MISMATCH, null, warningMsg)
            }
            val nonNulls = remainingFields.filter { it.nonNull }
            if (nonNulls.isNotEmpty()) {
                context.logger.e(ProcessorErrors.pojoMissingNonNull(
                        pojoTypeName = pojo.typeName,
                        missingPojoFields = nonNulls.map { it.name },
                        allQueryColumns = info.columns.map { it.name }))
            }
            if (matchedFields.isEmpty()) {
                context.logger.e(ProcessorErrors.CANNOT_FIND_QUERY_RESULT_ADAPTER)
            }
        } else {
            matchedFields = remainingFields.map { it }
            remainingFields.clear()
        }
        relationCollectors = RelationCollector.createCollectors(context, pojo.relations)

        mapping = Mapping(
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
        relationCollectors.forEach { it.writeInitCode(scope) }
        mapping.fieldsWithIndices = mapping.matchedFields.map {
            val indexVar = scope.getTmpVar("_cursorIndexOf${it.name.stripNonJava().capitalize()}")
            val indexMethod = if (info == null) {
                "getColumnIndex"
            } else {
                "getColumnIndexOrThrow"
            }
            scope.builder().addStatement("final $T $L = $L.$L($S)",
                    TypeName.INT, indexVar, cursorVarName, indexMethod, it.columnName)
            FieldWithIndex(field = it, indexVar = indexVar, alwaysExists = info != null)
        }
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            FieldReadWriteWriter.readFromCursor(
                    outVar = outVarName,
                    outPojo = pojo,
                    cursorVar = cursorVarName,
                    fieldsWithIndices = mapping.fieldsWithIndices,
                    relationCollectors = relationCollectors,
                    scope = scope)
        }
    }

    override fun onCursorFinished(): ((CodeGenScope) -> Unit)? =
            if (relationCollectors.isEmpty()) {
                // it is important to return empty to notify that we don't need any post process
                // task
                null
            } else {
                { scope ->
                    relationCollectors.forEach { collector ->
                        collector.writeCollectionCode(scope)
                    }
                }
            }

    data class Mapping(
            val matchedFields: List<Field>,
            val unusedColumns: List<String>,
            val unusedFields: List<Field>) {
        // set when cursor is ready.
        lateinit var fieldsWithIndices: List<FieldWithIndex>
    }
}

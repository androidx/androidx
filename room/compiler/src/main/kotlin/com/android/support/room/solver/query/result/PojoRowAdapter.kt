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

package com.android.support.room.solver.query.result

import com.android.support.room.RoomWarnings
import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.processor.Context
import com.android.support.room.processor.ProcessorErrors
import com.android.support.room.processor.SuppressWarningProcessor
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.verifier.QueryResultInfo
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Field
import com.android.support.room.vo.Pojo
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * Creates the entity from the given info.
 * <p>
 * The info comes from the query processor so we know about the order of columns in the result etc.
 */
class PojoRowAdapter(val info: QueryResultInfo, val pojo: Pojo, out: TypeMirror) : RowAdapter(out) {
    val mapping: Mapping

    init {
        // toMutableList documentation is not clear if it copies so lets be safe.
        val remainingFields = pojo.fields.mapTo(mutableListOf<Field>(), { it })
        val unusedColumns = arrayListOf<String>()
        val associations = info.columns.mapIndexed { index, column ->
            // first check remaining, otherwise check any. maybe developer wants to map the same
            // column into 2 fields. (if they want to post process etc)
            val field = remainingFields.firstOrNull { it.columnName == column.name } ?:
                    pojo.fields.firstOrNull { it.columnName == column.name }
            if (field == null) {
                unusedColumns.add(column.name)
                null
            } else {
                remainingFields.remove(field)
                Association(index, field)
            }
        }.filterNotNull()
        mapping = Mapping(
                associations = associations,
                unusedColumns = unusedColumns,
                unusedFields = remainingFields
        )
    }

    override fun reportErrors(context: Context, element: Element, suppressedWarnings: Set<String>) {
        if (!SuppressWarningProcessor.isSuppressed(RoomWarnings.CURSOR_MISMATCH, suppressedWarnings)
                && (mapping.unusedColumns.isNotEmpty() || mapping.unusedFields.isNotEmpty())) {
            context.logger.w(element, ProcessorErrors.cursorPojoMismatch(
                    pojoTypeName = pojo.typeName,
                    unusedColumns = mapping.unusedColumns,
                    allColumns = info.columns.map { it.name },
                    unusedFields = mapping.unusedFields,
                    allFields = pojo.fields
            ))
        }
        if (mapping.associations.isEmpty()) {
            // Nothing matched. Clearly an error.
            context.logger.e(element, ProcessorErrors.CANNOT_FIND_QUERY_RESULT_ADAPTER)
        }
    }

    override fun init(cursorVarName: String, scope: CodeGenScope): RowConverter {
        return object : RowConverter {
            override fun convert(outVarName: String, cursorVarName: String) {
                scope.builder().apply {
                    addStatement("$L = new $T()", outVarName, out.typeName())
                    mapping.associations.forEach { mapping ->
                        val field = mapping.field
                        val index = mapping.index
                        val columnAdapter = field.getter.columnAdapter
                        when (field.setter.callType) {
                            CallType.FIELD -> {
                                columnAdapter?.readFromCursor("$outVarName.${field.getter.name}",
                                        cursorVarName, index.toString(), scope)
                            }
                            CallType.METHOD -> {
                                val tmpField = scope.getTmpVar("_tmp${field.name.capitalize()}")
                                addStatement("final $T $L", field.getter.type.typeName(), tmpField)
                                columnAdapter?.readFromCursor(tmpField, cursorVarName,
                                        index.toString(), scope)
                                addStatement("$L.$L($L)", outVarName, field.setter.name, tmpField)
                            }
                        }
                    }
                }
            }
        }
    }

    data class Association(val index: Int, val field: Field)

    data class Mapping(val associations: List<Association>,
                       val unusedColumns: List<String>,
                       val unusedFields: List<Field>)
}

/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room.AmbiguousColumnResolver
import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.DoubleArrayLiteral
import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.T
import androidx.room.ext.W
import androidx.room.parser.ParsedQuery
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ColumnIndexVar
import com.squareup.javapoet.TypeName

/**
 * An index adapter that uses [AmbiguousColumnResolver] to create the index variables for
 * [QueryMappedRowAdapter]s.
 */
class AmbiguousColumnIndexAdapter(
    private val mappings: List<QueryMappedRowAdapter.Mapping>,
    private val query: ParsedQuery
) : IndexAdapter {

    lateinit var mappingToIndexVars: Map<QueryMappedRowAdapter.Mapping, List<ColumnIndexVar>>

    /**
     * Generates code that initializes and fills-in the return object mapping to query
     * result column indices. This function will store the name of the variable where the resolved
     * indices are located and can be retrieve via the [getIndexVarsForMapping] function.
     */
    override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
        val cursorIndexMappingVarName = scope.getTmpVar("_cursorIndices")
        scope.builder().apply {
            val resultInfo = query.resultInfo
            if (resultInfo != null && query.hasTopStarProjection == false) {
                // Query result columns are known, use ambiguous column resolver at compile-time
                // and generate arrays containing result object indices to query column index.
                val cursorIndices = AmbiguousColumnResolver.resolve(
                    resultColumns = resultInfo.columns.map { it.name }.toTypedArray(),
                    mappings = mappings.map { it.usedColumns.toTypedArray() }.toTypedArray()
                )
                val rowMappings = DoubleArrayLiteral(
                    type = TypeName.INT,
                    rowSize = cursorIndices.size,
                    columnSizeProducer = { i -> cursorIndices[i].size },
                    valueProducer = { i, j -> cursorIndices[i][j] }
                )
                addStatement(
                    "final $T[][] $L = $L",
                    TypeName.INT,
                    cursorIndexMappingVarName,
                    rowMappings
                )
            } else {
                // Generate code that uses ambiguous column resolver at runtime, providing the
                // query result column names from the Cursor and the result object column names in
                // an array literal.
                val rowMappings = DoubleArrayLiteral(
                    type = CommonTypeNames.STRING.toJavaPoet(),
                    rowSize = mappings.size,
                    columnSizeProducer = { i -> mappings[i].usedColumns.size },
                    valueProducer = { i, j -> mappings[i].usedColumns[j] }
                )
                addStatement(
                    "final $T[][] $L = $T.resolve($L.getColumnNames(),$W$L)",
                    TypeName.INT,
                    cursorIndexMappingVarName,
                    RoomTypeNames.AMBIGUOUS_COLUMN_RESOLVER,
                    cursorVarName,
                    rowMappings
                )
            }
        }
        mappingToIndexVars = buildMap {
            mappings.forEachIndexed { i, mapping ->
                val indexVars = mapping.usedColumns.mapIndexed { j, columnName ->
                    ColumnIndexVar(
                        column = columnName,
                        indexVar = "$cursorIndexMappingVarName[$i][$j]"
                    )
                }
                put(mapping, indexVars)
            }
        }
    }

    override fun getIndexVars() = mappingToIndexVars.values.flatten()

    fun getIndexVarsForMapping(mapping: QueryMappedRowAdapter.Mapping) =
        mappingToIndexVars.getValue(mapping)
}

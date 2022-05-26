/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.room.ext.W
import androidx.room.ext.implementsEqualsAndHashcode
import androidx.room.log.RLog
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.types.CursorValueReader
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.MapInfo
import androidx.room.vo.Warning
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock

/**
 * Abstract class for Map and Multimap result adapters.
 */
abstract class MultimapQueryResultAdapter(
    context: Context,
    parsedQuery: ParsedQuery,
    rowAdapters: List<RowAdapter>,
) : QueryResultAdapter(rowAdapters) {
    abstract val keyTypeArg: XType
    abstract val valueTypeArg: XType

    // List of duplicate columns in the query result. Note that if the query result info is not
    // available then we use the adapter mappings to determine if there are duplicate columns.
    // The latter approach might yield false positive (i.e. two POJOs that want the same column)
    // but the resolver will still produce correct results based on the result columns at runtime.
    val duplicateColumns: Set<String>

    init {
        val resultColumns =
            parsedQuery.resultInfo?.columns?.map { it.name } ?: mappings.flatMap { it.usedColumns }
        duplicateColumns = buildSet {
            val visitedColumns = mutableSetOf<String>()
            resultColumns.forEach {
                // When Set.add() returns false the column is already visited and therefore a dupe.
                if (!visitedColumns.add(it)) {
                    add(it)
                }
            }
        }

        if (parsedQuery.resultInfo != null && duplicateColumns.isNotEmpty()) {
            // If there are duplicate columns and one of the result object is for a single column
            // then we should warn the user to disambiguate in the query projections since the
            // current AmbiguousColumnResolver will choose the first matching column. Only show
            // this warning if the query has been analyzed or else we risk false positives.
            mappings.filter {
                it.usedColumns.size == 1 && duplicateColumns.contains(it.usedColumns.first())
            }.forEach {
                val ambiguousColumnName = it.usedColumns.first()
                val (location, objectTypeName) = when (it) {
                    is SingleNamedColumnRowAdapter.SingleNamedColumnRowMapping ->
                        ProcessorErrors.AmbiguousColumnLocation.MAP_INFO to null
                    is PojoRowAdapter.PojoMapping ->
                        ProcessorErrors.AmbiguousColumnLocation.POJO to it.pojo.typeName
                    is EntityRowAdapter.EntityMapping ->
                        ProcessorErrors.AmbiguousColumnLocation.ENTITY to it.entity.typeName
                    else -> error("Unknown mapping type: $it")
                }
                context.logger.w(
                    Warning.AMBIGUOUS_COLUMN_IN_RESULT,
                    ProcessorErrors.ambiguousColumn(ambiguousColumnName, location, objectTypeName)
                )
            }
        }
    }

    companion object {

        val declaredToImplCollection = mapOf<ClassName, ClassName>(
            ClassName.get(List::class.java) to ClassName.get(ArrayList::class.java),
            ClassName.get(Set::class.java) to ClassName.get(HashSet::class.java)
        )

        /**
         * Checks if the @MapInfo annotation is needed for clarification regarding the return type
         * of a Dao method.
         */
        fun validateMapTypeArgs(
            keyTypeArg: XType,
            valueTypeArg: XType,
            keyReader: CursorValueReader?,
            valueReader: CursorValueReader?,
            mapInfo: MapInfo?,
            logger: RLog
        ) {

            if (!keyTypeArg.implementsEqualsAndHashcode()) {
                logger.w(
                    Warning.DOES_NOT_IMPLEMENT_EQUALS_HASHCODE,
                    ProcessorErrors.classMustImplementEqualsAndHashCode(
                        keyTypeArg.typeName.toString()
                    )
                )
            }

            val hasKeyColumnName = mapInfo?.keyColumnName?.isNotEmpty() ?: false
            if (!hasKeyColumnName && keyReader != null) {
                logger.e(
                    ProcessorErrors.keyMayNeedMapInfo(
                        keyTypeArg.typeName
                    )
                )
            }

            val hasValueColumnName = mapInfo?.valueColumnName?.isNotEmpty() ?: false
            if (!hasValueColumnName && valueReader != null) {
                logger.e(
                    ProcessorErrors.valueMayNeedMapInfo(
                        valueTypeArg.typeName
                    )
                )
            }
        }
    }

    /**
     * Generates a code expression that verifies if all matched fields are null.
     */
    fun getColumnNullCheckCode(
        cursorVarName: String,
        indexVars: List<ColumnIndexVar>
    ): CodeBlock {
        val conditions = indexVars.map {
            CodeBlock.of(
                "$L.isNull($L)",
                cursorVarName,
                it.indexVar
            )
        }
        return CodeBlock.join(conditions, "$W&&$W")
    }
}

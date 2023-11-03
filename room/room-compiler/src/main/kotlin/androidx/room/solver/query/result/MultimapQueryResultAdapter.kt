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

import androidx.room.MapColumn
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XType
import androidx.room.ext.CollectionTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.implementsEqualsAndHashcode
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.processor.ProcessorErrors.AmbiguousColumnLocation.ENTITY
import androidx.room.processor.ProcessorErrors.AmbiguousColumnLocation.MAP_INFO
import androidx.room.processor.ProcessorErrors.AmbiguousColumnLocation.POJO
import androidx.room.solver.types.CursorValueReader
import androidx.room.verifier.ColumnInfo
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.Warning

/**
 * Abstract class for Map and Multimap result adapters.
 */
abstract class MultimapQueryResultAdapter(
    context: Context,
    parsedQuery: ParsedQuery,
    rowAdapters: List<RowAdapter>,
) : QueryResultAdapter(rowAdapters) {

    // List of duplicate columns in the query result. Note that if the query result info is not
    // available then we use the adapter mappings to determine if there are duplicate columns.
    // The latter approach might yield false positive (i.e. two POJOs that want the same column)
    // but the resolver will still produce correct results based on the result columns at runtime.
    val duplicateColumns: Set<String>

    val dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?

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
        dupeColumnsIndexAdapter = if (duplicateColumns.isNotEmpty()) {
            AmbiguousColumnIndexAdapter(mappings, parsedQuery)
        } else {
            null
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
                        MAP_INFO to null
                    is PojoRowAdapter.PojoMapping ->
                        POJO to it.pojo.typeName
                    is EntityRowAdapter.EntityMapping ->
                        ENTITY to it.entity.typeName
                    else -> error("Unknown mapping type: $it")
                }
                context.logger.w(
                    Warning.AMBIGUOUS_COLUMN_IN_RESULT,
                    ProcessorErrors.ambiguousColumn(
                        columnName = ambiguousColumnName,
                        location = location,
                        typeName = objectTypeName?.toString(context.codeLanguage)
                    )
                )
            }
        }
    }

    enum class MapType(val className: XClassName) {
        DEFAULT(CommonTypeNames.MUTABLE_MAP),
        ARRAY_MAP(CollectionTypeNames.ARRAY_MAP),
        LONG_SPARSE(CollectionTypeNames.LONG_SPARSE_ARRAY),
        INT_SPARSE(CollectionTypeNames.INT_SPARSE_ARRAY);

        companion object {
            fun MapType.isSparseArray() = this == LONG_SPARSE || this == INT_SPARSE
        }
    }

    enum class CollectionValueType(val className: XClassName) {
        LIST(CommonTypeNames.MUTABLE_LIST),
        SET(CommonTypeNames.MUTABLE_SET)
    }

    companion object {

        /**
         * Checks if the @MapColumn annotation is needed for clarification regarding the key type
         * arg of a Map return type.
         */
        fun validateMapKeyTypeArg(
            context: Context,
            keyTypeArg: XType,
            keyReader: CursorValueReader?,
            keyColumnName: String?,
        ) {
            if (!keyTypeArg.implementsEqualsAndHashcode()) {
                context.logger.w(
                    Warning.DOES_NOT_IMPLEMENT_EQUALS_HASHCODE,
                    ProcessorErrors.classMustImplementEqualsAndHashCode(
                        keyTypeArg.asTypeName().toString(context.codeLanguage)
                    )
                )
            }

            val hasKeyColumnName = keyColumnName?.isNotEmpty() ?: false
            if (!hasKeyColumnName && keyReader != null) {
                context.logger.e(
                    ProcessorErrors.mayNeedMapColumn(
                        keyTypeArg.asTypeName().toString(context.codeLanguage)
                    )
                )
            }
        }

        /**
         * Checks if the @MapColumn annotation is needed for clarification regarding the value type
         * arg of a Map return type.
         */
        fun validateMapValueTypeArg(
            context: Context,
            valueTypeArg: XType,
            valueReader: CursorValueReader?,
            valueColumnName: String?,
        ) {
            val hasValueColumnName = valueColumnName?.isNotEmpty() ?: false
            if (!hasValueColumnName && valueReader != null) {
                context.logger.e(
                    ProcessorErrors.mayNeedMapColumn(
                        valueTypeArg.asTypeName().toString(context.codeLanguage)
                    )
                )
            }
        }

        /**
         * Retrieves the `columnName` value from a @MapColumn annotation.
         */
        fun getMapColumnName(context: Context, query: ParsedQuery, type: XType): String? {
            val resultColumns = query.resultInfo?.columns
            val resultTableAliases = query.tables.associate { it.name to it.alias }
            val annotation = type.getAnnotation(MapColumn::class.asClassName()) ?: return null

            val mapColumnName = annotation.getAsString("columnName")
            val mapColumnTableName = annotation.getAsString("tableName")

            fun List<ColumnInfo>.contains(
                columnName: String,
                tableName: String?
            ) = any { resultColumn ->
                val resultTableAlias = resultColumn.originTable?.let {
                    resultTableAliases[it] ?: it
                }
                resultColumn.name == columnName && (
                    if (!tableName.isNullOrEmpty()) {
                        resultTableAlias == tableName || resultColumn.originTable == tableName
                    } else true)
            }

            if (resultColumns != null) {
                // Disambiguation check for MapColumn
                if (!resultColumns.contains(mapColumnName, mapColumnTableName)) {
                    val errorColumn = if (mapColumnTableName.isNotEmpty()) {
                        "$mapColumnTableName."
                    } else {
                        ""
                    } + mapColumnName
                    context.logger.e(
                        ProcessorErrors.cannotMapSpecifiedColumn(
                            errorColumn,
                            resultColumns.map { it.name },
                            MapColumn::class.java.simpleName
                        )
                    )
                }
            }
            return mapColumnName
        }
    }

    /**
     * Generates a code expression that verifies if all matched fields are null.
     */
    fun getColumnNullCheckCode(
        language: CodeLanguage,
        cursorVarName: String,
        indexVars: List<ColumnIndexVar>
    ) = XCodeBlock.builder(language).apply {
        val space = when (language) {
            CodeLanguage.JAVA -> "%W"
            CodeLanguage.KOTLIN -> " "
        }
        val conditions = indexVars.map {
            XCodeBlock.of(
                language,
                "%L.isNull(%L)",
                cursorVarName,
                it.indexVar
            )
        }
        val placeholders = conditions.joinToString(separator = "$space&&$space") { "%L" }
        add(placeholders, *conditions.toTypedArray())
    }.build()
}

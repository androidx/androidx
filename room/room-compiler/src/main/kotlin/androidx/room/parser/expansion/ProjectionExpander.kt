/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.parser.expansion

import androidx.annotation.VisibleForTesting
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SqlParser
import androidx.room.processor.QueryRewriter
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Entity
import androidx.room.vo.EntityOrView
import androidx.room.vo.Field
import androidx.room.vo.Pojo
import androidx.room.vo.columnNames
import java.util.Locale

/**
 * Interprets and rewrites SQL queries in the context of the provided entities and views such that
 * star projection (select *) turn into explicit column lists and embedded fields are re-named to
 * avoid conflicts in the response data set.
 */
class ProjectionExpander(
    private val tables: List<EntityOrView>
) : QueryRewriter {

    private class IdentifierMap<V> : HashMap<String, V>() {
        override fun put(key: String, value: V): V? {
            return super.put(key.lowercase(Locale.ENGLISH), value)
        }

        override fun get(key: String): V? {
            return super.get(key.lowercase(Locale.ENGLISH))
        }
    }

    /**
     * Rewrites the specified [query] in the context of the provided [pojo]. Expanding its start
     * projection ('SELECT *') and converting its named binding templates to positional
     * templates (i.e. ':VVV' to '?').
     */
    @VisibleForTesting
    fun interpret(
        query: ParsedQuery,
        pojo: Pojo?
    ) = interpret(
        query = ExpandableSqlParser.parse(query.original).also {
            it.resultInfo = query.resultInfo
        },
        pojo = pojo
    )

    override fun rewrite(
        query: ParsedQuery,
        resultAdapter: QueryResultAdapter
    ): ParsedQuery {
        if (resultAdapter.rowAdapters.isEmpty()) {
            return query
        }
        // Don't know how to expand when multiple POJO types are created from the same row.
        if (resultAdapter.rowAdapters.size > 1) {
            return query
        }
        val rowAdapter = resultAdapter.rowAdapters.single()
        return if (rowAdapter is PojoRowAdapter) {
            interpret(
                query = ExpandableSqlParser.parse(query.original),
                pojo = rowAdapter.pojo
            ).let {
                val reParsed = SqlParser.parse(it)
                if (reParsed.errors.isEmpty()) {
                    reParsed
                } else {
                    query // return original, expansion somewhat failed
                }
            }
        } else {
            query
        }
    }

    private fun interpret(
        query: ExpandableParsedQuery,
        pojo: Pojo?
    ): String {
        val queriedTableNames = query.tables.map { it.name }
        return query.sections.joinToString("") { section ->
            when (section) {
                is ExpandableSection.Text -> section.text
                is ExpandableSection.BindVar -> "?"
                is ExpandableSection.Newline -> "\n"
                is ExpandableSection.Projection -> {
                    if (pojo == null) {
                        section.text
                    } else {
                        interpretProjection(query, section, pojo, queriedTableNames)
                    }
                }
            }
        }
    }

    private fun interpretProjection(
        query: ExpandableParsedQuery,
        section: ExpandableSection.Projection,
        pojo: Pojo,
        queriedTableNames: List<String>
    ): String {
        val aliasToName = query.tables
            .map { (name, alias) -> alias to name }
            .toMap(IdentifierMap())
        val nameToAlias = query.tables
            .groupBy { it.name.lowercase(Locale.ENGLISH) }
            .filter { (_, pairs) -> pairs.size == 1 }
            .map { (name, pairs) -> name to pairs.first().alias }
            .toMap(IdentifierMap())
        return when (section) {
            is ExpandableSection.Projection.All -> {
                expand(
                    pojo = pojo,
                    ignoredColumnNames = query.explicitColumns,
                    // The columns come directly from the specified table.
                    // We should not prepend the prefix-dot to the columns.
                    shallow = findEntityOrView(pojo)?.tableName in queriedTableNames,
                    nameToAlias = nameToAlias,
                    resultInfo = query.resultInfo
                )
            }
            is ExpandableSection.Projection.Table -> {
                val embedded = findEmbeddedField(pojo, section.tableAlias)
                if (embedded != null) {
                    expandEmbeddedField(
                        embedded = embedded,
                        table = findEntityOrView(embedded.pojo),
                        shallow = false,
                        tableToAlias = nameToAlias
                    ).joinToString(", ")
                } else {
                    val tableName =
                        aliasToName[section.tableAlias] ?: section.tableAlias
                    val table = tables.find { it.tableName == tableName }
                    pojo.fields.filter { field ->
                        field.parent == null &&
                            field.columnName !in query.explicitColumns &&
                            table?.columnNames?.contains(field.columnName) == true
                    }.joinToString(", ") { field ->
                        "`${section.tableAlias}`.`${field.columnName}`"
                    }
                }
            }
        }
    }

    private fun findEntityOrView(pojo: Pojo): EntityOrView? {
        return tables.find { it.typeName == pojo.typeName }
    }

    private fun findEmbeddedField(
        pojo: Pojo,
        tableAlias: String
    ): EmbeddedField? {
        // Try to find by the prefix.
        val matchByPrefix = pojo.embeddedFields.find { it.prefix == tableAlias }
        if (matchByPrefix != null) {
            return matchByPrefix
        }
        // Try to find by the table name.
        return pojo.embeddedFields.find {
            it.prefix.isEmpty() &&
                findEntityOrView(it.pojo)?.tableName == tableAlias
        }
    }

    private fun expand(
        pojo: Pojo,
        ignoredColumnNames: List<String>,
        shallow: Boolean,
        nameToAlias: Map<String, String>,
        resultInfo: QueryResultInfo?
    ): String {
        val table = findEntityOrView(pojo)
        return (
            pojo.embeddedFields.flatMap {
                expandEmbeddedField(it, findEntityOrView(it.pojo), shallow, nameToAlias)
            } + pojo.fields.filter { field ->
                field.parent == null &&
                    field.columnName !in ignoredColumnNames &&
                    (resultInfo == null || resultInfo.hasColumn(field.columnName))
            }.map { field ->
                if (table != null && table is Entity) {
                    // Should not happen when defining a view
                    val tableAlias = nameToAlias[table.tableName.lowercase(Locale.ENGLISH)]
                        ?: table.tableName
                    "`$tableAlias`.`${field.columnName}` AS `${field.columnName}`"
                } else {
                    "`${field.columnName}`"
                }
            }
            ).joinToString(", ")
    }

    private fun QueryResultInfo.hasColumn(columnName: String): Boolean {
        return columns.any { column -> column.name == columnName }
    }

    private fun expandEmbeddedField(
        embedded: EmbeddedField,
        table: EntityOrView?,
        shallow: Boolean,
        tableToAlias: Map<String, String>
    ): List<String> {
        val pojo = embedded.pojo
        return if (table != null) {
            if (embedded.prefix.isNotEmpty()) {
                table.fields.map { field ->
                    if (shallow) {
                        "`${embedded.prefix}${field.columnName}`"
                    } else {
                        "`${embedded.prefix}`.`${field.columnName}` " +
                            "AS `${embedded.prefix}${field.columnName}`"
                    }
                }
            } else {
                table.fields.map { field ->
                    if (shallow) {
                        "`${field.columnName}`"
                    } else {
                        val tableAlias = tableToAlias[table.tableName] ?: table.tableName
                        "`$tableAlias`.`${field.columnName}` AS `${field.columnName}`"
                    }
                }
            }
        } else {
            if (!shallow &&
                embedded.prefix.isNotEmpty() &&
                embedded.prefix in tableToAlias.values
            ) {
                pojo.fields.map { field ->
                    "`${embedded.prefix}`.`${field.columnNameWithoutPrefix(embedded.prefix)}` " +
                        "AS `${field.columnName}`"
                }
            } else {
                pojo.fields.map { field ->
                    "`${field.columnName}`"
                }
            }
        }
    }

    private fun Field.columnNameWithoutPrefix(prefix: String): String {
        return if (columnName.startsWith(prefix)) {
            columnName.substring(prefix.length)
        } else {
            columnName
        }
    }
}

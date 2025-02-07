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
import androidx.room.solver.query.result.DataClassRowAdapter
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.DataClass
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Entity
import androidx.room.vo.EntityOrView
import androidx.room.vo.Property
import androidx.room.vo.columnNames
import java.util.Locale

/**
 * Interprets and rewrites SQL queries in the context of the provided entities and views such that
 * star projection (select *) turn into explicit column lists and embedded properties are re-named
 * to avoid conflicts in the response data set.
 */
class ProjectionExpander(private val tables: List<EntityOrView>) : QueryRewriter {

    private class IdentifierMap<V> : HashMap<String, V>() {
        override fun put(key: String, value: V): V? {
            return super.put(key.lowercase(Locale.ENGLISH), value)
        }

        override fun get(key: String): V? {
            return super.get(key.lowercase(Locale.ENGLISH))
        }
    }

    /**
     * Rewrites the specified [query] in the context of the provided [dataClass]. Expanding its
     * start projection ('SELECT *') and converting its named binding templates to positional
     * templates (i.e. ':VVV' to '?').
     */
    @VisibleForTesting
    fun interpret(query: ParsedQuery, dataClass: DataClass?) =
        interpret(
            query =
                ExpandableSqlParser.parse(query.original).also { it.resultInfo = query.resultInfo },
            dataClass = dataClass
        )

    override fun rewrite(query: ParsedQuery, resultAdapter: QueryResultAdapter): ParsedQuery {
        if (resultAdapter.rowAdapters.isEmpty()) {
            return query
        }
        // Don't know how to expand when multiple POJO types are created from the same row.
        if (resultAdapter.rowAdapters.size > 1) {
            return query
        }
        val rowAdapter = resultAdapter.rowAdapters.single()
        return if (rowAdapter is DataClassRowAdapter) {
            interpret(
                    query = ExpandableSqlParser.parse(query.original),
                    dataClass = rowAdapter.dataClass
                )
                .let {
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

    private fun interpret(query: ExpandableParsedQuery, dataClass: DataClass?): String {
        val queriedTableNames = query.tables.map { it.name }
        return query.sections.joinToString("") { section ->
            when (section) {
                is ExpandableSection.Text -> section.text
                is ExpandableSection.BindVar -> "?"
                is ExpandableSection.Newline -> "\n"
                is ExpandableSection.Projection -> {
                    if (dataClass == null) {
                        section.text
                    } else {
                        interpretProjection(query, section, dataClass, queriedTableNames)
                    }
                }
            }
        }
    }

    private fun interpretProjection(
        query: ExpandableParsedQuery,
        section: ExpandableSection.Projection,
        dataClass: DataClass,
        queriedTableNames: List<String>
    ): String {
        val aliasToName = query.tables.map { (name, alias) -> alias to name }.toMap(IdentifierMap())
        val nameToAlias =
            query.tables
                .groupBy { it.name.lowercase(Locale.ENGLISH) }
                .filter { (_, pairs) -> pairs.size == 1 }
                .map { (name, pairs) -> name to pairs.first().alias }
                .toMap(IdentifierMap())
        return when (section) {
            is ExpandableSection.Projection.All -> {
                expand(
                    dataClass = dataClass,
                    ignoredColumnNames = query.explicitColumns,
                    // The columns come directly from the specified table.
                    // We should not prepend the prefix-dot to the columns.
                    shallow = findEntityOrView(dataClass)?.tableName in queriedTableNames,
                    nameToAlias = nameToAlias,
                    resultInfo = query.resultInfo
                )
            }
            is ExpandableSection.Projection.Table -> {
                val embedded = findEmbeddedProperty(dataClass, section.tableAlias)
                if (embedded != null) {
                    expandEmbeddedProperty(
                            embedded = embedded,
                            table = findEntityOrView(embedded.dataClass),
                            shallow = false,
                            tableToAlias = nameToAlias
                        )
                        .joinToString(", ")
                } else {
                    val tableName = aliasToName[section.tableAlias] ?: section.tableAlias
                    val table = tables.find { it.tableName == tableName }
                    dataClass.properties
                        .filter { property ->
                            property.parent == null &&
                                property.columnName !in query.explicitColumns &&
                                table?.columnNames?.contains(property.columnName) == true
                        }
                        .joinToString(", ") { property ->
                            "`${section.tableAlias}`.`${property.columnName}`"
                        }
                }
            }
        }
    }

    private fun findEntityOrView(dataClass: DataClass): EntityOrView? {
        return tables.find { it.typeName == dataClass.typeName }
    }

    private fun findEmbeddedProperty(dataClass: DataClass, tableAlias: String): EmbeddedProperty? {
        // Try to find by the prefix.
        val matchByPrefix = dataClass.embeddedProperties.find { it.prefix == tableAlias }
        if (matchByPrefix != null) {
            return matchByPrefix
        }
        // Try to find by the table name.
        return dataClass.embeddedProperties.find {
            it.prefix.isEmpty() && findEntityOrView(it.dataClass)?.tableName == tableAlias
        }
    }

    private fun expand(
        dataClass: DataClass,
        ignoredColumnNames: List<String>,
        shallow: Boolean,
        nameToAlias: Map<String, String>,
        resultInfo: QueryResultInfo?
    ): String {
        val table = findEntityOrView(dataClass)
        return (dataClass.embeddedProperties.flatMap {
                expandEmbeddedProperty(it, findEntityOrView(it.dataClass), shallow, nameToAlias)
            } +
                dataClass.properties
                    .filter { property ->
                        property.parent == null &&
                            property.columnName !in ignoredColumnNames &&
                            (resultInfo == null || resultInfo.hasColumn(property.columnName))
                    }
                    .map { property ->
                        if (table != null && table is Entity) {
                            // Should not happen when defining a view
                            val tableAlias =
                                nameToAlias[table.tableName.lowercase(Locale.ENGLISH)]
                                    ?: table.tableName
                            "`$tableAlias`.`${property.columnName}` AS `${property.columnName}`"
                        } else {
                            "`${property.columnName}`"
                        }
                    })
            .joinToString(", ")
    }

    private fun QueryResultInfo.hasColumn(columnName: String): Boolean {
        return columns.any { column -> column.name == columnName }
    }

    private fun expandEmbeddedProperty(
        embedded: EmbeddedProperty,
        table: EntityOrView?,
        shallow: Boolean,
        tableToAlias: Map<String, String>
    ): List<String> {
        val pojo = embedded.dataClass
        return if (table != null) {
            if (embedded.prefix.isNotEmpty()) {
                table.properties.map { property ->
                    if (shallow) {
                        "`${embedded.prefix}${property.columnName}`"
                    } else {
                        "`${embedded.prefix}`.`${property.columnName}` " +
                            "AS `${embedded.prefix}${property.columnName}`"
                    }
                }
            } else {
                table.properties.map { property ->
                    if (shallow) {
                        "`${property.columnName}`"
                    } else {
                        val tableAlias = tableToAlias[table.tableName] ?: table.tableName
                        "`$tableAlias`.`${property.columnName}` AS `${property.columnName}`"
                    }
                }
            }
        } else {
            if (
                !shallow && embedded.prefix.isNotEmpty() && embedded.prefix in tableToAlias.values
            ) {
                pojo.properties.map { property ->
                    "`${embedded.prefix}`.`${property.columnNameWithoutPrefix(embedded.prefix)}` " +
                        "AS `${property.columnName}`"
                }
            } else {
                pojo.properties.map { property -> "`${property.columnName}`" }
            }
        }
    }

    private fun Property.columnNameWithoutPrefix(prefix: String): String {
        return if (columnName.startsWith(prefix)) {
            columnName.substring(prefix.length)
        } else {
            columnName
        }
    }
}

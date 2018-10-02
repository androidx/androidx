/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.parser.ParsedQuery
import androidx.room.parser.Section
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Entity
import androidx.room.vo.EntityOrView
import androidx.room.vo.Field
import androidx.room.vo.Pojo
import androidx.room.vo.columnNames
import java.util.Locale

/**
 * Interprets and rewrites SQL queries in the context of the provided entities and views.
 */
class QueryInterpreter(
    val tables: List<EntityOrView>
) {

    private class IdentifierMap<V> : HashMap<String, V>() {
        override fun put(key: String, value: V): V? {
            return super.put(key.toLowerCase(Locale.ENGLISH), value)
        }

        override fun get(key: String): V? {
            return super.get(key.toLowerCase(Locale.ENGLISH))
        }
    }

    /**
     * Analyzes and rewrites the specified [query] in the context of the provided [pojo].
     */
    fun interpret(
        query: ParsedQuery,
        pojo: Pojo?
    ): String {
        val queriedTableNames = query.tables.map { it.name }
        return query.sections.joinToString("") { section ->
            when (section) {
                is Section.Text -> section.text
                is Section.BindVar -> "?"
                is Section.Newline -> "\n"
                is Section.Projection -> if (pojo == null) {
                    section.text
                } else {
                    val aliasToName = query.tables
                        .map { (name, alias) -> alias to name }
                        .toMap(IdentifierMap())
                    val nameToAlias = query.tables
                        .groupBy { it.name.toLowerCase(Locale.ENGLISH) }
                        .filter { (_, pairs) -> pairs.size == 1 }
                        .map { (name, pairs) -> name to pairs.first().alias }
                        .toMap(IdentifierMap())
                    when (section) {
                        is Section.Projection.All -> {
                            expand(
                                pojo,
                                query.explicitColumns,
                                // The columns come directly from the specified table.
                                // We should not prepend the prefix-dot to the columns.
                                findEntityOrView(pojo)?.tableName in queriedTableNames,
                                nameToAlias,
                                query.resultInfo
                            )
                        }
                        is Section.Projection.Table -> {
                            val embedded = findEmbeddedField(pojo, section.tableAlias)
                            if (embedded != null) {
                                expandEmbeddedField(
                                    embedded,
                                    findEntityOrView(embedded.pojo),
                                    false,
                                    nameToAlias
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
        return (pojo.embeddedFields.flatMap {
            expandEmbeddedField(it, findEntityOrView(it.pojo), shallow, nameToAlias)
        } + pojo.fields.filter { field ->
            field.parent == null &&
                    field.columnName !in ignoredColumnNames &&
                    (resultInfo == null || resultInfo.hasColumn(field.columnName))
        }.map { field ->
            if (table != null && table is Entity) {
                // Should not happen when defining a view
                "`${table.tableName}`.`${field.columnName}` AS `${field.columnName}`"
            } else {
                "`${field.columnName}`"
            }
        }).joinToString(", ")
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

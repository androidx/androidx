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

package androidx.room3.vo

import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.migration.bundle.FtsEntityBundle
import androidx.room3.migration.bundle.TABLE_NAME_PLACEHOLDER
import androidx.room3.parser.FtsVersion

/** An Entity with a mapping FTS table. */
class FtsEntity(
    element: XTypeElement,
    tableName: String,
    type: XType,
    properties: List<Property>,
    embeddedProperties: List<EmbeddedProperty>,
    primaryKey: PrimaryKey,
    constructor: Constructor?,
    shadowTableName: String?,
    val ftsVersion: FtsVersion,
    val ftsOptions: FtsOptions,
) :
    Entity(
        element,
        tableName,
        type,
        properties,
        embeddedProperties,
        primaryKey,
        emptyList(),
        emptyList(),
        constructor,
        shadowTableName,
        false,
    ) {

    override val createTableQuery by lazy { createTableQuery(tableName) }

    val nonHiddenProperties by lazy {
        properties.filterNot {
            // 'rowid' primary key column and language id column are hidden columns
            primaryKey.properties.isNotEmpty() && primaryKey.properties.first() == it ||
                ftsOptions.languageIdColumnName == it.columnName
        }
    }

    // Create trigger queries to keep FTS table up to date with the content table as suggested in
    // https://www.sqlite.org/fts3.html#_external_content_fts4_tables_ and
    // https://www.sqlite.org/fts5.html#external_content_tables
    val contentSyncTriggerCreateQueries by lazy {
        if (ftsOptions.contentEntity != null) {
            createSyncTriggers(ftsOptions.contentEntity.tableName)
        } else {
            emptyList()
        }
    }

    private val contentRowId =
        if (!ftsOptions.contentRowId.isNullOrEmpty()) {
            ftsOptions.contentRowId
        } else {
            "rowid"
        }

    override fun getIdKey(): String {
        val identityKey = SchemaIdentityKey()
        identityKey.append(tableName)
        identityKey.appendSorted(this@FtsEntity.properties)
        identityKey.append(ftsVersion.name)
        identityKey.append(ftsOptions)
        return identityKey.hash()
    }

    fun getCreateTableQueryWithoutTokenizer() = createTableQuery(tableName, false)

    private fun createTableQuery(tableName: String, includeTokenizer: Boolean = true): String {
        val definitions =
            nonHiddenProperties.map {
                val columnDefinition =
                    if (ftsVersion == FtsVersion.FTS5) {
                        "`${it.columnName}`"
                    } else {
                        it.databaseDefinition(false)
                    }
                val isUnindexed =
                    ftsVersion == FtsVersion.FTS5 &&
                        ftsOptions.notIndexedColumns.contains(it.columnName)
                if (isUnindexed) {
                    "$columnDefinition UNINDEXED"
                } else {
                    columnDefinition
                }
            } + ftsOptions.databaseDefinition(includeTokenizer, ftsVersion)
        return "CREATE VIRTUAL TABLE IF NOT EXISTS `$tableName` " +
            "USING ${ftsVersion.name}(${definitions.joinToString(", ")})"
    }

    private fun createSyncTriggers(contentTable: String): List<String> {
        val contentColumnNames = nonHiddenProperties.map { it.columnName }
        return arrayOf("UPDATE", "DELETE").map { operation ->
            createBeforeTrigger(operation, tableName, contentTable)
        } +
            arrayOf("UPDATE", "INSERT").map { operation ->
                createAfterTrigger(operation, tableName, contentTable, contentColumnNames)
            }
    }

    private fun createBeforeTrigger(
        triggerOp: String,
        tableName: String,
        contentTableName: String,
    ): String {
        val rowId = if (ftsVersion == FtsVersion.FTS5) "rowid" else "docid"
        return "CREATE TRIGGER IF NOT EXISTS ${createTriggerName(tableName, "BEFORE_$triggerOp")} " +
            "BEFORE $triggerOp ON `$contentTableName` BEGIN " +
            "DELETE FROM `$tableName` WHERE `$rowId`=OLD.`$contentRowId`; " +
            "END"
    }

    private fun createAfterTrigger(
        triggerOp: String,
        tableName: String,
        contentTableName: String,
        columnNames: List<String>,
    ): String {
        val rowId = if (ftsVersion == FtsVersion.FTS5) "rowid" else "docid"
        return "CREATE TRIGGER IF NOT EXISTS ${createTriggerName(tableName, "AFTER_$triggerOp")} " +
            "AFTER $triggerOp ON `$contentTableName` BEGIN " +
            "INSERT INTO `$tableName`(" +
            (listOf(rowId) + columnNames).joinToString(separator = ", ") { "`$it`" } +
            ") " +
            "VALUES (" +
            (listOf(contentRowId) + columnNames).joinToString(separator = ", ") { "NEW.`$it`" } +
            "); " +
            "END"
    }

    // If trigger name prefix is changed be sure to update DBUtil#dropFtsSyncTriggers
    private fun createTriggerName(tableName: String, triggerOp: String) =
        "room_fts_content_sync_${tableName}_$triggerOp"

    override fun toBundle() =
        FtsEntityBundle(
            tableName,
            createTableQuery(TABLE_NAME_PLACEHOLDER),
            nonHiddenProperties.map { it.toBundle() },
            primaryKey.toBundle(),
            emptyList(),
            emptyList(),
            ftsVersion.name,
            ftsOptions.toBundle(),
            contentSyncTriggerCreateQueries,
        )
}

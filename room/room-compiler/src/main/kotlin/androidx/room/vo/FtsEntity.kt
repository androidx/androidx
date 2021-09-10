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

package androidx.room.vo

import androidx.room.compiler.processing.XType
import androidx.room.migration.bundle.BundleUtil
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.parser.FtsVersion
import androidx.room.compiler.processing.XTypeElement

/**
 * An Entity with a mapping FTS table.
 */
class FtsEntity(
    element: XTypeElement,
    tableName: String,
    type: XType,
    fields: List<Field>,
    embeddedFields: List<EmbeddedField>,
    primaryKey: PrimaryKey,
    constructor: Constructor?,
    shadowTableName: String?,
    val ftsVersion: FtsVersion,
    val ftsOptions: FtsOptions
) : Entity(
    element, tableName, type, fields, embeddedFields, primaryKey, emptyList(), emptyList(),
    constructor, shadowTableName
) {

    override val createTableQuery by lazy {
        createTableQuery(tableName)
    }

    val nonHiddenFields by lazy {
        fields.filterNot {
            // 'rowid' primary key column and language id column are hidden columns
            primaryKey.fields.isNotEmpty() && primaryKey.fields.first() == it ||
                ftsOptions.languageIdColumnName == it.columnName
        }
    }

    val contentSyncTriggerNames by lazy {
        if (ftsOptions.contentEntity != null) {
            arrayOf("UPDATE", "DELETE").map { operation ->
                createTriggerName(tableName, "BEFORE_$operation")
            } + arrayOf("UPDATE", "INSERT").map { operation ->
                createTriggerName(tableName, "AFTER_$operation")
            }
        } else {
            emptyList()
        }
    }

    // Create trigger queries to keep fts table up to date with the content table as suggested in
    // https://www.sqlite.org/fts3.html#_external_content_fts4_tables_
    val contentSyncTriggerCreateQueries by lazy {
        if (ftsOptions.contentEntity != null) {
            createSyncTriggers(ftsOptions.contentEntity.tableName)
        } else {
            emptyList()
        }
    }

    override fun getIdKey(): String {
        val identityKey = SchemaIdentityKey()
        identityKey.append(tableName)
        identityKey.appendSorted(fields)
        identityKey.append(ftsVersion.name)
        identityKey.append(ftsOptions)
        return identityKey.hash()
    }

    fun getCreateTableQueryWithoutTokenizer() = createTableQuery(tableName, false)

    private fun createTableQuery(tableName: String, includeTokenizer: Boolean = true): String {
        val definitions = nonHiddenFields.map { it.databaseDefinition(false) } +
            ftsOptions.databaseDefinition(includeTokenizer)
        return "CREATE VIRTUAL TABLE IF NOT EXISTS `$tableName` " +
            "USING ${ftsVersion.name}(${definitions.joinToString(", ")})"
    }

    private fun createSyncTriggers(contentTable: String): List<String> {
        val contentColumnNames = nonHiddenFields.map { it.columnName }
        return arrayOf("UPDATE", "DELETE").map { operation ->
            createBeforeTrigger(operation, tableName, contentTable)
        } + arrayOf("UPDATE", "INSERT").map { operation ->
            createAfterTrigger(operation, tableName, contentTable, contentColumnNames)
        }
    }

    private fun createBeforeTrigger(
        triggerOp: String,
        tableName: String,
        contentTableName: String
    ) = "CREATE TRIGGER IF NOT EXISTS ${createTriggerName(tableName, "BEFORE_$triggerOp")} " +
        "BEFORE $triggerOp ON `$contentTableName` BEGIN " +
        "DELETE FROM `$tableName` WHERE `docid`=OLD.`rowid`; " +
        "END"

    private fun createAfterTrigger(
        triggerOp: String,
        tableName: String,
        contentTableName: String,
        columnNames: List<String>
    ) = "CREATE TRIGGER IF NOT EXISTS ${createTriggerName(tableName, "AFTER_$triggerOp")} " +
        "AFTER $triggerOp ON `$contentTableName` BEGIN " +
        "INSERT INTO `$tableName`(" +
        (listOf("docid") + columnNames).joinToString(separator = ", ") { "`$it`" } + ") " +
        "VALUES (" +
        (listOf("rowid") + columnNames).joinToString(separator = ", ") { "NEW.`$it`" } + "); " +
        "END"

    // If trigger name prefix is changed be sure to update DBUtil#dropFtsSyncTriggers
    private fun createTriggerName(tableName: String, triggerOp: String) =
        "room_fts_content_sync_${tableName}_$triggerOp"

    override fun toBundle() = FtsEntityBundle(
        tableName,
        createTableQuery(BundleUtil.TABLE_NAME_PLACEHOLDER),
        nonHiddenFields.map { it.toBundle() },
        primaryKey.toBundle(),
        ftsVersion.name,
        ftsOptions.toBundle(),
        contentSyncTriggerCreateQueries
    )
}
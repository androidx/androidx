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

import androidx.room.migration.bundle.BundleUtil
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.parser.FtsVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

/**
 * An Entity with a mapping FTS table.
 */
class FtsEntity(
    element: TypeElement,
    tableName: String,
    type: DeclaredType,
    fields: List<Field>,
    embeddedFields: List<EmbeddedField>,
    primaryKey: PrimaryKey,
    constructor: Constructor?,
    shadowTableName: String?,
    val ftsVersion: FtsVersion,
    val ftsOptions: FtsOptions
) : Entity(element, tableName, type, fields, embeddedFields, primaryKey, emptyList(), emptyList(),
        constructor, shadowTableName) {

    override val createTableQuery by lazy {
        createTableQuery(tableName)
    }

    override fun getIdKey(): String {
        val identityKey = SchemaIdentityKey()
        identityKey.append(tableName)
        identityKey.appendSorted(fields)
        identityKey.append(ftsVersion.name)
        identityKey.append(ftsOptions)
        return identityKey.hash()
    }

    private fun createTableQuery(tableName: String): String {
        val definitions = fields.mapNotNull {
            when {
                // 'rowid' primary key column is omitted from create statement
                primaryKey.fields.isNotEmpty() && primaryKey.fields.first() == it -> null
                // language id column is omitted from create statement
                ftsOptions.languageIdColumnName == it.columnName -> null
                else -> it.databaseDefinition(false)
            }
        } + ftsOptions.databaseDefinition()
        return "CREATE VIRTUAL TABLE IF NOT EXISTS `$tableName` " +
                "USING ${ftsVersion.name}(${definitions.joinToString(", ")})"
    }

    override fun toBundle() = FtsEntityBundle(
            tableName,
            createTableQuery(BundleUtil.TABLE_NAME_PLACEHOLDER),
            fields.map { it.toBundle() },
            primaryKey.toBundle(),
            ftsVersion.name,
            ftsOptions.toBundle())
}
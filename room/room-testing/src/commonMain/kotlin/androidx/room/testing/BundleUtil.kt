/*
 * Copyright 2023 The Android Open Source Project
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

@file:JvmName("BundleUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.room.testing

import androidx.annotation.RestrictTo
import androidx.room.migration.bundle.BaseEntityBundle
import androidx.room.migration.bundle.DatabaseViewBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.ForeignKeyBundle
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.migration.bundle.IndexBundle
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.ViewInfo
import kotlin.jvm.JvmName

internal fun EntityBundle.toTableInfo(): TableInfo {
    return TableInfo(
        name = this.tableName,
        columns = this.toColumnMap(),
        foreignKeys = this.foreignKeys.toForeignKeys(),
        indices = this.indices.toIndices()
    )
}

internal fun FtsEntityBundle.toFtsTableInfo(): FtsTableInfo {
    return FtsTableInfo(
        name = this.tableName,
        columns = this.toColumnNamesSet(),
        createSql = this.createSql
    )
}

internal fun DatabaseViewBundle.toViewInfo(): ViewInfo {
    return ViewInfo(name = this.viewName, sql = this.createView())
}

private fun List<IndexBundle>?.toIndices(): Set<TableInfo.Index> {
    if (this == null) {
        return emptySet()
    }
    val result =
        this.map { bundle ->
                TableInfo.Index(
                    name = bundle.name,
                    unique = bundle.isUnique,
                    columns = bundle.columnNames ?: emptyList(),
                    orders = bundle.orders ?: emptyList()
                )
            }
            .toSet()
    return result
}

private fun List<ForeignKeyBundle>?.toForeignKeys(): Set<TableInfo.ForeignKey> {
    if (this == null) {
        return emptySet()
    }
    val result =
        this.map { bundle ->
                TableInfo.ForeignKey(
                    referenceTable = bundle.table,
                    onDelete = bundle.onDelete,
                    onUpdate = bundle.onUpdate,
                    columnNames = bundle.columns,
                    referenceColumnNames = bundle.referencedColumns
                )
            }
            .toSet()
    return result
}

private fun BaseEntityBundle.toColumnNamesSet(): Set<String> {
    return this.fields.map { field -> field.columnName }.toSet()
}

private fun EntityBundle.toColumnMap(): Map<String, TableInfo.Column> {
    val result: MutableMap<String, TableInfo.Column> = HashMap()
    this.fields.associateBy { bundle ->
        val column = bundle.toColumn(this)
        result[column.name] = column
    }
    return result
}

private fun FieldBundle.toColumn(ownerEntity: EntityBundle): TableInfo.Column {
    return TableInfo.Column(
        name = this.columnName,
        type = this.affinity,
        notNull = this.isNonNull,
        primaryKeyPosition = findPrimaryKeyPosition(ownerEntity, this),
        defaultValue = this.defaultValue,
        createdFrom = TableInfo.CREATED_FROM_ENTITY
    )
}

private fun findPrimaryKeyPosition(entity: EntityBundle, field: FieldBundle): Int {
    return entity.primaryKey.columnNames.indexOfFirst { columnName ->
        field.columnName.equals(columnName, ignoreCase = true)
    } + 1 // Shift by 1 to get primary key position
}

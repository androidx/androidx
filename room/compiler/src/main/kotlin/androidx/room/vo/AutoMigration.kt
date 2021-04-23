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

package androidx.room.vo

import androidx.room.compiler.processing.XTypeElement
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.util.SchemaDiffResult
import com.squareup.javapoet.ClassName

/**
 * Stores the changes detected in a database schema between the old and new versions.
 */
data class AutoMigration(
    val element: XTypeElement,
    val from: Int,
    val to: Int,
    val specElement: XTypeElement?,
    val schemaDiff: SchemaDiffResult,
    val isSpecProvided: Boolean,
) {
    val implTypeName: ClassName by lazy {
        ClassName.get(
            element.className.packageName(),
            "${element.className.simpleName()}_AutoMigration_${from}_${to}_Impl"
        )
    }

    val specClassName = specElement?.className

    /**
     * Stores the table name and the relevant field bundle of a column that was added to a
     * database in a newer version.
     */
    data class AddedColumn(val tableName: String, val fieldBundle: FieldBundle)

    /**
     * Stores the table name, original name, and the new name of a column that was renamed in the
     * new version of the database.
     */
    data class RenamedColumn(
        val tableName: String,
        val originalColumnName: String,
        val newColumnName: String
    )

    /**
     * Stores the table name and the column name of a column that was deleted from the database.
     */
    data class DeletedColumn(val tableName: String, val columnName: String)

    /**
     * Stores the table that was added to a database in a newer version.
     */
    data class AddedTable(val entityBundle: EntityBundle)

    /**
     * Stores the table that contains a change in the primary key, foreign key(s) or index(es)
     * in a newer version, as well as any complex changes and renames on the column-level.
     *
     * As it is possible to have a table with only simple (non-complex) changes, which will be
     * categorized as "AddedColumn" or "DeletedColumn" changes, all other
     * changes at the table level are categorized as "complex" changes, using the category
     * "ComplexChangedTable".
     *
     * The renamed columns map contains a mapping from the NEW name of the column to the OLD name
     * of the column.
     */
    data class ComplexChangedTable(
        val tableName: String,
        val tableNameWithNewPrefix: String,
        val oldVersionEntityBundle: EntityBundle,
        val newVersionEntityBundle: EntityBundle,
        val renamedColumnsMap: MutableMap<String, String>
    )

    /**
     * Stores the original name and the new name of a table that was renamed in the
     * new version of the database.
     *
     * This container will only be used for tables that got renamed, but do not have any complex
     * changes on it, both on the table and column level.
     */
    data class RenamedTable(val originalTableName: String, val newTableName: String)

    /**
     * Stores the name of the table that was deleted from the database.
     */
    data class DeletedTable(val deletedTableName: String)
}

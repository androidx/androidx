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
import com.squareup.javapoet.ClassName

data class AutoMigrationResult(
    val element: XTypeElement,
    val from: Int?,
    val to: Int?,
    val addedColumns: List<AddedColumn>,
    val addedTables: List<AddedTable>
) {

    val implTypeName: ClassName by lazy {
        ClassName.get(
            element.className.packageName(),
            "${element.className.simpleNames().joinToString("_")}_Impl"
        )
    }

    /**
     * Stores the table name and the relevant field bundle of a column that was added to a
     * database in a newer version.
     */
    data class AddedColumn(val tableName: String, val fieldBundle: FieldBundle)

    /**
     * Stores the table name and the relevant field bundle of a column that was present in both
     * the old and new version of the same database, but had a change in the field schema (e.g.
     * change in affinity).
     */
    data class ChangedColumn(
        val tableName: String,
        val fieldBundle: FieldBundle
    )

    /**
     * Stores the table name and the relevant field bundle of a column that was present in the
     * old version of a database but is not present in a new version of the same database, either
     * because it was removed or renamed.
     *
     * In the current implementation, we cannot differ between whether the column was removed or
     * renamed.
     */
    data class RemovedColumn(val tableName: String, val fieldBundle: FieldBundle)

    /**
     * Stores the table that was added to a database in a newer version.
     */
    data class AddedTable(val entityBundle: EntityBundle)

    /**
     * Stores the table name that contains a change in the primary key, foreign key(s) or index(es)
     * in a newer version. Explicitly provides information on whether a foreign key change and/or
     * an index change has occurred.
     *
     * As it is possible to have a table with only simple (non-complex) changes, which will be
     * categorized as "AddedColumn" or "RemovedColumn" changes, all other
     * changes at the table level are categorized as "complex" changes, using the category
     * "ComplexChangedTable".
     *
     * At the column level, any change that is not a column add or a
     * removal will be categorized as "ChangedColumn".
     */
    data class ComplexChangedTable(
        val tableName: String,
        val foreignKeyChanged: Boolean,
        val indexChanged: Boolean
    )

    /**
     * Stores the table that was present in the old version of a database but is not present in a
     * new version of the same database, either because it was removed or renamed.
     *
     * In the current implementation, we cannot differ between whether the table was removed or
     * renamed.
     */
    data class RemovedTable(val entityBundle: EntityBundle)
}

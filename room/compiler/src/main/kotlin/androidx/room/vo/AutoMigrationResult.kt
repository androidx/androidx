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
        val originalFieldBundle: FieldBundle,
        val newFieldBundle: FieldBundle
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
     * Stores the table that was present in the old version of a database but is not present in a
     * new version of the same database, either because it was removed or renamed.
     *
     * In the current implementation, we cannot differ between whether the table was removed or
     * renamed.
     */
    data class RemovedTable(val entityBundle: EntityBundle)
}

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

package androidx.room.util

import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.processor.ProcessorErrors
import androidx.room.vo.AutoMigrationResult

/**
 * This exception should be thrown to abandon processing an @AutoMigration.
 */
class DiffException(val errorMessage: String) : RuntimeException(errorMessage)

/**
 * Contains the added, changed and removed columns detected.
 */
data class SchemaDiffResult(
    val addedColumn: MutableList<AutoMigrationResult.AddedColumn>,
    val changedColumn: List<AutoMigrationResult.ChangedColumn>,
    val removedColumn: List<AutoMigrationResult.RemovedColumn>,
    val addedTable: List<AutoMigrationResult.AddedTable>,
    val removedTable: List<AutoMigrationResult.RemovedTable>
)

/**
 * Receives the two bundles, diffs and returns a @SchemaDiffResult.
 *
 * Throws an @AutoMigrationException with a detailed error message when an AutoMigration cannot
 * be generated.
 */
class SchemaDiffer(
    val fromSchemaBundle: DatabaseBundle,
    val toSchemaBundle: DatabaseBundle
) {

    /**
     * Compares the two versions of the database based on the schemas provided, and detects
     * schema changes.
     *
     * @return the AutoMigrationResult containing the schema changes detected
     */
    fun diffSchemas(): SchemaDiffResult {
        val addedTables = mutableListOf<AutoMigrationResult.AddedTable>()
        val removedTables = mutableListOf<AutoMigrationResult.RemovedTable>()

        val addedColumns = mutableListOf<AutoMigrationResult.AddedColumn>()
        val changedColumns = mutableListOf<AutoMigrationResult.ChangedColumn>()
        val removedColumns = mutableListOf<AutoMigrationResult.RemovedColumn>()

        // Check going from the original version of the schema to the new version for changed and
        // removed columns/tables
        fromSchemaBundle.entitiesByTableName.forEach { v1Table ->
            val v2Table = toSchemaBundle.entitiesByTableName[v1Table.key]
            if (v2Table == null) {
                removedTables.add(AutoMigrationResult.RemovedTable(v1Table.value))
            } else {
                val v1Columns = v1Table.value.fieldsByColumnName
                val v2Columns = v2Table.fieldsByColumnName
                v1Columns.entries.forEach { v1Column ->
                    val match = v2Columns[v1Column.key]
                    if (match != null && !match.isSchemaEqual(v1Column.value)) {
                        changedColumns.add(
                            AutoMigrationResult.ChangedColumn(
                                v1Table.key,
                                v1Column.value,
                                match
                            )
                        )
                    } else if (match == null) {
                        removedColumns.add(
                            AutoMigrationResult.RemovedColumn(
                                v1Table.key,
                                v1Column.value
                            )
                        )
                    }
                }
            }
        }
        // Check going from the new version of the schema to the original version for added
        // tables/columns. Skip the columns with the same name as the previous loop would have
        // processed them already.
        toSchemaBundle.entitiesByTableName.forEach { v2Table ->
            val v1Table = fromSchemaBundle.entitiesByTableName[v2Table.key]
            if (v1Table == null) {
                addedTables.add(AutoMigrationResult.AddedTable(v2Table.value))
            } else {
                val v2Columns = v2Table.value.fieldsByColumnName
                val v1Columns = v1Table.fieldsByColumnName
                v2Columns.entries.forEach { v2Column ->
                    val match = v1Columns[v2Column.key]
                    if (match == null) {
                        if (v2Column.value.isNonNull && v2Column.value.defaultValue == null) {
                            diffError(
                                ProcessorErrors.newNotNullColumnMustHaveDefaultValue(v2Column.key)
                            )
                        }
                        addedColumns.add(
                            AutoMigrationResult.AddedColumn(
                                v2Table.key,
                                v2Column.value
                            )
                        )
                    }
                }
            }
        }

        if (changedColumns.isNotEmpty()) {
            changedColumns.forEach { changedColumn ->
                diffError(
                    ProcessorErrors.columnWithChangedSchemaFound(
                        changedColumn.originalFieldBundle.columnName
                    )
                )
            }
        }

        if (removedColumns.isNotEmpty()) {
            removedColumns.forEach { removedColumn ->
                diffError(
                    ProcessorErrors.removedOrRenamedColumnFound(
                        removedColumn.fieldBundle.columnName
                    )
                )
            }
        }

        if (removedTables.isNotEmpty()) {
            removedTables.forEach { removedTable ->
                diffError(
                    ProcessorErrors.removedOrRenamedTableFound(
                        removedTable.entityBundle.tableName
                    )
                )
            }
        }

        return SchemaDiffResult(
            addedColumn = addedColumns,
            changedColumn = changedColumns,
            removedColumn = removedColumns,
            addedTable = addedTables,
            removedTable = removedTables
        )
    }

    private fun diffError(errorMsg: String) {
        throw DiffException(errorMsg)
    }
}
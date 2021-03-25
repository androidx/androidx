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
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.ForeignKeyBundle
import androidx.room.migration.bundle.IndexBundle
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
    val addedColumns: List<AutoMigrationResult.AddedColumn>,
    val changedColumns: List<AutoMigrationResult.ChangedColumn>,
    val removedColumns: List<AutoMigrationResult.RemovedColumn>,
    val addedTables: List<AutoMigrationResult.AddedTable>,
    val complexChangedTables: List<AutoMigrationResult.ComplexChangedTable>,
    val removedTables: List<AutoMigrationResult.RemovedTable>
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
        val complexChangedTables = mutableListOf<AutoMigrationResult.ComplexChangedTable>()
        val removedTables = mutableListOf<AutoMigrationResult.RemovedTable>()

        val addedColumns = mutableListOf<AutoMigrationResult.AddedColumn>()
        val changedColumns = mutableListOf<AutoMigrationResult.ChangedColumn>()
        val removedColumns = mutableListOf<AutoMigrationResult.RemovedColumn>()

        // Check going from the original version of the schema to the new version for changed and
        // removed columns/tables
        fromSchemaBundle.entitiesByTableName.forEach { fromTable ->
            val toTable = toSchemaBundle.entitiesByTableName[fromTable.key]
            if (toTable == null) {
                removedTables.add(AutoMigrationResult.RemovedTable(fromTable.value))
            } else {
                val complexChangedTable = tableContainsComplexChanges(fromTable.value, toTable)
                if (complexChangedTable != null) {
                    complexChangedTables.add(complexChangedTable)
                }
                val fromColumns = fromTable.value.fieldsByColumnName
                val toColumns = toTable.fieldsByColumnName
                fromColumns.entries.forEach { fromColumn ->
                    val match = toColumns[fromColumn.key]
                    if (match != null && !match.isSchemaEqual(fromColumn.value)) {
                        // Any change in the field bundle schema of a column will be complex
                        changedColumns.add(
                            AutoMigrationResult.ChangedColumn(
                                fromTable.key,
                                fromColumn.value
                            )
                        )
                    } else if (match == null) {
                        removedColumns.add(
                            AutoMigrationResult.RemovedColumn(
                                fromTable.key,
                                fromColumn.value
                            )
                        )
                    }
                }
            }
        }
        // Check going from the new version of the schema to the original version for added
        // tables/columns. Skip the columns with the same name as the previous loop would have
        // processed them already.
        toSchemaBundle.entitiesByTableName.forEach { toTable ->
            val fromTable = fromSchemaBundle.entitiesByTableName[toTable.key]
            if (fromTable == null) {
                addedTables.add(AutoMigrationResult.AddedTable(toTable.value))
            } else {
                val fromColumns = fromTable.fieldsByColumnName
                val toColumns = toTable.value.fieldsByColumnName
                toColumns.entries.forEach { toColumn ->
                    val match = fromColumns[toColumn.key]
                    if (match == null) {
                        if (toColumn.value.isNonNull && toColumn.value.defaultValue == null) {
                            diffError(
                                ProcessorErrors.newNotNullColumnMustHaveDefaultValue(toColumn.key)
                            )
                        }
                        addedColumns.add(
                            AutoMigrationResult.AddedColumn(
                                toTable.key,
                                toColumn.value
                            )
                        )
                    }
                }
            }
        }

        // TODO: (b/183007590) Remove the Processor Errors thrown when a complex change is
        //  encountered after AutoMigrationWriter supports generating the necessary migrations.
        if (changedColumns.isNotEmpty()) {
            changedColumns.forEach { changedColumn ->
                diffError(
                    ProcessorErrors.columnWithChangedSchemaFound(
                        changedColumn.fieldBundle.columnName
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

        if (complexChangedTables.isNotEmpty()) {
            complexChangedTables.forEach { changedTable ->
                diffError(
                    ProcessorErrors.tableWithComplexChangedSchemaFound(
                        changedTable.tableName
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
            addedColumns = addedColumns,
            changedColumns = changedColumns,
            removedColumns = removedColumns,
            addedTables = addedTables,
            complexChangedTables = complexChangedTables,
            removedTables = removedTables
        )
    }

    /**
     * Check for complex schema changes at a Table level and returns a ComplexTableChange
     * including information on which table changes were found on, and whether foreign key or
     * index related changes have occurred.
     *
     * @return null if complex schema change has not been found
     */
    // TODO: (b/181777611) Handle FTS tables
    private fun tableContainsComplexChanges(
        fromTable: EntityBundle,
        toTable: EntityBundle
    ): AutoMigrationResult.ComplexChangedTable? {
        val foreignKeyChanged = !isForeignKeyBundlesListEqual(
            fromTable.foreignKeys,
            toTable.foreignKeys
        )
        val indexChanged = !isIndexBundlesListEqual(fromTable.indices, toTable.indices)
        val primaryKeyChanged = !fromTable.primaryKey.isSchemaEqual(toTable.primaryKey)

        if (primaryKeyChanged || foreignKeyChanged || indexChanged) {
            return AutoMigrationResult.ComplexChangedTable(
                tableName = toTable.tableName,
                foreignKeyChanged = foreignKeyChanged,
                indexChanged = indexChanged
            )
        }
        return null
    }

    private fun diffError(errorMsg: String) {
        throw DiffException(errorMsg)
    }

    /**
     * Takes in two ForeignKeyBundle lists, attempts to find potential matches based on the columns
     * of the Foreign Keys. Processes these potential matches by checking for schema equality.
     *
     * @return true if the two lists of foreign keys are equal
     */
    private fun isForeignKeyBundlesListEqual(
        fromBundle: List<ForeignKeyBundle>,
        toBundle: List<ForeignKeyBundle>
    ): Boolean {
        val set = fromBundle + toBundle
        val matches = set.groupBy { it.columns }.entries

        matches.forEach { (_, bundles) ->
            if (bundles.size < 2) {
                // A bundle was not matched at all, there must be a change between two versions
                return false
            }
            val fromForeignKeyBundle = bundles[0]
            val toForeignKeyBundle = bundles[1]
            if (!fromForeignKeyBundle.isSchemaEqual(toForeignKeyBundle)) {
                // A potential match for a bundle was found, but schemas did not match
                return false
            }
        }
        return true
    }

    /**
     * Takes in two IndexBundle lists, attempts to find potential matches based on the names
     * of the indexes. Processes these potential matches by checking for schema equality.
     *
     * @return true if the two lists of indexes are equal
     */
    private fun isIndexBundlesListEqual(
        fromBundle: List<IndexBundle>,
        toBundle: List<IndexBundle>
    ): Boolean {
        val set = fromBundle + toBundle
        val matches = set.groupBy { it.name }.entries

        matches.forEach { bundlesWithSameName ->
            if (bundlesWithSameName.value.size < 2) {
                // A bundle was not matched at all, there must be a change between two versions
                return false
            } else if (!bundlesWithSameName.value[0].isSchemaEqual(bundlesWithSameName.value[1])) {
                // A potential match for a bundle was found, but schemas did not match
                return false
            }
        }
        return true
    }
}
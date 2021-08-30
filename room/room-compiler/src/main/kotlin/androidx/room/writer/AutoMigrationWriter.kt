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

package androidx.room.writer

import androidx.annotation.NonNull
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.T
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.vo.AutoMigration
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Writes the implementation of migrations that were annotated with @AutoMigration.
 */
class AutoMigrationWriter(
    private val dbElement: XElement,
    val autoMigration: AutoMigration
) : ClassWriter(autoMigration.implTypeName) {
    private val addedColumns = autoMigration.schemaDiff.addedColumns
    private val addedTables = autoMigration.schemaDiff.addedTables
    private val renamedTables = autoMigration.schemaDiff.renamedTables
    private val complexChangedTables = autoMigration.schemaDiff.complexChangedTables
    private val deletedTables = autoMigration.schemaDiff.deletedTables

    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(autoMigration.implTypeName)
        builder.apply {
            addOriginatingElement(dbElement)
            superclass(RoomTypeNames.MIGRATION)

            if (autoMigration.specClassName != null) {
                val callbackField =
                    FieldSpec.builder(
                        RoomTypeNames.AUTO_MIGRATION_SPEC,
                        "callback",
                        Modifier.PRIVATE,
                        Modifier.FINAL
                    ).apply {
                        if (!autoMigration.isSpecProvided) {
                            initializer("new $T()", autoMigration.specClassName)
                        }
                    }
                builder.addField(callbackField.build())
            }
            addMethod(createConstructor())
            addMethod(createMigrateMethod())
        }
        return builder
    }

    /**
     * Builds the constructor of the generated AutoMigration.
     *
     * @return The constructor of the generated AutoMigration
     */
    private fun createConstructor(): MethodSpec {
        return MethodSpec.constructorBuilder().apply {
            addModifiers(Modifier.PUBLIC)
            addStatement(
                "super($L, $L)",
                autoMigration.from,
                autoMigration.to
            )
            if (autoMigration.isSpecProvided) {
                addParameter(
                    ParameterSpec.builder(
                        RoomTypeNames.AUTO_MIGRATION_SPEC,
                        "callback"
                    ).addAnnotation(NonNull::class.java).build()
                )
                addStatement("this.callback = callback")
            }
        }.build()
    }

    private fun createMigrateMethod(): MethodSpec? {
        val migrateFunctionBuilder: MethodSpec.Builder = MethodSpec.methodBuilder("migrate")
            .apply {
                addParameter(
                    ParameterSpec.builder(
                        SupportDbTypeNames.DB,
                        "database"
                    ).addAnnotation(NonNull::class.java).build()
                )
                addAnnotation(Override::class.java)
                addModifiers(Modifier.PUBLIC)
                returns(TypeName.VOID)
                addMigrationStatements(this)
                if (autoMigration.specClassName != null) {
                    addStatement("callback.onPostMigrate(database)")
                }
            }
        return migrateFunctionBuilder.build()
    }

    /**
     * Takes the changes provided in the {@link AutoMigrationResult} which are differences detected
     * between the two versions of the same database, and converts them to the appropriate
     * sequence of SQL statements that migrate the database from one version to the other.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addMigrationStatements(migrateBuilder: MethodSpec.Builder) {
        addDropViewStatements(migrateBuilder)
        addSimpleChangeStatements(migrateBuilder)
        addComplexChangeStatements(migrateBuilder)
        addRecreateViewStatements(migrateBuilder)
    }

    /**
     * Adds SQL statements to drop all views of the database in the 'from' version.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addDropViewStatements(migrateBuilder: MethodSpec.Builder) {
        autoMigration.schemaDiff.fromViews.forEach { view ->
            addDatabaseExecuteSqlStatement(migrateBuilder, "DROP VIEW ${view.viewName}")
        }
    }

    /**
     * Adds SQL statements to create all views of the database in the 'to' version.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addRecreateViewStatements(migrateBuilder: MethodSpec.Builder) {
        autoMigration.schemaDiff.toViews.forEach { view ->
            addDatabaseExecuteSqlStatement(migrateBuilder, view.createView())
        }
    }

    /**
     * Adds SQL statements performing schema altering commands that are not directly supported by
     * SQLite (e.g. foreign key changes). These changes are referred to as "complex" changes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addComplexChangeStatements(migrateBuilder: MethodSpec.Builder) {
        // Create a collection that is sorted such that FTS bundles are handled after the normal
        // tables have been processed
        complexChangedTables.values.sortedBy {
            it.newVersionEntityBundle is FtsEntityBundle
        }.forEach {
            (
                _,
                tableNameWithNewPrefix,
                oldEntityBundle,
                newEntityBundle,
                renamedColumnsMap
            ) ->

            if (oldEntityBundle is FtsEntityBundle &&
                !oldEntityBundle.ftsOptions.contentTable.isNullOrBlank()
            ) {
                addStatementsToMigrateFtsTable(
                    migrateBuilder,
                    oldEntityBundle,
                    newEntityBundle,
                    renamedColumnsMap
                )
            } else {
                addStatementsToCreateNewTable(newEntityBundle, migrateBuilder)
                addStatementsToContentTransfer(
                    oldEntityBundle.tableName,
                    tableNameWithNewPrefix,
                    oldEntityBundle,
                    newEntityBundle,
                    renamedColumnsMap,
                    migrateBuilder
                )
                addStatementsToDropTableAndRenameTempTable(
                    oldEntityBundle.tableName,
                    newEntityBundle.tableName,
                    tableNameWithNewPrefix,
                    migrateBuilder
                )
                addStatementsToRecreateIndexes(newEntityBundle, migrateBuilder)
                if (newEntityBundle.foreignKeys.isNotEmpty()) {
                    addStatementsToCheckForeignKeyConstraint(
                        newEntityBundle.tableName,
                        migrateBuilder
                    )
                }
            }
        }
    }

    private fun addStatementsToMigrateFtsTable(
        migrateBuilder: MethodSpec.Builder,
        oldTable: EntityBundle,
        newTable: EntityBundle,
        renamedColumnsMap: MutableMap<String, String>
    ) {
        addDatabaseExecuteSqlStatement(migrateBuilder, "DROP TABLE `${oldTable.tableName}`")
        addDatabaseExecuteSqlStatement(migrateBuilder, newTable.createTable())

        // Transfer contents of the FTS table, using the content table if available.
        val newColumnSequence = oldTable.fieldsByColumnName.keys.filter {
            oldTable.fieldsByColumnName.keys.contains(it) ||
                renamedColumnsMap.containsKey(it)
        }.toMutableList()
        val oldColumnSequence = mutableListOf<String>()
        newColumnSequence.forEach { column ->
            oldColumnSequence.add(renamedColumnsMap[column] ?: column)
        }
        if (oldTable is FtsEntityBundle) {
            oldColumnSequence.add("rowid")
            newColumnSequence.add("docid")
        }
        val contentTable = (newTable as FtsEntityBundle).ftsOptions.contentTable
        val selectFromTable = if (contentTable.isEmpty()) {
            oldTable.tableName
        } else {
            contentTable
        }
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            buildString {
                append(
                    "INSERT INTO `${newTable.tableName}` (${newColumnSequence.joinToString(",")})" +
                        " SELECT ${oldColumnSequence.joinToString(",")} FROM " +
                        "`$selectFromTable`",
                )
            }
        )
    }

    /**
     * Adds SQL statements performing schema altering commands directly supported by SQLite
     * (adding tables/columns, renaming tables/columns, dropping tables/columns). These changes
     * are referred to as "simple" changes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addSimpleChangeStatements(migrateBuilder: MethodSpec.Builder) {
        addDeleteTableStatements(migrateBuilder)
        addRenameTableStatements(migrateBuilder)
        addNewColumnStatements(migrateBuilder)
        addNewTableStatements(migrateBuilder)
    }

    /**
     * Adds the SQL statements for creating a new table in the desired revised format of table.
     *
     * @param newTable Schema of the new table to be created
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToCreateNewTable(
        newTable: EntityBundle,
        migrateBuilder: MethodSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            newTable.createNewTable()
        )
    }

    /**
     * Adds the SQL statements for transferring the contents of the old table to the new version.
     *
     * @param oldTableName Name of the table in the old version of the database
     * @param tableNameWithNewPrefix Name of the table with the '_new_' prefix added
     * @param oldEntityBundle Entity bundle of the table in the old version of the database
     * @param newEntityBundle Entity bundle of the table in the new version of the database
     * @param renamedColumnsMap Map of the renamed columns of the table (new name -> old name)
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToContentTransfer(
        oldTableName: String,
        tableNameWithNewPrefix: String,
        oldEntityBundle: EntityBundle,
        newEntityBundle: EntityBundle,
        renamedColumnsMap: MutableMap<String, String>,
        migrateBuilder: MethodSpec.Builder
    ) {
        val newColumnSequence = newEntityBundle.fieldsByColumnName.keys.filter {
            oldEntityBundle.fieldsByColumnName.keys.contains(it) ||
                renamedColumnsMap.containsKey(it)
        }.toMutableList()
        val oldColumnSequence = mutableListOf<String>()
        newColumnSequence.forEach { column ->
            oldColumnSequence.add(renamedColumnsMap[column] ?: column)
        }

        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            buildString {
                append(
                    "INSERT INTO `$tableNameWithNewPrefix` " +
                        "(${newColumnSequence.joinToString(",")})" +
                        " SELECT ${oldColumnSequence.joinToString(",")} FROM " +
                        "`$oldTableName`",
                )
            }
        )
    }

    /**
     * Adds the SQL statements for dropping the table at the old version and renaming the
     * temporary table to the name of the original table.
     *
     * @param oldTableName Name of the table in the old version of the database
     * @param newTableName Name of the table in the new version of the database
     * @param tableNameWithNewPrefix Name of the table with the '_new_' prefix added
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToDropTableAndRenameTempTable(
        oldTableName: String,
        newTableName: String,
        tableNameWithNewPrefix: String,
        migrateBuilder: MethodSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            "DROP TABLE `$oldTableName`"
        )
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            "ALTER TABLE `$tableNameWithNewPrefix` RENAME TO `$newTableName`"
        )
    }

    /**
     * Adds the SQL statements for recreating indexes.
     *
     * @param table The table the indexes of which will be recreated
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToRecreateIndexes(
        table: EntityBundle,
        migrateBuilder: MethodSpec.Builder
    ) {
        table.indices.forEach { index ->
            addDatabaseExecuteSqlStatement(
                migrateBuilder,
                index.getCreateSql(table.tableName)
            )
        }
    }

    /**
     * Adds the SQL statement for checking the foreign key constraints.
     *
     * @param tableName Name of the table
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToCheckForeignKeyConstraint(
        tableName: String,
        migrateBuilder: MethodSpec.Builder
    ) {
        migrateBuilder.addStatement(
            "$T.foreignKeyCheck(database, $S)",
            RoomTypeNames.DB_UTIL,
            tableName
        )
    }

    /**
     * Adds the SQL statements for removing a table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addDeleteTableStatements(migrateBuilder: MethodSpec.Builder) {
        deletedTables.forEach { tableName ->
            val deleteTableSql = buildString {
                append(
                    "DROP TABLE `$tableName`"
                )
            }
            addDatabaseExecuteSqlStatement(
                migrateBuilder,
                deleteTableSql
            )
        }
    }

    /**
     * Adds the SQL statements for renaming a table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addRenameTableStatements(migrateBuilder: MethodSpec.Builder) {
        renamedTables.forEach { (oldName, newName) ->
            val renameTableSql = buildString {
                append(
                    "ALTER TABLE `$oldName` RENAME TO `$newName`"
                )
            }
            addDatabaseExecuteSqlStatement(
                migrateBuilder,
                renameTableSql
            )
        }
    }

    /**
     * Adds the SQL statements for adding new columns to a table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addNewColumnStatements(migrateBuilder: MethodSpec.Builder) {
        addedColumns.forEach {
            val addNewColumnSql = buildString {
                append(
                    "ALTER TABLE `${it.value.tableName}` ADD COLUMN `${it.key}` " +
                        "${it.value.fieldBundle.affinity} "
                )
                if (it.value.fieldBundle.isNonNull) {
                    append("NOT NULL DEFAULT ${it.value.fieldBundle.defaultValue}")
                } else {
                    append("DEFAULT NULL")
                }
            }
            addDatabaseExecuteSqlStatement(
                migrateBuilder,
                addNewColumnSql
            )
        }
    }

    /**
     * Adds the SQL statements for adding new tables to a database.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addNewTableStatements(migrateBuilder: MethodSpec.Builder) {
        addedTables.forEach { addedTable ->
            addDatabaseExecuteSqlStatement(
                migrateBuilder,
                addedTable.entityBundle.createTable()
            )
            addStatementsToRecreateIndexes(addedTable.entityBundle, migrateBuilder)
        }
    }

    /**
     * Adds the given SQL statements into the generated migrate() function to be executed by the
     * database.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     * @param sql The SQL statement to be executed by the database
     */
    private fun addDatabaseExecuteSqlStatement(
        migrateBuilder: MethodSpec.Builder,
        sql: String
    ) {
        migrateBuilder.addStatement(
            "database.execSQL($S)",
            sql
        )
    }
}

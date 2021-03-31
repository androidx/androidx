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
import androidx.room.vo.AutoMigrationResult
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Writes the implementation of migrations that were annotated with @AutoMigration.
 */
// TODO: (b/181777611) Handle FTS tables
class AutoMigrationWriter(
    private val dbElement: XElement,
    val autoMigrationResult: AutoMigrationResult
) : ClassWriter(autoMigrationResult.implTypeName) {
    val addedColumns = autoMigrationResult.schemaDiff.addedColumns
    val removedOrRenamedColumns = autoMigrationResult.schemaDiff.removedOrRenamedColumns
    val addedTables = autoMigrationResult.schemaDiff.addedTables
    val complexChangedTables = autoMigrationResult.schemaDiff.complexChangedTables
    val removedOrRenamedTables = autoMigrationResult.schemaDiff.removedOrRenamedTables

    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(autoMigrationResult.implTypeName)
        builder.apply {
            addOriginatingElement(dbElement)
            addSuperinterface(RoomTypeNames.AUTO_MIGRATION_CALLBACK)
            superclass(RoomTypeNames.MIGRATION)
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
            addStatement("super($L, $L)", autoMigrationResult.from, autoMigrationResult.to)
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
                addAutoMigrationResultToMigrate(this)
                addStatement("onPostMigrate(database)")
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
    private fun addAutoMigrationResultToMigrate(migrateBuilder: MethodSpec.Builder) {
        if (complexChangedTables.isNotEmpty()) {
            addComplexChangeStatements(migrateBuilder)
        }
        addSimpleChangeStatements(migrateBuilder)
    }

    /**
     * Adds SQL statements performing schema altering commands that are not directly supported by
     * SQLite (e.g. foreign key changes). These changes are referred to as "complex" changes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addComplexChangeStatements(migrateBuilder: MethodSpec.Builder) {
        val tablesToProcess = complexChangedTables
        tablesToProcess.forEach { table ->
            // TODO: (b/183007590) Check for column / table renames here before processing
            //  complex changes
            addStatementsToCreateNewTable(table.value, migrateBuilder)
            addStatementsToContentTransfer(table.value, migrateBuilder)
            addStatementsToDropTableAndRenameTempTable(table.value, migrateBuilder)
            addStatementsToRecreateIndexes(table.value, migrateBuilder)
            addStatementsToCheckForeignKeyConstraint(table.value, migrateBuilder)
        }
    }

    /**
     * Adds SQL statements performing schema altering commands directly supported by SQLite
     * (adding tables/columns, renaming tables/columns, dropping tables/columns). These changes
     * are referred to as "simple" changes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    // TODO: (b/183007590) Handle column/table renames here
    private fun addSimpleChangeStatements(migrateBuilder: MethodSpec.Builder) {

        if (addedColumns.isNotEmpty()) {
            addNewColumnStatements(migrateBuilder)
        }

        if (addedTables.isNotEmpty()) {
            addNewTableStatements(migrateBuilder)
        }
    }

    /**
     * Adds the SQL statements for creating a new table in the desired revised format of table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToCreateNewTable(
        table: AutoMigrationResult.ComplexChangedTable,
        migrateBuilder: MethodSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            table.newVersionEntityBundle.createNewTable()
        )
    }

    /**
     * Adds the SQL statements for transferring the contents of the old table to the new version.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToContentTransfer(
        table: AutoMigrationResult.ComplexChangedTable,
        migrateBuilder: MethodSpec.Builder
    ) {
        // TODO: (b/183007590) Account for renames and deletes here as ordering is important.
        val oldColumnSequence = table.oldVersionEntityBundle.fieldsByColumnName.keys
            .joinToString(",") { "`$it`" }
        val newColumnSequence = (
            table.newVersionEntityBundle.fieldsByColumnName.keys - addedColumns.keys
            ).joinToString(",") { "`$it`" }

        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            buildString {
                append(
                    "INSERT INTO `${table.newTableName}` ($newColumnSequence) " +
                        "SELECT $oldColumnSequence FROM `${table.tableName}`"
                )
            }
        )
    }

    /**
     * Adds the SQL statements for dropping the table at the old version and renaming the
     * temporary table to the name of the original table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToDropTableAndRenameTempTable(
        table: AutoMigrationResult.ComplexChangedTable,
        migrateBuilder: MethodSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            "DROP TABLE `${table.tableName}`"
        )
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            "ALTER TABLE `${table.newTableName}` RENAME TO `${table.tableName}`"
        )
    }

    /**
     * Adds the SQL statements for recreating indexes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToRecreateIndexes(
        table: AutoMigrationResult.ComplexChangedTable,
        migrateBuilder: MethodSpec.Builder
    ) {
        table.newVersionEntityBundle.indices.forEach { index ->
            addDatabaseExecuteSqlStatement(
                migrateBuilder,
                index.getCreateSql(table.tableName)
            )
        }
    }

    /**
     * Adds the SQL statement for checking the foreign key constraints.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToCheckForeignKeyConstraint(
        table: AutoMigrationResult.ComplexChangedTable,
        migrateBuilder: MethodSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            "PRAGMA foreign_key_check(`${table.tableName}`)"
        )
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
        }
    }

    /**
     * Adds the given SQL statements into the generated migrate() function to be executed by the
     * database.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addDatabaseExecuteSqlStatement(
        migrateBuilder: MethodSpec.Builder,
        sql: String
    ) {
        migrateBuilder.addStatement(
            "database.execSQL($S)", sql
        )
    }
}

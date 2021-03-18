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
class AutoMigrationWriter(
    private val dbElement: XElement,
    val autoMigrationResult: AutoMigrationResult
) : ClassWriter(autoMigrationResult.implTypeName) {

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
     * @param migrateFunctionBuilder Builder for the migrate() function to be generated
     */
    private fun addAutoMigrationResultToMigrate(migrateFunctionBuilder: MethodSpec.Builder) {
        if (autoMigrationResult.addedTables.isNotEmpty()) {
            addNewTableStatements(migrateFunctionBuilder)
        }
        if (autoMigrationResult.addedColumns.isNotEmpty()) {
            addNewColumnStatements(migrateFunctionBuilder)
        }
    }

    /**
     * Adds the appropriate SQL statements for adding new columns to a table, into the
     * generated migrate() function.
     *
     * @param migrateFunctionBuilder Builder for the migrate() function to be generated
     */
    private fun addNewColumnStatements(migrateFunctionBuilder: MethodSpec.Builder) {
        autoMigrationResult.addedColumns.forEach {
            val addNewColumnSql = buildString {
                append(
                    "ALTER TABLE `${it.tableName}` ADD COLUMN `${it.fieldBundle.columnName}` " +
                        "${it.fieldBundle.affinity} "
                )
                if (it.fieldBundle.isNonNull) {
                    append("NOT NULL DEFAULT ${it.fieldBundle.defaultValue}")
                } else {
                    append("DEFAULT NULL")
                }
            }
            migrateFunctionBuilder.addStatement("database.execSQL($S)", addNewColumnSql)
        }
    }

    /**
     * Adds the appropriate SQL statements for adding new tables to a database, into the
     * generated migrate() function.
     *
     * @param migrateFunctionBuilder Builder for the migrate() function to be generated
     */
    private fun addNewTableStatements(migrateFunctionBuilder: MethodSpec.Builder) {
        autoMigrationResult.addedTables.forEach { addedTable ->
            migrateFunctionBuilder.addStatement(
                "database.execSQL($S)", addedTable.entityBundle.createTable()
            )
        }
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
}

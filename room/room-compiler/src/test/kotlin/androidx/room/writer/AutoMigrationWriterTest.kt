/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.PrimaryKeyBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.migration.bundle.TABLE_NAME_PLACEHOLDER
import androidx.room.processor.AutoMigrationProcessor
import androidx.room.testing.context
import androidx.room.util.SchemaDiffResult
import androidx.room.vo.AutoMigration
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoMigrationWriterTest {

    private val databaseSource = Source.java(
        "foo.bar.MyDatabase",
        """
        package foo.bar;
        import androidx.room.*;
        @Database(entities = {}, version = 1)
        public abstract class MyDatabase extends RoomDatabase {
        }
        """.trimIndent()
    )

    @Test
    fun validAutoMigrationWithDefaultValue() {
        val source = Source.java(
            "foo.bar.ValidAutoMigrationWithDefault",
            """
            package foo.bar;
            import androidx.room.migration.AutoMigrationSpec;
            import androidx.sqlite.db.SupportSQLiteDatabase;
            public class ValidAutoMigrationWithDefault implements AutoMigrationSpec {}
            """.trimIndent()
        )

        runProcessorTest(listOf(source, databaseSource)) { invocation ->
            val autoMigrationResultWithNewAddedColumn = AutoMigration(
                from = 1,
                to = 2,
                schemaDiff = SchemaDiffResult(
                    addedColumns = listOf(
                        AutoMigration.AddedColumn(
                            "Song",
                            FieldBundle(
                                "artistId",
                                "artistId",
                                "INTEGER",
                                false,
                                ""
                            )
                        )
                    ),
                    deletedColumns = listOf(),
                    addedTables = setOf(),
                    complexChangedTables = mapOf(),
                    renamedTables = mapOf(),
                    deletedTables = listOf(),
                    fromViews = emptyList(),
                    toViews = emptyList()
                ),
                specElement = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithDefault"
                ),
                isSpecProvided = false
            )
            AutoMigrationWriter(
                invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                autoMigrationResultWithNewAddedColumn
            ).write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithDefault.java",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun validAutoMigrationWithoutDefaultValue() {
        val source = Source.java(
            "foo.bar.ValidAutoMigrationWithoutDefault",
            """
            package foo.bar;
            import androidx.room.migration.AutoMigrationSpec;
            import androidx.sqlite.db.SupportSQLiteDatabase;
            public class ValidAutoMigrationWithoutDefault implements AutoMigrationSpec {}
            """.trimIndent()
        )

        runProcessorTest(listOf(source, databaseSource)) { invocation ->
            val autoMigrationResultWithNewAddedColumn = AutoMigration(
                from = 1,
                to = 2,
                schemaDiff = SchemaDiffResult(
                    addedColumns = listOf(
                        AutoMigration.AddedColumn(
                            "Song",
                            FieldBundle(
                                "artistId",
                                "artistId",
                                "INTEGER",
                                false,
                                ""
                            )
                        )
                    ),
                    deletedColumns = listOf(),
                    addedTables = setOf(),
                    complexChangedTables = mapOf(),
                    renamedTables = mapOf(),
                    deletedTables = listOf(),
                    fromViews = emptyList(),
                    toViews = emptyList()
                ),
                specElement = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithoutDefault"
                ),
                isSpecProvided = false
            )
            AutoMigrationWriter(
                invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                autoMigrationResultWithNewAddedColumn
            ).write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithoutDefault.java",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun testRenameTwoColumnsOnComplexChangedTable() {
        val source = Source.java(
            "foo.bar.MyAutoMigration",
            """
            package foo.bar;
            import androidx.room.*;
            import androidx.room.migration.AutoMigrationSpec;
            @RenameTable(fromTableName = "Song", toTableName = "SongTable")
            @RenameColumn(tableName = "Song", fromColumnName = "title", toColumnName = "songTitle")
            @RenameColumn(tableName = "Song", fromColumnName = "length", toColumnName = "songLength")
            class MyAutoMigration implements AutoMigrationSpec { }
            """.trimIndent()
        )

        runProcessorTest(listOf(source, databaseSource)) { invocation ->
            val autoMigrationResult = AutoMigrationProcessor(
                context = invocation.context,
                spec = invocation.processingEnv.requireType(
                    "foo.bar.MyAutoMigration"
                ),
                fromSchemaBundle = fromSchemaBundle.database,
                toSchemaBundle = toSchemaBundleRenamedTable2RenamedColumns.database
            ).process()

            AutoMigrationWriter(
                invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                autoMigrationResult!!
            ).write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidMultiColumnRename.java",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }

    /**
     * Schemas for processor testing.
     */
    private val fromSchemaBundle = SchemaBundle(
        1,
        DatabaseBundle(
            1,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `$TABLE_NAME_PLACEHOLDER` (`id` " +
                        "INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    private val toSchemaBundleRenamedTable2RenamedColumns = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "SongTable",
                    "CREATE TABLE IF NOT EXISTS `$TABLE_NAME_PLACEHOLDER` (`id` " +
                        "INTEGER NOT NULL, " +
                        "`songTitle` TEXT NOT NULL, `songLength` " +
                        "INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "songTitle",
                            "songTitle",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "songLength",
                            "songLength",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )
}

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
import androidx.room.migration.bundle.FieldBundle
import androidx.room.util.SchemaDiffResult
import androidx.room.vo.AutoMigration
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoMigrationWriterTest {

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

        runProcessorTest(listOf(source)) { invocation ->
            val autoMigrationResultWithNewAddedColumn = AutoMigration(
                element = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithDefault"
                ),
                from = 1,
                to = 2,
                schemaDiff = SchemaDiffResult(
                    addedColumns = mapOf(
                        Pair(
                            "artistId",
                            AutoMigration.AddedColumn(
                                "Song",
                                FieldBundle(
                                    "artistId",
                                    "artistId",
                                    "INTEGER",
                                    true,
                                    "0"
                                )
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
                autoMigrationResultWithNewAddedColumn.element,
                autoMigrationResultWithNewAddedColumn
            )
                .write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithDefault" +
                            ".java",
                        "foo.bar.ValidAutoMigrationWithDefault_AutoMigration_1_2_Impl"
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

        runProcessorTest(listOf(source)) { invocation ->
            val autoMigrationResultWithNewAddedColumn = AutoMigration(
                element = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithoutDefault"
                ),
                from = 1,
                to = 2,
                schemaDiff = SchemaDiffResult(
                    addedColumns = mapOf(
                        Pair(
                            "artistId",
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
                autoMigrationResultWithNewAddedColumn.element,
                autoMigrationResultWithNewAddedColumn
            )
                .write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithoutDefault.java",
                        "foo.bar.ValidAutoMigrationWithoutDefault_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }
}

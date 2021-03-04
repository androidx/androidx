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

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.PrimaryKeyBundle
import androidx.room.vo.AutoMigrationResult
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class AutoMigrationWriterTest {

    @Test
    fun validAutoMigrationWithDefaultValue() {
        val source = Source.java(
            "foo.bar.ValidAutoMigrationWithDefault",
            """
            package foo.bar;
            import androidx.room.migration.AutoMigrationCallback;
            import androidx.room.AutoMigration;
            import androidx.sqlite.db.SupportSQLiteDatabase;
            interface ValidAutoMigrationWithDefault extends AutoMigrationCallback {}
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { invocation ->
            val autoMigrationResultWithNewAddedColumn = AutoMigrationResult(
                element = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithDefault"
                ),
                from = 1,
                to = 2,
                addedColumns = listOf(
                    AutoMigrationResult.AddedColumn(
                        "Song",
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            "0"
                        )
                    )
                ),
                addedTables = listOf()
            )
            AutoMigrationWriter(mock(XElement::class.java), autoMigrationResultWithNewAddedColumn)
                .write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithDefault" +
                            ".java",
                        "foo.bar.ValidAutoMigrationWithDefault_Impl"
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
            import androidx.room.migration.AutoMigrationCallback;
            import androidx.room.AutoMigration;
            import androidx.sqlite.db.SupportSQLiteDatabase;
            @AutoMigration(from=1, to=2)
            interface ValidAutoMigrationWithoutDefault extends AutoMigrationCallback {}
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { invocation ->
            val autoMigrationResultWithNewAddedColumn = AutoMigrationResult(
                element = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithoutDefault"
                ),
                from = 1,
                to = 2,
                addedColumns = listOf(
                    AutoMigrationResult.AddedColumn(
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
                addedTables = listOf()
            )
            AutoMigrationWriter(mock(XElement::class.java), autoMigrationResultWithNewAddedColumn)
                .write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithoutDefault.java",
                        "foo.bar.ValidAutoMigrationWithoutDefault_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun validAutoMigrationWithNewTableAdded() {
        val source = Source.java(
            "foo.bar.ValidAutoMigrationWithoutDefault",
            """
            package foo.bar;
            import androidx.room.migration.AutoMigrationCallback;
            import androidx.room.AutoMigration;
            import androidx.sqlite.db.SupportSQLiteDatabase;
            @AutoMigration(from=1, to=2)
            interface ValidAutoMigrationWithNewTableAdded extends AutoMigrationCallback {
                @Override
                void onPostMigrate(SupportSQLiteDatabase db);
            }
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { invocation ->
            val autoMigrationResultWithNewTableAdded = AutoMigrationResult(
                element = invocation.processingEnv.requireTypeElement(
                    "foo.bar.ValidAutoMigrationWithNewTableAdded"
                ),
                from = 1,
                to = 2,
                addedColumns = listOf(
                    AutoMigrationResult.AddedColumn(
                        "Song",
                        FieldBundle(
                            "songId",
                            "songId",
                            "INTEGER",
                            false,
                            ""
                        )
                    )
                ),
                addedTables = listOf(
                    AutoMigrationResult.AddedTable(
                        EntityBundle(
                            "Artist",
                            "CREATE TABLE IF NOT EXISTS `Artist` (`artistId` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, PRIMARY KEY(`artistId`))",
                            listOf(
                                FieldBundle(
                                    "artistId",
                                    "artistId",
                                    "INTEGER",
                                    true,
                                    "1"
                                )
                            ),
                            PrimaryKeyBundle(true, listOf("artistId")),
                            listOf(),
                            listOf()
                        ),
                    ),
                    AutoMigrationResult.AddedTable(
                        EntityBundle(
                            "Album",
                            "CREATE TABLE IF NOT EXISTS `Album` (`albumId` INTEGER NOT NULL, " +
                                "PRIMARY KEY(`albumId`))",
                            listOf(
                                FieldBundle(
                                    "albumId",
                                    "albumId",
                                    "INTEGER",
                                    true,
                                    "1"
                                )
                            ),
                            PrimaryKeyBundle(true, listOf("albumId")),
                            listOf(),
                            listOf()
                        )
                    )
                )
            )
            AutoMigrationWriter(mock(XElement::class.java), autoMigrationResultWithNewTableAdded)
                .write(invocation.processingEnv)

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/ValidAutoMigrationWithNewTableAdded.java",
                        "foo.bar.ValidAutoMigrationWithNewTableAdded_Impl"
                    )
                )
            }
        }
    }
}

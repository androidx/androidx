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

package androidx.room3.writer

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.migration.bundle.FieldBundle
import androidx.room3.util.SchemaDiffResult
import androidx.room3.vo.AutoMigration
import loadTestSource
import org.junit.Test
import org.junit.runners.Parameterized

class AutoMigrationWriterTest() {

    private val kotlinDatabaseSource =
        Source.kotlin(
            "MyDatabase.kt",
            """
            package foo.bar
            import androidx.room3.*
            @Database(entities = [], version = 1)
            abstract class MyDatabase : RoomDatabase() {
            }
            """
                .trimIndent(),
        )

    @Test
    fun validAutoMigrationWithDefaultValue() {
        val specSource =
            Source.kotlin(
                "ValidAutoMigrationWithDefault.kt",
                """
                package foo.bar
                import androidx.room3.migration.AutoMigrationSpec
                import androidx.sqlite.db.SupportSQLiteDatabase
                class ValidAutoMigrationWithDefault : AutoMigrationSpec {}
                """
                    .trimIndent(),
            )

        runKspTest(sources = listOf(specSource)) { invocation ->
            val autoMigrationResultWithNewAddedColumn =
                AutoMigration(
                    from = 1,
                    to = 2,
                    schemaDiff =
                        SchemaDiffResult(
                            addedColumns =
                                listOf(
                                    AutoMigration.AddedColumn(
                                        "Song",
                                        FieldBundle("artistId", "artistId", "INTEGER", true, "0"),
                                    )
                                ),
                            deletedColumns = listOf(),
                            addedTables = setOf(),
                            complexChangedTables = mapOf(),
                            renamedTables = mapOf(),
                            deletedTables = listOf(),
                            fromViews = emptyList(),
                            toViews = emptyList(),
                        ),
                    specElement =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.ValidAutoMigrationWithDefault"
                        ),
                    isSpecProvided = false,
                )
            AutoMigrationWriter(
                    autoMigration = autoMigrationResultWithNewAddedColumn,
                    dbElement = invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                    writerContext =
                        TypeWriter.WriterContext(
                            codeLanguage = CodeLanguage.KOTLIN,
                            javaLambdaSyntaxAvailable = false,
                            targetPlatforms = setOf(XProcessingEnv.Platform.JVM),
                        ),
                )
                .write(invocation.processingEnv)

            val expectedFile = "kotlin/ValidAutoMigrationWithDefault.kt"
            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/$expectedFile",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl",
                    )
                )
            }
        }
    }

    @Test
    fun validAutoMigrationWithoutDefaultValue() {
        val specSource =
            Source.kotlin(
                "ValidAutoMigrationWithoutDefault.kt",
                """
                package foo.bar
                import androidx.room3.migration.AutoMigrationSpec
                import androidx.sqlite.db.SupportSQLiteDatabase
                class ValidAutoMigrationWithoutDefault : AutoMigrationSpec {}
                """
                    .trimIndent(),
            )

        runKspTest(listOf(specSource)) { invocation ->
            val autoMigrationResultWithNewAddedColumn =
                AutoMigration(
                    from = 1,
                    to = 2,
                    schemaDiff =
                        SchemaDiffResult(
                            addedColumns =
                                listOf(
                                    AutoMigration.AddedColumn(
                                        "Song",
                                        FieldBundle("artistId", "artistId", "INTEGER", false, ""),
                                    )
                                ),
                            deletedColumns = listOf(),
                            addedTables = setOf(),
                            complexChangedTables = mapOf(),
                            renamedTables = mapOf(),
                            deletedTables = listOf(),
                            fromViews = emptyList(),
                            toViews = emptyList(),
                        ),
                    specElement =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.ValidAutoMigrationWithoutDefault"
                        ),
                    isSpecProvided = false,
                )
            AutoMigrationWriter(
                    autoMigration = autoMigrationResultWithNewAddedColumn,
                    dbElement = invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                    writerContext =
                        TypeWriter.WriterContext(
                            codeLanguage = CodeLanguage.KOTLIN,
                            javaLambdaSyntaxAvailable = false,
                            targetPlatforms = setOf(XProcessingEnv.Platform.JVM),
                        ),
                )
                .write(invocation.processingEnv)

            val expectedFile = "kotlin/ValidAutoMigrationWithoutDefault.kt"

            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/$expectedFile",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl",
                    )
                )
            }
        }
    }

    @Test
    fun validAutoMigrationWithProvidedSpec() {
        val specSource =
            Source.kotlin(
                "AutoMigrationWithProvidedSpec.kt",
                """
                package foo.bar
                import androidx.room3.ProvidedAutoMigrationSpec
                import androidx.room3.migration.AutoMigrationSpec
                import androidx.sqlite.db.SupportSQLiteDatabase

                @ProvidedAutoMigrationSpec
                class AutoMigrationWithProvidedSpec(val data: String) : AutoMigrationSpec {}
                """
                    .trimIndent(),
            )

        runKspTest(listOf(specSource)) { invocation ->
            val autoMigrationResultWithNewAddedColumn =
                AutoMigration(
                    from = 1,
                    to = 2,
                    schemaDiff =
                        SchemaDiffResult(
                            addedColumns =
                                listOf(
                                    AutoMigration.AddedColumn(
                                        "Song",
                                        FieldBundle("artistId", "artistId", "INTEGER", false, ""),
                                    )
                                ),
                            deletedColumns = listOf(),
                            addedTables = setOf(),
                            complexChangedTables = mapOf(),
                            renamedTables = mapOf(),
                            deletedTables = listOf(),
                            fromViews = emptyList(),
                            toViews = emptyList(),
                        ),
                    specElement =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.AutoMigrationWithProvidedSpec"
                        ),
                    isSpecProvided = true,
                )
            AutoMigrationWriter(
                    autoMigration = autoMigrationResultWithNewAddedColumn,
                    dbElement = invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                    writerContext =
                        TypeWriter.WriterContext(
                            codeLanguage = CodeLanguage.KOTLIN,
                            javaLambdaSyntaxAvailable = false,
                            targetPlatforms = setOf(XProcessingEnv.Platform.JVM),
                        ),
                )
                .write(invocation.processingEnv)

            val expectedFile = "kotlin/AutoMigrationWithProvidedSpec.kt"
            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/$expectedFile",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl",
                    )
                )
            }
        }
    }

    private fun runKspTest(sources: List<Source>, handler: (XTestInvocation) -> Unit) {
        runKspTest(
            sources = sources + kotlinDatabaseSource,
            kotlincArguments = listOf("-jvm-target=11"),
            handler = handler,
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "genLanguage={0}")
        fun params() = arrayOf(CodeLanguage.JAVA, CodeLanguage.KOTLIN)
    }
}

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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runJavaProcessorTest
import androidx.room.migration.bundle.FieldBundle
import androidx.room.processor.Context
import androidx.room.runKspTestWithK1
import androidx.room.util.SchemaDiffResult
import androidx.room.vo.AutoMigration
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AutoMigrationWriterTest(private val codeLanguage: CodeLanguage) {

    private val javaDatabaseSource =
        Source.java(
            "foo.bar.MyDatabase",
            """
        package foo.bar;
        import androidx.room.*;
        @Database(entities = {}, version = 1)
        public abstract class MyDatabase extends RoomDatabase {
        }
        """
                .trimIndent()
        )

    private val kotlinDatabaseSource =
        Source.kotlin(
            "MyDatabase.kt",
            """
        package foo.bar
        import androidx.room.*
        @Database(entities = [], version = 1)
        abstract class MyDatabase : RoomDatabase() {
        }
        """
                .trimIndent()
        )

    @Test
    fun validAutoMigrationWithDefaultValue() {
        val specSource =
            when (codeLanguage) {
                CodeLanguage.JAVA ->
                    Source.java(
                        "foo.bar.ValidAutoMigrationWithDefault",
                        """
                package foo.bar;
                import androidx.room.migration.AutoMigrationSpec;
                import androidx.sqlite.db.SupportSQLiteDatabase;
                public class ValidAutoMigrationWithDefault implements AutoMigrationSpec {}
                """
                            .trimIndent()
                    )
                CodeLanguage.KOTLIN ->
                    Source.kotlin(
                        "ValidAutoMigrationWithDefault.kt",
                        """
                package foo.bar
                import androidx.room.migration.AutoMigrationSpec
                import androidx.sqlite.db.SupportSQLiteDatabase
                class ValidAutoMigrationWithDefault : AutoMigrationSpec {}
                """
                            .trimIndent()
                    )
            }

        runProcessorTestWithK1(listOf(specSource)) { invocation ->
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
                                        FieldBundle("artistId", "artistId", "INTEGER", true, "0")
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
                    specElement =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.ValidAutoMigrationWithDefault"
                        ),
                    isSpecProvided = false
                )
            AutoMigrationWriter(
                    autoMigration = autoMigrationResultWithNewAddedColumn,
                    dbElement = invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                    writerContext =
                        TypeWriter.WriterContext(
                            codeLanguage = codeLanguage,
                            javaLambdaSyntaxAvailable = false,
                            targetPlatforms = setOf(XProcessingEnv.Platform.JVM)
                        )
                )
                .write(invocation.processingEnv)

            val expectedFile =
                when (codeLanguage) {
                    CodeLanguage.JAVA -> "java/ValidAutoMigrationWithDefault.java"
                    CodeLanguage.KOTLIN -> "kotlin/ValidAutoMigrationWithDefault.kt"
                }
            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/$expectedFile",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun validAutoMigrationWithoutDefaultValue() {
        val specSource =
            when (codeLanguage) {
                CodeLanguage.JAVA ->
                    Source.java(
                        "foo.bar.ValidAutoMigrationWithoutDefault",
                        """
                package foo.bar;
                import androidx.room.migration.AutoMigrationSpec;
                import androidx.sqlite.db.SupportSQLiteDatabase;
                public class ValidAutoMigrationWithoutDefault implements AutoMigrationSpec {}
                """
                            .trimIndent()
                    )
                CodeLanguage.KOTLIN ->
                    Source.kotlin(
                        "ValidAutoMigrationWithoutDefault.kt",
                        """
                package foo.bar
                import androidx.room.migration.AutoMigrationSpec
                import androidx.sqlite.db.SupportSQLiteDatabase
                class ValidAutoMigrationWithoutDefault : AutoMigrationSpec {}
                """
                            .trimIndent()
                    )
            }

        runProcessorTestWithK1(listOf(specSource)) { invocation ->
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
                                        FieldBundle("artistId", "artistId", "INTEGER", false, "")
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
                    specElement =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.ValidAutoMigrationWithoutDefault"
                        ),
                    isSpecProvided = false
                )
            AutoMigrationWriter(
                    autoMigration = autoMigrationResultWithNewAddedColumn,
                    dbElement = invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                    writerContext =
                        TypeWriter.WriterContext(
                            codeLanguage = codeLanguage,
                            javaLambdaSyntaxAvailable = false,
                            targetPlatforms = setOf(XProcessingEnv.Platform.JVM)
                        )
                )
                .write(invocation.processingEnv)

            val expectedFile =
                when (codeLanguage) {
                    CodeLanguage.JAVA -> "java/ValidAutoMigrationWithoutDefault.java"
                    CodeLanguage.KOTLIN -> "kotlin/ValidAutoMigrationWithoutDefault.kt"
                }
            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/$expectedFile",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun validAutoMigrationWithProvidedSpec() {
        val specSource =
            when (codeLanguage) {
                CodeLanguage.JAVA ->
                    Source.java(
                        "foo.bar.AutoMigrationWithProvidedSpec",
                        """
                package foo.bar;
                import androidx.room.ProvidedAutoMigrationSpec;
                import androidx.room.migration.AutoMigrationSpec;
                import androidx.sqlite.db.SupportSQLiteDatabase;
                
                @ProvidedAutoMigrationSpec
                public class AutoMigrationWithProvidedSpec implements AutoMigrationSpec {
                    public AutoMigrationWithProvidedSpec(String data) {}
                }
                """
                            .trimIndent()
                    )
                CodeLanguage.KOTLIN ->
                    Source.kotlin(
                        "AutoMigrationWithProvidedSpec.kt",
                        """
                package foo.bar
                import androidx.room.ProvidedAutoMigrationSpec
                import androidx.room.migration.AutoMigrationSpec
                import androidx.sqlite.db.SupportSQLiteDatabase
                
                @ProvidedAutoMigrationSpec
                class AutoMigrationWithProvidedSpec(val data: String) : AutoMigrationSpec {}
                """
                            .trimIndent()
                    )
            }

        runProcessorTestWithK1(listOf(specSource)) { invocation ->
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
                                        FieldBundle("artistId", "artistId", "INTEGER", false, "")
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
                    specElement =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.AutoMigrationWithProvidedSpec"
                        ),
                    isSpecProvided = true
                )
            AutoMigrationWriter(
                    autoMigration = autoMigrationResultWithNewAddedColumn,
                    dbElement = invocation.processingEnv.requireTypeElement("foo.bar.MyDatabase"),
                    writerContext =
                        TypeWriter.WriterContext(
                            codeLanguage = codeLanguage,
                            javaLambdaSyntaxAvailable = false,
                            targetPlatforms = setOf(XProcessingEnv.Platform.JVM)
                        )
                )
                .write(invocation.processingEnv)

            val expectedFile =
                when (codeLanguage) {
                    CodeLanguage.JAVA -> "java/AutoMigrationWithProvidedSpec.java"
                    CodeLanguage.KOTLIN -> "kotlin/AutoMigrationWithProvidedSpec.kt"
                }
            invocation.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        "autoMigrationWriter/output/$expectedFile",
                        "foo.bar.MyDatabase_AutoMigration_1_2_Impl"
                    )
                )
            }
        }
    }

    private fun runProcessorTestWithK1(sources: List<Source>, handler: (XTestInvocation) -> Unit) {
        when (codeLanguage) {
            CodeLanguage.JAVA ->
                runJavaProcessorTest(
                    sources = sources + javaDatabaseSource,
                    options =
                        mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false"),
                    handler = handler
                )
            CodeLanguage.KOTLIN ->
                runKspTestWithK1(
                    sources = sources + kotlinDatabaseSource,
                    options =
                        mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
                    handler = handler
                )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "genLanguage={0}")
        fun params() = arrayOf(CodeLanguage.JAVA, CodeLanguage.KOTLIN)
    }
}

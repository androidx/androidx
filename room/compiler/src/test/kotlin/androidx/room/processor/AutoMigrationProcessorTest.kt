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

package androidx.room.processor

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.PrimaryKeyBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.testing.context
import org.junit.Test

class AutoMigrationProcessorTest {
    @Test
    fun testElementIsInterface() {
        val source = Source.java(
            "foo.bar.MyAutoMigration",
            """
            package foo.bar;
            import androidx.room.AutoMigration;
            @AutoMigration(from=1, to=2)
            class MyAutoMigration implements AutoMigration {}
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { invocation ->
            AutoMigrationProcessor(
                invocation.context,
                invocation.processingEnv.requireTypeElement("foo.bar.MyAutoMigration"),
                from.database
            ).process()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.AUTOMIGRATION_ANNOTATED_TYPE_ELEMENT_MUST_BE_INTERFACE)
            }
        }
    }

    @Test
    fun testInterfaceExtendsAutoMigrationInterface() {
        val source = Source.java(
            "foo.bar.MyAutoMigration",
            """
            package foo.bar;
            import androidx.room.migration.AutoMigrationCallback;
            import androidx.room.AutoMigration;
            import androidx.sqlite.db.SupportSQLiteDatabase;
            interface MyAutoMigration {}
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { invocation ->
            AutoMigrationProcessor(
                invocation.context,
                invocation.processingEnv.requireTypeElement("foo.bar.MyAutoMigration"),
                from.database
            ).process()
            invocation.assertCompilationResult {
                hasError(
                    ProcessorErrors.AUTOMIGRATION_ELEMENT_MUST_IMPLEMENT_AUTOMIGRATION_CALLBACK
                )
            }
        }
    }

    /**
     * Schemas for processor testing.
     */
    val from = SchemaBundle(
        1,
        DatabaseBundle(
            1,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
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
}

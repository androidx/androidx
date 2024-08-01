/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.runKspTestWithK1
import androidx.room.testing.context
import org.junit.Test

class DatabaseConstructorProcessorTest {

    private val databaseSource =
        Source.kotlin(
            "Database.kt",
            """
        package test

        import androidx.room.*

        @Database(entities = [TestEntity::class], version = 1, exportSchemas = false)
        @ConstructedBy(TestDatabaseCtor::class)
        abstract class TestDatabase : RoomDatabase

        @Entity
        data class TestEntity(@PrimaryKey val id: Long)
        """
                .trimIndent()
        )

    @Test
    fun notObjectError() {
        runTest(
            Source.kotlin(
                "Constructor.kt",
                """
                package test

                import androidx.room.*

                expect class TestDatabaseCtor : RoomDatabaseConstructor<TestDatabase>
                """
                    .trimIndent()
            )
        ) {
            it.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_CONSTRUCTED_BY_NOT_OBJECT)
            }
        }
    }

    @Test
    fun notExpectError() {
        runTest(
            Source.kotlin(
                "Constructor.kt",
                """
                package test

                import androidx.room.*

                object TestDatabaseCtor : RoomDatabaseConstructor<TestDatabase>
                """
                    .trimIndent()
            )
        ) {
            it.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_CONSTRUCTED_BY_NOT_EXPECT)
            }
        }
    }

    @Test
    fun missingSuperInterfaceError() {
        runTest(
            Source.kotlin(
                "Constructor.kt",
                """
                package test

                import androidx.room.*

                expect object TestDatabaseCtor
                """
                    .trimIndent()
            )
        ) {
            it.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidConstructedBySuperInterface(
                        "androidx.room.RoomDatabaseConstructor<test.TestDatabase>"
                    )
                )
            }
        }
    }

    @Test
    fun incorrectSuperInterfaceTypeArgError() {
        runTest(
            Source.kotlin(
                "Constructor.kt",
                """
                package test

                import androidx.room.*

                expect object TestDatabaseCtor : RoomDatabaseConstructor<RoomDatabase>
                """
                    .trimIndent()
            )
        ) {
            it.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidConstructedBySuperInterface(
                        "androidx.room.RoomDatabaseConstructor<test.TestDatabase>"
                    )
                )
            }
        }
    }

    private fun runTest(constructorSource: Source, handler: (XTestInvocation) -> Unit = { _ -> }) {
        runKspTestWithK1(sources = listOf(databaseSource, constructorSource)) { invocation ->
            val entity =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Database::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .first()
            DatabaseProcessor(invocation.context, entity).process()
            handler(invocation)
        }
    }
}

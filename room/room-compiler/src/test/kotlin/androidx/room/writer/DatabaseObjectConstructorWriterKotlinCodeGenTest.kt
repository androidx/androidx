/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room.DatabaseProcessingStep
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.processor.Context
import androidx.room.runKspTestWithK1
import loadTestSource
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class DatabaseObjectConstructorWriterKotlinCodeGenTest {

    @get:Rule val testName = TestName()

    private val databaseSrc =
        Source.kotlin(
            "MyDatabase.kt",
            """
            import androidx.room.*

            @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
            @ConstructedBy(MyDatabaseCtor::class)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )
            """
                .trimIndent()
        )

    @Test
    fun actualDatabaseConstructor() {
        val ctorSrc =
            Source.kotlin(
                "MyDatabaseCtor.kt",
                """
            import androidx.room.*

            expect object MyDatabaseCtor : RoomDatabaseConstructor<MyDatabase>
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(databaseSrc, ctorSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun actualDatabaseConstructor_internal() {
        val ctorSrc =
            Source.kotlin(
                "MyDatabaseCtor.kt",
                """
            import androidx.room.*

            internal expect object MyDatabaseCtor : RoomDatabaseConstructor<MyDatabase>
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(databaseSrc, ctorSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    private fun getTestGoldenPath(testName: String): String {
        return "kotlinCodeGen/$testName.kt"
    }

    private fun runTest(
        sources: List<Source>,
        expectedFilePath: String,
        handler: (XTestInvocation) -> Unit = {}
    ) {
        runKspTestWithK1(
            sources = sources,
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) {
            val databaseFqn = "androidx.room.Database"
            DatabaseProcessingStep()
                .process(
                    it.processingEnv,
                    mapOf(databaseFqn to it.roundEnv.getElementsAnnotatedWith(databaseFqn)),
                    it.roundEnv.isProcessingOver
                )
            it.assertCompilationResult {
                this.generatedSource(loadTestSource(expectedFilePath, "MyDatabaseCtor"))
                // runKspTest only does JVM compilation (b) so it is expected to get an error due to
                // usage of expect / actual in a non KMP compilation, however the test is still
                // useful to validate generated code
                this.hasErrorContaining(
                    "'expect' and 'actual' declarations can be used only in multiplatform projects."
                )
            }
            handler.invoke(it)
        }
    }
}
